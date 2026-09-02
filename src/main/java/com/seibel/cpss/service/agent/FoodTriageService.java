package com.seibel.cpss.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.Nutrition;
import com.seibel.cpss.common.enums.WarningCategoryEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides whether a food is worth researching. This is a classifier, not an
 * agent: one call, no tools, fixed output shape.
 *
 * <p>It exists to keep the expensive research loop off the ~80% of the catalog
 * that is romaine and cucumber. If this step starts flagging most of the
 * catalog, the rubric is wrong and nothing downstream is worth running.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodTriageService {

    private final AnthropicClient client;

    private static final String SYSTEM_PROMPT = """
            You are triaging foods in a salad-app catalog to decide whether each one
            needs a health warning researched.

            Given a food's name, category, description, and per-100g nutrition, decide
            whether it plausibly falls into any of these concerns:
            oxalate, vitamin_k, allergen, glycemic, purine, tyramine, solanine,
            raw_risk, interaction.

            Most foods need NO warning. Lettuce, cucumber, bell pepper, olive oil,
            and the like are unremarkable — say so and move on.

            Do not flag a food merely because it contains sugar or fat; those values
            are already displayed to the user. Flag only concerns a reasonable person
            would not infer from the nutrition panel alone.

            Respond ONLY as JSON, no prose and no code fences:
            {
              "needs_research": true | false,
              "suspected_categories": ["oxalate", ...],
              "reasoning": "one sentence"
            }
            """;

    public TriageResult triage(Food food) {
        try {
            ArrayNode messages = client.newArray();
            messages.add(client.userMessage(describeFood(food)));

            JsonNode response = client.send(SYSTEM_PROMPT, messages, null);
            if (response == null) {
                return TriageResult.failure("Triage API call failed after retries");
            }

            String text = client.firstText(response);
            if (text == null || text.isBlank()) {
                return TriageResult.failure("Triage returned no text content");
            }

            return parse(text);

        } catch (Exception e) {
            log.error("Triage failed for food id={} name={}", food.getId(), food.getName(), e);
            return TriageResult.failure("Triage threw: " + e.getMessage());
        }
    }

    /**
     * Renders the food as the model sees it. Everything here is already in the
     * catalog — the triage step performs no lookups.
     */
    String describeFood(Food food) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(food.getName()).append('\n');
        sb.append("Category: ").append(food.getCategory());
        if (food.getSubcategory() != null) {
            sb.append(" / ").append(food.getSubcategory());
        }
        sb.append('\n');

        if (food.getDescription() != null) {
            sb.append("Description: ").append(food.getDescription()).append('\n');
        }
        if (food.getNotes() != null) {
            sb.append("Notes: ").append(food.getNotes()).append('\n');
        }
        if (food.getTypicalServingGrams() != null) {
            sb.append("Typical serving: ").append(food.getTypicalServingGrams()).append("g\n");
        }

        Nutrition n = food.getNutrition();
        if (n != null) {
            sb.append("Per 100g — ")
              .append("carbohydrate: ").append(n.getCarbohydrate()).append("g, ")
              .append("protein: ").append(n.getProtein()).append("g, ")
              .append("fat: ").append(n.getFat()).append("g, ")
              .append("sugar: ").append(n.getSugar()).append("g, ")
              .append("fiber: ").append(n.getFiber()).append("g\n");
        }

        return sb.toString();
    }

    TriageResult parse(String text) {
        try {
            String json = stripFences(text);
            JsonNode node = client.mapper().readTree(json);

            boolean needsResearch = node.path("needs_research").asBoolean(false);
            String reasoning = node.path("reasoning").asText("");

            List<WarningCategoryEnum> categories = new ArrayList<>();
            for (JsonNode c : node.path("suspected_categories")) {
                WarningCategoryEnum parsed = WarningCategoryEnum.fromValue(c.asText());
                if (parsed != null) {
                    categories.add(parsed);
                } else {
                    log.warn("Triage returned unknown category '{}' — ignoring", c.asText());
                }
            }

            // A "yes" with no recognizable category gives the research loop
            // nothing to work from; treat it as an abstention rather than
            // burning tool calls on an unfocused search.
            if (needsResearch && categories.isEmpty()) {
                log.warn("Triage said needs_research with no valid categories — treating as abstain");
                return TriageResult.abstain(reasoning.isBlank()
                        ? "Flagged without a recognizable category" : reasoning);
            }

            return TriageResult.builder()
                    .needsResearch(needsResearch)
                    .suspectedCategories(categories)
                    .reasoning(reasoning)
                    .build();

        } catch (Exception e) {
            log.error("Could not parse triage response: {}", text, e);
            return TriageResult.failure("Unparseable triage response");
        }
    }

    /** Models sometimes wrap JSON in ``` fences despite instructions. */
    private String stripFences(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return t.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return t;
    }
}
