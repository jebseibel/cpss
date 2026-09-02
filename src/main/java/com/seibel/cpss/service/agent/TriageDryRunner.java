package com.seibel.cpss.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs triage over the whole catalog and prints the result. Writes nothing.
 *
 * <p>This is the go/no-go gate for the food-warning agent. The design bets that
 * roughly 80% of the catalog is unremarkable and should be abstained on. If this
 * run flags 100+ of 166 foods, the rubric is wrong and the research loop
 * underneath it is not worth running yet.
 *
 * <p>Behind a profile rather than an endpoint so it cannot be triggered
 * accidentally and needs no route or auth:
 *
 * <pre>
 *   ./gradlew bootRun --args='--spring.profiles.active=triage-dry-run'
 * </pre>
 *
 * <p>Requires ANTHROPIC_API_KEY. Costs one cheap model call per food.
 */
@Slf4j
@Component
@Profile("triage-dry-run")
@RequiredArgsConstructor
public class TriageDryRunner implements ApplicationRunner {

    private final FoodWarningAgentService agentService;
    private final AnthropicClient client;

    @Override
    public void run(ApplicationArguments args) {
        if (!client.isConfigured()) {
            System.out.println();
            System.out.println("  ANTHROPIC_API_KEY is not set — cannot run triage.");
            System.out.println("  Add it to .env and re-run.");
            System.out.println();
            return;
        }

        System.out.println();
        System.out.println("=".repeat(100));
        System.out.printf("  TRIAGE DRY RUN — model: %s — no database writes%n", client.getModel());
        System.out.println("=".repeat(100));
        System.out.println();

        long started = System.currentTimeMillis();
        List<String> lines = agentService.triageDryRun();
        long elapsed = System.currentTimeMillis() - started;

        lines.forEach(System.out::println);

        System.out.println();
        System.out.printf("  Completed in %.1fs%n", elapsed / 1000.0);
        System.out.println();
        System.out.println("  Read the FLAG list before building anything downstream.");
        System.out.println("  If most of the catalog is flagged, fix the rubric first.");
        System.out.println("=".repeat(100));
        System.out.println();
    }
}
