import { Link, Outlet, useNavigate } from 'react-router-dom';
import { ChefHat, Home, Salad, Users, Building2, LogOut, Apple, Scale, Blend } from 'lucide-react';
import { authHelpers } from '../services/api';

export default function Layout() {
    const navigate = useNavigate();

    const handleLogout = () => {
        authHelpers.removeToken();
        navigate('/login');
    };

    return (
        <div className="min-h-screen bg-gray-50">
            {/* Navigation */}
            <nav className="bg-white shadow-sm border-b">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16">
                        <div className="flex">
                            <Link to="/" className="flex items-center px-2 text-gray-900">
                                <ChefHat className="h-8 w-8 text-green-600" />
                                <span className="ml-2 text-xl font-bold">CPSS</span>
                            </Link>
                            <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                                <Link
                                    to="/"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-900 border-b-2 border-transparent hover:border-gray-300"
                                >
                                    <Home className="h-4 w-4 mr-2" />
                                    Dashboard
                                </Link>
                                <Link
                                    to="/salads"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-500 border-b-2 border-transparent hover:border-gray-300 hover:text-gray-700"
                                >
                                    <Salad className="h-4 w-4 mr-2" />
                                    Salads
                                </Link>
                                <Link
                                    to="/mixtures"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-500 border-b-2 border-transparent hover:border-gray-300 hover:text-gray-700"
                                >
                                    <Blend className="h-4 w-4 mr-2" />
                                    Mixtures
                                </Link>
                                <Link
                                    to="/foods"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-500 border-b-2 border-transparent hover:border-gray-300 hover:text-gray-700"
                                >
                                    <Apple className="h-4 w-4 mr-2" />
                                    Foods
                                </Link>
                                <Link
                                    to="/nutrition"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-500 border-b-2 border-transparent hover:border-gray-300 hover:text-gray-700"
                                >
                                    <Scale className="h-4 w-4 mr-2" />
                                    Nutrition
                                </Link>
                            </div>
                        </div>
                        <div className="flex items-center space-x-4">
                            <Link
                                to="/profiles"
                                className="text-gray-500 hover:text-gray-700"
                                title="Profiles"
                            >
                                <Users className="h-5 w-5" />
                            </Link>
                            <Link
                                to="/companies"
                                className="text-gray-500 hover:text-gray-700"
                                title="Companies"
                            >
                                <Building2 className="h-5 w-5" />
                            </Link>
                            <button
                                onClick={handleLogout}
                                className="text-gray-500 hover:text-gray-700"
                                title="Logout"
                            >
                                <LogOut className="h-5 w-5" />
                            </button>
                        </div>
                    </div>
                </div>
            </nav>

            {/* Main Content */}
            <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
                <Outlet />
            </main>
        </div>
    );
}