package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandBlue

@Composable
fun LoadingScreen(
  progress: Float,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "spin")
  val rotation by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1400, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "spin_angle",
    )

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
        .padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.width(300.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      // Circular loading graphic with PDF document icon inside
      Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          progress = { 1f },
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
          strokeWidth = 4.dp,
        )
        CircularProgressIndicator(
          modifier = Modifier.fillMaxSize().rotate(rotation),
          color = BrandBlue,
          strokeWidth = 4.dp,
        )
        Icon(
          imageVector = Icons.Default.Description,
          contentDescription = null,
          tint = BrandBlue,
          modifier = Modifier.size(28.dp),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Opening PDF…",
        style =
          MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          ),
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Preparing your document for distraction-free reading",
        style =
          MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
          ),
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Progress Bar
      LinearProgressIndicator(
        progress = { progress },
        modifier =
          Modifier.fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50.dp)),
        color = BrandBlue,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
      )

      Spacer(modifier = Modifier.height(28.dp))

      OutlinedButton(
        onClick = onCancel,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("btn_cancel_loading"),
      ) {
        Text(
          text = "Cancel",
          style =
            MaterialTheme.typography.labelMedium.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Medium,
            ),
        )
      }
    }
  }
}
