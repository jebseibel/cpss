import { useQuery } from '@tanstack/react-query';
import { mixtureApi } from '../services/api';
import { ShoppingCart, ArrowLeft } from 'lucide-react';
import { useParams, useNavigate } from 'react-router-dom';

export default function MixtureShop() {
    const { extid } = useParams<{ extid: string }>();
    const navigate = useNavigate();

    const { data: mixture, isLoading, error } = useQuery({
        queryKey: ['mixture', extid],
        queryFn: async () => {
            if (!extid) return null;
            const response = await mixtureApi.getById(extid);
            return response.data;
        },
        enabled: !!extid,
    });

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-gray-500">Loading shopping list...</div>
            </div>
        );
    }

    if (error || !mixture) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-red-500">Error loading mixture. Please try again.</div>
            </div>
        );
    }

    return (
        <div className="px-4 py-6 sm:px-0 max-w-2xl mx-auto">
            {/* Header */}
            <div className="mb-6">
                <button
                    onClick={() => navigate('/mixtures')}
                    className="inline-flex items-center text-sm text-gray-500 hover:text-gray-700 mb-4"
                >
                    <ArrowLeft className="h-4 w-4 mr-1" />
                    Back to Mixtures
                </button>
                <h1 className="text-3xl font-bold text-gray-900 flex items-center">
                    <ShoppingCart className="h-8 w-8 mr-3 text-purple-600" />
                    Shopping List
                </h1>
                <p className="mt-2 text-lg text-gray-700 font-medium">{mixture.name}</p>
                {mixture.description && (
                    <p className="mt-1 text-sm text-gray-600">{mixture.description}</p>
                )}
            </div>

            {/* Shopping List */}
            <div className="bg-white shadow rounded-lg overflow-hidden">
                <div className="px-6 py-4 bg-purple-50 border-b border-purple-100">
                    <h2 className="text-lg font-semibold text-purple-900">Ingredients</h2>
                </div>
                <ul className="divide-y divide-gray-200">
                    {mixture.ingredients && mixture.ingredients.length > 0 ? (
                        mixture.ingredients.map((ingredient) => (
                            <li key={ingredient.extid} className="px-6 py-4 hover:bg-gray-50">
                                <div className="flex items-center justify-between">
                                    <span className="text-gray-900 font-medium">
                                        {ingredient.foodName || 'Unknown Food'}
                                    </span>
                                    <span className="text-gray-600 font-semibold">
                                        {ingredient.grams}g
                                    </span>
                                </div>
                            </li>
                        ))
                    ) : (
                        <li className="px-6 py-8 text-center text-gray-500">
                            No ingredients found
                        </li>
                    )}
                </ul>
                {mixture.totalGrams && (
                    <div className="px-6 py-4 bg-gray-50 border-t border-gray-200">
                        <div className="flex items-center justify-between font-semibold">
                            <span className="text-gray-700">Total Weight:</span>
                            <span className="text-purple-600 text-lg">{mixture.totalGrams}g</span>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
