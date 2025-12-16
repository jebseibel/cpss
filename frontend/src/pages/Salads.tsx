import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { saladApi } from '../services/api';
import { Salad as SaladIcon, ArrowUpDown, ArrowUp, ArrowDown, ChevronRight, ChevronDown, Copy, Plus, Trash2, Edit } from 'lucide-react';
import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Salad, SaladRequest } from '../types/api';

type SortField = 'name' | 'description' | 'userExtid';
type SortDirection = 'asc' | 'desc' | null;
type FilterType = 'all' | 'system' | 'user';

export default function Salads() {
    const [sortField, setSortField] = useState<SortField | null>(null);
    const [sortDirection, setSortDirection] = useState<SortDirection>(null);
    const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
    const [expandedMicros, setExpandedMicros] = useState<Set<string>>(new Set());
    const [filter, setFilter] = useState<FilterType>('all');
    const queryClient = useQueryClient();
    const navigate = useNavigate();

    const { data: salads, isLoading, error } = useQuery({
        queryKey: ['salads'],
        queryFn: async () => {
            const response = await saladApi.getAll();
            return response.data;
        },
    });

    const createSaladMutation = useMutation({
        mutationFn: (request: SaladRequest) => saladApi.create(request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['salads'] });
        },
    });

    const deleteSaladMutation = useMutation({
        mutationFn: (extid: string) => saladApi.delete(extid),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['salads'] });
        },
        onError: (error) => {
            console.error('Delete failed:', error);
            alert('Failed to delete salad. Please try again.');
        },
    });

    const filteredAndSortedSalads = useMemo(() => {
        if (!salads) return salads;

        // Filter out inactive/deleted salads
        let filtered = salads.filter(s => s.active === 'ACTIVE' || !s.active);

        // Apply user filter
        if (filter === 'system') {
            filtered = filtered.filter(s => !s.userExtid);
        } else if (filter === 'user') {
            filtered = filtered.filter(s => s.userExtid);
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
    }, [salads, sortField, sortDirection, filter]);

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
            return <ArrowUp className="h-4 w-4 ml-1 text-green-600" />;
        }
        return <ArrowDown className="h-4 w-4 ml-1 text-green-600" />;
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

    const handleMakeItMyOwn = (salad: Salad) => {
        const request: SaladRequest = {
            name: `${salad.name} (My Version)`,
            description: salad.description,
            foodIngredients: salad.foodIngredients.map(ing => ({
                foodExtid: ing.foodExtid,
                grams: ing.grams,
            })),
        };
        createSaladMutation.mutate(request);
    };

    const handleDelete = (salad: Salad) => {
        if (window.confirm(`Are you sure you want to delete "${salad.name}"?`)) {
            deleteSaladMutation.mutate(salad.extid);
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-gray-500">Loading salads...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-red-500">Error loading salads. Make sure the backend is running.</div>
            </div>
        );
    }

    return (
        <div className="px-4 py-6 sm:px-0">
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-3xl font-bold text-gray-900 flex items-center">
                    <SaladIcon className="h-8 w-8 mr-3 text-green-600" />
                    Salads
                </h1>
                <button
                    onClick={() => navigate('/salad-builder')}
                    className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                >
                    <Plus className="h-5 w-5 mr-2" />
                    Create Salad
                </button>
            </div>

            {/* Filter Tabs */}
            <div className="mb-4">
                <div className="border-b border-gray-200">
                    <nav className="-mb-px flex space-x-8">
                        <button
                            onClick={() => setFilter('all')}
                            className={`${
                                filter === 'all'
                                    ? 'border-green-500 text-green-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                            } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
                        >
                            All Salads
                        </button>
                        <button
                            onClick={() => setFilter('system')}
                            className={`${
                                filter === 'system'
                                    ? 'border-green-500 text-green-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                            } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
                        >
                            System Salads
                        </button>
                        <button
                            onClick={() => setFilter('user')}
                            className={`${
                                filter === 'user'
                                    ? 'border-green-500 text-green-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                            } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
                        >
                            My Salads
                        </button>
                    </nav>
                </div>
            </div>

            <div className="bg-white shadow overflow-hidden sm:rounded-lg">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                    <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">
                            {/* Expand column */}
                        </th>
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
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            Type
                        </th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                            Actions
                        </th>
                    </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                    {filteredAndSortedSalads && filteredAndSortedSalads.length > 0 ? (
                        filteredAndSortedSalads.map((salad) => (
                            <>
                                <tr key={salad.extid} className="hover:bg-gray-50">
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        <button
                                            onClick={() => toggleRow(salad.extid)}
                                            className="text-gray-400 hover:text-gray-600"
                                        >
                                            {expandedRows.has(salad.extid) ? (
                                                <ChevronDown className="h-5 w-5" />
                                            ) : (
                                                <ChevronRight className="h-5 w-5" />
                                            )}
                                        </button>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                        {salad.name}
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-500">
                                        {salad.description || '-'}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {salad.userExtid ? (
                                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                                                User
                                            </span>
                                        ) : (
                                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                                                System
                                            </span>
                                        )}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <div className="flex justify-end space-x-2">
                                            {!salad.userExtid && (
                                                <button
                                                    onClick={() => handleMakeItMyOwn(salad)}
                                                    className="text-green-600 hover:text-green-900"
                                                    title="Make It My Own"
                                                >
                                                    <Copy className="h-5 w-5" />
                                                </button>
                                            )}
                                            {salad.userExtid && (
                                                <>
                                                    <button
                                                        onClick={() => navigate(`/salad-builder/${salad.extid}`)}
                                                        className="text-blue-600 hover:text-blue-900"
                                                        title="Edit"
                                                    >
                                                        <Edit className="h-5 w-5" />
                                                    </button>
                                                    <button
                                                        onClick={() => handleDelete(salad)}
                                                        className="text-red-600 hover:text-red-900"
                                                        title="Delete"
                                                    >
                                                        <Trash2 className="h-5 w-5" />
                                                    </button>
                                                </>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                                {expandedRows.has(salad.extid) && (
                                    <tr className="bg-gray-50">
                                        <td colSpan={5} className="px-6 py-4">
                                            <div className="space-y-4">
                                                {/* Ingredients Section */}
                                                <div>
                                                    <h4 className="text-sm font-semibold text-gray-700 mb-2">Ingredients ({salad.totalGrams}g total)</h4>
                                                    <div className="bg-white rounded-md border border-gray-200 overflow-hidden">
                                                        <table className="min-w-full divide-y divide-gray-200">
                                                            <thead className="bg-gray-100">
                                                            <tr>
                                                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Food</th>
                                                                <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">Grams</th>
                                                            </tr>
                                                            </thead>
                                                            <tbody className="divide-y divide-gray-200">
                                                            {salad.foodIngredients.map((ing) => (
                                                                <tr key={ing.extid}>
                                                                    <td className="px-4 py-2 text-sm text-gray-900">{ing.foodName || ing.foodExtid}</td>
                                                                    <td className="px-4 py-2 text-sm text-gray-500 text-right">{ing.grams}g</td>
                                                                </tr>
                                                            ))}
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                </div>

                                                {/* Nutrition & Flavor Section */}
                                                <div className="grid grid-cols-2 gap-4">
                                                    {/* Macros */}
                                                    {salad.totalNutrition && (
                                                        <div>
                                                            <h4 className="text-sm font-semibold text-gray-700 mb-2">Nutrition (Total Batch)</h4>
                                                            <div className="bg-white rounded-md border border-gray-200 p-3 space-y-1">
                                                                <div className="flex justify-between text-sm">
                                                                    <span className="text-gray-600">Calories:</span>
                                                                    <span className="font-medium">{salad.totalNutrition.calories || 0} cal</span>
                                                                </div>
                                                                <div className="flex justify-between text-sm">
                                                                    <span className="text-gray-600">Carbs:</span>
                                                                    <span className="font-medium">{salad.totalNutrition.carbohydrate || 0}g</span>
                                                                </div>
                                                                <div className="flex justify-between text-sm">
                                                                    <span className="text-gray-600">Protein:</span>
                                                                    <span className="font-medium">{salad.totalNutrition.protein || 0}g</span>
                                                                </div>
                                                                <div className="flex justify-between text-sm">
                                                                    <span className="text-gray-600">Fat:</span>
                                                                    <span className="font-medium">{salad.totalNutrition.fat || 0}g</span>
                                                                </div>
                                                                <button
                                                                    onClick={() => toggleMicros(salad.extid)}
                                                                    className="text-xs text-green-600 hover:text-green-800 flex items-center mt-2"
                                                                >
                                                                    {expandedMicros.has(salad.extid) ? 'Hide' : 'Show'} micronutrients
                                                                    {expandedMicros.has(salad.extid) ? (
                                                                        <ChevronDown className="h-3 w-3 ml-1" />
                                                                    ) : (
                                                                        <ChevronRight className="h-3 w-3 ml-1" />
                                                                    )}
                                                                </button>
                                                                {expandedMicros.has(salad.extid) && (
                                                                    <div className="pt-2 mt-2 border-t border-gray-200 space-y-1">
                                                                        <div className="flex justify-between text-sm">
                                                                            <span className="text-gray-600">Fiber:</span>
                                                                            <span className="font-medium">{salad.totalNutrition.fiber || 0}g</span>
                                                                        </div>
                                                                        <div className="flex justify-between text-sm">
                                                                            <span className="text-gray-600">Sugar:</span>
                                                                            <span className="font-medium">{salad.totalNutrition.sugar || 0}g</span>
                                                                        </div>
                                                                        <div className="flex justify-between text-sm">
                                                                            <span className="text-gray-600">Vitamin D:</span>
                                                                            <span className="font-medium">{salad.totalNutrition.vitaminD || 0}mcg</span>
                                                                        </div>
                                                                        <div className="flex justify-between text-sm">
                                                                            <span className="text-gray-600">Vitamin E:</span>
                                                                            <span className="font-medium">{salad.totalNutrition.vitaminE || 0}mg</span>
                                                                        </div>
                                                                    </div>
                                                                )}
                                                            </div>
                                                        </div>
                                                    )}

                                                    {/* Flavor */}
                                                    <div>
                                                        <h4 className="text-sm font-semibold text-gray-700 mb-2">Flavor Profile</h4>
                                                        <div className="bg-white rounded-md border border-gray-200 p-3 space-y-1">
                                                            <div className="flex justify-between text-sm">
                                                                <span className="text-gray-600">Crunch:</span>
                                                                <span className="font-medium">{salad.totalCrunch || 0}</span>
                                                            </div>
                                                            <div className="flex justify-between text-sm">
                                                                <span className="text-gray-600">Punch:</span>
                                                                <span className="font-medium">{salad.totalPunch || 0}</span>
                                                            </div>
                                                            <div className="flex justify-between text-sm">
                                                                <span className="text-gray-600">Sweet:</span>
                                                                <span className="font-medium">{salad.totalSweet || 0}</span>
                                                            </div>
                                                            <div className="flex justify-between text-sm">
                                                                <span className="text-gray-600">Savory:</span>
                                                                <span className="font-medium">{salad.totalSavory || 0}</span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                )}
                            </>
                        ))
                    ) : (
                        <tr>
                            <td colSpan={5} className="px-6 py-12 text-center text-gray-500">
                                No salads found.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
