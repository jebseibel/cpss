package com.seibel.cpss.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.seibel.cpss.common.enums.AssessmentOutcomeEnum;
import com.seibel.cpss.common.enums.WarningCategoryEnum;
import com.seibel.cpss.common.enums.WarningSeverityEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the verify gate — the logic standing between a model's claimed verdict
 * and a row in {@code food_warning}.
 *
 * <p>These assertions matter more than they look. Every rejection path here is a
 * case where the model asserted a warning was warranted and the code declined to
 * store it. If the gate regresses, unsourced or malformed health claims reach the
 * review queue looking legitimate.
 */
class FoodResearchAgentTest {

    private final AnthropicClient client = new AnthropicClient("", "test-model", 1024);

    private final FoodResearchAgent agent = new FoodResearchAgent(
            client, null, new FoodTriageService(client));

    /** parseVerdict is private; the gate is worth testing directly. */
    private ResearchResult parseVerdict(String json) throws Exception {
        JsonNode input = client.mapper().readTree(json);
        Method m = FoodResearchAgent.class.getDeclaredMethod(
                "parseVerdict", JsonNode.class, int.class, int.class);
        m.setAccessible(true);
        return (ResearchResult) m.invoke(agent, input, 2, 1);
    }

    @Test
    void acceptsCompleteSourcedWarning() throws Exception {
        ResearchResult result = parseVerdict("""
                {
                  "needs_warning": true,
                  "category": "vitamin_k",
                  "severity": "avoid_if",
                  "applies_to": "people taking warfarin",
                  "text": "High in vitamin K; may reduce the effect of warfarin.",
                  "sources": "[{\\"title\\":\\"NIH ODS\\",\\"url\\":\\"https://ods.od.nih.gov\\"}]",
                  "confidence": 0.9,
                  "reasoning": "Well documented interaction."
                }
                """);

        assertEquals(AssessmentOutcomeEnum.WARNING_DRAFTED, result.getOutcome());
        assertTrue(result.hasWarning());
        assertEquals(WarningCategoryEnum.VITAMIN_K, result.getCategory());
        assertEquals(WarningSeverityEnum.AVOID_IF, result.getSeverity());
        assertEquals("people taking warfarin", result.getAppliesTo());
        assertEquals(0, result.getConfidence().compareTo(new java.math.BigDecimal("0.9")));
    }

    @Test
    void withdrawalIsASuccessfulOutcomeNotAFailure() throws Exception {
        ResearchResult result = parseVerdict("""
                {"needs_warning": false, "reasoning": "Oxalate levels are not meaningful at salad portions."}
                """);

        assertEquals(AssessmentOutcomeEnum.WITHDRAWN, result.getOutcome());
        assertFalse(result.hasWarning());
        assertNull(result.getWarningText());
    }

    @Test
    void rejectsWarningWithNoSources() throws Exception {
        ResearchResult result = parseVerdict("""
                {
                  "needs_warning": true,
                  "category": "oxalate",
                  "severity": "caution",
                  "text": "Contains oxalates.",
                  "reasoning": "I recall this being true."
                }
                """);

        assertEquals(AssessmentOutcomeEnum.RESEARCH_FAILED, result.getOutcome());
        assertFalse(result.hasWarning());
    }

    @Test
    void rejectsWarningWithEmptySourceArray() throws Exception {
        ResearchResult result = parseVerdict("""
                {
                  "needs_warning": true,
                  "category": "oxalate",
                  "severity": "caution",
                  "text": "Contains oxalates.",
                  "sources": "[]",
                  "reasoning": "No source found."
                }
                """);

        assertEquals(AssessmentOutcomeEnum.RESEARCH_FAILED, result.getOutcome());
    }

    @Test
    void rejectsWarningWithUnknownCategory() throws Exception {
        ResearchResult result = parseVerdict("""
                {
                  "needs_warning": true,
                  "category": "vibes",
                  "severity": "caution",
                  "text": "Seems risky.",
                  "sources": "[{\\"title\\":\\"x\\",\\"url\\":\\"https://x\\"}]",
                  "reasoning": "Invented a category."
                }
                """);

        assertEquals(AssessmentOutcomeEnum.RESEARCH_FAILED, result.getOutcome());
    }

    @Test
    void rejectsWarningWithBlankText() throws Exception {
        ResearchResult result = parseVerdict("""
                {
                  "needs_warning": true,
                  "category": "allergen",
                  "severity": "info",
                  "text": "   ",
                  "sources": "[{\\"title\\":\\"x\\",\\"url\\":\\"https://x\\"}]",
                  "reasoning": "Empty text."
                }
                """);

        assertEquals(AssessmentOutcomeEnum.RESEARCH_FAILED, result.getOutcome());
    }

    @Test
    void normalizesLiteralNullAppliesTo() throws Exception {
        ResearchResult result = parseVerdict("""
                {
                  "needs_warning": true,
                  "category": "allergen",
                  "severity": "info",
                  "applies_to": "null",
                  "text": "Tree nut.",
                  "sources": "[{\\"title\\":\\"x\\",\\"url\\":\\"https://x\\"}]",
                  "reasoning": "General allergen note."
                }
                """);

        assertEquals(AssessmentOutcomeEnum.WARNING_DRAFTED, result.getOutcome());
        assertNull(result.getAppliesTo(), "the string \"null\" must not be stored as applies_to");
    }

    @Test
    void recordsBudgetInstrumentation() throws Exception {
        ResearchResult result = parseVerdict("""
                {"needs_warning": false, "reasoning": "Fine."}
                """);

        assertEquals(2, result.getToolCallsUsed());
        assertEquals(1, result.getTurnsUsed());
    }

    @Test
    void budgetIsConfigurable() {
        ReflectionTestUtils.setField(agent, "maxToolCalls", 3);
        ReflectionTestUtils.setField(agent, "maxTurns", 2);

        assertEquals(3, ReflectionTestUtils.getField(agent, "maxToolCalls"));
        assertEquals(2, ReflectionTestUtils.getField(agent, "maxTurns"));
    }
}
