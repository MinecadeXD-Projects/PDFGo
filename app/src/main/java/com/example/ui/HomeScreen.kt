package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.Rose500

@Composable
fun HomeScreen(
  urlInput: String,
  urlError: String?,
  onUrlChange: (String) -> Unit,
  onPaste: (String) -> Unit,
  onOpenPdf: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val clipboardManager = LocalClipboardManager.current
  val scrollState = rememberScrollState()

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 24.dp)
        .verticalScroll(scrollState),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Top Bar
    Column(modifier = Modifier.fillMaxWidth()) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier =
              Modifier.size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandBlue)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Description,
              contentDescription = "PDFGo Logo",
              tint = Color.White,
              modifier = Modifier.size(20.dp),
            )
          }
          Text(
            text = "PDFGo",
            style =
              MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground,
              ),
          )
        }

        IconButton(
          onClick = onOpenSettings,
          modifier =
            Modifier.size(44.dp)
              .clip(CircleShape)
              .testTag("btn_settings"),
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }

    // Center Content
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(vertical = 32.dp),
      horizontalAlignment = Alignment.Start,
    ) {
      // Badge
      Row(
        modifier =
          Modifier.clip(RoundedCornerShape(50.dp))
            .background(BrandBlue.copy(alpha = 0.12f))
            .border(
              width = 1.dp,
              color = BrandBlue.copy(alpha = 0.25f),
              shape = RoundedCornerShape(50.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.size(6.dp)
              .clip(CircleShape)
              .background(BrandBlue)
        )
        Text(
          text = "One active document at a time",
          style =
            MaterialTheme.typography.labelMedium.copy(
              color = BrandBlue,
              fontWeight = FontWeight.Medium,
              fontSize = 12.sp,
            ),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Headline
      Text(
        text = "Read. Download.",
        style =
          MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            color = MaterialTheme.colorScheme.onBackground,
          ),
      )
      Text(
        text = "Move on.",
        style =
          MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            color = BrandBlue,
          ),
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Subtitle
      Text(
        text = "Paste any direct PDF link to start a focused, distraction-free reading session.",
        style =
          MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
          ),
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Label
      Text(
        text = "DOCUMENT WEB LINK",
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
          ),
      )

      Spacer(modifier = Modifier.height(8.dp))

      // URL Input with Link icon and Paste button
      OutlinedTextField(
        value = urlInput,
        onValueChange = onUrlChange,
        placeholder = {
          Text(
            text = "Paste PDF link (e.g., https://.../paper.pdf)",
            style =
              MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 13.sp,
              ),
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        },
        trailingIcon = {
          TextButton(
            onClick = {
              val clip = clipboardManager.getText()?.text
              if (!clip.isNullOrBlank()) {
                onPaste(clip)
              } else {
                onPaste("https://arxiv.org/pdf/2402.quantum_mechanics.pdf")
              }
            },
            modifier =
              Modifier.padding(end = 4.dp)
                .testTag("btn_paste"),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = "Paste",
              style =
                MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = BrandBlue,
                ),
            )
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
          ),
        modifier =
          Modifier.fillMaxWidth()
            .testTag("pdf_url_input"),
      )

      // Invalid URL Error Banner
      if (urlError != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(Rose500.copy(alpha = 0.12f))
              .border(
                width = 1.dp,
                color = Rose500.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp),
              )
              .padding(12.dp),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = Rose500,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
          )
          Column {
            Text(
              text = urlError,
              style =
                MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = Rose500,
                ),
            )
            Text(
              text = "Link must start with http:// or https:// and point to a readable PDF document.",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  color = Rose500.copy(alpha = 0.85f),
                  fontSize = 11.sp,
                ),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Primary Button: Open PDF
      Button(
        onClick = onOpenPdf,
        modifier =
          Modifier.fillMaxWidth()
            .height(52.dp)
            .testTag("btn_open_pdf"),
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = BrandBlue,
            contentColor = Color.White,
          ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Text(
            text = "Open PDF",
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
              ),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    // Developer attribution footer
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        thickness = 1.dp,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "PDFGo • Developed by Minecade",
        style =
          MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 12.sp,
          ),
      )
    }
  }
}
