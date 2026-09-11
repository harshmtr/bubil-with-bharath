package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.ViolationItem
import com.example.domain.models.ViolationSeverity
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleRedLight
import com.example.ui.theme.GoogleYellow

@Composable
fun ViolationCard(
    violation: ViolationItem,
    modifier: Modifier = Modifier
) {
    val severityColor = when (violation.severity) {
        ViolationSeverity.CRITICAL -> GoogleRed
        ViolationSeverity.MAJOR -> GoogleYellow
        ViolationSeverity.MINOR -> Color(0xFF1A73E8)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("violation_card_${violation.ruleId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = GoogleRedLight.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, GoogleRed.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Violation Icon",
                        tint = GoogleRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = violation.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = violation.severity.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(severityColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = violation.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!violation.detectedValue.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Detected: ${violation.detectedValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoogleRed
                )
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    title: String,
    subtitle: String,
    value: String?,
    isPresent: Boolean,
    isMandatory: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPresent) Color(0xFFF6FAF7) else Color(0xFFFFF7F6)
        ),
        border = BorderStroke(
            1.dp,
            if (isPresent) GoogleGreen.copy(alpha = 0.25f) else GoogleRed.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (isPresent) "Verified" else "Missing",
                tint = if (isPresent) GoogleGreen else GoogleRed,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isMandatory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MANDATORY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GoogleRed
                        )
                    }
                }
                Text(
                    text = if (isPresent && !value.isNullOrBlank()) value else subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPresent) MaterialTheme.colorScheme.onSurfaceVariant else GoogleRed
                )
            }
        }
    }
}
