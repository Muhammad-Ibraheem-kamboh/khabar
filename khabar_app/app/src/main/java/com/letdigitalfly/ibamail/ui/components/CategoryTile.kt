package com.letdigitalfly.ibamail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Festival
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letdigitalfly.ibamail.domain.model.MailCategory
import com.letdigitalfly.ibamail.ui.theme.SquircleIconShape

@Composable
fun CategoryTile(
    category: MailCategory,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val icon: ImageVector = when (category) {
        MailCategory.CLASS_CHANGE -> Icons.Rounded.EventBusy
        MailCategory.EXAM -> Icons.Rounded.EditCalendar
        MailCategory.DEADLINE -> Icons.Rounded.Assignment
        MailCategory.FEE -> Icons.Rounded.AccountBalance
        MailCategory.REGISTRATION -> Icons.Rounded.HowToReg
        MailCategory.RESULT -> Icons.Rounded.Grade
        MailCategory.ATTENDANCE -> Icons.Rounded.Warning
        MailCategory.CAREER -> Icons.Rounded.Work
        MailCategory.EVENT -> Icons.Rounded.Festival
        MailCategory.GENERAL -> Icons.Rounded.Campaign
    }

    Box(
        modifier = modifier
            .size(size)
            .background(color = Color(category.colorHex), shape = SquircleIconShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.displayName,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}
