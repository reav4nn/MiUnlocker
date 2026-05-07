package com.reavann.miunlocker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reavann.miunlocker.R
import com.reavann.miunlocker.data.AppSettings
import com.reavann.miunlocker.data.formatSignedMillis
import com.reavann.miunlocker.ui.theme.MiUnlockerTheme
import java.util.Locale

@Composable
fun MainRoute(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreen(
        uiState = uiState,
        onOffsetSelected = viewModel::onOffsetSelected,
        onDailyEnabledChange = viewModel::onDailyEnabledChange,
        modifier = modifier,
    )
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    onOffsetSelected: (Int) -> Unit,
    onDailyEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderCard()
            PhaseNoticeCard()
            StatusSection(uiState = uiState)
            TimingSection(
                settings = uiState.settings,
                onOffsetSelected = onOffsetSelected,
            )
            AutomationSection(
                uiState = uiState,
                onDailyEnabledChange = onDailyEnabledChange,
            )
            PlaceholderActionsSection()
        }
    }
}

@Composable
private fun HeaderCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.miunlocker_logo),
                contentDescription = stringResource(id = R.string.logo_content_description),
                modifier = Modifier.size(72.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "MiUnlocker",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = R.string.unofficial_notice),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhaseNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Phase 3: settings and UI only",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "No alarm, AccessibilityService, app launch, or tap automation is active yet.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatusSection(uiState: MainUiState) {
    SectionCard(title = "Setup Status") {
        InfoRow(
            label = "Accessibility service",
            value = "Not checked yet",
            supportingText = "Status check is planned for Phase 4.",
        )
        HorizontalDivider()
        InfoRow(
            label = "Exact alarm permission",
            value = "Not checked yet",
            supportingText = "Permission guidance is planned for Phase 4.",
        )
        HorizontalDivider()
        InfoRow(
            label = "Battery optimization",
            value = "Not checked yet",
            supportingText = "Battery settings guidance is planned for Phase 4.",
        )
        HorizontalDivider()
        InfoRow(
            label = "Selected target app",
            value = uiState.selectedTargetAppText,
            supportingText = "App picker and manual package input are planned for Phase 4.",
        )
    }
}

@Composable
private fun TimingSection(
    settings: AppSettings,
    onOffsetSelected: (Int) -> Unit,
) {
    SectionCard(title = "Timing") {
        InfoRow(
            label = "Base target time",
            value = "${settings.targetTimeText} Asia/Baku",
            supportingText = "Default recommendation is 19:59:59.800.",
        )
        HorizontalDivider()
        InfoRow(
            label = "Stored offset",
            value = formatSignedMillis(settings.offsetMillis),
            supportingText = "Final effective-time calculation is implemented in the scheduler phase.",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSettings.ALLOWED_OFFSETS_MILLIS.forEach { offset ->
                FilterChip(
                    selected = settings.offsetMillis == offset,
                    onClick = { onOffsetSelected(offset) },
                    label = { Text(text = formatSignedMillis(offset)) },
                )
            }
        }
        HorizontalDivider()
        InfoRow(
            label = "Fallback tap position",
            value = "x=${settings.tapXRatio.formatRatio()}, y=${settings.tapYRatio.formatRatio()}",
            supportingText = "Calibration screen is planned for Phase 7. Defaults are x=0.50, y=0.89.",
        )
    }
}

@Composable
private fun AutomationSection(
    uiState: MainUiState,
    onDailyEnabledChange: (Boolean) -> Unit,
) {
    SectionCard(title = "Daily Automation") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Arm daily automation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.dailyAutomationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = uiState.settings.dailyEnabled,
                onCheckedChange = onDailyEnabledChange,
            )
        }
        Text(
            text = "This toggle is persisted now. Actual scheduling starts in Phase 5.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaceholderActionsSection() {
    SectionCard(title = "Actions") {
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Select target app - Phase 4")
        }
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Calibrate tap position - Phase 7")
        }
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Test now - Phase 7")
        }
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Logs - Phase 8")
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    supportingText: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Float.formatRatio(): String = String.format(Locale.US, "%.2f", this)

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    MiUnlockerTheme {
        MainScreen(
            uiState = MainUiState(),
            onOffsetSelected = {},
            onDailyEnabledChange = {},
        )
    }
}
