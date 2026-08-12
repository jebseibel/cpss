# Mixture Nutrition Plan

## Goal
Display calculated nutrition for mixtures - macros by default, micros (Vitamin D & E only) on click

## Backend Changes - ✅ COMPLETED

### 1. Calculate Nutrition (in `MixtureConverter`)
- ✅ For each ingredient: get food.nutrition, scale by (grams/100)
- ✅ Sum all ingredients to get total mixture nutrition
- ✅ Calculate totalGrams (sum of all ingredient grams)
- ✅ Calculate calories from macros: (carbs × 4) + (protein × 4) + (fat × 9)

### 2. Update Response (`ResponseMixture`)
- ✅ Add `totalNutrition` field (ResponseNutrition)
- ✅ Add `totalGrams` field (Integer)

### 3. Add Fiber Support
- ✅ Added fiber column to nutrition database table (migration 017)
- ✅ Updated NutritionDb entity with fiber field
- ✅ Updated Nutrition domain model with fiber field
- ✅ Updated ResponseNutrition with fiber field
- ✅ Updated all nutrition CSV data files with realistic fiber values

### 4. Add Calories Support
- ✅ Added calories field to ResponseNutrition
- ✅ Calculated on-the-fly from macronutrients

## Frontend Changes - ✅ MOSTLY COMPLETED

### 3. Display Macros (in expanded mixture view)
- ✅ Show: Protein, Carbs, Fat, Sugar
- ✅ Display format: "Per batch (XXXg)" and "Per 100g"
- ⏳ TODO: Add Calories and Fiber to display

### 4. Toggle for Micros
- ✅ Add "Show Vitamins & Minerals" button/link
- ✅ Collapsible section showing Vitamin D & E only

## Implementation Status
1. ✅ Backend calculation + response update (DONE)
2. ✅ Backend fiber and calories support (DONE)
3. ⏳ Frontend macro display needs Calories and Fiber added
4. ✅ Frontend micro toggle (DONE - Vitamin D & E only)

## Notes
- Nutrition is calculated on-the-fly, not stored in database
- Always up-to-date if food nutrition changes
- Displayed only in expanded mixture view on Mixtures page
- Micronutrients limited to Vitamin D & E only (intentional design choice)
