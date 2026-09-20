package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SavedDocumentStatus
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.DangerGradient
import com.example.ui.theme.Emerald400
import com.example.ui.theme.HeroBadgeGradient
import com.example.ui.theme.HeroTextGradient
import com.example.ui.theme.PrimaryGradient
import com.example.ui.theme.Rose500

@Composable
fun HomeScreen(
  urlInput: String,
  urlError: String?,
  savedDocument: SavedDocumentStatus? = null,
  onUrlChange: (String) -> Unit,
  onPaste: (String) -> Unit,
  onOpenPdf: () -> Unit,
  onResumeSaved: () -> Unit = {},
  onRemovePdf: () -> Unit = {},
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
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp), spotColor = BrandBlue)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryGradient),
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
            .background(HeroBadgeGradient)
            .border(
              width = 1.dp,
              brush = Brush.horizontalGradient(listOf(BrandBlue.copy(alpha = 0.35f), BrandIndigo.copy(alpha = 0.3f))),
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
              .background(PrimaryGradient)
        )
        Text(
          text = "One active document at a time",
          style =
            MaterialTheme.typography.labelMedium.copy(
              color = BrandBlue,
              fontWeight = FontWeight.SemiBold,
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
          TextStyle(
            brush = HeroTextGradient,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            fontSize = 36.sp,
            lineHeight = 42.sp,
          ),
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Subtitle
      Text(
        text =
          if (savedDocument != null) {
            "An active PDF is loaded in your session. Resume reading where you left off or remove it to load a new PDF."
          } else {
            "Paste any direct PDF link to start a focused, distraction-free reading session."
          },
        style =
          MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
          ),
      )

      Spacer(modifier = Modifier.height(24.dp))

      if (savedDocument != null) {
        // When PDF is active: Hide link input completely, show resume card in its place
        Text(
          text = "ACTIVE DOCUMENT",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp,
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
          onClick = onResumeSaved,
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier =
            Modifier.fillMaxWidth()
              .border(1.2.dp, CardBorderGradient, RoundedCornerShape(16.dp))
              .testTag("card_resume_pdf"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f).padding(end = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            val thumbnailRatio = savedDocument.thumbnailBitmap?.let {
              it.width.toFloat() / it.height.toFloat()
            } ?: 0.707f // Default A4

            Box(
                modifier =
                  Modifier
                    .width(50.dp)
                    .aspectRatio(thumbnailRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(BrandBlue.copy(alpha = 0.16f), BrandIndigo.copy(alpha = 0.12f))))
                    .border(1.dp, BrandBlue.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
              ) {
                if (savedDocument.thumbnailBitmap != null) {
                  Image(
                    bitmap = savedDocument.thumbnailBitmap.asImageBitmap(),
                    contentDescription = "Page 1 Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Column {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "CONTINUE READING",
                    style =
                      MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp,
                      ),
                  )
                  Box(
                    modifier =
                      Modifier.size(4.dp)
                        .clip(CircleShape)
                        .background(Emerald400),
                  )
                  Text(
                    text = "Page ${savedDocument.page} of ${savedDocument.totalPages}",
                    style =
                      MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                      ),
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = savedDocument.title,
                  style =
                    MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                    ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Box(
              modifier =
                Modifier.shadow(4.dp, RoundedCornerShape(12.dp), spotColor = BrandBlue)
                  .clip(RoundedCornerShape(12.dp))
                  .background(PrimaryGradient)
                  .clickable { onResumeSaved() }
                  .padding(horizontal = 16.dp, vertical = 10.dp)
                  .testTag("btn_resume_pdf"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "Resume",
                style =
                  MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                  ),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // In place of the Open PDF button: Remove PDF button
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(52.dp)
              .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), spotColor = Rose500.copy(alpha = 0.5f))
              .clip(RoundedCornerShape(16.dp))
              .background(DangerGradient)
              .clickable { onRemovePdf() }
              .testTag("btn_remove_pdf_home"),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Remove PDF",
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 15.sp,
                  color = Color.White,
                ),
            )
          }
        }
      } else {
        // When no active PDF: Show URL link input and Open PDF button
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
                  onPaste("")
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
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(52.dp)
              .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), spotColor = BrandBlue.copy(alpha = 0.5f))
              .clip(RoundedCornerShape(16.dp))
              .background(PrimaryGradient)
              .clickable { onOpenPdf() }
              .testTag("btn_open_pdf"),
          contentAlignment = Alignment.Center,
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
                  color = Color.White,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp),
            )
          }
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
