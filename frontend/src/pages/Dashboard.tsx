import { Salad, Apple, Scale, Blend } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { foodApi, nutritionApi, saladApi, mixtureApi } from '../services/api';

export default function Dashboard() {
    // Fetch data for stats
    const { data: foods } = useQuery({
        queryKey: ['foods'],
        queryFn: async () => {
            const response = await foodApi.getAll();
            return response.data;
        },
    });

    const { data: nutritions } = useQuery({
        queryKey: ['nutritions'],
        queryFn: async () => {
            const response = await nutritionApi.getAll();
            return response.data;
        },
    });

    const { data: salads } = useQuery({
        queryKey: ['salads'],
        queryFn: async () => {
            const response = await saladApi.getAll();
            return response.data;
        },
    });

    const { data: mixtures } = useQuery({
        queryKey: ['mixtures'],
        queryFn: async () => {
            const response = await mixtureApi.getAll();
            return response.data;
        },
    });

    return (
        <div className="px-4 py-6 sm:px-0">
            <h2 className="text-3xl font-bold text-gray-900 mb-2">
                Crunch Punch Sweet & Savory
            </h2>
            <h1 className="text-xl text-gray-900 mb-8">
                (Custom Profile Salad System)
            </h1>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-5 mb-8">
                <Link to="/foods" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <Apple className="h-6 w-6 text-green-600" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">
                                        Food Items
                                    </dt>
                                    <dd className="text-lg font-medium text-gray-900">
                                        {foods?.length || 0}
                                    </dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>

                <Link to="/nutrition" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <Scale className="h-6 w-6 text-blue-600" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">
                                        Nutritions
                                    </dt>
                                    <dd className="text-lg font-medium text-gray-900">
                                        {nutritions?.length || 0}
                                    </dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>

                <Link to="/salads" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <Salad className="h-6 w-6 text-green-600" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">
                                        Salads
                                    </dt>
                                    <dd className="text-lg font-medium text-gray-900">
                                        {salads?.length || 0}
                                    </dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>

                <Link to="/mixtures" className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
                    <div className="p-5">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <Blend className="h-6 w-6 text-purple-600" />
                            </div>
                            <div className="ml-5 w-0 flex-1">
                                <dl>
                                    <dt className="text-sm font-medium text-gray-500 truncate">
                                        Mixtures
                                    </dt>
                                    <dd className="text-lg font-medium text-gray-900">
                                        {mixtures?.length || 0}
                                    </dd>
                                </dl>
                            </div>
                        </div>
                    </div>
                </Link>
            </div>

            {/* Salad Actions */}
            <div className="bg-white shadow rounded-lg p-6 mb-8">
                <h2 className="text-lg font-medium text-gray-900 mb-4">Salad Actions</h2>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <a
                        href="/salad-builder"
                        className="relative rounded-lg border border-gray-300 bg-white px-6 py-5 shadow-sm flex items-center space-x-3 hover:border-gray-400 focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-orange-500"
                    >
                        <div className="flex-shrink-0">
                            <Salad className="h-10 w-10 text-orange-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <span className="absolute inset-0" aria-hidden="true" />
                            <p className="text-sm font-medium text-gray-900">Build a Salad</p>
                            <p className="text-sm text-gray-500 truncate">
                                Create custom salad with nutrition analysis
                            </p>
                        </div>
                    </a>
                </div>
            </div>
        </div>
    );
}