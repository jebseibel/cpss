import axios from 'axios';
import type {
    Food,
    FoodRequest,
    Nutrition,
    NutritionRequest,
    Company,
    CompanyRequest,
    Salad,
    SaladRequest,
    Mixture,
    MixtureRequest,
    LoginRequest,
    RegisterRequest,
    AuthResponse,
} from '../types/api';

// API Configuration
// Use relative path for production, localhost for development
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Axios interceptor to attach JWT token to requests
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Axios interceptor to handle 403 responses and redirect to login
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        // Don't redirect on auth endpoints (login/register) - let the form handle errors
        const isAuthEndpoint = error.config?.url?.includes('/auth/');

        if ((error.response?.status === 403 || error.response?.status === 401) && !isAuthEndpoint) {
            // Clear invalid token
            localStorage.removeItem('token');
            // Redirect to login page
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// API Endpoints
export const foodApi = {
    getAll: () => apiClient.get<Food[]>('/food'),
    getById: (extid: string) => apiClient.get<Food>(`/food/${extid}`),
    create: (food: FoodRequest) => apiClient.post<Food>('/food', food),
    update: (extid: string, food: FoodRequest) => apiClient.put<Food>(`/food/${extid}`, food),
    delete: (extid: string) => apiClient.delete(`/food/${extid}`),
};

export const nutritionApi = {
    getAll: () => apiClient.get<Nutrition[]>('/nutrition'),
    getById: (extid: string) => apiClient.get<Nutrition>(`/nutrition/${extid}`),
    create: (nutrition: NutritionRequest) => apiClient.post<Nutrition>('/nutrition', nutrition),
    update: (extid: string, nutrition: NutritionRequest) => apiClient.put<Nutrition>(`/nutrition/${extid}`, nutrition),
    delete: (extid: string) => apiClient.delete(`/nutrition/${extid}`),
};

export const companyApi = {
    getAll: () => apiClient.get<Company[]>('/company'),
    getById: (extid: string) => apiClient.get<Company>(`/company/${extid}`),
    create: (company: CompanyRequest) => apiClient.post<Company>('/company', company),
    update: (extid: string, company: CompanyRequest) => apiClient.put<Company>(`/company/${extid}`, company),
    delete: (extid: string) => apiClient.delete(`/company/${extid}`),
};

export const saladApi = {
    getAll: () => apiClient.get<Salad[]>('/salad'),
    getById: (extid: string) => apiClient.get<Salad>(`/salad/${extid}`),
    create: (salad: SaladRequest) => apiClient.post<Salad>('/salad', salad),
    update: (extid: string, salad: SaladRequest) => apiClient.put<Salad>(`/salad/${extid}`, salad),
    delete: (extid: string) => apiClient.delete(`/salad/${extid}`),
};

export const mixtureApi = {
    getAll: () => apiClient.get<Mixture[]>('/mixture'),
    getById: (extid: string) => apiClient.get<Mixture>(`/mixture/${extid}`),
    create: (mixture: MixtureRequest) => apiClient.post<Mixture>('/mixture', mixture),
    update: (extid: string, mixture: MixtureRequest) => apiClient.put<Mixture>(`/mixture/${extid}`, mixture),
    delete: (extid: string) => apiClient.delete(`/mixture/${extid}`),
};

export const authApi = {
    login: (credentials: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', credentials),
    register: (userData: RegisterRequest) => apiClient.post<AuthResponse>('/auth/register', userData),
};

// Auth helper functions
export const authHelpers = {
    saveToken: (token: string) => localStorage.setItem('token', token),
    getToken: () => localStorage.getItem('token'),
    removeToken: () => localStorage.removeItem('token'),
    isAuthenticated: () => !!localStorage.getItem('token'),
};