import { Link, Outlet, useNavigate } from 'react-router-dom';
import { ChefHat, Home, Salad, LogOut, Apple, Scale, Blend, Menu, X, BookOpen, Heart } from 'lucide-react';
import { authHelpers } from '../services/api';
import { useState } from 'react';

export default function Layout() {
    const navigate = useNavigate();
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    const handleLogout = () => {
        authHelpers.removeToken();
        navigate('/login');
    };

    const closeMobileMenu = () => {
        setMobileMenuOpen(false);
    };

    return (
        <div className="min-h-screen bg-gray-50">
            {/* Navigation */}
            <nav className="bg-white shadow-sm border-b">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16">
                        <div className="flex">
                            <Link to="/" className="flex items-center px-2 text-gray-900" onClick={closeMobileMenu}>
                                <ChefHat className="h-8 w-8 text-green-600" />
                                <span className="ml-2 text-xl font-bold">CPSS</span>
                            </Link>
                            {/* Desktop Navigation */}
                            <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                                <Link
                                    to="/"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-900 border-b-2 border-transparent hover:border-gray-300"
                                >
                                    <Home className="h-4 w-4 mr-2" />
                                    Dashboard
                                </Link>
                                <Link
                                    to="/begin-here"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-500 border-b-2 border-transparent hover:border-gray-300 hover:text-gray-700"
                                >
                                    <BookOpen className="h-4 w-4 mr-2" />
                                    Begin Here
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
                                <Link
                                    to="/my-story"
                                    className="inline-flex items-center px-1 pt-1 text-sm font-medium text-gray-500 border-b-2 border-transparent hover:border-gray-300 hover:text-gray-700"
                                >
                                    <Heart className="h-4 w-4 mr-2" />
                                    My Story
                                </Link>
                            </div>
                        </div>
                        <div className="flex items-center space-x-4">
                            {/* Desktop Action Icons */}
                            <div className="hidden sm:flex sm:items-center sm:space-x-4">
                                <button
                                    onClick={handleLogout}
                                    className="text-gray-500 hover:text-gray-700"
                                    title="Logout"
                                >
                                    <LogOut className="h-5 w-5" />
                                </button>
                            </div>
                            {/* Mobile Menu Button */}
                            <button
                                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                                className="sm:hidden p-2 rounded-md text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                                aria-label="Toggle menu"
                            >
                                {mobileMenuOpen ? (
                                    <X className="h-6 w-6" />
                                ) : (
                                    <Menu className="h-6 w-6" />
                                )}
                            </button>
                        </div>
                    </div>
                </div>

                {/* Mobile Navigation Menu */}
                {mobileMenuOpen && (
                    <div className="sm:hidden border-t border-gray-200">
                        <div className="px-2 pt-2 pb-3 space-y-1">
                            <Link
                                to="/"
                                onClick={closeMobileMenu}
                                className="flex items-center px-3 py-2 rounded-md text-base font-medium text-gray-900 hover:bg-gray-100"
                            >
                                <Home className="h-5 w-5 mr-3" />
                                Dashboard
                            </Link>
                            <Link
                                to="/begin-here"
                                onClick={closeMobileMenu}
                                className="flex items-center px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100"
                            >
                                <BookOpen className="h-5 w-5 mr-3" />
                                Begin Here
                            </Link>
                            <Link
                                to="/salads"
                                onClick={closeMobileMenu}
                                className="flex items-center px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100"
                            >
                                <Salad className="h-5 w-5 mr-3" />
                                Salads
                            </Link>
                            <Link
                                to="/mixtures"
                                onClick={closeMobileMenu}
                                className="flex items-center px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100"
                            >
                                <Blend className="h-5 w-5 mr-3" />
                                Mixtures
                            </Link>
                            <Link
                                to="/foods"
                                onClick={closeMobileMenu}
                                className="flex items-center px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100"
                            >
                                <Apple className="h-5 w-5 mr-3" />
                                Foods
                            </Link>
                            <Link
                                to="/nutrition"
                                onClick={closeMobileMenu}
                                className="flex items-center px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100"
                            >
                                <Scale className="h-5 w-5 mr-3" />
                                Nutrition
                            </Link>
                            <Link
                                to="/my-story"
                                onClick={closeMobileMenu}
                                className="flex items-center px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100"
                            >
                                <Heart className="h-5 w-5 mr-3" />
                                My Story
                            </Link>
                            <div className="border-t border-gray-200 pt-2 mt-2">
                                <button
                                    onClick={() => {
                                        closeMobileMenu();
                                        handleLogout();
                                    }}
                                    className="flex items-center w-full px-3 py-2 rounded-md text-base font-medium text-red-600 hover:bg-red-50"
                                >
                                    <LogOut className="h-5 w-5 mr-3" />
                                    Logout
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </nav>

            {/* Main Content */}
            <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
                <Outlet />
            </main>
        </div>
    );
}