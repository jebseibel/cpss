package com.seibel.cpss.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seibel.cpss.common.domain.Food;
import com.seibel.cpss.common.domain.FoodWarning;
import com.seibel.cpss.common.enums.WarningCategoryEnum;
import com.seibel.cpss.database.db.service.FoodDbService;
import com.seibel.cpss.database.db.service.FoodWarningDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The tools the research loop can call, and their dispatch.
 *
 * <p>Web search is delegated to Anthropic's server-side {@code web_search} tool
 * rather than a local HTTP client — the model issues the query and receives
 * results without this application brokering it. The tools defined here are the
 * ones that need CPSS's own data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResearchTools {

    private final FoodDbService foodDbService;
    private final FoodWarningDbService warningDbService;
    private final AnthropicClient client;

    public static final String TOOL_GET_EXISTING_WARNINGS = "get_existing_warnings";
    public static final String TOOL_GET_FOOD = "get_food";
    public static final String TOOL_SUBMIT = "submit_verdict";

    /**
     * Tool definitions sent to the API. Includes the server-side web_search
     * tool plus CPSS-local tools.
     */
    public ArrayNode toolDefinitions(int maxSearches) {
        ArrayNode tools = client.newArray();

        // Anthropic server-side web search.
        ObjectNode search = client.newObject();
        search.put("type", "web_search_20250305");
        search.put("name", "web_search");
        search.put("max_uses", maxSearches);
        tools.add(search);

        tools.add(simpleTool(TOOL_GET_EXISTING_WARNINGS,
                "Fetch already-approved warnings in a given category, so your wording "
                        + "matches how this catalog already phrases the same concern.",
                "category",
                "One of: " + WarningCategoryEnum.ALLOWED_VALUES));

        tools.add(simpleTool(TOOL_GET_FOOD,
                "Look up another food in this catalog by name, to compare nutrition "
                        + "or check whether a related ingredient exists.",
                "name",
                "The food name to look up."));

        tools.add(submitTool());

        return tools;
    }

    private ObjectNode simpleTool(String name, String description, String paramName, String paramDescription) {
        ObjectNode tool = client.newObject();
        tool.put("name", name);
        tool.put("description", description);

        ObjectNode schema = client.newObject();
        schema.put("type", "object");

        ObjectNode props = client.newObject();
        ObjectNode param = client.newObject();
        param.put("type", "string");
        param.put("description", paramDescription);
        props.set(paramName, param);

        schema.set("properties", props);
        ArrayNode required = client.newArray();
        required.add(paramName);
        schema.set("required", required);

        tool.set("input_schema", schema);
        return tool;
    }

    /**
     * The loop's only exit that produces a decision. Making the verdict a tool
     * rather than parsed prose means the model must supply every field — and
     * the schema, not a regex, enforces that.
     */
    private ObjectNode submitTool() {
        ObjectNode tool = client.newObject();
        tool.put("name", TOOL_SUBMIT);
        tool.put("description",
                "Submit your final decision for this food. Call this exactly once, when you "
                        + "have enough evidence. Set needs_warning to false if the suspected "
                        + "concern does not hold up — withdrawing a suspicion is a correct outcome.");

        ObjectNode schema = client.newObject();
        schema.put("type", "object");
        ObjectNode props = client.newObject();

        props.set("needs_warning", prop("boolean", "True only if this food warrants a user-facing note."));
        props.set("category", prop("string", "One of: " + WarningCategoryEnum.ALLOWED_VALUES));
        props.set("severity", prop("string", "One of: info, caution, avoid_if"));
        props.set("applies_to", prop("string", "Who this concerns, e.g. 'people taking warfarin'. Null for general info."));
        props.set("text", prop("string", "One or two sentences, user-facing. No blanket medical advice."));
        props.set("sources", prop("string", "JSON array of {title, url}. Required when needs_warning is true."));
        props.set("confidence", prop("number", "0.0 to 1.0"));
        props.set("reasoning", prop("string", "One sentence on how you reached this."));

        schema.set("properties", props);
        ArrayNode required = client.newArray();
        required.add("needs_warning");
        required.add("reasoning");
        schema.set("required", required);

        tool.set("input_schema", schema);
        return tool;
    }

    private ObjectNode prop(String type, String description) {
        ObjectNode p = client.newObject();
        p.put("type", type);
        p.put("description", description);
        return p;
    }

    /**
     * Executes a CPSS-local tool call. Web search never reaches here — the API
     * resolves it server-side.
     *
     * @return the tool result content as a string, always non-null
     */
    public String dispatch(String toolName, JsonNode input) {
        try {
            return switch (toolName) {
                case TOOL_GET_EXISTING_WARNINGS -> getExistingWarnings(input.path("category").asText());
                case TOOL_GET_FOOD -> getFood(input.path("name").asText());
                default -> "Unknown tool: " + toolName;
            };
        } catch (Exception e) {
            log.warn("Tool {} failed: {}", toolName, e.getMessage());
            return "Tool error: " + e.getMessage();
        }
    }

    private String getExistingWarnings(String categoryValue) {
        WarningCategoryEnum category = WarningCategoryEnum.fromValue(categoryValue);
        if (category == null) {
            return "Unknown category '" + categoryValue + "'. Valid: " + WarningCategoryEnum.ALLOWED_VALUES;
        }

        List<FoodWarning> approved = warningDbService.findByStatus(
                com.seibel.cpss.common.enums.WarningStatusEnum.APPROVED);

        List<String> lines = approved.stream()
                .filter(w -> w.getCategory() == category)
                .map(w -> "- [" + w.getSeverity().value + "] " + w.getWarningText())
                .toList();

        if (lines.isEmpty()) {
            return "No approved warnings yet in category '" + categoryValue
                    + "'. You are setting the precedent for how this concern is phrased.";
        }
        return "Existing approved '" + categoryValue + "' warnings:\n" + String.join("\n", lines);
    }

    private String getFood(String name) {
        List<Food> all = foodDbService.findAll();
        return all.stream()
                .filter(f -> f.getName() != null && f.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(f -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(f.getName()).append(" (").append(f.getCategory()).append(")\n");
                    if (f.getNutrition() != null) {
                        sb.append("Per 100g — carbohydrate: ").append(f.getNutrition().getCarbohydrate())
                          .append("g, protein: ").append(f.getNutrition().getProtein())
                          .append("g, fat: ").append(f.getNutrition().getFat())
                          .append("g, sugar: ").append(f.getNutrition().getSugar())
                          .append("g, fiber: ").append(f.getNutrition().getFiber()).append("g");
                    }
                    return sb.toString();
                })
                .orElse("No food named '" + name + "' in this catalog.");
    }
}
