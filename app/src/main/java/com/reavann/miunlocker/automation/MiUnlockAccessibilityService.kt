package com.reavann.miunlocker.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.roundToInt

class MiUnlockAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        clearActiveService()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clearActiveService()
        super.onDestroy()
    }

    private fun executeTapCommand(
        targetPackage: String,
        xRatio: Float,
        yRatio: Float,
    ): TapExecutionResult {
        val root = rootInActiveWindow
            ?: return TapExecutionResult(
                title = "Tap skipped",
                text = "No active window was available to inspect.",
            )
        val activePackage = root.packageName?.toString().orEmpty()

        if (activePackage != targetPackage) {
            return TapExecutionResult(
                title = "Tap skipped",
                text = if (activePackage.isBlank()) {
                    "Active window package was unavailable, so no tap was sent."
                } else {
                    "Active app was $activePackage, not the selected target."
                },
            )
        }

        val matchingNode = findApplyNode(root)
        val clickableNode = matchingNode?.findClickableSelfOrParent()
        if (clickableNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
            return TapExecutionResult(
                title = "Tap executed",
                text = "Clicked the matching Apply for unlocking node.",
            )
        }

        return dispatchCoordinateFallback(xRatio, yRatio, matchingNode != null)
    }

    private fun findApplyNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val text = node.text?.toString().orEmpty()
        if (text.contains(APPLY_BUTTON_TEXT, ignoreCase = true)) {
            return node
        }

        repeat(node.childCount) { index ->
            val child = node.getChild(index) ?: return@repeat
            val matchingChild = findApplyNode(child)
            if (matchingChild != null) {
                return matchingChild
            }
        }

        return null
    }

    private fun AccessibilityNodeInfo.findClickableSelfOrParent(): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = this
        var depth = 0

        while (current != null && depth <= MAX_CLICKABLE_PARENT_DEPTH) {
            if (current.isClickable && current.isEnabled) {
                return current
            }
            current = current.parent
            depth += 1
        }

        return null
    }

    private fun dispatchCoordinateFallback(
        xRatio: Float,
        yRatio: Float,
        nodeWasFound: Boolean,
    ): TapExecutionResult {
        val bounds = displayBounds()
            ?: return TapExecutionResult(
                title = "Tap failed",
                text = "Display size was unavailable for coordinate fallback.",
            )

        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return TapExecutionResult(
                title = "Tap failed",
                text = "Display size was invalid for coordinate fallback.",
            )
        }

        val x = (bounds.left + bounds.width() * xRatio.coerceIn(0f, 1f))
            .coerceIn(bounds.left.toFloat(), (bounds.right - 1).toFloat())
        val y = (bounds.top + bounds.height() * yRatio.coerceIn(0f, 1f))
            .coerceIn(bounds.top.toFloat(), (bounds.bottom - 1).toFloat())
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, TAP_DURATION_MILLIS))
            .build()

        val dispatched = dispatchGesture(gesture, null, null)
        if (!dispatched) {
            return TapExecutionResult(
                title = "Tap failed",
                text = "Accessibility gesture dispatch was rejected.",
            )
        }

        val fallbackReason = if (nodeWasFound) {
            "Node click was unavailable"
        } else {
            "Button text was not found"
        }

        return TapExecutionResult(
            title = "Tap sent",
            text = "$fallbackReason; dispatched fallback tap at ${x.roundToInt()}, ${y.roundToInt()}.",
        )
    }

    private fun displayBounds(): Rect? {
        val windowManager = getSystemService(WindowManager::class.java) ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
    }

    private fun clearActiveService() {
        if (activeService === this) {
            activeService = null
        }
    }

    companion object {
        private const val APPLY_BUTTON_TEXT = "Apply for unlocking"
        private const val MAX_CLICKABLE_PARENT_DEPTH = 8
        private const val TAP_DURATION_MILLIS = 80L

        @Volatile
        private var activeService: MiUnlockAccessibilityService? = null

        fun executeTapCommand(
            targetPackage: String,
            xRatio: Float,
            yRatio: Float,
        ): TapExecutionResult {
            val safeTargetPackage = targetPackage.trim()
            if (safeTargetPackage.isBlank()) {
                return TapExecutionResult(
                    title = "Tap skipped",
                    text = "No selected target package was available.",
                )
            }

            val service = activeService
                ?: return TapExecutionResult(
                    title = "Tap skipped",
                    text = "MiUnlocker Accessibility service is not enabled.",
                )

            return service.executeTapCommand(
                targetPackage = safeTargetPackage,
                xRatio = xRatio,
                yRatio = yRatio,
            )
        }
    }
}

data class TapExecutionResult(
    val title: String,
    val text: String,
)
