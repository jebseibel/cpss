import { Heart, Lightbulb, Target, Users } from 'lucide-react';

export default function MyStory() {
    return (
        <div className="px-4 py-6 sm:px-0">
            <div className="max-w-4xl mx-auto">
                <h1 className="text-3xl font-bold text-gray-900 mb-8">My Story</h1>

                {/* Introduction */}
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <div className="flex flex-col md:flex-row items-start gap-6">
                        {/* Profile Photo */}
                        <div className="flex-shrink-0">
                            <img
                                src="/profile.jpg"
                                alt="Profile"
                                className="w-32 h-32 md:w-40 md:h-40 rounded-full object-cover shadow-lg"
                            />
                        </div>

                        {/* Text Content */}
                        <div className="flex-1">
                            <div className="flex items-start">
                                <Heart className="h-8 w-8 text-red-500 flex-shrink-0 mt-1" />
                                <div className="ml-4">
                                    <h2 className="text-xl font-semibold text-gray-900 mb-3">
                                        Why I Built Crunch Punch Sweet Savory
                                    </h2>
                                    <p className="text-gray-700">
                                        Welcome to Crunch Punch Sweet Savory. This project was born from a simple realization: eating healthy shouldn't be complicated, and finding the perfect balance of nutrition and flavor should be easy.
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* The Journey */}
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <div className="flex items-start">
                        <Lightbulb className="h-8 w-8 text-yellow-500 flex-shrink-0 mt-1" />
                        <div className="ml-4">
                            <h2 className="text-xl font-semibold text-gray-900 mb-3">
                                The Journey
                            </h2>
                            <div className="space-y-4 text-gray-700">
                                <p>
                                    It started with a simple question: "What am I really eating?" I wanted to understand not just the calories, but the complete nutritional profile of my meals. More importantly, I wanted to know how different ingredients worked together to create satisfying, flavorful dishes.
                                </p>
                                <p>
                                    I began tracking salads because they're incredibly versatile. You can create endless combinations with different vegetables, fruits, nuts, seeds, and toppings. But keeping track of nutrition and finding the right balance was challenging.
                                </p>
                                <p>
                                    That's when I decided to build Crunch Punch Sweet Savory - a system that would let me (and others) create personalized salads based on four key flavor dimensions: crunch, punch, sweet, and savory. Combined with detailed nutritional tracking, it became the perfect tool for mindful eating.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* The Philosophy */}
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <div className="flex items-start">
                        <Target className="h-8 w-8 text-green-600 flex-shrink-0 mt-1" />
                        <div className="ml-4">
                            <h2 className="text-xl font-semibold text-gray-900 mb-3">
                                The Philosophy
                            </h2>
                            <div className="space-y-3">
                                <div className="border-l-4 border-green-500 pl-4">
                                    <h3 className="font-semibold text-gray-900">Crunch</h3>
                                    <p className="text-gray-700">Texture matters. The satisfying crispness of fresh vegetables, nuts, and seeds.</p>
                                </div>
                                <div className="border-l-4 border-red-500 pl-4">
                                    <h3 className="font-semibold text-gray-900">Punch</h3>
                                    <p className="text-gray-700">Bold, tangy flavors that wake up your taste buds and add excitement.</p>
                                </div>
                                <div className="border-l-4 border-blue-500 pl-4">
                                    <h3 className="font-semibold text-gray-900">Sweet</h3>
                                    <p className="text-gray-700">Natural sweetness from fruits that balances and complements other flavors.</p>
                                </div>
                                <div className="border-l-4 border-amber-500 pl-4">
                                    <h3 className="font-semibold text-gray-900">Savory</h3>
                                    <p className="text-gray-700">Rich, umami flavors from cheese, nuts, and vegetables that create depth.</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* The Vision */}
                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <div className="flex items-start">
                        <Users className="h-8 w-8 text-blue-600 flex-shrink-0 mt-1" />
                        <div className="ml-4">
                            <h2 className="text-xl font-semibold text-gray-900 mb-3">
                                The Vision
                            </h2>
                            <p className="text-gray-700 mb-4">
                                Crunch Punch Sweet Savory isn't just about salads - it's about empowering people to take control of their nutrition in a way that's sustainable and enjoyable. By tracking both flavor and nutrition, you can create meals that you actually want to eat, not just meals you think you should eat.
                            </p>
                            <p className="text-gray-700 mb-4">
                                The system helps you discover your personal preferences. Do you love crunchy salads? Prefer sweet and savory combinations? Want to hit specific protein targets? Crunch Punch Sweet Savory adapts to your needs.
                            </p>
                            <p className="text-gray-700">
                                My hope is that this tool helps you develop a healthier relationship with food, makes meal planning easier, and brings a little more joy to your daily nutrition.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Call to Action */}
                <div className="bg-gradient-to-r from-green-50 to-blue-50 rounded-lg p-6">
                    <h2 className="text-xl font-semibold text-gray-900 mb-3">
                        Join the Journey
                    </h2>
                    <p className="text-gray-700 mb-4">
                        Whether you're looking to eat healthier, track your nutrition more accurately, or simply discover new flavor combinations, Crunch Punch Sweet Savory is here to help. Start building your perfect salad today.
                    </p>
                    <p className="text-sm text-gray-600 italic">
                        - Built with passion for better nutrition and mindful eating
                    </p>
                </div>
            </div>
        </div>
    );
}
