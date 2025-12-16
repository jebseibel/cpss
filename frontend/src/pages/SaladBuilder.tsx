import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { saladApi, foodApi } from '../services/api';
import { Salad as SaladIcon, Plus, Trash2, ArrowLeft, AlertCircle } from 'lucide-react';
import { useState, useMemo, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { SaladRequest, SaladFoodIngredientRequest } from '../types/api';

export default function SaladBuilder() {
    const { extid } = useParams<{ extid: string }>();
    const isEditMode = !!extid;

    const [saladName, setSaladName] = useState('');
    const [saladDescription, setSaladDescription] = useState('');
    const [ingredients, setIngredients] = useState<SaladFoodIngredientRequest[]>([]);
    const queryClient = useQueryClient();
    const navigate = useNavigate();

    const { data: foods } = useQuery({
        queryKey: ['foods'],
        queryFn: async () => {
            const response = await foodApi.getAll();
            return response.data;
        },
    });

    const { data: existingSalad, isLoading: isLoadingSalad } = useQuery({
        queryKey: ['salad', extid],
        queryFn: async () => {
            if (!extid) return null;
            const response = await saladApi.getById(extid);
            return response.data;
        },
        enabled: !!extid,
    });

    useEffect(() => {
        if (existingSalad) {
            setSaladName(existingSalad.name);
            setSaladDescription(existingSalad.description || '');
            setIngredients(existingSalad.foodIngredients.map(ing => ({
                foodExtid: ing.foodExtid,
                grams: ing.grams,
            })));
        }
    }, [existingSalad]);

    const foundationCount = useMemo(() => {
        if (!foods) return 0;
        return ingredients.filter(ing => {
            const food = foods.find(f => f.extid === ing.foodExtid);
            return food?.foundation === true;
        }).length;
    }, [ingredients, foods]);

    const createSaladMutation = useMutation({
        mutationFn: (request: SaladRequest) => saladApi.create(request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['salads'] });
            navigate('/salads');
        },
        onError: (error: any) => {
            alert(error.response?.data?.message || 'Failed to create salad');
        },
    });

    const updateSaladMutation = useMutation({
        mutationFn: ({ extid, request }: { extid: string; request: SaladRequest }) =>
            saladApi.update(extid, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['salads'] });
            queryClient.invalidateQueries({ queryKey: ['salad', extid] });
            navigate('/salads');
        },
        onError: (error: any) => {
            alert(error.response?.data?.message || 'Failed to update salad');
        },
    });

    const handleAddIngredient = () => {
        setIngredients([...ingredients, { foodExtid: '', grams: 1 }]);
    };

    const handleRemoveIngredient = (index: number) => {
        setIngredients(ingredients.filter((_, i) => i !== index));
    };

    const handleIngredientChange = (index: number, field: 'foodExtid' | 'grams', value: string | number) => {
        const newIngredients = [...ingredients];
        if (field === 'grams') {
            // Ensure grams is at least 1
            const grams = typeof value === 'number' ? value : parseInt(value) || 1;
            newIngredients[index] = { ...newIngredients[index], grams: Math.max(1, grams) };
        } else {
            newIngredients[index] = { ...newIngredients[index], foodExtid: value as string };
        }
        setIngredients(newIngredients);
    };

    const handleSaveSalad = () => {
        if (!saladName.trim() || ingredients.length === 0) {
            return;
        }

        // Validate that all ingredients have valid food selection and grams > 0
        const hasInvalidIngredients = ingredients.some(
            ing => !ing.foodExtid || !ing.grams || ing.grams < 1
        );

        if (hasInvalidIngredients) {
            return;
        }

        // Validate foundation count (1-3)
        if (foundationCount < 1 || foundationCount > 3) {
            return;
        }

        const request: SaladRequest = {
            name: saladName,
            description: saladDescription,
            foodIngredients: ingredients,
        };

        if (isEditMode && extid) {
            updateSaladMutation.mutate({ extid, request });
        } else {
            createSaladMutation.mutate(request);
        }
    };

    const isFoodFoundation = (foodExtid: string): boolean => {
        const food = foods?.find(f => f.extid === foodExtid);
        return food?.foundation === true;
    };

    if (isEditMode && isLoadingSalad) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-gray-500">Loading salad...</div>
            </div>
        );
    }

    return (
        <div className="px-4 py-6 sm:px-0 max-w-4xl mx-auto">
            {/* Header */}
            <div className="mb-6">
                <button
                    onClick={() => navigate('/salads')}
                    className="inline-flex items-center text-sm text-gray-500 hover:text-gray-700 mb-4"
                >
                    <ArrowLeft className="h-4 w-4 mr-1" />
                    Back to Salads
                </button>
                <h1 className="text-3xl font-bold text-gray-900 flex items-center">
                    <SaladIcon className="h-8 w-8 mr-3 text-green-600" />
                    {isEditMode ? 'Edit Salad' : 'Make a Salad'}
                </h1>
                <p className="mt-2 text-sm text-gray-600">
                    {isEditMode
                        ? 'Update your salad by modifying ingredients and amounts.'
                        : 'Create your own custom salad by selecting ingredients and specifying amounts.'}
                </p>
            </div>

            {/* Foundation Indicator - Prominent Banner */}
            <div className={`mb-4 p-4 rounded-lg border-2 shadow-sm ${
                foundationCount >= 1
                    ? 'bg-green-50 border-green-300'
                    : 'bg-amber-50 border-amber-300'
            }`}>
                <div className="flex items-start">
                    <AlertCircle className={`h-6 w-6 mt-0.5 mr-3 flex-shrink-0 ${
                        foundationCount >= 1
                            ? 'text-green-600'
                            : 'text-amber-600'
                    }`} />
                    <div className="flex-1">
                        <h4 className={`text-base font-bold ${
                            foundationCount >= 1
                                ? 'text-green-900'
                                : 'text-amber-900'
                        }`}>
                            Foundation Ingredients: {foundationCount} of 1 required
                        </h4>
                        <p className={`text-sm mt-1 ${
                            foundationCount >= 1
                                ? 'text-green-700'
                                : 'text-amber-700'
                        }`}>
                            {foundationCount === 0 && 'You must add at least 1 foundation ingredient (base greens marked with 🥬 like lettuce, spinach, tomatoes, cucumber, etc.)'}
                            {foundationCount >= 1 && '✓ Foundation requirement met! You can now add other ingredients or save your salad.'}
                        </p>
                    </div>
                </div>
            </div>

            <div className="bg-white shadow sm:rounded-lg">
                <div className="px-6 py-6 space-y-6">
                    {/* Name Input */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700">
                            Name <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            value={saladName}
                            onChange={(e) => setSaladName(e.target.value)}
                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500 sm:text-sm border px-3 py-2"
                            placeholder="e.g., Greek Salad"
                        />
                    </div>

                    {/* Description Input */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700">
                            Description
                        </label>
                        <textarea
                            value={saladDescription}
                            onChange={(e) => setSaladDescription(e.target.value)}
                            rows={3}
                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500 sm:text-sm border px-3 py-2"
                            placeholder="Optional description of your salad"
                        />
                    </div>

                    {/* Ingredients Section */}
                    <div>
                        <div className="flex items-center justify-between mb-3">
                            <label className="block text-sm font-medium text-gray-700">
                                Ingredients <span className="text-red-500">*</span>
                            </label>
                            <button
                                onClick={handleAddIngredient}
                                className="inline-flex items-center px-3 py-1.5 border border-green-600 text-sm font-medium rounded-md text-green-600 bg-white hover:bg-green-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                            >
                                <Plus className="h-4 w-4 mr-1" />
                                Add Ingredient
                            </button>
                        </div>

                        {ingredients.length === 0 ? (
                            <div className="text-center py-12 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300">
                                <SaladIcon className="mx-auto h-12 w-12 text-gray-400" />
                                <p className="mt-2 text-sm text-gray-500">No ingredients added yet</p>
                                <p className="text-xs text-gray-400">Click "Add Ingredient" to get started</p>
                                <p className="mt-3 text-xs font-medium text-green-700">💡 Remember: Add at least 1 foundation ingredient (marked with 🥬)</p>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {ingredients.map((ingredient, index) => (
                                    <div key={index} className={`flex items-center gap-3 p-3 rounded-lg border-2 ${
                                        isFoodFoundation(ingredient.foodExtid)
                                            ? 'bg-green-50 border-green-200'
                                            : 'bg-gray-50 border-gray-200'
                                    }`}>
                                        <div className="flex-1">
                                            <select
                                                value={ingredient.foodExtid}
                                                onChange={(e) => handleIngredientChange(index, 'foodExtid', e.target.value)}
                                                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500 sm:text-sm border px-3 py-2"
                                            >
                                                <option value="">Select food...</option>
                                                {foods?.map((food) => {
                                                    // Disable if this food is already selected in another ingredient
                                                    const isAlreadySelected = ingredients.some(
                                                        (ing, idx) => idx !== index && ing.foodExtid === food.extid
                                                    );
                                                    return (
                                                        <option
                                                            key={food.extid}
                                                            value={food.extid}
                                                            disabled={isAlreadySelected}
                                                        >
                                                            {food.name}{food.foundation ? ' 🥬' : ''}
                                                        </option>
                                                    );
                                                })}
                                            </select>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <input
                                                type="number"
                                                min="1"
                                                value={ingredient.grams || ''}
                                                onChange={(e) => handleIngredientChange(index, 'grams', parseInt(e.target.value) || 1)}
                                                placeholder="Grams"
                                                className="w-28 rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500 sm:text-sm border px-3 py-2"
                                            />
                                            <span className="text-sm text-gray-500">g</span>
                                        </div>
                                        {isFoodFoundation(ingredient.foodExtid) && (
                                            <span className="text-xs font-semibold text-green-700 bg-green-100 px-2 py-1 rounded">
                                                BASE
                                            </span>
                                        )}
                                        <button
                                            onClick={() => handleRemoveIngredient(index)}
                                            className="text-red-600 hover:text-red-800 p-1"
                                            title="Remove ingredient"
                                        >
                                            <Trash2 className="h-5 w-5" />
                                        </button>
                                    </div>
                                ))}

                                {ingredients.length > 0 && (
                                    <div className="mt-4 pt-4 border-t border-gray-200">
                                        <div className="flex justify-between text-sm">
                                            <span className="font-medium text-gray-700">Total Weight:</span>
                                            <span className="font-semibold text-green-600">
                                                {ingredients.reduce((sum, ing) => sum + (ing.grams || 0), 0)}g
                                            </span>
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Action Buttons */}
                    <div className="flex justify-end gap-3 pt-6 border-t border-gray-200">
                        <button
                            onClick={() => navigate('/salads')}
                            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={handleSaveSalad}
                            disabled={
                                !saladName.trim() ||
                                ingredients.length === 0 ||
                                ingredients.some(ing => !ing.foodExtid || !ing.grams || ing.grams < 1) ||
                                foundationCount < 1 ||
                                createSaladMutation.isPending ||
                                updateSaladMutation.isPending
                            }
                            className="px-6 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {(createSaladMutation.isPending || updateSaladMutation.isPending)
                                ? (isEditMode ? 'Updating...' : 'Creating...')
                                : (isEditMode ? 'Update Salad' : 'Create Salad')}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
