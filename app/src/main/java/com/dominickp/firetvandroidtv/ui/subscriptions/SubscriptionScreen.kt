package com.dominickp.firetvandroidtv.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dominickp.firetvandroidtv.ui.focus.trackFocus
import com.dominickp.firetvandroidtv.ui.theme.BrandColors

/**
 * SubscriptionScreen — plan selection and mock payment flow.
 *
 * Mirrors Roku's Subscription screen from roku-channel-template:
 *   - Displays subscription plans (monthly/yearly)
 *   - Shows plan benefits and pricing
 *   - Mock payment with delay(2000) to simulate Stripe processing
 *   - Tracks subscribe event via AnalyticsTracker
 *
 * This is a mock implementation — no real payment processing.
 * In production, replace with Google Play Billing or Stripe SDK.
 */
@Composable
fun SubscriptionScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: SubscriptionViewModel = viewModel(),
) {
    val subscriptionState by viewModel.state.collectAsStateWithLifecycle()

    // Kick off plan loading once on first screen entry.
    // LaunchedEffect must NOT be inside a conditional block — that's a Compose anti-pattern.
    // Key on Unit (runs once when composable enters composition).
    LaunchedEffect(Unit) {
        viewModel.loadPlans()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\u2190 Back",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .trackFocus("subscription_back")
                            .clickable(onClick = onBack),
                )
                Text(
                    text = "Subscribe",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BrandColors.Focus,
                )
                Spacer(modifier = Modifier.width(64.dp))
            }
        }

        // Content based on state
        when (val state = subscriptionState) {
            is SubscriptionState.Loading -> {
                LoadingState()
            }

            is SubscriptionState.Success -> {
                ActiveSubscriptionScreen(
                    plan = state.plan,
                    status = state.status,
                    onBack = onBack,
                )
            }

            is SubscriptionState.Paying -> {
                ProcessingScreen(
                    plan = state.plan,
                )
            }

            is SubscriptionState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.loadPlans() },
                    onBack = onBack,
                )
            }

            is SubscriptionState.Idle -> {
                // First screen entry — show loading while plans are fetched.
                // loadPlans() is called by LaunchedEffect(Unit) at screen entry.
                LoadingState()
            }

            is SubscriptionState.PlanSelection -> {
                PlanSelectionScreen(
                    plans = state.plans,
                    onPlanSelected = { plan ->
                        viewModel.selectPlan(plan)
                        viewModel.processPayment()
                    },
                )
            }
        }
    }
}

/**
 * Loading spinner — shown while plans are being fetched.
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = BrandColors.Accent,
            strokeWidth = 4.dp,
        )
    }
}

/**
 * Plan selection screen — shows available subscription plans.
 */
@Composable
private fun PlanSelectionScreen(
    plans: List<SubscriptionPlan>,
    onPlanSelected: (SubscriptionPlan) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header text
        Text(
            text = "Choose Your Plan",
            style = MaterialTheme.typography.headlineMedium,
            color = BrandColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = "Support the ministry and get exclusive content",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Plan cards
        plans.forEach { plan ->
            PlanCard(
                plan = plan,
                onSelect = { onPlanSelected(plan) },
            )
        }
    }
}

/**
 * Single plan card — displays plan details and select button.
 */
@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    onSelect: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .trackFocus("plan_${plan.id}")
                .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandColors.TextPrimary,
                    )
                    Text(
                        text = plan.billingCycle,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandColors.TextSecondary,
                    )
                }

                Text(
                    text = "$${plan.price}/month",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandColors.Accent,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Benefits
            plan.benefits.forEach { benefit ->
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\u2713",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandColors.Success,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = benefit,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandColors.TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Select button
            Button(
                onClick = onSelect,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .trackFocus("select_${plan.id}"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Accent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "Select Plan",
                    color = BrandColors.Background,
                )
            }
        }
    }
}

/**
 * Active subscription screen — shows current subscription status.
 */
@Composable
private fun ActiveSubscriptionScreen(
    plan: SubscriptionPlan,
    status: String,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Success icon
        Text(
            text = "\u2713",
            style = MaterialTheme.typography.displayLarge,
            color = BrandColors.Success,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Text(
            text = "You're Subscribed!",
            style = MaterialTheme.typography.headlineMedium,
            color = BrandColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = "${plan.name} - $${plan.price}/month",
            style = MaterialTheme.typography.titleLarge,
            color = BrandColors.Accent,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Text(
            text = "Status: $status",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextSecondary,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        Button(
            onClick = onBack,
            modifier = Modifier.trackFocus("subscription_done"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Accent),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "Done",
                color = BrandColors.Background,
            )
        }
    }
}

/**
 * Processing screen — shows payment in progress.
 */
@Composable
private fun ProcessingScreen(plan: SubscriptionPlan) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = BrandColors.Accent,
            strokeWidth = 4.dp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Processing Payment...",
            style = MaterialTheme.typography.titleLarge,
            color = BrandColors.TextPrimary,
        )

        Text(
            text = "${plan.name} - $${plan.price}/month",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * Error screen — shows error message with retry option.
 */
@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Payment Failed",
            style = MaterialTheme.typography.headlineMedium,
            color = BrandColors.Error,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(
                onClick = onRetry,
                modifier = Modifier.trackFocus("retry_payment"),
            ) {
                Text("Retry", color = BrandColors.Accent)
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier.trackFocus("cancel_payment"),
            ) {
                Text("Cancel", color = BrandColors.TextSecondary)
            }
        }
    }
}
