package com.dominickp.firetvandroidtv.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dominickp.firetvandroidtv.analytics.AnalyticsTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Mock delay constants for the subscription flow.
 */
private const val LOAD_PLANS_DELAY_MS = 500L
private const val PAYMENT_PROCESSING_DELAY_MS = 2000L

/**
 * Default subscription plans used when no server plans are available.
 */
object DefaultSubscriptionPlans {
    val all: List<SubscriptionPlan> =
        listOf(
            SubscriptionPlan(
                id = "monthly",
                name = "Monthly Supporter",
                price = 4.99,
                billingCycle = "Billed monthly",
                benefits =
                    listOf(
                        "Exclusive sermons",
                        "Ad-free experience",
                        "Community access",
                    ),
            ),
            SubscriptionPlan(
                id = "yearly",
                name = "Annual Supporter",
                price = 39.99,
                billingCycle = "Billed annually (save 33%)",
                benefits =
                    listOf(
                        "Exclusive sermons",
                        "Ad-free experience",
                        "Community access",
                        "Priority support",
                    ),
            ),
        )
}

/**
 * SubscriptionPlan — represents a subscription tier.
 *
 * In production, this is fetched from the server via `GET /api/subscription/plans`.
 * For the mock implementation, we use hardcoded plans.
 */
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val price: Double,
    val billingCycle: String,
    val benefits: List<String>,
)

/**
 * SubscriptionState — represents the current state of the subscription flow.
 *
 * State machine:
 *   Idle → Loading → (PlanSelection | Success | Error)
 *   PlanSelection → Paying → (Success | Error)
 */
sealed interface SubscriptionState {
    object Idle : SubscriptionState

    object Loading : SubscriptionState

    data class PlanSelection(
        val plans: List<SubscriptionPlan>,
    ) : SubscriptionState

    data class Paying(
        val plan: SubscriptionPlan,
    ) : SubscriptionState

    data class Success(
        val plan: SubscriptionPlan,
        val status: String,
    ) : SubscriptionState

    data class Error(
        val message: String,
    ) : SubscriptionState
}

/**
 * SubscriptionViewModel — manages subscription flow state and mock payment processing.
 *
 * In production, this would:
 *   1. Fetch plans from `GET /api/subscription/plans`
 *   2. Create a Stripe Checkout session via `POST /api/checkout`
 *   3. Verify payment status via `GET /api/subscription/status`
 *   4. Handle cancellation via `POST /api/subscription/cancel`
 *
 * For the mock implementation:
 *   - Plans are hardcoded
 *   - Payment succeeds after a 2-second delay
 *   - Subscribe event is tracked via AnalyticsTracker
 */
class SubscriptionViewModel : ViewModel() {
    private val _state = MutableStateFlow<SubscriptionState>(SubscriptionState.Idle)
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    // AnalyticsTracker is a singleton initialized in FireTVApplication.
    // Cache the reference at construction time — getInstance() is double-checked-locked.
    private val analyticsTracker = AnalyticsTracker.getInstance()

    /**
     * Load subscription plans.
     * In production, this fetches from the server.
     * For mock, it transitions from Idle → Loading → PlanSelection.
     */
    fun loadPlans() {
        _state.value = SubscriptionState.Loading
        viewModelScope.launch {
            delay(LOAD_PLANS_DELAY_MS)
            _state.value = SubscriptionState.PlanSelection(DefaultSubscriptionPlans.all)
        }
    }

    /**
     * Select a plan and initiate mock payment.
     */
    fun selectPlan(plan: SubscriptionPlan) {
        _state.value = SubscriptionState.Paying(plan)
    }

    /**
     * Process mock payment.
     * Simulates Stripe checkout with a 2-second delay.
     * On success: tracks subscribe event and transitions to Success.
     * On failure: transitions to Error.
     */
    fun processPayment() {
        val payingState = _state.value
        if (payingState !is SubscriptionState.Paying) return

        viewModelScope.launch {
            try {
                delay(PAYMENT_PROCESSING_DELAY_MS)

                // Mock: always succeeds
                _state.value =
                    SubscriptionState.Success(
                        plan = payingState.plan,
                        status = "active",
                    )

                // Track subscribe event
                analyticsTracker.trackSubscribe(payingState.plan.id)
            } catch (e: Exception) {
                _state.value = SubscriptionState.Error(e.message ?: "Payment failed")
            }
        }
    }

    /**
     * Cancel current subscription.
     * Mock implementation: just resets to Idle state.
     */
    fun cancelSubscription() {
        _state.value = SubscriptionState.Idle
    }

    /**
     * Retry after payment failure.
     */
    fun retryPayment() {
        loadPlans()
    }
}
