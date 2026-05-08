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
import kotlin.random.Random
import kotlinx.coroutines.delay

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
        val root = activeRootForPackage(targetPackage) ?: return activePackageMismatchResult()

        val matchingNode = findApplyNode(root)
            ?: return TapExecutionResult(
                title = "Tap skipped",
                text = "Apply for unlocking was not visible, so no fallback tap was sent.",
                nodeFound = false,
            )
        val clickableNode = matchingNode.findClickableSelfOrParent()
        if (clickableNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
            return TapExecutionResult(
                title = "Tap executed",
                text = "Clicked the matching Apply for unlocking node.",
                nodeFound = true,
                fallbackUsed = false,
            )
        }

        return dispatchCoordinateFallback(xRatio, yRatio).copy(
            nodeFound = true,
            fallbackUsed = true,
        )
    }

    private suspend fun prepareUnlockPageCommand(
        targetPackage: String,
        timeoutMillis: Long,
    ): TapExecutionResult {
        val deadline = System.currentTimeMillis() + timeoutMillis.coerceAtLeast(0L)
        val root = waitForActiveRoot(targetPackage, deadline)
            ?: return activePackageMismatchResult()

        dismissNotificationPrompt(
            targetPackage = targetPackage,
            deadlineEpochMillis = deadline,
            waitForPrompt = true,
        )

        val currentRoot = activeRootForPackage(targetPackage) ?: root
        if (findApplyNode(currentRoot) != null) {
            return targetReadyResult("Apply for unlocking is already visible.")
        }

        if (findUnlockBootloaderNode(currentRoot) == null && !openMeTab(targetPackage, deadline)) {
            return TapExecutionResult(
                title = "Prepare skipped",
                text = "ME tab was not visible before the preparation timeout.",
            )
        }

        val unlockClicked = clickUnlockBootloaderWhenVisible(targetPackage, deadline)
        if (!unlockClicked) {
            return TapExecutionResult(
                title = "Prepare skipped",
                text = "Unlock bootloader was not visible before the preparation timeout.",
            )
        }

        return if (waitForApplyVisible(targetPackage, deadline)) {
            targetReadyResult("Unlock bootloader opened and Apply for unlocking is visible.")
        } else {
            TapExecutionResult(
                title = "Prepare incomplete",
                text = "Unlock bootloader was opened, but Apply for unlocking was not visible before timeout.",
            )
        }
    }

    private fun findApplyNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNode(node) { candidate ->
            candidate.hasVisibleTextMatching { it.contains(APPLY_BUTTON_TEXT, ignoreCase = true) }
        }
    }

    private fun findUnlockBootloaderNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNode(node) { candidate ->
            candidate.hasVisibleTextMatching { it.contains(UNLOCK_BOOTLOADER_TEXT, ignoreCase = true) }
        }
    }

    private fun findNoThanksNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNode(node) { candidate ->
            candidate.hasVisibleTextMatching { text ->
                NO_THANKS_TEXTS.any { label -> text.equals(label, ignoreCase = true) }
            }
        }
    }

    private fun findMeTabNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNode(node) { candidate ->
            candidate.hasVisibleTextMatching { text ->
                val normalized = text.trim()
                normalized.equals(ME_TAB_TEXT, ignoreCase = true) ||
                    normalized.equals("Me tab", ignoreCase = true) ||
                    normalized.equals("Profile", ignoreCase = true) ||
                    normalized.equals("Account", ignoreCase = true) ||
                    normalized.equals("I", ignoreCase = true) ||
                    normalized.startsWith("Me", ignoreCase = true)
            }
        }
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNode(node) { candidate -> candidate.isScrollable && candidate.isEnabled }
    }

    private fun findNode(
        node: AccessibilityNodeInfo,
        depth: Int = 0,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        if (depth >= MAX_NODE_SEARCH_DEPTH) return null

        repeat(node.childCount) { index ->
            val child = node.getChild(index) ?: return@repeat
            val matchingChild = findNode(child, depth + 1, predicate)
            if (matchingChild != null) {
                return matchingChild
            }
        }

        return null
    }

    private fun AccessibilityNodeInfo.hasVisibleTextMatching(
        predicate: (String) -> Boolean,
    ): Boolean {
        text?.toString()?.takeIf { it.isNotBlank() }?.let { if (predicate(it)) return true }
        contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { if (predicate(it)) return true }
        return false
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

    private suspend fun waitForActiveRoot(
        targetPackage: String,
        deadlineEpochMillis: Long,
    ): AccessibilityNodeInfo? {
        do {
            activeRootForPackage(targetPackage)?.let { root -> return root }
            delay(POLL_INTERVAL_MILLIS)
        } while (System.currentTimeMillis() <= deadlineEpochMillis)

        return activeRootForPackage(targetPackage)
    }

    private suspend fun dismissNotificationPrompt(
        targetPackage: String,
        deadlineEpochMillis: Long,
        waitForPrompt: Boolean = false,
    ) {
        val promptWaitMillis = if (waitForPrompt) OPTIONAL_PROMPT_WAIT_MILLIS else 0L
        val promptDeadline = (System.currentTimeMillis() + promptWaitMillis)
            .coerceAtMost(deadlineEpochMillis)

        do {
            val root = activeRootForPackage(targetPackage) ?: return
            val noThanksNode = findNoThanksNode(root)
            if (noThanksNode == null) {
                if (!waitForPrompt) return
                delay(POLL_INTERVAL_MILLIS)
                continue
            }

            noThanksNode.findClickableSelfOrParent()
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            delay(AFTER_CLICK_DELAY_MILLIS)
            return
        } while (System.currentTimeMillis() <= promptDeadline)
    }

    private suspend fun openMeTab(
        targetPackage: String,
        deadlineEpochMillis: Long,
    ): Boolean {
        do {
            val root = activeRootForPackage(targetPackage) ?: return false
            val meNode = findMeTabNode(root)
            if (meNode != null) {
                if (meNode.findClickableSelfOrParent()
                        ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
                ) {
                    delay(AFTER_CLICK_DELAY_MILLIS)
                    return true
                }

                if (dispatchTapAtNodeCenter(meNode)) {
                    delay(AFTER_CLICK_DELAY_MILLIS)
                    return true
                }

                if (dispatchTapAtRatioInRoot(root, ME_TAB_X_RATIO, ME_TAB_Y_RATIO)) {
                    delay(AFTER_CLICK_DELAY_MILLIS)
                    return true
                }
            } else {
                scrollForward(root)
            }

            delay(POLL_INTERVAL_MILLIS + pollJitter())
        } while (System.currentTimeMillis() <= deadlineEpochMillis)

        return false
    }

    private suspend fun clickUnlockBootloaderWhenVisible(
        targetPackage: String,
        deadlineEpochMillis: Long,
    ): Boolean {
        var pollCount = 0

        do {
            val root = activeRootForPackage(targetPackage) ?: return false
            if (findApplyNode(root) != null) return true

            val unlockNode = findUnlockBootloaderNode(root)
            if (unlockNode?.findClickableSelfOrParent()
                    ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            ) {
                delay(AFTER_CLICK_DELAY_MILLIS)
                return true
            }

            if (pollCount % SCROLL_EVERY_POLLS == 0) {
                scrollForward(root)
            }
            pollCount += 1
            delay(POLL_INTERVAL_MILLIS + pollJitter())
        } while (System.currentTimeMillis() <= deadlineEpochMillis)

        return false
    }

    private suspend fun waitForApplyVisible(
        targetPackage: String,
        deadlineEpochMillis: Long,
    ): Boolean {
        do {
            val root = activeRootForPackage(targetPackage) ?: return false
            if (findApplyNode(root) != null) return true
            delay(POLL_INTERVAL_MILLIS)
        } while (System.currentTimeMillis() <= deadlineEpochMillis)

        return isApplyVisible(targetPackage)
    }

    private fun isApplyVisible(targetPackage: String): Boolean {
        val root = activeRootForPackage(targetPackage) ?: return false
        return findApplyNode(root) != null
    }

    private fun isUnlockBootloaderVisible(targetPackage: String): Boolean {
        val root = activeRootForPackage(targetPackage) ?: return false
        return findUnlockBootloaderNode(root) != null
    }

    private fun scrollForward(root: AccessibilityNodeInfo): Boolean {
        val scrollableNode = findScrollableNode(root)
        if (scrollableNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true) {
            return true
        }

        return dispatchSwipeUp()
    }

    private fun dispatchCoordinateFallback(
        xRatio: Float,
        yRatio: Float,
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
        val dispatched = dispatchTapAtPoint(x, y)
        if (!dispatched) {
            return TapExecutionResult(
                title = "Tap failed",
                text = "Accessibility gesture dispatch was rejected.",
            )
        }

        val fallbackReason = "Node click was unavailable"

        return TapExecutionResult(
            title = "Tap sent",
            text = "$fallbackReason; dispatched fallback tap at ${x.roundToInt()}, ${y.roundToInt()}.",
        )
    }

    private fun dispatchTapAtRatioInRoot(
        root: AccessibilityNodeInfo,
        xRatio: Float,
        yRatio: Float,
    ): Boolean {
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val bounds = if (rootBounds.width() > 0 && rootBounds.height() > 0) {
            rootBounds
        } else {
            displayBounds() ?: return false
        }

        return dispatchTapAtRatioInBounds(bounds, xRatio, yRatio)
    }

    private fun dispatchTapAtRatioInBounds(
        bounds: Rect,
        xRatio: Float,
        yRatio: Float,
    ): Boolean {
        if (bounds.width() <= 0 || bounds.height() <= 0) return false

        val x = (bounds.left + bounds.width() * xRatio.coerceIn(0f, 1f))
            .coerceIn(bounds.left.toFloat(), (bounds.right - 1).toFloat())
        val y = (bounds.top + bounds.height() * yRatio.coerceIn(0f, 1f))
            .coerceIn(bounds.top.toFloat(), (bounds.bottom - 1).toFloat())

        return dispatchTapAtPoint(x, y)
    }

    private fun dispatchTapAtNodeCenter(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return false

        return dispatchTapAtPoint(
            x = bounds.centerX().toFloat(),
            y = bounds.centerY().toFloat(),
        )
    }

    private fun dispatchTapAtPoint(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, TAP_DURATION_MILLIS))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    private fun dispatchSwipeUp(): Boolean {
        val bounds = displayBounds() ?: return false
        if (bounds.width() <= 0 || bounds.height() <= 0) return false

        val x = bounds.left + bounds.width() * 0.5f
        val startY = bounds.top + bounds.height() * 0.78f
        val endY = bounds.top + bounds.height() * 0.28f
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, SWIPE_DURATION_MILLIS))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    private fun activeRootForPackage(targetPackage: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val activePackage = root.packageName?.toString().orEmpty()
        return root.takeIf { activePackage == targetPackage }
    }

    private fun pollJitter(): Long = Random.nextLong(-50, 51)

    private fun activePackageMismatchResult(): TapExecutionResult {
        val activePackage = rootInActiveWindow?.packageName?.toString().orEmpty()
        return TapExecutionResult(
            title = "Tap skipped",
            text = if (activePackage.isBlank()) {
                "Active window package was unavailable, so no tap was sent."
            } else {
                "Active app was $activePackage, not the selected target."
            },
        )
    }

    private fun targetReadyResult(text: String): TapExecutionResult {
        return TapExecutionResult(
            title = "Target ready",
            text = text,
            readyForFinalTap = true,
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
        private const val UNLOCK_BOOTLOADER_TEXT = "Unlock bootloader"
        private const val ME_TAB_TEXT = "ME"
        private const val MAX_CLICKABLE_PARENT_DEPTH = 8
        private const val OPTIONAL_PROMPT_WAIT_MILLIS = 2_000L
        private const val SCROLL_EVERY_POLLS = 4
        private const val POLL_INTERVAL_MILLIS = 250L
        private const val AFTER_CLICK_DELAY_MILLIS = 500L
        private const val TAP_DURATION_MILLIS = 80L
        private const val SWIPE_DURATION_MILLIS = 260L
        private const val ME_TAB_X_RATIO = 0.79f
        private const val ME_TAB_Y_RATIO = 0.93f
        private const val MAX_NODE_SEARCH_DEPTH = 128
        private val NO_THANKS_TEXTS = listOf(
            "No, thanks",
            "No thanks",
            "Not now",
        )

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
                    text = "Accessibility service connection lost. Disable and re-enable MiUnlocker tap service in Accessibility settings.",
                )

            return service.executeTapCommand(
                targetPackage = safeTargetPackage,
                xRatio = xRatio,
                yRatio = yRatio,
            )
        }

        suspend fun prepareUnlockPageCommand(
            targetPackage: String,
            timeoutMillis: Long,
        ): TapExecutionResult {
            val safeTargetPackage = targetPackage.trim()
            if (safeTargetPackage.isBlank()) {
                return TapExecutionResult(
                    title = "Prepare skipped",
                    text = "No selected target package was available.",
                )
            }

            val service = activeService
                ?: return TapExecutionResult(
                    title = "Prepare skipped",
                    text = "Accessibility service connection lost. Disable and re-enable MiUnlocker tap service in Accessibility settings.",
                )

            return service.prepareUnlockPageCommand(
                targetPackage = safeTargetPackage,
                timeoutMillis = timeoutMillis,
            )
        }
    }
}

data class TapExecutionResult(
    val title: String,
    val text: String,
    val readyForFinalTap: Boolean = false,
    val nodeFound: Boolean? = null,
    val fallbackUsed: Boolean? = null,
)
