import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mixtureApi } from '../services/api';
import { Blend, ArrowUpDown, ArrowUp, ArrowDown, ChevronRight, ChevronDown, Copy, Plus, Trash2, Edit, ShoppingCart } from 'lucide-react';
import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Mixture, MixtureRequest } from '../types/api';

type SortField = 'name' | 'description' | 'userExtid';
type SortDirection = 'asc' | 'desc' | null;

type FilterType = 'all' | 'system' | 'user';

export default function Mixtures() {
    const [sortField, setSortField] = useState<SortField | null>(null);
    const [sortDirection, setSortDirection] = useState<SortDirection>(null);
    const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
    const [expandedMicros, setExpandedMicros] = useState<Set<string>>(new Set());
    const [filter, setFilter] = useState<FilterType>('all');
    const queryClient = useQueryClient();
    const navigate = useNavigate();

    const { data: mixtures, isLoading, error } = useQuery({
        queryKey: ['mixtures'],
        queryFn: async () => {
            const response = await mixtureApi.getAll();
            return response.data;
        },
    });

    const createMixtureMutation = useMutation({
        mutationFn: (request: MixtureRequest) => mixtureApi.create(request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['mixtures'] });
        },
    });

    const deleteMixtureMutation = useMutation({
        mutationFn: (extid: string) => mixtureApi.delete(extid),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['mixtures'] });
        },
        onError: (error) => {
            console.error('Delete failed:', error);
            alert('Failed to delete mixture. Please try again.');
        },
    });

    const filteredAndSortedMixtures = useMemo(() => {
        if (!mixtures) return mixtures;

        // Filter out inactive/deleted mixtures
        let filtered = mixtures.filter(m => m.active === 'ACTIVE' || !m.active);

        // Apply user filter
        if (filter === 'system') {
            filtered = filtered.filter(m => !m.userExtid);
        } else if (filter === 'user') {
            filtered = filtered.filter(m => m.userExtid);
        }

        // Then apply sorting if needed
        if (!sortField || !sortDirection) return filtered;

        return [...filtered].sort((a, b) => {
            let aValue = a[sortField];
            let bValue = b[sortField];

            // Handle null/undefined values
            if (aValue == null) aValue = '';
            if (bValue == null) bValue = '';

            // Convert to lowercase for string comparison
            if (typeof aValue === 'string') aValue = aValue.toLowerCase();
            if (typeof bValue === 'string') bValue = bValue.toLowerCase();

            if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
            if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
            return 0;
        });
    }, [mixtures, sortField, sortDirection, filter]);

    const handleSort = (field: SortField) => {
        if (sortField === field) {
            // Cycle through: asc -> desc -> null
            if (sortDirection === 'asc') {
                setSortDirection('desc');
            } else if (sortDirection === 'desc') {
                setSortDirection(null);
                setSortField(null);
            }
        } else {
            setSortField(field);
            setSortDirection('asc');
        }
    };

    const getSortIcon = (field: SortField) => {
        if (sortField !== field) {
            return <ArrowUpDown className="h-4 w-4 ml-1 text-gray-400" />;
        }
        if (sortDirection === 'asc') {
            return <ArrowUp className="h-4 w-4 ml-1 text-purple-600" />;
        }
        return <ArrowDown className="h-4 w-4 ml-1 text-purple-600" />;
    };

    const toggleRow = (extid: string) => {
        setExpandedRows(prev => {
            const newSet = new Set(prev);
            if (newSet.has(extid)) {
                newSet.delete(extid);
            } else {
                newSet.add(extid);
            }
            return newSet;
        });
    };

    const toggleMicros = (extid: string) => {
        setExpandedMicros(prev => {
            const newSet = new Set(prev);
            if (newSet.has(extid)) {
                newSet.delete(extid);
            } else {
                newSet.add(extid);
            }
            return newSet;
        });
    };

    const handleMakeItMyOwn = (mixture: Mixture) => {
        const request: MixtureRequest = {
            name: `${mixture.name} (My Version)`,
            description: mixture.description,
            ingredients: mixture.ingredients.map(ing => ({
                foodExtid: ing.foodExtid,
                grams: ing.grams,
            })),
        };
        createMixtureMutation.mutate(request);
    };

    const handleDelete = (mixture: Mixture) => {
        if (window.confirm(`Are you sure you want to delete "${mixture.name}"?`)) {
            deleteMixtureMutation.mutate(mixture.extid);
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-gray-500">Loading mixtures...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-red-500">Error loading mixtures. Make sure the backend is running.</div>
            </div>
        );
    }

    return (
        <div className="px-4 py-6 sm:px-0">
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900 flex items-center">
                        <Blend className="h-8 w-8 mr-3 text-purple-600" />
                        Mixtures
                    </h1>
                    <p className="mt-1 text-sm text-gray-600">
                        Mixtures are to be used in salads and other things like yogurts for nutritional value.
                    </p>
                    <div className="flex items-center gap-4 mt-3">
                        <span className="text-sm font-medium text-gray-700">Show:</span>
                        <div className="flex items-center gap-3">
                            <button
                                onClick={() => setFilter('all')}
                                className={`px-3 py-1 text-sm font-medium rounded-md ${
                                    filter === 'all'
                                        ? 'bg-purple-600 text-white'
                                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                }`}
                            >
                                All
                            </button>
                            <button
                                onClick={() => setFilter('system')}
                                className={`px-3 py-1 text-sm font-medium rounded-md ${
                                    filter === 'system'
                                        ? 'bg-purple-600 text-white'
                                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                }`}
                            >
                                System
                            </button>
                            <button
                                onClick={() => setFilter('user')}
                                className={`px-3 py-1 text-sm font-medium rounded-md ${
                                    filter === 'user'
                                        ? 'bg-purple-600 text-white'
                                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                }`}
                            >
                                My Mixtures
                            </button>
                        </div>
                    </div>
                </div>
                <button
                    onClick={() => navigate('/mixtures/new')}
                    className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500"
                >
                    <Plus className="h-5 w-5 mr-2" />
                    Make a Mixture
                </button>
            </div>

            <div className="bg-white shadow overflow-hidden sm:rounded-lg">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                    <tr>
                        <th className="px-6 py-3 w-12"></th>
                        <th
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                            onClick={() => handleSort('name')}
                        >
                            <div className="flex items-center">
                                Name
                                {getSortIcon('name')}
                            </div>
                        </th>
                        <th
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                            onClick={() => handleSort('description')}
                        >
                            <div className="flex items-center">
                                Description
                                {getSortIcon('description')}
                            </div>
                        </th>
                        <th
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                            onClick={() => handleSort('userExtid')}
                        >
                            <div className="flex items-center">
                                User
                                {getSortIcon('userExtid')}
                            </div>
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            Ingredients
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            Actions
                        </th>
                    </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                    {filteredAndSortedMixtures && filteredAndSortedMixtures.length > 0 ? (
                        filteredAndSortedMixtures.map((mixture) => {
                            const isExpanded = expandedRows.has(mixture.extid);
                            const hasIngredients = mixture.ingredients && mixture.ingredients.length > 0;

                            return (
                                <>
                                    <tr key={mixture.extid} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {hasIngredients && (
                                                <button
                                                    onClick={() => toggleRow(mixture.extid)}
                                                    className="text-purple-600 hover:text-purple-800 focus:outline-none"
                                                >
                                                    {isExpanded ? (
                                                        <ChevronDown className="h-5 w-5" />
                                                    ) : (
                                                        <ChevronRight className="h-5 w-5" />
                                                    )}
                                                </button>
                                            )}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                            {mixture.name}
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-500">
                                            {mixture.description || '-'}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {mixture.userExtid || 'System'}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {hasIngredients ? `${mixture.ingredients.length} items` : 'None'}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            <div className="flex items-center gap-2">
                                                {!mixture.userExtid && hasIngredients && (
                                                    <button
                                                        onClick={() => handleMakeItMyOwn(mixture)}
                                                        disabled={createMixtureMutation.isPending}
                                                        className="inline-flex items-center px-3 py-1.5 border border-purple-600 text-sm font-medium rounded-md text-purple-600 bg-white hover:bg-purple-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 disabled:opacity-50 disabled:cursor-not-allowed"
                                                    >
                                                        <Copy className="h-4 w-4 mr-1.5" />
                                                        Make it my Own
                                                    </button>
                                                )}
                                                <button
                                                    onClick={() => navigate(`/mixtures/shop/${mixture.extid}`)}
                                                    className="text-green-600 hover:text-green-800 p-1"
                                                    title="Shopping list"
                                                >
                                                    <ShoppingCart className="h-5 w-5" />
                                                </button>
                                                {mixture.userExtid && (
                                                    <>
                                                        <button
                                                            onClick={() => navigate(`/mixtures/edit/${mixture.extid}`)}
                                                            className="text-purple-600 hover:text-purple-800 p-1"
                                                            title="Edit mixture"
                                                        >
                                                            <Edit className="h-5 w-5" />
                                                        </button>
                                                        <button
                                                            onClick={() => handleDelete(mixture)}
                                                            disabled={deleteMixtureMutation.isPending}
                                                            className="text-red-600 hover:text-red-800 p-1 disabled:opacity-50 disabled:cursor-not-allowed"
                                                            title="Delete mixture"
                                                        >
                                                            <Trash2 className="h-5 w-5" />
                                                        </button>
                                                    </>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                    {isExpanded && hasIngredients && (
                                        <tr key={`${mixture.extid}-details`}>
                                            <td colSpan={6} className="px-6 py-4 bg-purple-50">
                                                <div className="ml-8">
                                                    <h4 className="text-sm font-medium text-gray-900 mb-3">Ingredients:</h4>
                                                    <div className="space-y-2">
                                                        {mixture.ingredients.map((ingredient) => (
                                                            <div
                                                                key={ingredient.extid}
                                                                className="flex items-center justify-between bg-white px-4 py-2 rounded-md shadow-sm"
                                                            >
                                                                <span className="text-sm font-medium text-gray-900">
                                                                    {ingredient.foodName}
                                                                </span>
                                                                <span className="text-sm text-gray-600">
                                                                    {ingredient.grams}g
                                                                </span>
                                                            </div>
                                                        ))}
                                                    </div>
                                                    <div className="mt-3 text-sm text-gray-600">
                                                        Total: {mixture.ingredients.reduce((sum, ing) => sum + ing.grams, 0)}g
                                                    </div>
                                                </div>

                                                {/* Nutrition Info */}
                                                {mixture.totalNutrition && mixture.totalGrams && (
                                                    <div className="mt-6 pt-6 border-t border-purple-200">
                                                        <h4 className="text-sm font-medium text-gray-900 mb-3">Nutrition (Macros):</h4>
                                                        <div className="grid grid-cols-2 gap-4">
                                                            {/* Per Batch */}
                                                            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
                                                                <h5 className="text-xs font-semibold text-purple-700 uppercase mb-3">
                                                                    Per Batch ({mixture.totalGrams}g)
                                                                </h5>
                                                                <div className="space-y-2 text-sm">
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Calories:</span>
                                                                        <span className="font-medium text-gray-900">{mixture.totalNutrition.calories || 0} kcal</span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Carbs:</span>
                                                                        <span className="font-medium text-gray-900">{mixture.totalNutrition.carbohydrate}g</span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Fat:</span>
                                                                        <span className="font-medium text-gray-900">{mixture.totalNutrition.fat}g</span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Protein:</span>
                                                                        <span className="font-medium text-gray-900">{mixture.totalNutrition.protein}g</span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Fiber:</span>
                                                                        <span className="font-medium text-gray-900">{mixture.totalNutrition.fiber || 0}g</span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Sugar:</span>
                                                                        <span className="font-medium text-gray-900">{mixture.totalNutrition.sugar}g</span>
                                                                    </div>
                                                                </div>
                                                            </div>

                                                            {/* Per 100g */}
                                                            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
                                                                <h5 className="text-xs font-semibold text-purple-700 uppercase mb-3">
                                                                    Per 100g
                                                                </h5>
                                                                <div className="space-y-2 text-sm">
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Calories:</span>
                                                                        <span className="font-medium text-gray-900">
                                                                            {Math.round(((mixture.totalNutrition.calories || 0) / mixture.totalGrams) * 100)} kcal
                                                                        </span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Carbs:</span>
                                                                        <span className="font-medium text-gray-900">
                                                                            {Math.round(((mixture.totalNutrition.carbohydrate || 0) / (mixture.totalGrams || 1)) * 100)}g
                                                                        </span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Fat:</span>
                                                                        <span className="font-medium text-gray-900">
                                                                            {Math.round(((mixture.totalNutrition.fat || 0) / (mixture.totalGrams || 1)) * 100)}g
                                                                        </span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Protein:</span>
                                                                        <span className="font-medium text-gray-900">
                                                                            {Math.round(((mixture.totalNutrition.protein || 0) / (mixture.totalGrams || 1)) * 100)}g
                                                                        </span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Fiber:</span>
                                                                        <span className="font-medium text-gray-900">
                                                                            {Math.round(((mixture.totalNutrition.fiber || 0) / (mixture.totalGrams || 1)) * 100)}g
                                                                        </span>
                                                                    </div>
                                                                    <div className="flex justify-between">
                                                                        <span className="text-gray-600">Sugar:</span>
                                                                        <span className="font-medium text-gray-900">
                                                                            {Math.round(((mixture.totalNutrition.sugar || 0) / (mixture.totalGrams || 1)) * 100)}g
                                                                        </span>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </div>

                                                        {/* Micronutrients Toggle */}
                                                        <div className="mt-4">
                                                            <button
                                                                onClick={() => toggleMicros(mixture.extid)}
                                                                className="text-sm text-purple-600 hover:text-purple-800 font-medium flex items-center gap-1"
                                                            >
                                                                {expandedMicros.has(mixture.extid) ? (
                                                                    <>
                                                                        <ChevronDown className="h-4 w-4" />
                                                                        Hide Vitamins & Minerals
                                                                    </>
                                                                ) : (
                                                                    <>
                                                                        <ChevronRight className="h-4 w-4" />
                                                                        Show Vitamins & Minerals
                                                                    </>
                                                                )}
                                                            </button>
                                                        </div>

                                                        {/* Micronutrients Display */}
                                                        {expandedMicros.has(mixture.extid) && (
                                                            <div className="mt-4 pt-4 border-t border-purple-200">
                                                                <div className="grid grid-cols-2 gap-4">
                                                                    {/* Per Batch */}
                                                                    <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
                                                                        <h5 className="text-xs font-semibold text-purple-700 uppercase mb-3">
                                                                            Per Batch ({mixture.totalGrams}g)
                                                                        </h5>
                                                                        <div className="space-y-2 text-sm">
                                                                            <div className="flex justify-between">
                                                                                <span className="text-gray-600">Vitamin D:</span>
                                                                                <span className="font-medium text-gray-900">
                                                                                    {mixture.totalNutrition.vitaminD || 0}mg
                                                                                </span>
                                                                            </div>
                                                                            <div className="flex justify-between">
                                                                                <span className="text-gray-600">Vitamin E:</span>
                                                                                <span className="font-medium text-gray-900">
                                                                                    {mixture.totalNutrition.vitaminE || 0}mg
                                                                                </span>
                                                                            </div>
                                                                        </div>
                                                                    </div>

                                                                    {/* Per 100g */}
                                                                    <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
                                                                        <h5 className="text-xs font-semibold text-purple-700 uppercase mb-3">
                                                                            Per 100g
                                                                        </h5>
                                                                        <div className="space-y-2 text-sm">
                                                                            <div className="flex justify-between">
                                                                                <span className="text-gray-600">Vitamin D:</span>
                                                                                <span className="font-medium text-gray-900">
                                                                                    {Math.round(((mixture.totalNutrition.vitaminD || 0) / mixture.totalGrams) * 100)}mg
                                                                                </span>
                                                                            </div>
                                                                            <div className="flex justify-between">
                                                                                <span className="text-gray-600">Vitamin E:</span>
                                                                                <span className="font-medium text-gray-900">
                                                                                    {Math.round(((mixture.totalNutrition.vitaminE || 0) / mixture.totalGrams) * 100)}mg
                                                                                </span>
                                                                            </div>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        )}
                                                    </div>
                                                )}
                                            </td>
                                        </tr>
                                    )}
                                </>
                            );
                        })
                    ) : (
                        <tr>
                            <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                                No mixtures found.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
