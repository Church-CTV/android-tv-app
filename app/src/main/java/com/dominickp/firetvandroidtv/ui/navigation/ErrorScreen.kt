package com.dominickp.firetvandroidtv.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dominickp.firetvandroidtv.ui.focus.trackFocus
import com.dominickp.firetvandroidtv.ui.theme.BrandColors

/**
 * ErrorScreen — shows error state with retry button.
 *
 * Used when feed fails to load or network error occurs.
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            // Error icon
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = BrandColors.Accent,
                strokeWidth = 4.dp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error title — matches Roku's errorTitle: red #FF4444 ExtraBoldSystemFont
            Text(
                text = "Connection Error",
                style = MaterialTheme.typography.headlineMedium,
                color = BrandColors.Error,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Retry button
            Button(
                onClick = onRetry,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .trackFocus("retry_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Accent),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandColors.Background,
                )
            }
        }
    }
}
