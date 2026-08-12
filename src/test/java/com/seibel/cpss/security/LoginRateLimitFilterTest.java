package com.seibel.cpss.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link LoginRateLimitFilter}.
 *
 * <p>The most important case here is {@link #lockoutIsScopedToUsername_notIpAlone()}. Keying the
 * counter on IP alone turns this security control into a denial of service: failed guesses at any
 * username would lock out every other user behind the same address, which behind a proxy or CGNAT
 * is potentially everyone. That test is the guard against reintroducing it.
 */
class LoginRateLimitFilterTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String FORGOT_PASSWORD = "/api/auth/forgot-password";
    private static final int MAX_ATTEMPTS = 8;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
        ReflectionTestUtils.setField(filter, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(filter, "lockoutMinutes", 15L);
    }

    private MockHttpServletRequest request(String path, String ip, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(ip);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private String loginBody(String username) {
        return "{\"username\":\"" + username + "\",\"password\":\"wrong\"}";
    }

    /** Runs one request through the filter, with the downstream chain returning {@code status}. */
    private MockHttpServletResponse attempt(String path, String ip, String body, int status)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(status);
        filter.doFilter(request(path, ip, body), response, chain);
        return response;
    }

    private MockHttpServletResponse failedLogin(String ip, String username) throws Exception {
        return attempt(LOGIN, ip, loginBody(username), 401);
    }

    @Test
    void allowsAttemptsBelowTheLimit() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            MockHttpServletResponse response = failedLogin("10.0.0.1", "someone");
            assertEquals(401, response.getStatus(), "attempt " + (i + 1) + " should reach the controller");
        }
    }

    @Test
    void locksOutAfterMaxConsecutiveFailures() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            failedLogin("10.0.0.1", "someone");
        }

        MockHttpServletResponse locked = failedLogin("10.0.0.1", "someone");

        assertEquals(429, locked.getStatus());
        assertNotEquals(null, locked.getHeader("Retry-After"));
        assertTrue(Integer.parseInt(locked.getHeader("Retry-After")) > 0);
    }

    @Test
    void lockoutRefusesEvenTheCorrectPassword() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            failedLogin("10.0.0.1", "someone");
        }

        // A correct password would return 200 from the chain, but the filter must reject before
        // reaching it - otherwise the lockout is not actually stopping anything.
        MockHttpServletResponse response = attempt(LOGIN, "10.0.0.1", loginBody("someone"), 200);

        assertEquals(429, response.getStatus());
    }

    @Test
    void successfulLoginClearsTheCounter() throws Exception {
        // Someone mistypes a few times, then gets it right, then mistypes again. They must never
        // be locked out - this limits guessing, not using.
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            failedLogin("10.0.0.1", "someone");
        }
        attempt(LOGIN, "10.0.0.1", loginBody("someone"), 200);

        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            MockHttpServletResponse response = failedLogin("10.0.0.1", "someone");
            assertEquals(401, response.getStatus(), "counter should have reset after the success");
        }
    }

    @Test
    void lockoutIsScopedToUsername_notIpAlone() throws Exception {
        // THE IMPORTANT ONE. Lock out a nonexistent username from this address...
        for (int i = 0; i < MAX_ATTEMPTS + 1; i++) {
            failedLogin("10.0.0.1", "nosuchuser");
        }

        // ...a real user from the SAME address must still get through. If this returns 429 the
        // key is IP-only and the filter has become a denial of service against legitimate users.
        MockHttpServletResponse response = attempt(LOGIN, "10.0.0.1", loginBody("realuser"), 200);

        assertEquals(200, response.getStatus());
    }

    @Test
    void lockoutIsScopedToIp_soOneUsernameIsNotLockedEverywhere() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS + 1; i++) {
            failedLogin("10.0.0.1", "someone");
        }

        // Same username, different address: the IP half of the key means one attacker cannot
        // lock a known username out from every location.
        MockHttpServletResponse response = attempt(LOGIN, "10.0.0.2", loginBody("someone"), 200);

        assertEquals(200, response.getStatus());
    }

    @Test
    void honoursXForwardedForSoProxiedUsersAreNotOneBucket() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS + 1; i++) {
            MockHttpServletRequest request = request(LOGIN, "10.0.0.1", loginBody("someone"));
            request.addHeader("X-Forwarded-For", "203.0.113.9");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(401));
        }

        // Behind a proxy every request shares one socket address. A different forwarded client
        // must be tracked separately or the limiter throttles all users as one.
        MockHttpServletRequest other = request(LOGIN, "10.0.0.1", loginBody("someone"));
        other.addHeader("X-Forwarded-For", "203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(other, response, (req, res) -> ((HttpServletResponse) res).setStatus(200));

        assertEquals(200, response.getStatus());
    }

    @Test
    void replaysRequestBodyDownstream() throws Exception {
        // The filter reads the body to find the username, and a servlet input stream is
        // single-pass - if it is not replayed the controller sees an empty body and every login
        // breaks with a validation error.
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] seen = new String[1];

        filter.doFilter(request(LOGIN, "10.0.0.1", loginBody("someone")), response,
                (req, res) -> seen[0] = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

        assertEquals(loginBody("someone"), seen[0]);
    }

    @Test
    void throttlesForgotPasswordByEmail() throws Exception {
        String body = "{\"email\":\"victim@example.com\"}";

        // forgot-password returns 200 whether or not the account exists, so successful calls are
        // what gets counted - otherwise it would never throttle and stays an email-bomb vector.
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            attempt(FORGOT_PASSWORD, "10.0.0.1", body, 200);
        }

        MockHttpServletResponse locked = attempt(FORGOT_PASSWORD, "10.0.0.1", body, 200);

        assertEquals(429, locked.getStatus());
    }

    @Test
    void doesNotFilterUnrelatedEndpoints() throws Exception {
        MockHttpServletRequest request = request("/api/food", "10.0.0.1", "{}");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void doesNotFilterRegister() throws Exception {
        // Register returns 409 for a duplicate username, never 401, so it would never trip the
        // counter - it is excluded rather than tracked pointlessly.
        MockHttpServletRequest request = request("/api/auth/register", "10.0.0.1", "{}");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void malformedBodyDoesNotThrowAndStillThrottles() throws Exception {
        // Runs before authentication on a public endpoint, so hostile input must not blow up.
        // An unreadable body collapses to one shared bucket, which still throttles.
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            attempt(LOGIN, "10.0.0.1", "not json at all", 401);
        }

        MockHttpServletResponse locked = attempt(LOGIN, "10.0.0.1", "not json at all", 401);

        assertEquals(429, locked.getStatus());
    }
}
