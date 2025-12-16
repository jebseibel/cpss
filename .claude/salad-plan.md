# Salad Plan

## Current State
- SaladBuilder.tsx exists in the frontend (basic list view)
- No backend Salad entity or API endpoints exist yet

## Concept - ANSWERED

### What is a Mixture?
- **Mixtures are dry ingredients** meant to be sprinkled on salads or other foods (like yogurt)
- Pre-made combinations to enhance nutrition
- Added at the end of salad preparation
- Do NOT have flavor profiles

### What is a Salad?
- A complete dish/meal
- Contains individual food ingredients (vegetables, cheese, fruits, nuts, etc.)
- Can have Mixtures added to it (as toppings)
- **Has Flavor attributes** (unlike mixtures)
- Nutrition calculated from all ingredients + any mixtures

### Key Difference
- **Mixture**: Dry topping/enhancement (no flavor tracking)
- **Salad**: Complete dish with ingredients + optional mixture toppings (HAS flavor tracking)

## Salad Structure - NEEDS CLARIFICATION

### Ingredients
A salad should contain:
1. **Food Ingredients**: Individual foods with grams (e.g., 100g lettuce, 50g tomatoes)
2. **Mixture Toppings**: Pre-made mixtures with grams (e.g., 30g "Berry Crunch Mix")
3. **Flavor Profile**: Track overall flavor characteristics

### Questions - ANSWERED

#### 1. Flavor Calculation ✓
- **Salad flavor comes from ONLY food ingredients**
- Mixtures do NOT contribute to salad flavor (they have no flavor data)
- Calculate flavor from food ingredients (weighted by grams, like nutrition)

#### 2. Salad Ingredients Structure ✓
- **Two separate lists**:
  - `foodIngredients`: List of foods (with grams)
  - `mixtureIngredients`: List of mixtures (with grams)

#### 3. Nutrition Calculation ✓
- Food nutrition: food.nutrition × (grams/100)
- Mixture nutrition: mixture.totalNutrition × (grams/totalGrams) × 100
  - Wait, need to clarify: How is mixture nutrition scaled?
  - Is mixture.totalNutrition already "per batch"? So we scale by (grams/mixture.totalGrams)?

#### 4. Database Design ✓
Create separate tables:
- `salad` table (name, description, user_extid, active, created_at, etc.)
- `salad_food_ingredient` table (salad_id, food_id, grams)
- `salad_mixture_ingredient` table (salad_id, mixture_id, grams)

#### 5. Food Categories in Salads ✓
- **YES** - salads can contain foods from ALL categories:
  - Vegetables
  - Cheese
  - Fresh Fruit
  - Dried Fruit
  - Nuts
  - Dried Crunch

### Remaining Question:

#### Nutrition Scaling for Mixtures
When a mixture is added to a salad, how do we scale the nutrition?

**Example Scenario:**
- Mixture "Berry Mix" has totalGrams = 200g, totalNutrition shows 500 calories (total for 200g)
- User adds 50g of this mixture to their salad
- How do we calculate nutrition for those 50g?

**Option A**: Scale proportionally
- Calories for 50g = (500 calories × 50g) / 200g = 125 calories

**Option B**: Different calculation?

Need to understand how mixture.totalNutrition is stored to calculate correctly.

## Implementation Approach - DECIDED

### Database Schema
1. **salad table**
   - extid (UUID, PK)
   - code (unique)
   - name
   - description
   - user_extid (nullable - null = system salad)
   - active (ACTIVE/DELETED)
   - created_at, updated_at, deleted_at

2. **salad_food_ingredient table**
   - extid (UUID, PK)
   - salad_id (FK to salad)
   - food_id (FK to food)
   - grams (integer)
   - created_at, updated_at

3. **salad_mixture_ingredient table**
   - extid (UUID, PK)
   - salad_id (FK to salad)
   - mixture_id (FK to mixture)
   - grams (integer)
   - created_at, updated_at

### Calculation Logic

#### Flavor (from food ingredients only)
```
For each food ingredient:
  - Get food.flavor values (crunch, punch, sweet, savory)
  - Scale by grams: flavorValue × (grams/100)
  - Sum across all food ingredients
  - Return total flavor profile
```

#### Nutrition (from foods + mixtures)
```
For each food ingredient:
  - Get food.nutrition values
  - Scale by grams: nutritionValue × (grams/100)

For each mixture ingredient:
  - Get mixture.totalNutrition values
  - Get mixture.totalGrams
  - Scale by grams: nutritionValue × (grams/mixture.totalGrams)

Sum all nutrition values
Calculate calories: (carbs × 4) + (protein × 4) + (fat × 9)
```

### API Endpoints
- `GET /api/salad` - Get all salads
- `GET /api/salad/{extid}` - Get salad by extid
- `GET /api/salad/user/{userExtid}` - Get user's salads
- `POST /api/salad` - Create salad
- `PUT /api/salad/{extid}` - Update salad
- `DELETE /api/salad/{extid}` - Delete salad (soft delete)

### Response Structure
```json
{
  "extid": "uuid",
  "name": "Greek Salad",
  "description": "...",
  "userExtid": "user-uuid or null",
  "foodIngredients": [
    {
      "extid": "ingredient-uuid",
      "foodExtid": "food-uuid",
      "foodName": "Romaine Lettuce",
      "grams": 100
    }
  ],
  "mixtureIngredients": [
    {
      "extid": "ingredient-uuid",
      "mixtureExtid": "mixture-uuid",
      "mixtureName": "Berry Crunch Mix",
      "grams": 30
    }
  ],
  "totalNutrition": {
    "calories": 500,
    "carbohydrate": 50,
    "fat": 20,
    "protein": 30,
    "fiber": 10,
    "sugar": 15,
    "vitaminD": 5,
    "vitaminE": 10
  },
  "totalFlavor": {
    "crunch": 7,
    "punch": 5,
    "sweet": 3,
    "savory": 8
  },
  "totalGrams": 250,
  "active": "ACTIVE",
  "createdAt": "...",
  "updatedAt": "..."
}
```

### User Functionality
- ✓ Create salad from scratch (add foods one by one)
- ✓ Add pre-made mixtures to salad
- ✓ Edit existing salads
- ✓ Delete salads (soft delete)
- ✓ View nutrition (per batch, per 100g)
- ✓ View flavor profile
- ✓ Copy system salads to "make it my own"
- ✓ Filter: All/System/My Salads

## Remaining Question
**Mixture nutrition scaling** - need to confirm the calculation is correct (see question above)

## Foundation Ingredient Validation - IMPLEMENTED

### Concept
- **Foundation ingredients** are base greens/vegetables that form the base of every salad
- Foods marked with `foundation=true` in the database (e.g., lettuce, spinach, kale)
- Ensures every salad has a proper base before adding toppings

### Validation Rule
- ✓ Salads must have **at least 1 foundation ingredient**
- Ensures every salad has a proper base before adding toppings
- Enforced on both salad creation and update

### Implementation Details
**Backend** (✓ Completed):
- `SaladService.validateFoundationCount()` method
- Runs during `create()` and `update()` operations
- **Optimized N+1 Query Fix**: Uses batch query `findByExtidIn()` instead of individual lookups
- Counts `foundation=true` items from all food ingredients
- Throws `ValidationException` if count < 1
- Error messages:
  - "Salad must have at least one ingredient"
  - "Salad must have at least one foundation ingredient"

**Testing** (✓ Completed):
- ✓ Unit tests for validation logic (tests passing)
  - Valid cases: 1+ foundation ingredients
  - Invalid cases: 0 foundation ingredients, null/empty lists
  - Update operations with validation
  - Error message verification
  - Updated to use `findByExtidIn()` mocking
- ⏳ Integration tests with actual salad creation (pending)

**Frontend** (✓ Completed):
- UI guidance to select at least 1 foundation item
- Visual feedback showing foundation count (e.g., "1 of 1 required")
- Prevent submission if foundation count invalid (< 1)
- Clear error messages from backend validation
- Foundation foods marked with 🥬 emoji and highlighted in green
- "BASE" badge on foundation ingredient rows

### Foundation Foods (from CSV data)
**Vegetables:**
- Romaine Lettuce
- Fresh Spinach
- Cherry Tomatoes
- English Cucumber
- Shredded Carrots
- Bell Pepper Mix
- Red Onion

**Cheese:**
- Feta Cheese
- Fresh Mozzarella
- Parmesan
- Cheddar

**Fruits:**
- Strawberries
- Blueberries

**Nuts:**
- Sliced Almonds
- Chopped Walnuts

**Seeds:**
- Sunflower Seeds
- Pumpkin Seeds

**Crunch:**
- Croutons

## Implementation Status

### Backend - COMPLETED ✓
1. ✓ Design complete
2. ✓ Database migrations created (`018-create-salad-food-ingredient.yaml`)
3. ✓ Java entities created (Salad, SaladFoodIngredient)
4. ✓ Repositories created (SaladRepository, SaladFoodIngredientRepository)
5. ✓ Mappers created (SaladMapper, SaladFoodIngredientMapper)
6. ✓ Service layer implemented (SaladService, SaladDbService)
7. ✓ Controller with converter created (SaladController with SaladConverter)
8. ✓ Foundation validation implemented with unit tests (all passing)
9. ✓ API endpoints implemented:
   - `GET /api/salad` - List all salads
   - `GET /api/salad/{extid}` - Get specific salad
   - `GET /api/salad/user/{userExtid}` - Get user's salads
   - `POST /api/salad` - Create salad
   - `PUT /api/salad/{extid}` - Update salad
   - `DELETE /api/salad/{extid}` - Delete salad (soft delete)

### Code Quality Improvements - COMPLETED ✓
**Date: 2025-11-16**

1. ✓ **Fixed SaladMapper relationship mapping**
   - Added `SaladFoodIngredientMapper` injection
   - Now properly maps `foodIngredients` in `toModel()` method
   - File: `src/main/java/com/seibel/cpss/database/db/mapper/SaladMapper.java`

2. ✓ **Optimized foundation validation (N+1 query fix)**
   - Added `findByExtidIn()` to `FoodRepository`, `FoodService`, and `FoodDbService`
   - Updated `SaladService.validateFoundationCount()` to use batch query
   - Reduced from N individual queries to 1 batch query
   - Files modified:
     - `src/main/java/com/seibel/cpss/service/SaladService.java`
     - `src/main/java/com/seibel/cpss/database/db/repository/FoodRepository.java`
     - `src/main/java/com/seibel/cpss/service/FoodService.java`
     - `src/main/java/com/seibel/cpss/database/db/service/FoodDbService.java`

3. ✓ **Added name validation to SaladService update**
   - Added `requireNonBlank(salad.getName(), "name")` in update method
   - Consistent validation with create method
   - File: `src/main/java/com/seibel/cpss/service/SaladService.java`

4. ✓ **Updated unit tests**
   - All tests updated to mock `findByExtidIn()` instead of `findByExtid()`
   - File: `src/test/java/com/seibel/cpss/service/SaladServiceTest.java`
   - Status: 101 tests passing, BUILD SUCCESSFUL

### Frontend - COMPLETED ✓
9. ✓ Update frontend types
10. ✓ Update SaladBuilder.tsx (similar to Mixtures.tsx)
11. ✓ Implement frontend foundation selection UI
12. ✓ Create salad create/edit pages

#### Frontend Implementation Details
**Salads.tsx** (List/View Page):
- ✓ Full salad listing with sorting (name, description)
- ✓ Filter tabs: All Salads / System Salads / My Salads
- ✓ Expandable rows showing ingredients, nutrition, and flavor
- ✓ Actions: "Make It My Own", Edit, Delete
- ✓ Nutrition display with expandable micronutrients
- ✓ Flavor profile display (crunch, punch, sweet, savory)

**SaladBuilder.tsx** (Create/Edit Page):
- ✓ Create and edit functionality with React Query
- ✓ Foundation ingredient validation UI:
  - Prominent banner showing foundation count (1+ required)
  - Visual indicators: 🥬 emoji, green highlighting, "BASE" badge
  - Prevents submission if < 1 foundation ingredient
- ✓ Dynamic ingredient management:
  - Add/remove ingredients
  - Prevent duplicate food selection
  - Grams input with validation (min 1g)
  - Total weight calculation
- ✓ Error handling and loading states
- ✓ Navigation and routing

### Known Limitations
- **Note**: Current implementation only supports food ingredients (no mixture ingredients yet)
- Mixture support (salad_mixture_ingredient table) is designed but not implemented
- This matches the simplified salad structure from the refactoring
