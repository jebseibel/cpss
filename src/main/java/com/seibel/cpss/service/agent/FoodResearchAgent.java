package com.seibel.cpss.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.enums.AssessmentOutcomeEnum;
import com.seibel.cpss.common.enums.WarningCategoryEnum;
import com.seibel.cpss.common.enums.WarningSeverityEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * The agentic part of the system.
 *
 * <p>Unlike the surrounding pipeline, the trajectory here is not knowable in
 * advance: the model chooses which tools to call and when it has enough
 * evidence to stop. A food whose first source is authoritative costs one
 * lookup; a drug-specific interaction may take four, each query shaped by what
 * the previous one returned.
 *
 * <p>Two rails make an open-ended loop safe to run unattended over the whole
 * catalog: a tool-call budget and a turn cap. Hitting either is a recorded
 * outcome ({@code BUDGET_EXHAUSTED}), never a partial warning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodResearchAgent {

    private final AnthropicClient client;
    private final ResearchTools tools;
    private final FoodTriageService triageService;

    @Value("${app.agent.research.max-tool-calls:6}")
    private int maxToolCalls;

    @Value("${app.agent.research.max-turns:4}")
    private int maxTurns;

    private static final String SYSTEM_PROMPT = """
            You are researching one food in a salad-app catalog to decide whether it
            needs a health note, and writing the note if so.

            Work until you can answer, then call submit_verdict. Some foods take one
            lookup; some take several because the concern is dose- or drug-specific.
            Spend searches where the answer is genuinely unclear, not to confirm what
            you already know.

            Before writing a warning, call get_existing_warnings for your category and
            match its phrasing — the catalog should read as one voice.

            Rules:
            - Cite a source for every factual claim. No citation, no warning.
            - Name the affected population, never issue blanket medical advice.
              Good: "High in vitamin K; may interfere with warfarin."
              Bad:  "Consult your doctor before eating."
            - Do not restate what a user can already see. "Contains sugar" on a food
              whose sugar content is displayed is not a warning.
            - If the concern does not hold up under research, submit needs_warning:false.
              Withdrawing a suspicion is a correct outcome, not a failure.
            - This app is not a medical device. Notes are informational and are
              reviewed by a human before publication.

            You must finish by calling submit_verdict exactly once.
            """;

    /**
     * Runs the research loop for one food.
     *
     * @param suspected categories triage flagged; shapes the opening prompt but
     *                  does not constrain the model's conclusion
     */
    public ResearchResult research(Food food, List<WarningCategoryEnum> suspected) {
        ArrayNode messages = client.newArray();
        messages.add(client.userMessage(openingPrompt(food, suspected)));

        ArrayNode toolDefs = tools.toolDefinitions(maxToolCalls);

        int toolCallsUsed = 0;
        int turnsUsed = 0;

        while (turnsUsed < maxTurns) {
            turnsUsed++;

            JsonNode response = client.send(SYSTEM_PROMPT, messages, toolDefs);
            if (response == null) {
                return failed("Anthropic API call failed after retries", toolCallsUsed, turnsUsed);
            }

            List<JsonNode> toolUses = client.toolUses(response);

            // No tool calls and no verdict — the model is done talking but never
            // submitted. Treat as exhausted rather than parsing prose for a verdict.
            if (toolUses.isEmpty()) {
                log.warn("Research loop for '{}' ended without submit_verdict", food.getName());
                return exhausted("Model stopped without submitting a verdict", toolCallsUsed, turnsUsed);
            }

            // Carry the assistant turn forward verbatim.
            ObjectNode assistantMsg = client.newObject();
            assistantMsg.put("role", "assistant");
            assistantMsg.set("content", response.get("content"));
            messages.add(assistantMsg);

            // A verdict ends the loop immediately, whatever else is in the turn.
            for (JsonNode use : toolUses) {
                if (ResearchTools.TOOL_SUBMIT.equals(use.path("name").asText())) {
                    return parseVerdict(use.path("input"), toolCallsUsed, turnsUsed);
                }
            }

            ArrayNode results = client.newArray();
            for (JsonNode use : toolUses) {
                String toolName = use.path("name").asText();

                // web_search is resolved server-side; it never needs a local result
                // block, but it does consume budget.
                if ("web_search".equals(toolName)) {
                    toolCallsUsed++;
                    continue;
                }

                if (toolCallsUsed >= maxToolCalls) {
                    return exhausted("Tool-call budget of " + maxToolCalls + " exhausted",
                            toolCallsUsed, turnsUsed);
                }

                toolCallsUsed++;
                String result = tools.dispatch(toolName, use.path("input"));

                ObjectNode block = client.newObject();
                block.put("type", "tool_result");
                block.put("tool_use_id", use.path("id").asText());
                block.put("content", result);
                results.add(block);
            }

            if (results.isEmpty()) {
                // Only server-side searches this turn; let the model continue.
                continue;
            }

            ObjectNode userMsg = client.newObject();
            userMsg.put("role", "user");
            userMsg.set("content", results);
            messages.add(userMsg);
        }

        return exhausted("Turn cap of " + maxTurns + " reached without a verdict",
                toolCallsUsed, turnsUsed);
    }

    String openingPrompt(Food food, List<WarningCategoryEnum> suspected) {
        StringBuilder sb = new StringBuilder();
        sb.append(triageService.describeFood(food));
        sb.append('\n');

        if (suspected != null && !suspected.isEmpty()) {
            sb.append("Triage suspected these concerns: ");
            sb.append(String.join(", ", suspected.stream().map(c -> c.value).toList()));
            sb.append("\nThese are suspicions, not conclusions. Confirm or withdraw them.\n");
        }

        return sb.toString();
    }

    private ResearchResult parseVerdict(JsonNode input, int toolCallsUsed, int turnsUsed) {
        boolean needsWarning = input.path("needs_warning").asBoolean(false);
        String reasoning = input.path("reasoning").asText("");

        if (!needsWarning) {
            return ResearchResult.builder()
                    .outcome(AssessmentOutcomeEnum.WITHDRAWN)
                    .reasoning(reasoning)
                    .toolCallsUsed(toolCallsUsed)
                    .turnsUsed(turnsUsed)
                    .build();
        }

        WarningCategoryEnum category = WarningCategoryEnum.fromValue(input.path("category").asText());
        WarningSeverityEnum severity = WarningSeverityEnum.fromValue(input.path("severity").asText());
        String text = input.path("text").asText(null);
        String sources = input.path("sources").asText(null);

        // The verify gate. A warning missing any of these is not publishable, and
        // recording it as WITHDRAWN would be a lie — it is a failed draft.
        if (category == null || severity == null || text == null || text.isBlank()) {
            log.warn("Verdict claimed a warning but omitted required fields — rejecting");
            return failed("Verdict incomplete: missing category, severity, or text",
                    toolCallsUsed, turnsUsed);
        }

        if (sources == null || sources.isBlank() || "[]".equals(sources.trim())) {
            log.warn("Verdict claimed a warning with no sources — rejecting");
            return failed("Verdict had no sources; unsourced warnings are not accepted",
                    toolCallsUsed, turnsUsed);
        }

        BigDecimal confidence = input.has("confidence")
                ? BigDecimal.valueOf(input.path("confidence").asDouble())
                : null;

        String appliesTo = input.path("applies_to").asText(null);
        if (appliesTo != null && (appliesTo.isBlank() || "null".equalsIgnoreCase(appliesTo))) {
            appliesTo = null;
        }

        return ResearchResult.builder()
                .outcome(AssessmentOutcomeEnum.WARNING_DRAFTED)
                .category(category)
                .severity(severity)
                .appliesTo(appliesTo)
                .warningText(text)
                .sources(sources)
                .confidence(confidence)
                .reasoning(reasoning)
                .toolCallsUsed(toolCallsUsed)
                .turnsUsed(turnsUsed)
                .build();
    }

    private ResearchResult exhausted(String reasoning, int toolCallsUsed, int turnsUsed) {
        return ResearchResult.builder()
                .outcome(AssessmentOutcomeEnum.BUDGET_EXHAUSTED)
                .reasoning(reasoning)
                .toolCallsUsed(toolCallsUsed)
                .turnsUsed(turnsUsed)
                .build();
    }

    private ResearchResult failed(String reasoning, int toolCallsUsed, int turnsUsed) {
        return ResearchResult.builder()
                .outcome(AssessmentOutcomeEnum.RESEARCH_FAILED)
                .reasoning(reasoning)
                .toolCallsUsed(toolCallsUsed)
                .turnsUsed(turnsUsed)
                .build();
    }
}
