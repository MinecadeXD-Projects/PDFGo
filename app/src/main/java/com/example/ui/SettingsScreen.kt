package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PageSpacing
import com.example.ui.theme.AppThemeSetting
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.PrimaryGradient

@Composable
fun SettingsScreen(
  currentTheme: AppThemeSetting,
  pageSpacing: PageSpacing,
  keepScreenAwake: Boolean,
  saveReadingPosition: Boolean,
  onBack: () -> Unit,
  onThemeChange: (AppThemeSetting) -> Unit,
  onPageSpacingChange: (PageSpacing) -> Unit,
  onToggleKeepAwake: () -> Unit,
  onToggleSavePosition: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  var isSpacingDropdownOpen by remember { mutableStateOf(false) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(scrollState),
  ) {
    // Header
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("btn_settings_back"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onBackground,
        )
      }
      Text(
        text = "Settings",
        style =
          MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          ),
      )
    }

    HorizontalDivider(
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
      thickness = 1.dp,
    )
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(2.dp)
          .background(PrimaryGradient),
    )

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      // SECTION 1: APPEARANCE
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "APPEARANCE",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp,
            ),
        )

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surface)
              .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp),
              )
              .padding(14.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Theme",
              style =
                MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                ),
            )

            // Segmented Theme Control (System, Light, Dark)
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              ThemeOptionButton(
                label = "System",
                isSelected = currentTheme == AppThemeSetting.SYSTEM,
                onClick = { onThemeChange(AppThemeSetting.SYSTEM) },
                modifier = Modifier.weight(1f).testTag("theme_btn_system"),
              )
              ThemeOptionButton(
                label = "Light",
                isSelected = currentTheme == AppThemeSetting.LIGHT,
                onClick = { onThemeChange(AppThemeSetting.LIGHT) },
                modifier = Modifier.weight(1f).testTag("theme_btn_light"),
              )
              ThemeOptionButton(
                label = "Dark",
                isSelected = currentTheme == AppThemeSetting.DARK,
                onClick = { onThemeChange(AppThemeSetting.DARK) },
                modifier = Modifier.weight(1f).testTag("theme_btn_dark"),
              )
            }
          }
        }
      }

      // SECTION 2: READER CONTROLS
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "READER",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp,
            ),
        )

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surface)
              .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp),
              ),
        ) {
          // Page Spacing
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "Page Spacing",
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                  ),
              )
              Text(
                text = "Gap between consecutive document pages",
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                  ),
              )
            }

            Box {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                      1.dp,
                      MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                      RoundedCornerShape(8.dp),
                    )
                    .clickable { isSpacingDropdownOpen = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("select_spacing_mode"),
              ) {
                Text(
                  text = pageSpacing.label,
                  style =
                    MaterialTheme.typography.bodySmall.copy(
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontSize = 11.sp,
                    ),
                )
              }

              DropdownMenu(
                expanded = isSpacingDropdownOpen,
                onDismissRequest = { isSpacingDropdownOpen = false },
              ) {
                PageSpacing.entries.forEach { sp ->
                  DropdownMenuItem(
                    text = { Text(sp.label) },
                    onClick = {
                      isSpacingDropdownOpen = false
                      onPageSpacingChange(sp)
                    },
                  )
                }
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

          // Keep Screen Awake
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "Keep Screen Awake",
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                  ),
              )
              Text(
                text = "Prevent display sleep during active reading",
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                  ),
              )
            }

            Switch(
              checked = keepScreenAwake,
              onCheckedChange = { onToggleKeepAwake() },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = BrandBlue,
                ),
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

          // Save Reading Position
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "Save Reading Position",
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                  ),
              )
              Text(
                text = "Resume from current page during active session",
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                  ),
              )
            }

            Switch(
              checked = saveReadingPosition,
              onCheckedChange = { onToggleSavePosition() },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = BrandBlue,
                ),
            )
          }
        }
      }

      // SECTION 3: ABOUT
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "ABOUT",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp,
            ),
        )

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surface)
              .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp),
              )
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier =
                Modifier.size(38.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(BrandBlue)
                  .shadow(2.dp),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
              )
            }
            Column {
              Text(
                text = "PDFGo",
                style =
                  MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  ),
              )
              Text(
                text = "Developed by Minecade",
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                  ),
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "App Version",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp,
                ),
            )
            Text(
              text = "1.0.0",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp,
                ),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ThemeOptionButton(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .shadow(if (isSelected) 3.dp else 0.dp, RoundedCornerShape(8.dp), spotColor = BrandBlue)
        .clip(RoundedCornerShape(8.dp))
        .then(
          if (isSelected) Modifier.background(PrimaryGradient)
          else Modifier.background(Color.Transparent)
        )
        .clickable { onClick() }
        .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style =
        MaterialTheme.typography.labelMedium.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
        ),
    )
  }
}
