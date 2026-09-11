package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
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
import com.example.domain.models.ComplianceStatus
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleGreenLight
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleRedLight
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.GoogleYellowLight

@Composable
fun StatusChip(
    status: ComplianceStatus,
    modifier: Modifier = Modifier,
    customText: String? = null
) {
    val (bgColor, contentColor, icon, defaultText) = when (status) {
        ComplianceStatus.COMPLIANT -> Quad(
            GoogleGreenLight,
            GoogleGreen,
            Icons.Default.CheckCircle,
            "Compliant"
        )
        ComplianceStatus.WARNING -> Quad(
            GoogleYellowLight,
            Color(0xFFB06000),
            Icons.Default.Warning,
            "Warning"
        )
        ComplianceStatus.VIOLATION -> Quad(
            GoogleRedLight,
            GoogleRed,
            Icons.Default.Error,
            "Violation"
        )
    }

    Row(
        modifier = modifier
            .testTag("status_chip")
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = customText ?: defaultText,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
