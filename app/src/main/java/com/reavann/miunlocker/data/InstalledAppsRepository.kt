package com.reavann.miunlocker.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class InstalledAppsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun getLaunchableApps(): List<InstalledAppInfo> {
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launchIntent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launchIntent, 0)
        }

        return resolveInfos
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == appContext.packageName) return@mapNotNull null

                val label = resolveInfo
                    .loadLabel(packageManager)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                    .ifBlank { packageName }

                InstalledAppInfo(
                    label = label,
                    packageName = packageName,
                )
            }
            .distinctBy { app -> app.packageName }
            .sortedWith(
                compareBy<InstalledAppInfo, String>(String.CASE_INSENSITIVE_ORDER) { app -> app.label }
                    .thenBy { app -> app.packageName },
            )
    }
}
