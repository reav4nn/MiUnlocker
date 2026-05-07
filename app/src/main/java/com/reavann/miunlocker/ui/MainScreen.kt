package com.reavann.miunlocker.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reavann.miunlocker.R
import com.reavann.miunlocker.data.AppSettings
import com.reavann.miunlocker.data.InstalledAppInfo
import com.reavann.miunlocker.data.formatSignedMillis
import com.reavann.miunlocker.ui.theme.MiUnlockerTheme
import java.util.Locale

@Composable
fun MainRoute(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.refreshSetupStatus()
    }
    var showAppPicker by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSetupStatus()
    }

    MainScreen(
        uiState = uiState,
        showAppPicker = showAppPicker,
        onOffsetSelected = viewModel::onOffsetSelected,
        onDailyEnabledChange = viewModel::onDailyEnabledChange,
        onShowAppPicker = {
            showAppPicker = true
            viewModel.refreshInstalledApps()
        },
        onBackFromAppPicker = { showAppPicker = false },
        onManualPackageInputChange = viewModel::onManualPackageInputChange,
        onManualPackageSubmitted = viewModel::onManualPackageSubmitted,
        onTargetPackageSelected = viewModel::onTargetPackageSelected,
        onClearTargetPackage = viewModel::onTargetPackageCleared,
        onOpenAccessibilitySettings = {
            context.openSettingsIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        onOpenExactAlarmSettings = {
            context.openExactAlarmSettings()
        },
        onOpenBatteryOptimizationSettings = {
            context.openSettingsIntent(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.refreshSetupStatus()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    showAppPicker: Boolean,
    onOffsetSelected: (Int) -> Unit,
    onDailyEnabledChange: (Boolean) -> Unit,
    onShowAppPicker: () -> Unit,
    onBackFromAppPicker: () -> Unit,
    onManualPackageInputChange: (String) -> Unit,
    onManualPackageSubmitted: () -> Unit,
    onTargetPackageSelected: (String) -> Unit,
    onClearTargetPackage: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (showAppPicker) {
        AppPickerScreen(
            uiState = uiState,
            onBack = onBackFromAppPicker,
            onManualPackageInputChange = onManualPackageInputChange,
            onManualPackageSubmitted = onManualPackageSubmitted,
            onTargetPackageSelected = onTargetPackageSelected,
            onClearTargetPackage = onClearTargetPackage,
            modifier = modifier,
        )
    } else {
        MainDashboardScreen(
            uiState = uiState,
            onOffsetSelected = onOffsetSelected,
            onDailyEnabledChange = onDailyEnabledChange,
            onShowAppPicker = onShowAppPicker,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenExactAlarmSettings = onOpenExactAlarmSettings,
            onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
            onRequestNotificationPermission = onRequestNotificationPermission,
            modifier = modifier,
        )
    }
}

@Composable
private fun MainDashboardScreen(
    uiState: MainUiState,
    onOffsetSelected: (Int) -> Unit,
    onDailyEnabledChange: (Boolean) -> Unit,
    onShowAppPicker: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
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
            StatusSection(
                uiState = uiState,
                onShowAppPicker = onShowAppPicker,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
                onRequestNotificationPermission = onRequestNotificationPermission,
            )
            TimingSection(
                settings = uiState.settings,
                onOffsetSelected = onOffsetSelected,
            )
            AutomationSection(
                uiState = uiState,
                onDailyEnabledChange = onDailyEnabledChange,
            )
            FutureActionsSection()
        }
    }
}

@Composable
private fun AppPickerScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onManualPackageInputChange: (String) -> Unit,
    onManualPackageSubmitted: () -> Unit,
    onTargetPackageSelected: (String) -> Unit,
    onClearTargetPackage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Select target app",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Choose Xiaomi Community or enter a launchable package manually.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onBack) {
                        Text(text = "Back")
                    }
                }
            }

            item {
                SectionCard(title = "Current Target") {
                    InfoRow(
                        label = "Selected target app",
                        value = uiState.selectedTargetAppText,
                        supportingText = "The selected package will be used by later launch and Accessibility phases.",
                    )
                }
            }

            item {
                SectionCard(title = "Manual Package") {
                    OutlinedTextField(
                        value = uiState.manualPackageInput,
                        onValueChange = onManualPackageInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Package name") },
                        placeholder = { Text(text = "com.example.app") },
                        singleLine = true,
                        isError = uiState.targetPackageError != null,
                    )
                    if (uiState.targetPackageError != null) {
                        Text(
                            text = uiState.targetPackageError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = "Manual entries must be installed, launchable Android packages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onManualPackageSubmitted,
                            enabled = uiState.manualPackageInput.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Save package")
                        }
                        OutlinedButton(
                            onClick = onClearTargetPackage,
                            enabled = uiState.settings.targetPackage.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Clear")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Installed Apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Only launchable apps are shown. Use manual input if the target is not listed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "App names stay on this device and are used only for target selection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                uiState.isAppListLoading -> item {
                    Text(
                        text = "Loading installed apps...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                uiState.installedApps.isEmpty() -> item {
                    Text(
                        text = "No launchable apps were found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> items(
                    items = uiState.installedApps,
                    key = { app -> app.packageName },
                ) { app ->
                    InstalledAppRow(
                        app = app,
                        selected = app.packageName == uiState.settings.targetPackage,
                        onSelected = {
                            onTargetPackageSelected(app.packageName)
                            onBack()
                        },
                    )
                }
            }
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
                text = "Phase 5: exact alarm and foreground service",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Daily alarms can start the foreground countdown and launch the target app. Accessibility tap execution, calibration, and logs are still later phases.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatusSection(
    uiState: MainUiState,
    onShowAppPicker: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    SectionCard(title = "Setup Status") {
        StatusActionRow(
            label = "Accessibility service",
            value = uiState.accessibilityStatusText,
            supportingText = "Open Android Accessibility settings. The MiUnlocker service itself is implemented in Phase 6.",
            actionLabel = "Open Accessibility Settings",
            onAction = onOpenAccessibilitySettings,
        )
        HorizontalDivider()
        StatusActionRow(
            label = "Exact alarm permission",
            value = uiState.exactAlarmStatusText,
            supportingText = "Required on Android 12+ to schedule the daily foreground countdown alarm.",
            actionLabel = "Open Exact Alarm Settings",
            actionEnabled = uiState.setupStatus.exactAlarmPermissionRequired,
            onAction = onOpenExactAlarmSettings,
        )
        HorizontalDivider()
        StatusActionRow(
            label = "Battery optimization",
            value = uiState.batteryOptimizationStatusText,
            supportingText = "Unrestricted battery behavior improves alarm and foreground-service reliability later.",
            actionLabel = "Open Battery Settings",
            onAction = onOpenBatteryOptimizationSettings,
        )
        HorizontalDivider()
        StatusActionRow(
            label = "Notifications",
            value = uiState.notificationStatusText,
            supportingText = "Android 13+ requires notification permission before the Phase 5 foreground status can be visible.",
            actionLabel = "Request Notification Permission",
            actionEnabled = uiState.setupStatus.notificationPermissionRequired &&
                !uiState.setupStatus.notificationPermissionGranted,
            onAction = onRequestNotificationPermission,
        )
        HorizontalDivider()
        StatusActionRow(
            label = "Selected target app",
            value = uiState.selectedTargetAppText,
            supportingText = "The foreground service opens this package 5 seconds before the configured tap time.",
            actionLabel = if (uiState.settings.targetPackage.isBlank()) {
                "Select Target App"
            } else {
                "Change Target App"
            },
            onAction = onShowAppPicker,
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
            supportingText = "Offset is applied to the base target time before scheduling.",
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
                enabled = uiState.canChangeDailyAutomation,
            )
        }
        if (uiState.scheduleState.schedulingError != null) {
            Text(
                text = uiState.scheduleState.schedulingError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        HorizontalDivider()
        InfoRow(
            label = "Next tap time",
            value = uiState.nextTapTimeText,
            supportingText = "This is the final target time after applying the stored offset.",
        )
        HorizontalDivider()
        InfoRow(
            label = "Foreground service starts",
            value = uiState.foregroundStartTimeText,
            supportingText = "The selected target app is launched 5 seconds before tap time to reduce app-open delay.",
        )
        Text(
            text = uiState.dailyAutomationSupportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FutureActionsSection() {
    SectionCard(title = "Later Actions") {
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
private fun InstalledAppRow(
    app: InstalledAppInfo,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            OutlinedButton(
                onClick = onSelected,
                enabled = !selected,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (selected) "Selected" else "Select")
            }
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
private fun StatusActionRow(
    label: String,
    value: String,
    supportingText: String,
    actionLabel: String,
    onAction: () -> Unit,
    actionEnabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoRow(
            label = label,
            value = value,
            supportingText = supportingText,
        )
        OutlinedButton(
            onClick = onAction,
            enabled = actionEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = actionLabel)
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

private fun Context.openExactAlarmSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:$packageName"),
        )
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    }
    openSettingsIntent(intent)
}

private fun Context.openSettingsIntent(intent: Intent) {
    val fallbackIntent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:$packageName"),
    )
    val targetIntent = intent.withSystemComponent(packageManager)
        ?: fallbackIntent.withSystemComponent(packageManager)
        ?: return

    runCatching { startActivity(targetIntent) }
}

private fun Intent.withSystemComponent(packageManager: PackageManager): Intent? {
    val activityInfo = packageManager.querySystemActivities(this)
        .firstOrNull()
        ?.activityInfo
        ?: return null

    return Intent(this).setComponent(
        ComponentName(activityInfo.packageName, activityInfo.name),
    )
}

private fun PackageManager.querySystemActivities(intent: Intent): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_SYSTEM_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, PackageManager.MATCH_SYSTEM_ONLY)
    }
}

private fun Float.formatRatio(): String = String.format(Locale.US, "%.2f", this)

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    MiUnlockerTheme {
        MainScreen(
            uiState = MainUiState(),
            showAppPicker = false,
            onOffsetSelected = {},
            onDailyEnabledChange = {},
            onShowAppPicker = {},
            onBackFromAppPicker = {},
            onManualPackageInputChange = {},
            onManualPackageSubmitted = {},
            onTargetPackageSelected = {},
            onClearTargetPackage = {},
            onOpenAccessibilitySettings = {},
            onOpenExactAlarmSettings = {},
            onOpenBatteryOptimizationSettings = {},
            onRequestNotificationPermission = {},
        )
    }
}
