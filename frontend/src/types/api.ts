// Base types
export interface Food {
    extid: string;
    code: string;
    name: string;
    category?: string;
    subcategory?: string;
    description?: string;
    notes?: string;
    foundation?: boolean;
    mixable?: boolean;
    // Flavor properties are now directly on Food
    crunch?: number;
    punch?: number;
    sweet?: number;
    savory?: number;
    // Nutrition is still nested
    nutrition?: Nutrition;
    // Serving is now just a number (grams)
    typicalServingGrams?: number;
}

export interface FoodRequest {
    code: string;
    name: string;
    category?: string;
    subcategory?: string;
    description?: string;
    notes?: string;
    foundation?: boolean;
    mixable?: boolean;
    crunch?: number;
    punch?: number;
    sweet?: number;
    savory?: number;
    nutritionExtid?: string;
    typicalServingGrams?: number;
}

export interface Nutrition {
    extid?: string;
    code?: string;
    name?: string;
    description?: string;
    calories?: number;
    carbohydrate?: number;
    fat?: number;
    protein?: number;
    sugar?: number;
    fiber?: number;
    vitaminD?: number;
    vitaminE?: number;
}

export interface NutritionRequest {
    code: string;
    name: string;
    description?: string;
    carbohydrate?: number;
    fat?: number;
    protein?: number;
    sugar?: number;
    fiber?: number;
    vitaminD?: number;
    vitaminE?: number;
}

export interface Profile {
    extid: string;
    nickname: string;
    fullname: string;
}

export interface ProfileRequest {
    nickname: string;
    fullname: string;
}

export interface Company {
    extid: string;
    code: string;
    name: string;
    description?: string;
}

export interface CompanyRequest {
    code: string;
    name: string;
    description?: string;
}

// Salad Types
export interface SaladFoodIngredient {
    extid: string;
    foodExtid: string;
    foodName?: string;
    grams: number;
}

export interface SaladFoodIngredientRequest {
    foodExtid: string;
    grams: number;
}

export interface Salad {
    extid: string;
    name: string;
    description?: string;
    userExtid: string;
    foodIngredients: SaladFoodIngredient[];
    totalNutrition?: Nutrition;
    totalCrunch?: number;
    totalPunch?: number;
    totalSweet?: number;
    totalSavory?: number;
    totalGrams?: number;
    active?: string;
    createdAt: string;
    updatedAt: string;
}

export interface SaladRequest {
    name: string;
    description?: string;
    foodIngredients: SaladFoodIngredientRequest[];
}

// Mixture Types
export interface MixtureIngredient {
    extid: string;
    foodExtid: string;
    foodName: string;
    grams: number;
}

export interface Mixture {
    extid: string;
    name: string;
    description?: string;
    userExtid: string;
    ingredients: MixtureIngredient[];
    totalNutrition?: Nutrition;
    totalGrams?: number;
    active?: string;
    createdAt: string;
    updatedAt: string;
}

export interface MixtureIngredientRequest {
    foodExtid: string;
    grams: number;
}

export interface MixtureRequest {
    name: string;
    description?: string;
    ingredients: MixtureIngredientRequest[];
}

// Authentication
export interface User {
    username: string;
    email?: string;
    role: string;
}

export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    password: string;
    email?: string;
}

export interface AuthResponse {
    token: string;
    username: string;
    email?: string;
    role: string;
}

// Pagination
export interface PageRequest {
    page?: number;
    size?: number;
    sort?: string;
    active?: boolean;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}