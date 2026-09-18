package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ToastMessage
import com.example.model.ToastType
import com.example.ui.theme.Amber400
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun ToastPill(
  toastMessage: ToastMessage?,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize().padding(bottom = 48.dp, start = 20.dp, end = 20.dp),
    contentAlignment = Alignment.BottomCenter,
  ) {
    AnimatedVisibility(
      visible = toastMessage != null,
      enter = slideInVertically { it / 2 } + fadeIn(),
      exit = slideOutVertically { it / 2 } + fadeOut(),
    ) {
      if (toastMessage != null) {
        Surface(
          shape = RoundedCornerShape(50.dp),
          color = Slate900,
          contentColor = Color.White,
          shadowElevation = 10.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val icon =
              when (toastMessage.type) {
                ToastType.SUCCESS -> Icons.Default.CheckCircle
                ToastType.WARNING -> Icons.Default.Warning
                ToastType.INFO -> Icons.Default.Info
              }
            val iconTint =
              when (toastMessage.type) {
                ToastType.SUCCESS -> Emerald400
                ToastType.WARNING -> Amber400
                ToastType.INFO -> BrandBlue
              }

            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = iconTint,
              modifier = Modifier.size(16.dp),
            )

            Text(
              text = toastMessage.text,
              style =
                MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp,
                  color = Color.White,
                ),
            )
          }
        }
      }
    }
  }
}
