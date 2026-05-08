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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
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
import com.reavann.miunlocker.data.LogEntry
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
    var showCalibration by rememberSaveable { mutableStateOf(false) }
    var showLogs by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSetupStatus()
    }

    MainScreen(
        uiState = uiState,
        showAppPicker = showAppPicker,
        showCalibration = showCalibration,
        showLogs = showLogs,
        onOffsetSelected = viewModel::onOffsetSelected,
        onTapRatiosChanged = viewModel::onTapRatiosChanged,
        onTapRatiosReset = viewModel::onTapRatiosReset,
        onTestNow = viewModel::onTestNow,
        onDailyEnabledChange = viewModel::onDailyEnabledChange,
        onShowAppPicker = {
            showCalibration = false
            showLogs = false
            showAppPicker = true
            viewModel.refreshInstalledApps()
        },
        onBackFromAppPicker = { showAppPicker = false },
        onShowCalibration = {
            showAppPicker = false
            showLogs = false
            showCalibration = true
        },
        onBackFromCalibration = { showCalibration = false },
        onShowLogs = {
            showAppPicker = false
            showCalibration = false
            showLogs = true
            viewModel.refreshLogs()
        },
        onBackFromLogs = { showLogs = false },
        onClearLogs = viewModel::clearLogs,
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
    showCalibration: Boolean,
    showLogs: Boolean,
    onOffsetSelected: (Int) -> Unit,
    onTapRatiosChanged: (Float, Float) -> Unit,
    onTapRatiosReset: () -> Unit,
    onTestNow: () -> Unit,
    onDailyEnabledChange: (Boolean) -> Unit,
    onShowAppPicker: () -> Unit,
    onBackFromAppPicker: () -> Unit,
    onShowCalibration: () -> Unit,
    onBackFromCalibration: () -> Unit,
    onShowLogs: () -> Unit,
    onBackFromLogs: () -> Unit,
    onClearLogs: () -> Unit,
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
    } else if (showCalibration) {
        CalibrationScreen(
            settings = uiState.settings,
            onBack = onBackFromCalibration,
            onTapRatiosChanged = onTapRatiosChanged,
            onTapRatiosReset = onTapRatiosReset,
            modifier = modifier,
        )
    } else if (showLogs) {
        LogsScreen(
            logsState = uiState.logsState,
            onBack = onBackFromLogs,
            onClearLogs = onClearLogs,
            modifier = modifier,
        )
    } else {
        MainDashboardScreen(
            uiState = uiState,
            onOffsetSelected = onOffsetSelected,
            onDailyEnabledChange = onDailyEnabledChange,
            onShowAppPicker = onShowAppPicker,
            onShowCalibration = onShowCalibration,
            onShowLogs = onShowLogs,
            onTestNow = onTestNow,
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
    onShowCalibration: () -> Unit,
    onShowLogs: () -> Unit,
    onTestNow: () -> Unit,
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
            ActionsSection(
                uiState = uiState,
                onShowCalibration = onShowCalibration,
                onShowLogs = onShowLogs,
                onTestNow = onTestNow,
            )
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
private fun CalibrationScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onTapRatiosChanged: (Float, Float) -> Unit,
    onTapRatiosReset: () -> Unit,
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
                        text = "Calibrate tap position",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Set the fallback coordinate used when the Apply button node is unavailable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onBack) {
                    Text(text = "Back")
                }
            }

            SectionCard(title = "Fallback Coordinate") {
                InfoRow(
                    label = "Saved tap ratios",
                    value = "x=${settings.tapXRatio.formatRatio()}, y=${settings.tapYRatio.formatRatio()}",
                    supportingText = "Ratios are relative to the full display: x=0 is left, x=1 is right, y=0 is top, y=1 is bottom.",
                )
                CalibrationPreview(
                    xRatio = settings.tapXRatio,
                    yRatio = settings.tapYRatio,
                    onTapRatiosChanged = onTapRatiosChanged,
                )
                RatioSlider(
                    label = "Horizontal position",
                    value = settings.tapXRatio,
                    onValueChange = { xRatio ->
                        onTapRatiosChanged(xRatio, settings.tapYRatio)
                    },
                )
                RatioSlider(
                    label = "Vertical position",
                    value = settings.tapYRatio,
                    onValueChange = { yRatio ->
                        onTapRatiosChanged(settings.tapXRatio, yRatio)
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onTapRatiosReset,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "Reset defaults")
                    }
                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "Use position")
                    }
                }
                Text(
                    text = "Use Test now after calibration to verify the fallback point on the real target screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LogsScreen(
    logsState: LogsUiState,
    onBack: () -> Unit,
    onClearLogs: () -> Unit,
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
                        text = "Logs",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Recent automation events and tap results.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onBack) {
                    Text(text = "Back")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onClearLogs,
                    enabled = logsState.logs.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Clear logs")
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Done")
                }
            }

            if (logsState.isLoading) {
                Text(
                    text = "Loading logs...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (logsState.logs.isEmpty()) {
                Text(
                    text = "No logs yet. Run Test now or wait for a scheduled tap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                logsState.logs.asReversed().forEach { entry ->
                    LogEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = entry.eventTypeLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = entry.formattedTimestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.targetPackage.isNotBlank()) {
                Text(
                    text = "Package: ${entry.targetPackage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.scheduledTime != null && entry.actualTime != null) {
                Text(
                    text = "Scheduled: ${entry.scheduledTime} | Actual: ${entry.actualTime} | Delta: ${entry.deltaMillis}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.nodeFound != null) {
                Text(
                    text = "Node found: ${if (entry.nodeFound) "Yes" else "No"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.fallbackUsed != null) {
                Text(
                    text = "Fallback used: ${if (entry.fallbackUsed) "Yes" else "No"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.resultTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (entry.resultText.isNotBlank()) {
                Text(
                    text = entry.resultText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalibrationPreview(
    xRatio: Float,
    yRatio: Float,
    onTapRatiosChanged: (Float, Float) -> Unit,
) {
    val markerColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val safeX = xRatio.coerceIn(0f, 1f)
    val safeY = yRatio.coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (size.width > 0 && size.height > 0) {
                            onTapRatiosChanged(
                                (offset.x / size.width).coerceIn(0f, 1f),
                                (offset.y / size.height).coerceIn(0f, 1f),
                            )
                        }
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val x = size.width * safeX
                val y = size.height * safeY
                val thinStroke = 1.dp.toPx()
                val markerStroke = 2.dp.toPx()

                drawLine(
                    color = gridColor,
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = thinStroke,
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = thinStroke,
                )
                drawLine(
                    color = markerColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = markerStroke,
                )
                drawLine(
                    color = markerColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = markerStroke,
                )
                drawCircle(
                    color = markerColor,
                    radius = 8.dp.toPx(),
                    center = Offset(x, y),
                )
            }
            Text(
                text = "Tap preview to move marker",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RatioSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoRow(
            label = label,
            value = value.formatRatio(),
            supportingText = "Drag to adjust between 0.00 and 1.00.",
        )
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            valueRange = 0f..1f,
        )
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
            supportingText = "Enable the MiUnlocker tap service so scheduled commands can inspect and tap only the selected target app.",
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
            supportingText = "On HyperOS/MIUI: also enable Auto-start for MiUnlocker in system settings and lock it in recent tasks. Unrestricted battery and auto-start are both required for reliable alarms.",
            actionLabel = "Open Battery Settings",
            onAction = onOpenBatteryOptimizationSettings,
        )
        HorizontalDivider()
        StatusActionRow(
            label = "Notifications",
            value = uiState.notificationStatusText,
            supportingText = "Android 13+ requires notification permission before foreground countdown status can be visible.",
            actionLabel = "Request Notification Permission",
            actionEnabled = uiState.setupStatus.notificationPermissionRequired &&
                !uiState.setupStatus.notificationPermissionGranted,
            onAction = onRequestNotificationPermission,
        )
        HorizontalDivider()
        StatusActionRow(
            label = "Selected target app",
            value = uiState.selectedTargetAppText,
            supportingText = "The foreground service opens this package before the configured tap time and prepares the unlock page.",
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
            supportingText = "Used only after Apply for unlocking is visible but not directly clickable. Open calibration to adjust these ratios.",
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
            supportingText = "The selected target app is launched about 2 minutes before tap time so server-loaded unlock UI can appear.",
        )
        HorizontalDivider()
        InfoRow(
            label = "Unlock reminder",
            value = uiState.preWarningTimeText,
            supportingText = "Audible reminder to unlock the phone and keep the screen on about 4 minutes before tap time.",
        )
        Text(
            text = uiState.dailyAutomationSupportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionsSection(
    uiState: MainUiState,
    onShowCalibration: () -> Unit,
    onShowLogs: () -> Unit,
    onTestNow: () -> Unit,
) {
    SectionCard(title = "Actions") {
        OutlinedButton(
            onClick = onShowCalibration,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Calibrate tap position")
        }
        Button(
            onClick = onTestNow,
            enabled = uiState.canRunManualTest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = uiState.manualTestButtonText)
        }
        Text(
            text = uiState.manualTestSupportingText,
            style = MaterialTheme.typography.bodySmall,
            color = if (uiState.manualTestState.resultTitle == "Test skipped") {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        HorizontalDivider()
        OutlinedButton(
            onClick = onShowLogs,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "View logs")
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
            showCalibration = false,
            showLogs = false,
            onOffsetSelected = {},
            onTapRatiosChanged = { _, _ -> },
            onTapRatiosReset = {},
            onTestNow = {},
            onDailyEnabledChange = {},
            onShowAppPicker = {},
            onBackFromAppPicker = {},
            onShowCalibration = {},
            onBackFromCalibration = {},
            onShowLogs = {},
            onBackFromLogs = {},
            onClearLogs = {},
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
