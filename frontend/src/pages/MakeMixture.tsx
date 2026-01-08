import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mixtureApi, foodApi } from '../services/api';
import { Blend, ArrowLeft } from 'lucide-react';
import { useState, useMemo, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { MixtureRequest, MixtureIngredientRequest } from '../types/api';

export default function MakeMixture() {
    const { extid } = useParams<{ extid: string }>();
    const isEditMode = !!extid;

    const [mixtureName, setMixtureName] = useState('');
    const [mixtureDescription, setMixtureDescription] = useState('');
    const [ingredients, setIngredients] = useState<MixtureIngredientRequest[]>([]);
    const queryClient = useQueryClient();
    const navigate = useNavigate();

    const { data: foods } = useQuery({
        queryKey: ['foods'],
        queryFn: async () => {
            const response = await foodApi.getAll();
            return response.data;
        },
    });

    const { data: existingMixture, isLoading: isLoadingMixture } = useQuery({
        queryKey: ['mixture', extid],
        queryFn: async () => {
            if (!extid) return null;
            const response = await mixtureApi.getById(extid);
            return response.data;
        },
        enabled: !!extid,
    });

    useEffect(() => {
        if (existingMixture) {
            setMixtureName(existingMixture.name);
            setMixtureDescription(existingMixture.description || '');
            setIngredients(existingMixture.ingredients.map(ing => ({
                foodExtid: ing.foodExtid,
                grams: ing.grams,
            })));
        }
    }, [existingMixture]);

    const mixableFoods = useMemo(() => {
        return foods?.filter(food => food.mixable) || [];
    }, [foods]);

    const createMixtureMutation = useMutation({
        mutationFn: (request: MixtureRequest) => mixtureApi.create(request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['mixtures'] });
            navigate('/mixtures');
        },
    });

    const updateMixtureMutation = useMutation({
        mutationFn: ({ extid, request }: { extid: string; request: MixtureRequest }) =>
            mixtureApi.update(extid, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['mixtures'] });
            queryClient.invalidateQueries({ queryKey: ['mixture', extid] });
            navigate('/mixtures');
        },
    });

    const handleSaveMixture = () => {
        if (!mixtureName.trim() || ingredients.length < 3) {
            return;
        }

        // Validate that all ingredients have valid food selection and grams > 0
        const hasInvalidIngredients = ingredients.some(
            ing => !ing.foodExtid || !ing.grams || ing.grams < 1
        );

        if (hasInvalidIngredients) {
            return;
        }

        const request: MixtureRequest = {
            name: mixtureName,
            description: mixtureDescription,
            ingredients: ingredients,
        };

        if (isEditMode && extid) {
            updateMixtureMutation.mutate({ extid, request });
        } else {
            createMixtureMutation.mutate(request);
        }
    };

    if (isEditMode && isLoadingMixture) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-gray-500">Loading mixture...</div>
            </div>
        );
    }

    return (
        <div className="px-4 py-6 sm:px-0 max-w-4xl mx-auto">
            {/* Header */}
            <div className="mb-6">
                <button
                    onClick={() => navigate('/mixtures')}
                    className="inline-flex items-center text-sm text-gray-500 hover:text-gray-700 mb-4"
                >
                    <ArrowLeft className="h-4 w-4 mr-1" />
                    Back to Mixtures
                </button>
                <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 flex items-center">
                    <Blend className="h-7 w-7 sm:h-8 sm:w-8 mr-2 sm:mr-3 text-purple-600" />
                    {isEditMode ? 'Edit Mixture' : 'Make a Mixture'}
                </h1>
                <p className="mt-2 text-sm text-gray-600">
                    {isEditMode
                        ? 'Update your mixture by modifying ingredients and amounts.'
                        : 'Create your own custom mixture by selecting ingredients and specifying amounts.'}
                </p>
            </div>

            <div className="bg-white shadow sm:rounded-lg">
                <div className="px-4 py-4 sm:px-6 sm:py-6 space-y-6">
                    {/* Name Input */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700">
                            Name <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            value={mixtureName}
                            onChange={(e) => setMixtureName(e.target.value)}
                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-purple-500 focus:ring-purple-500 sm:text-sm border px-3 py-2"
                            placeholder="e.g., My Custom Mix"
                        />
                    </div>

                    {/* Description Input */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700">
                            Description
                        </label>
                        <textarea
                            value={mixtureDescription}
                            onChange={(e) => setMixtureDescription(e.target.value)}
                            rows={3}
                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-purple-500 focus:ring-purple-500 sm:text-sm border px-3 py-2"
                            placeholder="Optional description of your mixture"
                        />
                    </div>

                    {/* Ingredients Section */}
                    <div>
                        <div className="flex items-center justify-between mb-3">
                            <label className="block text-sm font-medium text-gray-700">
                                Ingredients <span className="text-red-500">*</span>
                                <span className="ml-2 text-gray-500 font-normal">
                                    ({ingredients.length} selected, minimum 3)
                                </span>
                            </label>
                        </div>

                        <div className="space-y-2 max-h-96 overflow-y-auto border border-gray-200 rounded-lg p-3 bg-gray-50">
                            {mixableFoods.map((food) => {
                                const ingredientIndex = ingredients.findIndex(ing => ing.foodExtid === food.extid);
                                const isSelected = ingredientIndex !== -1;
                                const currentGrams = isSelected ? ingredients[ingredientIndex].grams : 10;

                                return (
                                    <div
                                        key={food.extid}
                                        className={`flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3 p-3 rounded-lg border transition-colors ${
                                            isSelected
                                                ? 'bg-purple-50 border-purple-300'
                                                : 'bg-white border-gray-200 hover:border-gray-300'
                                        }`}
                                    >
                                        <div className="flex items-center gap-3 flex-1">
                                            <input
                                                type="checkbox"
                                                checked={isSelected}
                                                onChange={(e) => {
                                                    if (e.target.checked) {
                                                        setIngredients([...ingredients, { foodExtid: food.extid, grams: 10 }]);
                                                    } else {
                                                        setIngredients(ingredients.filter(ing => ing.foodExtid !== food.extid));
                                                    }
                                                }}
                                                className="h-5 w-5 text-purple-600 focus:ring-purple-500 border-gray-300 rounded cursor-pointer flex-shrink-0"
                                            />
                                            <span className={`flex-1 ${isSelected ? 'text-purple-900 font-medium' : 'text-gray-700'}`}>
                                                {food.name}
                                            </span>
                                        </div>
                                        {isSelected && (
                                            <div className="flex items-center gap-2 ml-8 sm:ml-0">
                                                <input
                                                    type="number"
                                                    min="1"
                                                    value={currentGrams}
                                                    onChange={(e) => {
                                                        const grams = Math.max(1, parseInt(e.target.value) || 1);
                                                        const newIngredients = [...ingredients];
                                                        newIngredients[ingredientIndex] = { ...newIngredients[ingredientIndex], grams };
                                                        setIngredients(newIngredients);
                                                    }}
                                                    className="w-16 sm:w-20 rounded-md border-gray-300 shadow-sm focus:border-purple-500 focus:ring-purple-500 text-sm border px-2 py-1 text-center"
                                                />
                                                <span className="text-sm text-gray-500">g</span>
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>

                        {ingredients.length > 0 && (
                            <div className="mt-4 pt-4 border-t border-gray-200">
                                <div className="flex justify-between text-sm">
                                    <span className="font-medium text-gray-700">Total Weight:</span>
                                    <span className="font-semibold text-purple-600">
                                        {ingredients.reduce((sum, ing) => sum + (ing.grams || 0), 0)}g
                                    </span>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Action Buttons */}
                    <div className="flex flex-col sm:flex-row sm:justify-end gap-3 pt-6 border-t border-gray-200">
                        <button
                            onClick={() => navigate('/mixtures')}
                            className="w-full sm:w-auto px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={handleSaveMixture}
                            disabled={
                                !mixtureName.trim() ||
                                ingredients.length < 3 ||
                                ingredients.some(ing => !ing.foodExtid || !ing.grams || ing.grams < 1) ||
                                createMixtureMutation.isPending ||
                                updateMixtureMutation.isPending
                            }
                            className="w-full sm:w-auto px-6 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {(createMixtureMutation.isPending || updateMixtureMutation.isPending)
                                ? (isEditMode ? 'Updating...' : 'Creating...')
                                : (isEditMode ? 'Update Mixture' : 'Create Mixture')}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
