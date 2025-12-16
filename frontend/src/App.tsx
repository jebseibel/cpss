import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Salads from './pages/Salads';
import SaladBuilder from './pages/SaladBuilder';
import Mixtures from './pages/Mixtures';
import MakeMixture from './pages/MakeMixture';
import MixtureShop from './pages/MixtureShop';
import Foods from './pages/Foods';
import Nutrition from './pages/Nutrition';

// Create a client
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            retry: 1,
        },
    },
});

function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <Router>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route
                        path="/"
                        element={
                            <ProtectedRoute>
                                <Layout />
                            </ProtectedRoute>
                        }
                    >
                        <Route index element={<Dashboard />} />
                        <Route path="salads" element={<Salads />} />
                        <Route path="salad-builder" element={<SaladBuilder />} />
                        <Route path="salad-builder/:extid" element={<SaladBuilder />} />
                        <Route path="mixtures" element={<Mixtures />} />
                        <Route path="mixtures/new" element={<MakeMixture />} />
                        <Route path="mixtures/edit/:extid" element={<MakeMixture />} />
                        <Route path="mixtures/shop/:extid" element={<MixtureShop />} />
                        <Route path="foods" element={<Foods />} />
                        <Route path="nutrition" element={<Nutrition />} />
                        <Route path="profiles" element={<div className="p-6">Profiles page coming soon...</div>} />
                        <Route path="companies" element={<div className="p-6">Companies page coming soon...</div>} />
                    </Route>
                </Routes>
            </Router>
        </QueryClientProvider>
    );
}

export default App;