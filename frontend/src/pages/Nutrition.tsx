import { useQuery } from '@tanstack/react-query';
import { nutritionApi } from '../services/api';
import { Scale, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { useState, useMemo } from 'react';
import type { Nutrition } from '../types/api';

type SortField = 'name' | 'carbohydrate' | 'fat' | 'protein' | 'sugar' | 'vitaminD' | 'vitaminE';
type SortDirection = 'asc' | 'desc' | null;

export default function Nutrition() {
    const [sortField, setSortField] = useState<SortField | null>(null);
    const [sortDirection, setSortDirection] = useState<SortDirection>(null);

    const { data: nutritions, isLoading, error } = useQuery({
        queryKey: ['nutritions'],
        queryFn: async () => {
            const response = await nutritionApi.getAll();
            return response.data;
        },
    });

    const sortedNutritions = useMemo(() => {
        if (!nutritions || !sortField || !sortDirection) return nutritions;

        return [...nutritions].sort((a, b) => {
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
    }, [nutritions, sortField, sortDirection]);

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
            return <ArrowUp className="h-4 w-4 ml-1 text-blue-600" />;
        }
        return <ArrowDown className="h-4 w-4 ml-1 text-blue-600" />;
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-gray-500">Loading nutrition profiles...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-red-500">Error loading nutrition profiles. Make sure the backend is running.</div>
            </div>
        );
    }

    return (
        <div className="px-4 py-6 sm:px-0">
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-3xl font-bold text-gray-900 flex items-center">
                    <Scale className="h-8 w-8 mr-3 text-blue-600" />
                    Nutrition Profiles
                </h1>
            </div>

            <div className="bg-white shadow overflow-hidden sm:rounded-lg">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                    <tr>
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
                            onClick={() => handleSort('carbohydrate')}
                        >
                            <div className="flex items-center">
                                Carbs (g)
                                {getSortIcon('carbohydrate')}
                            </div>
                        </th>
                        <th
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                            onClick={() => handleSort('fat')}
                        >
                            <div className="flex items-center">
                                Fat (g)
                                {getSortIcon('fat')}
                            </div>
                        </th>
                        <th
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                            onClick={() => handleSort('protein')}
                        >
                            <div className="flex items-center">
                                Protein (g)
                                {getSortIcon('protein')}
                            </div>
                        </th>
                        <th
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                            onClick={() => handleSort('sugar')}
                        >
                            <div className="flex items-center">
                                Sugar (g)
                                {getSortIcon('sugar')}
                            </div>
                        </th>
                        <th
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                            onClick={() => handleSort('vitaminD')}
                        >
                            <div className="flex items-center">
                                Vitamin D
                                {getSortIcon('vitaminD')}
                            </div>
                        </th>
                        <th
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                            onClick={() => handleSort('vitaminE')}
                        >
                            <div className="flex items-center">
                                Vitamin E
                                {getSortIcon('vitaminE')}
                            </div>
                        </th>
                    </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                    {sortedNutritions && sortedNutritions.length > 0 ? (
                        sortedNutritions.map((nutrition) => (
                            <tr key={nutrition.extid} className="hover:bg-gray-50">
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                    {nutrition.name}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {nutrition.carbohydrate}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {nutrition.fat}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {nutrition.protein}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {nutrition.sugar}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {nutrition.vitaminD ?? '-'}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {nutrition.vitaminE ?? '-'}
                                </td>
                            </tr>
                        ))
                    ) : (
                        <tr>
                            <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                                No nutrition profiles found.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}