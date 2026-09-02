import { Info } from 'lucide-react';

interface NutritionDisclaimerProps {
    /** Tightens spacing when placed directly under a heading. */
    compact?: boolean;
}

/**
 * Standing notice that nutrition figures here are informational.
 *
 * Deliberately muted — grey, not amber. The salad builder already uses a
 * colored AlertCircle banner for the foundation rule, which is an actionable
 * requirement. This is a persistent footnote, and styling it like an alert
 * would either dilute the real alert or train users to ignore both.
 *
 * Shown on every page that displays nutrition numbers. The app is publicly
 * reachable with open registration, so a first-time visitor sees sugar and
 * macro figures with no other context.
 */
export default function NutritionDisclaimer({ compact = false }: NutritionDisclaimerProps) {
    return (
        <div
            className={`${compact ? 'mb-4' : 'mb-6'} rounded-md bg-gray-50 border border-gray-200 px-3 py-2.5 sm:px-4 sm:py-3`}
            role="note"
        >
            <div className="flex items-start">
                <Info className="h-4 w-4 mt-0.5 mr-2 flex-shrink-0 text-gray-400" aria-hidden="true" />
                <p className="text-xs sm:text-sm text-gray-600 leading-relaxed">
                    <span className="font-medium text-gray-700">Informational only.</span>{' '}
                    Nutrition figures are calculated from per-100g reference values and are
                    estimates, not measurements of what you actually prepare. This is a
                    demonstration project and does not provide medical or dietary advice.
                    If you manage a health condition such as diabetes, or take medication
                    affected by diet, check with a qualified professional rather than
                    relying on these numbers.
                </p>
            </div>
        </div>
    );
}
