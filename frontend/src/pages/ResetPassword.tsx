import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../services/api';
import { Lock, ArrowLeft } from 'lucide-react';

export default function ResetPassword() {
    const [searchParams] = useSearchParams();
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);
    const [token, setToken] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        const tokenParam = searchParams.get('token');
        if (!tokenParam) {
            setError('Invalid reset link. Token is missing.');
            return;
        }
        setToken(tokenParam);
    }, [searchParams]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        // Validate passwords match
        if (newPassword !== confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        // Validate password length
        if (newPassword.length < 6) {
            setError('Password must be at least 6 characters');
            return;
        }

        setLoading(true);

        try {
            await authApi.resetPassword(token, newPassword);
            setSuccess('Password has been reset successfully');
            setNewPassword('');
            setConfirmPassword('');

            // Redirect to login after 2 seconds
            setTimeout(() => {
                navigate('/login');
            }, 2000);
        } catch (err: any) {
            setError(err.response?.data?.message || 'Failed to reset password. The link may have expired.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center px-4">
            <div className="max-w-md w-full space-y-8">
                <div>
                    <h3 className="text-center text-3xl font-extrabold text-gray-900 mb-2">
                        Crunch Punch Sweet & Savory
                    </h3>
                    <h2 className="mt-2 text-center text-xl text-gray-900">
                        Create New Password
                    </h2>
                    <p className="mt-2 text-center text-sm text-gray-600">
                        Enter your new password below
                    </p>
                </div>

                <div className="bg-white rounded-lg shadow-xl p-8">
                    {error && !error.includes('Invalid reset link') ? (
                        <form className="space-y-6" onSubmit={handleSubmit}>
                            <div>
                                <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700">
                                    New Password
                                </label>
                                <input
                                    id="newPassword"
                                    name="newPassword"
                                    type="password"
                                    required
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    placeholder="••••••••"
                                    className="mt-1 appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-green-500 focus:border-green-500"
                                />
                            </div>

                            <div>
                                <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
                                    Confirm Password
                                </label>
                                <input
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    type="password"
                                    required
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    placeholder="••••••••"
                                    className="mt-1 appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-green-500 focus:border-green-500"
                                />
                            </div>

                            {error && (
                                <div className="rounded-md bg-red-50 p-4">
                                    <p className="text-sm text-red-800">{error}</p>
                                </div>
                            )}

                            {success && (
                                <div className="rounded-md bg-green-50 p-4">
                                    <p className="text-sm text-green-800">{success}</p>
                                </div>
                            )}

                            <div>
                                <button
                                    type="submit"
                                    disabled={loading || !token}
                                    className="w-full flex justify-center items-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {loading ? (
                                        'Resetting...'
                                    ) : (
                                        <>
                                            <Lock className="mr-2 h-4 w-4" />
                                            Reset Password
                                        </>
                                    )}
                                </button>
                            </div>
                        </form>
                    ) : (
                        <>
                            {error && (
                                <div className="rounded-md bg-red-50 p-4">
                                    <p className="text-sm text-red-800">{error}</p>
                                </div>
                            )}
                        </>
                    )}

                    <div className="mt-6">
                        <button
                            type="button"
                            onClick={() => navigate('/login')}
                            className="w-full flex justify-center items-center py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                        >
                            <ArrowLeft className="mr-2 h-4 w-4" />
                            Back to Login
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
