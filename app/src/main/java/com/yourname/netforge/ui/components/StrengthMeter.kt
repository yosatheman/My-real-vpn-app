package com.yourname.netforge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.netforge.ui.theme.StatusAmber
import com.yourname.netforge.ui.theme.StatusGreen
import com.yourname.netforge.ui.theme.StatusRed
import com.yourname.netforge.ui.theme.VioletPrimary

enum class PasswordStrength(val label: String, val score: Float, val color: Color) {
    EMPTY("Too Short", 0f, Color.Gray),
    WEAK("Weak", 0.25f, StatusRed),
    FAIR("Fair", 0.5f, StatusAmber),
    GOOD("Good", 0.75f, VioletPrimary),
    STRONG("Strong", 1.0f, StatusGreen)
}

object PasswordStrengthEvaluator {
    fun evaluate(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.EMPTY
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when (score) {
            0, 1 -> PasswordStrength.WEAK
            2 -> PasswordStrength.FAIR
            3, 4 -> PasswordStrength.GOOD
            else -> PasswordStrength.STRONG
        }
    }
}

@Composable
fun StrengthMeter(
    password: String,
    modifier: Modifier = Modifier
) {
    val strength = PasswordStrengthEvaluator.evaluate(password)
    val animatedProgress by animateFloatAsState(targetValue = strength.score, label = "progress")
    val animatedColor by animateColorAsState(targetValue = strength.color, label = "color")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Passphrase Strength",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strength.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = animatedColor
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = animatedColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
