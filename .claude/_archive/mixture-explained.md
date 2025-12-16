# Mixture - Definition and Requirements

## What is a Mixture?

A Mixture is a saved recipe for a weekly batch/blend made from food items that are ground up in a food processor and used as a nutrient supplement. Each mixture is like a recipe that can be reused or modified over time.

## Key Characteristics

- **Saved recipes**: Each mixture is a reusable recipe with specific ingredients and quantities
- **Weekly preparation**: Mixtures are typically made once per week as a batch
- **Mixable foods only**: Uses food items from `foodDb` table marked with `mixable = true`
- **Physical processing**: All ingredients are ground up (not pulverized) in a food processor
- **External usage**: The resulting mix is added to foods outside the system (e.g., yogurt, smoothies, etc.)
- **No flavor tracking**: Unlike individual foods, mixtures do not track flavor profiles
- **User-specific**: Each user maintains their own collection of mixture recipes

## Recipe Model

A Mixture is essentially a recipe with:
- **Name and description**: Identifying information for the mixture
- **Ingredient list**: Multiple ingredients, each specifying:
  - Which mixable food to use
  - Amount in grams

**Real-world example:**
```
Mixture: "Seed Mix"
- 15g walnuts
- 10g chia seeds
- 5g pumpkin seeds
```

Users can create multiple mixture recipes with different ingredients and proportions. When they want to
adjust quantities or try different combinations, they create a new mixture recipe.

## How Mixtures Work

### Real-World Process
1. User decides to make a mixture (e.g., "Seed Mix")
2. User gathers mixable ingredients (walnuts, chia seeds, pumpkin seeds)
3. User references recipe: "15g walnuts, 10g chia, 5g pumpkin"
4. User measures and blends ingredients
5. User stores the physical mixture
6. Next week, user references the same recipe and makes it again

### In Code

**Database Structure:**
- `mixture` table: Stores recipe metadata (name, description, user_id)
- `mixture_ingredient` table: Stores recipe ingredients (mixture_id, food_id, grams)

**Creating a Mixture:**
1. User selects mixable ingredients from available foods (`mixable = true`)
2. User enters quantity in grams for each ingredient
   - Grams field auto-fills with `typical_serving_grams` as a helpful default
   - User can adjust quantities as needed
3. System saves mixture and its ingredients to database

**Using a Mixture:**
1. User views their saved mixtures
2. User selects a mixture to see the recipe details
3. User can make it again, edit it, or create a variation

### Mixture Types

**User Mixtures:** Custom recipes created by users through the UI for their personal use

**Prebuilt Mixtures:** System-provided example/template mixtures that users can reference or clone (loaded from CSV at startup)


