package com.seibel.cpss.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Throttles repeated failed logins and password-reset requests from one client.
 *
 * <p><b>Why this exists.</b> {@code /api/auth/login} has to stay public — a browser cannot
 * present a JWT before it has one — so it is the single unauthenticated door into an otherwise
 * secured app. Unthrottled, an attacker guesses passwords as fast as the network allows and a
 * weak password falls in seconds. Every other access control is worth only as much as this door.
 *
 * <p><b>Registered before authentication</b> in {@code SecurityConfig}, so a locked-out caller
 * is rejected before any bcrypt hashing happens. BCrypt is deliberately expensive; that cost
 * becomes an attacker's lever if guesses are allowed to reach it.
 *
 * <p><b>Only consecutive failures count.</b> A successful login clears the counter, so someone
 * who mistypes a password twice and then gets it right is never locked out. What is limited is
 * guessing, not using.
 *
 * <p><b>Keyed on client IP AND username, never IP alone.</b> Per-IP alone is a denial of
 * service: failed guesses against any username would lock out every other user from the same
 * address, and behind a proxy or CGNAT many people share one address. The IP half still matters
 * — without it, one attacker could lock a known username from anywhere.
 *
 * <p><b>In-memory and per-instance</b>, which is honest about what it is. It resets on restart
 * and does not coordinate across instances. That is fine for this single-instance deployment; if
 * it ever scales out, move the counter to Redis. It stops online guessing from one source, not a
 * distributed attempt from many addresses — a strong password remains the real control.
 */
@Slf4j
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    /** Consecutive failures for one IP+username pair before it is locked out. */
    @Value("${security.login.max-attempts:8}")
    private int maxAttempts;

    /** How long a lockout lasts. */
    @Value("${security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    /**
     * Failure counters by IP+username.
     *
     * <p>Bounded by {@link #MAX_TRACKED_ADDRESSES} so a spray from many forged addresses cannot
     * grow this without limit and exhaust heap — the memory-pressure version of the attack this
     * filter exists to stop.
     */
    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    private static final int MAX_TRACKED_ADDRESSES = 10_000;

    private static final class Attempts {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant lockedUntil = Instant.EPOCH;
    }

    /**
     * Applies to the three public auth endpoints that an attacker can abuse.
     *
     * <p>{@code /login} is password guessing. {@code /forgot-password} and {@code /forgot-username}
     * each send an email to an address the caller supplies, so unthrottled they are an
     * email-bombing vector against a third party. {@code /register} is deliberately excluded —
     * a duplicate username returns 409, not 401, so it would never trip this counter anyway.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean isThrottledAuthPost = "POST".equalsIgnoreCase(request.getMethod())
                && ("/api/auth/login".equals(path)
                || "/api/auth/forgot-password".equals(path)
                || "/api/auth/forgot-username".equals(path));
        return !isThrottledAuthPost;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // The body has to be read here for the username and read again by the controller, and a
        // servlet input stream is single-pass - so it is buffered and replayed downstream.
        CachedBodyRequest cached = new CachedBodyRequest(request);
        String path = request.getServletPath();
        String key = clientIp(request) + "|" + subjectFrom(cached.body());
        Attempts attempts = attemptsByKey.get(key);

        if (attempts != null && Instant.now().isBefore(attempts.lockedUntil)) {
            long retryAfter = Duration.between(Instant.now(), attempts.lockedUntil).toSeconds();
            log.warn("login rate limit: {} is locked out for a further {}s", key, retryAfter);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many sign-in attempts. Please wait and try again.\"}");
            return;
        }

        chain.doFilter(cached, response);

        boolean isLogin = "/api/auth/login".equals(path);
        if (isLogin) {
            // The controller returns 401 for bad credentials; anything below 400 is a real login.
            if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
                recordFailure(key);
            } else if (response.getStatus() < 400) {
                attemptsByKey.remove(key);
            }
        } else if (response.getStatus() < 400) {
            // The forgot-* endpoints deliberately return 200 whether or not the account exists,
            // so there is no failure signal to count - every successful call is counted instead.
            // Without this they would never be throttled at all, since they never return 401.
            recordFailure(key);
        }
    }

    private void recordFailure(String key) {
        if (attemptsByKey.size() >= MAX_TRACKED_ADDRESSES && !attemptsByKey.containsKey(key)) {
            // Full. Dropping the new entry is deliberate: the alternative is unbounded growth,
            // and the addresses already tracked are the ones actively guessing.
            log.warn("login rate limit: tracking table full, not tracking {}", key);
            return;
        }
        Attempts attempts = attemptsByKey.computeIfAbsent(key, k -> new Attempts());
        int failures = attempts.count.incrementAndGet();
        if (failures >= maxAttempts) {
            attempts.lockedUntil = Instant.now().plus(Duration.ofMinutes(lockoutMinutes));
            attempts.count.set(0);
            log.warn("login rate limit: {} locked out for {} minutes after {} failed attempts",
                    key, lockoutMinutes, failures);
        }
    }

    /**
     * The caller's address, preferring the first hop in {@code X-Forwarded-For}.
     *
     * <p>That header is client-controlled and trivially forged, so this is only trustworthy
     * behind a proxy that overwrites it. Read directly on a public port it lets an attacker
     * rotate the key and evade the limit; that is a reason to terminate at a proxy, not a reason
     * to ignore the header behind one.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * The username or email from an auth request body, lower-cased, or {@code "?"} when it
     * cannot be read.
     *
     * <p>Deliberately a small regex rather than a JSON parse: this runs before authentication on
     * a public endpoint, so it must not throw on malformed or hostile input. An unreadable body
     * collapses to one shared bucket, which still throttles — it does not fail open.
     *
     * <p>Truncated because the value is attacker-controlled and becomes a map key; without a cap
     * a caller could send megabyte usernames and blow out the tracking table.
     */
    private String subjectFrom(String body) {
        if (body == null || body.isBlank()) {
            return "?";
        }
        Matcher m = SUBJECT_IN_JSON.matcher(body);
        if (!m.find()) {
            return "?";
        }
        String value = m.group(2).trim().toLowerCase();
        return value.length() > 64 ? value.substring(0, 64) : value;
    }

    /**
     * Matches the {@code username} of a login body or the {@code email} of a forgot-* body, so
     * one filter can key both without knowing which endpoint it is on.
     */
    private static final Pattern SUBJECT_IN_JSON =
            Pattern.compile("\"(username|email)\"\\s*:\\s*\"([^\"]{0,256})\"");

    /**
     * Replays a buffered request body downstream.
     *
     * <p>A servlet input stream can only be read once. Reading it here to find the username
     * would otherwise leave the controller with an empty body and break every login.
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        String body() {
            return new String(body, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream buffer = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return buffer.read();
                }

                @Override
                public boolean isFinished() {
                    return buffer.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    // Blocking reads only; the container never drives this asynchronously here.
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
