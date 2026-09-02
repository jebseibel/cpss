package com.seibel.cpss.service.agent;

import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.FoodWarning;
import com.seibel.cpss.common.domain.FoodWarningAssessment;
import com.seibel.cpss.common.enums.AssessmentOutcomeEnum;
import com.seibel.cpss.database.db.service.FoodDbService;
import com.seibel.cpss.database.db.service.FoodWarningAssessmentDbService;
import com.seibel.cpss.database.db.service.FoodWarningDbService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The outer work queue: deterministic, one food at a time, each independently
 * retryable. Triage gates the expensive research loop; every food gets an
 * assessment row whether or not it produces a warning.
 *
 * <p>Nothing here publishes. Warnings land PENDING and wait for a human.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodWarningAgentService {

    private final FoodDbService foodDbService;
    private final FoodWarningDbService warningDbService;
    private final FoodWarningAssessmentDbService assessmentDbService;
    private final FoodTriageService triageService;
    private final FoodResearchAgent researchAgent;
    private final AnthropicClient client;

    @Data
    @Builder
    public static class RunSummary {
        private int foodsExamined;
        private int abstained;
        private int researched;
        private int warningsDrafted;
        private int withdrawn;
        private int budgetExhausted;
        private int failed;
        private int totalToolCalls;

        public boolean hasDrafts() {
            return warningsDrafted > 0;
        }
    }

    /** Processes every food with no assessment yet. */
    public RunSummary runSweep() {
        List<Long> unassessed = assessmentDbService.findUnassessedFoodIds();
        log.info("Food-warning sweep starting: {} unassessed food(s)", unassessed.size());
        return run(unassessed);
    }

    /**
     * Processes the given foods. A failure on one food is recorded and the run
     * continues — the unit of work is a food, not the batch.
     */
    public RunSummary run(List<Long> foodIds) {
        if (!client.isConfigured()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY is not set — the food-warning agent cannot run without it");
        }

        List<Food> allFoods = foodDbService.findAll();
        int examined = 0, abstained = 0, researched = 0, drafted = 0;
        int withdrawn = 0, exhausted = 0, failed = 0, toolCalls = 0;

        for (Long foodId : foodIds) {
            Food food = allFoods.stream()
                    .filter(f -> foodId.equals(f.getId()))
                    .findFirst()
                    .orElse(null);

            if (food == null) {
                log.warn("Food id={} not found or inactive — skipping", foodId);
                continue;
            }

            examined++;

            try {
                TriageResult triage = triageService.triage(food);

                if (triage.isFailed()) {
                    recordAssessment(food, AssessmentOutcomeEnum.RESEARCH_FAILED,
                            triage.getReasoning(), 0, 0);
                    failed++;
                    continue;
                }

                if (!triage.isNeedsResearch()) {
                    recordAssessment(food, AssessmentOutcomeEnum.NO_WARNING,
                            triage.getReasoning(), 0, 0);
                    abstained++;
                    log.debug("Abstained on '{}': {}", food.getName(), triage.getReasoning());
                    continue;
                }

                researched++;
                ResearchResult result = researchAgent.research(food, triage.getSuspectedCategories());
                toolCalls += result.getToolCallsUsed();

                recordAssessment(food, result.getOutcome(), result.getReasoning(),
                        result.getToolCallsUsed(), result.getTurnsUsed());

                switch (result.getOutcome()) {
                    case WARNING_DRAFTED -> {
                        persistWarning(food, result);
                        drafted++;
                        log.info("Drafted {} warning for '{}'", result.getCategory().value, food.getName());
                    }
                    case WITHDRAWN -> {
                        withdrawn++;
                        log.info("Withdrew suspicion on '{}': {}", food.getName(), result.getReasoning());
                    }
                    case BUDGET_EXHAUSTED -> {
                        exhausted++;
                        log.warn("Budget exhausted on '{}': {}", food.getName(), result.getReasoning());
                    }
                    default -> {
                        failed++;
                        log.warn("Research failed on '{}': {}", food.getName(), result.getReasoning());
                    }
                }

            } catch (Exception e) {
                log.error("Unhandled failure processing food '{}' (id={})", food.getName(), foodId, e);
                try {
                    recordAssessment(food, AssessmentOutcomeEnum.RESEARCH_FAILED,
                            "Unhandled: " + e.getMessage(), 0, 0);
                } catch (Exception inner) {
                    log.error("Could not even record the failure for food id={}", foodId, inner);
                }
                failed++;
            }
        }

        RunSummary summary = RunSummary.builder()
                .foodsExamined(examined)
                .abstained(abstained)
                .researched(researched)
                .warningsDrafted(drafted)
                .withdrawn(withdrawn)
                .budgetExhausted(exhausted)
                .failed(failed)
                .totalToolCalls(toolCalls)
                .build();

        log.info("Sweep complete: {} examined, {} abstained, {} researched → "
                        + "{} drafted, {} withdrawn, {} exhausted, {} failed ({} tool calls)",
                examined, abstained, researched, drafted, withdrawn, exhausted, failed, toolCalls);

        return summary;
    }

    private void persistWarning(Food food, ResearchResult result) {
        FoodWarning warning = new FoodWarning();
        warning.setFoodId(food.getId());
        warning.setCategory(result.getCategory());
        warning.setSeverity(result.getSeverity());
        warning.setAppliesTo(result.getAppliesTo());
        warning.setWarningText(result.getWarningText());
        warning.setSources(result.getSources());
        warning.setConfidence(result.getConfidence());
        warningDbService.create(warning);
    }

    private void recordAssessment(Food food, AssessmentOutcomeEnum outcome, String reasoning,
                                  int toolCalls, int turns) {
        FoodWarningAssessment assessment = new FoodWarningAssessment();
        assessment.setFoodId(food.getId());
        assessment.setOutcome(outcome);
        assessment.setReasoning(truncate(reasoning, 500));
        assessment.setModel(client.getModel());
        assessment.setToolCallsUsed(toolCalls);
        assessment.setTurnsUsed(turns);
        assessment.setAssessedAt(LocalDateTime.now());
        assessmentDbService.create(assessment);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    /**
     * Triage-only dry run — no research, no writes. The go/no-go gate.
     *
     * <p>Prints a per-food progress glyph to stderr as it goes ({@code !} flagged,
     * {@code .} abstained, {@code x} failed), since 166 sequential API calls take
     * long enough to look hung otherwise.
     */
    public List<String> triageDryRun() {
        List<Food> foods = foodDbService.findAll();
        List<String> lines = new ArrayList<>();
        int flagged = 0;
        int failed = 0;
        int done = 0;

        for (Food food : foods) {
            TriageResult triage = triageService.triage(food);

            if (triage.isFailed()) {
                failed++;
                System.err.print('x');
                lines.add(String.format("FAIL  %-28s %s", food.getName(), triage.getReasoning()));
            } else if (triage.isNeedsResearch()) {
                flagged++;
                System.err.print('!');
                lines.add(String.format("FLAG  %-28s %-40s %s",
                        food.getName(),
                        triage.getSuspectedCategories().stream().map(c -> c.value).toList(),
                        triage.getReasoning()));
            } else {
                System.err.print('.');
                lines.add(String.format("  ok  %-28s %s", food.getName(), triage.getReasoning()));
            }

            if (++done % 50 == 0) {
                System.err.printf("  %d/%d%n", done, foods.size());
            }
        }
        System.err.println();

        int abstained = foods.size() - flagged - failed;
        lines.add("");
        lines.add(String.format("%d of %d flagged for research (%.0f%%), %d abstained, %d failed",
                flagged, foods.size(),
                foods.isEmpty() ? 0.0 : 100.0 * flagged / foods.size(),
                abstained, failed));

        // The design's central bet. Surface a violation rather than leaving it to
        // be noticed in the list above.
        if (!foods.isEmpty() && flagged > foods.size() / 2) {
            lines.add("");
            lines.add("  WARNING: more than half the catalog was flagged. The rubric is");
            lines.add("  too permissive — fix it before running the research loop.");
        }

        return lines;
    }
}
