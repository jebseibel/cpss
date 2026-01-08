import { BookOpen, Salad, Apple, Blend, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function BeginHere() {
    return (
        <div className="px-4 py-6 sm:px-0">
            <div className="max-w-4xl mx-auto">
                <h1 className="text-3xl font-bold text-gray-900 mb-8">Begin Here</h1>

                {/* Introduction Section */}
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <div className="flex items-start">
                        <BookOpen className="h-8 w-8 text-green-600 flex-shrink-0 mt-1" />
                        <div className="ml-4">
                            <h2 className="text-xl font-semibold text-gray-900 mb-3">
                                Welcome to CPSS
                            </h2>
                            <p className="text-gray-700 mb-4">
                                The Custom Profile Salad System helps you create perfectly balanced salads tailored to your taste and nutritional needs. Whether you're looking for crunch, punch, sweet, or savory flavors, CPSS makes it easy to build salads you'll love.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Getting Started Steps */}
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <h2 className="text-xl font-semibold text-gray-900 mb-4">Getting Started</h2>

                    <div className="space-y-6">
                        {/* Step 1 */}
                        <div className="flex items-start">
                            <div className="flex-shrink-0">
                                <div className="flex items-center justify-center h-10 w-10 rounded-full bg-green-100 text-green-600 font-bold">
                                    1
                                </div>
                            </div>
                            <div className="ml-4 flex-1">
                                <h3 className="text-lg font-medium text-gray-900 mb-2">Explore Foods</h3>
                                <p className="text-gray-700 mb-2">
                                    Browse our collection of ingredients including vegetables, fruits, nuts, and more. Each food item includes detailed nutritional information and flavor profiles.
                                </p>
                                <Link
                                    to="/foods"
                                    className="inline-flex items-center text-green-600 hover:text-green-700 font-medium"
                                >
                                    View Foods <ArrowRight className="ml-1 h-4 w-4" />
                                </Link>
                            </div>
                        </div>

                        {/* Step 2 */}
                        <div className="flex items-start border-t pt-6">
                            <div className="flex-shrink-0">
                                <div className="flex items-center justify-center h-10 w-10 rounded-full bg-green-100 text-green-600 font-bold">
                                    2
                                </div>
                            </div>
                            <div className="ml-4 flex-1">
                                <h3 className="text-lg font-medium text-gray-900 mb-2">Build Your First Salad</h3>
                                <p className="text-gray-700 mb-2">
                                    Use the Salad Builder to create your perfect salad. Select a foundation ingredient (like lettuce or spinach), add your favorite toppings, and watch the nutrition and flavor profiles update in real-time.
                                </p>
                                <Link
                                    to="/salad-builder"
                                    className="inline-flex items-center text-green-600 hover:text-green-700 font-medium"
                                >
                                    Build a Salad <ArrowRight className="ml-1 h-4 w-4" />
                                </Link>
                            </div>
                        </div>

                        {/* Step 3 */}
                        <div className="flex items-start border-t pt-6">
                            <div className="flex-shrink-0">
                                <div className="flex items-center justify-center h-10 w-10 rounded-full bg-green-100 text-green-600 font-bold">
                                    3
                                </div>
                            </div>
                            <div className="ml-4 flex-1">
                                <h3 className="text-lg font-medium text-gray-900 mb-2">Create Custom Mixtures</h3>
                                <p className="text-gray-700 mb-2">
                                    Mixtures are dry ingredient combinations perfect for sprinkling on salads. Create your own custom blends of nuts, seeds, dried fruits, and more to enhance your salads.
                                </p>
                                <Link
                                    to="/mixtures"
                                    className="inline-flex items-center text-green-600 hover:text-green-700 font-medium"
                                >
                                    Explore Mixtures <ArrowRight className="ml-1 h-4 w-4" />
                                </Link>
                            </div>
                        </div>

                        {/* Step 4 */}
                        <div className="flex items-start border-t pt-6">
                            <div className="flex-shrink-0">
                                <div className="flex items-center justify-center h-10 w-10 rounded-full bg-green-100 text-green-600 font-bold">
                                    4
                                </div>
                            </div>
                            <div className="ml-4 flex-1">
                                <h3 className="text-lg font-medium text-gray-900 mb-2">Track Your Nutrition</h3>
                                <p className="text-gray-700 mb-2">
                                    Monitor your nutritional intake across all your salads and mixtures. View detailed breakdowns of calories, macronutrients, and micronutrients.
                                </p>
                                <Link
                                    to="/nutrition"
                                    className="inline-flex items-center text-green-600 hover:text-green-700 font-medium"
                                >
                                    View Nutrition <ArrowRight className="ml-1 h-4 w-4" />
                                </Link>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Key Features */}
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <h2 className="text-xl font-semibold text-gray-900 mb-4">Key Features</h2>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="flex items-start">
                            <Salad className="h-6 w-6 text-green-600 flex-shrink-0 mt-1" />
                            <div className="ml-3">
                                <h3 className="font-medium text-gray-900">Flavor Profiles</h3>
                                <p className="text-sm text-gray-600">Track crunch, punch, sweet, and savory levels</p>
                            </div>
                        </div>

                        <div className="flex items-start">
                            <Apple className="h-6 w-6 text-green-600 flex-shrink-0 mt-1" />
                            <div className="ml-3">
                                <h3 className="font-medium text-gray-900">Ingredient Library</h3>
                                <p className="text-sm text-gray-600">Comprehensive food database with nutrition data</p>
                            </div>
                        </div>

                        <div className="flex items-start">
                            <Blend className="h-6 w-6 text-green-600 flex-shrink-0 mt-1" />
                            <div className="ml-3">
                                <h3 className="font-medium text-gray-900">Custom Mixtures</h3>
                                <p className="text-sm text-gray-600">Create reusable topping combinations</p>
                            </div>
                        </div>

                        <div className="flex items-start">
                            <BookOpen className="h-6 w-6 text-green-600 flex-shrink-0 mt-1" />
                            <div className="ml-3">
                                <h3 className="font-medium text-gray-900">Save & Share</h3>
                                <p className="text-sm text-gray-600">Save your favorite salads for later</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Quick Links */}
                <div className="bg-gradient-to-r from-green-50 to-blue-50 rounded-lg p-6">
                    <h2 className="text-xl font-semibold text-gray-900 mb-4">Ready to Start?</h2>
                    <div className="flex flex-col sm:flex-row gap-3">
                        <Link
                            to="/salad-builder"
                            className="inline-flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md text-white bg-green-600 hover:bg-green-700"
                        >
                            <Salad className="h-5 w-5 mr-2" />
                            Build Your First Salad
                        </Link>
                        <Link
                            to="/my-story"
                            className="inline-flex items-center justify-center px-6 py-3 border border-gray-300 text-base font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
                        >
                            Learn My Story
                        </Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
