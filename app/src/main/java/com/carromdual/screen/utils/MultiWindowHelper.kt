package com.carromdual.screen.utils

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.UserHandle
import android.os.UserManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.carromdual.screen.data.PrefsManager
import com.carromdual.screen.model.AppProfileEntry
import com.carromdual.screen.model.LaunchResult
import com.carromdual.screen.service.DualScreenAccessibilityService

object MultiWindowHelper {

    fun executeAutoDualScreen(
        context: Context,
        handler: Handler,
        prefs: PrefsManager,
        launchScreen1First: Boolean,
        onStatus: (String) -> Unit
    ) {
        val useFreeform = prefs.launchMode == PrefsManager.MODE_FREEFORM

        if (useFreeform) {
            val res1 = launchInMultiWindow(
                context = context,
                packageName = prefs.screen1Package,
                userSerial = prefs.screen1UserSerial,
                useFreeform = true,
                slotIndex = 1,
                preset = prefs.splitLayoutPreset
            )
            onStatus("Screen 1 Freeform: ${res1.message}")
            handler.postDelayed({
                val res2 = launchInMultiWindow(
                    context = context,
                    packageName = prefs.screen2Package,
                    userSerial = prefs.screen2UserSerial,
                    useFreeform = true,
                    slotIndex = 2,
                    preset = prefs.splitLayoutPreset
                )
                onStatus("Freeform Dual:\n1) ${res1.message}\n2) ${res2.message}")
            }, 650L)
            return
        }

        val triggerSplitAndScreen2 = {
            if (DualScreenAccessibilityService.isEnabled()) {
                val toggled = DualScreenAccessibilityService.requestSplitScreenToggle()
                if (toggled) {
                    onStatus("Split-Screen triggered! Launching Screen 2…")
                    handler.postDelayed({
                        val res2 = launchInMultiWindow(
                            context = context,
                            packageName = prefs.screen2Package,
                            userSerial = prefs.screen2UserSerial,
                            useFreeform = false,
                            slotIndex = 2,
                            preset = prefs.splitLayoutPreset
                        )
                        onStatus("Dual Screen active: ${res2.message}")
                    }, 750L)
                } else {
                    DualScreenAccessibilityService.requestRecents()
                    onStatus(
                        "Android 12+ blocked 1-tap split toggle. Opening Recents: tap Carrom Pool icon → Split Screen, then tap Screen 2 in Floating Control."
                    )
                }
            } else {
                onStatus(
                    "Enable 'Auto Split-Screen Service' in Permission Manager, OR switch Window Mode to Freeform, OR open Recents → Split Screen and tap Screen 2."
                )
            }
        }

        if (launchScreen1First) {
            val res1 = launchSingle(context, prefs.screen1Package, prefs.screen1UserSerial)
            if (!res1.success) {
                onStatus(res1.message)
                return
            }
            onStatus("Opened Screen 1. Triggering Split-Screen…")
            handler.postDelayed({ triggerSplitAndScreen2() }, 950L)
        } else {
            triggerSplitAndScreen2()
        }
    }

    fun getAvailableApps(context: Context): List<AppProfileEntry> {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val list = mutableListOf<AppProfileEntry>()

        val profiles = launcherApps.profiles
        val primarySerial = userManager.getSerialNumberForUser(android.os.Process.myUserHandle())

        for (profile in profiles) {
            val serial = userManager.getSerialNumberForUser(profile)
            val isSecondary = serial != primarySerial
            val activities = try {
                launcherApps.getActivityList(null, profile)
            } catch (_: Throwable) {
                emptyList()
            }
            for (info in activities) {
                val pkg = info.applicationInfo.packageName
                if (pkg == context.packageName) continue
                val appLabel = info.label?.toString() ?: pkg
                val isCarrom = pkg == PrefsManager.DEFAULT_CARROM_PACKAGE ||
                    pkg == "com.miniclip.carrom.lite"
                val displayLabel = if (isSecondary) "$appLabel (Twin / Work Profile)" else appLabel
                list.add(
                    AppProfileEntry(
                        label = displayLabel,
                        packageName = pkg,
                        userHandleSerial = serial,
                        isSecondaryProfile = isSecondary,
                        isCarromPool = isCarrom
                    )
                )
            }
        }

        return list.distinctBy { "${it.packageName}:${it.userHandleSerial}" }
            .sortedWith(
                compareByDescending<AppProfileEntry> { it.isCarromPool }
                    .thenByDescending { it.isSecondaryProfile }
                    .thenBy { it.label.lowercase() }
            )
    }

    fun findDefaultScreen2(context: Context): Pair<String, Long> {
        val apps = getAvailableApps(context)
        val secondaryCarrom = apps.firstOrNull { it.isCarromPool && it.isSecondaryProfile }
        if (secondaryCarrom != null) {
            return secondaryCarrom.packageName to secondaryCarrom.userHandleSerial
        }
        val primaryCarrom = apps.firstOrNull { it.isCarromPool }
        if (primaryCarrom != null) {
            return primaryCarrom.packageName to primaryCarrom.userHandleSerial
        }
        return PrefsManager.DEFAULT_CARROM_PACKAGE to 0L
    }

    fun launchSingle(
        context: Context,
        packageName: String,
        userSerial: Long = 0L
    ): LaunchResult {
        return launchInternal(
            context = context,
            packageName = packageName,
            userSerial = userSerial,
            adjacentSplit = false,
            freeformBounds = null
        )
    }

    fun launchInMultiWindow(
        context: Context,
        packageName: String,
        userSerial: Long,
        useFreeform: Boolean,
        slotIndex: Int,
        preset: String
    ): LaunchResult {
        val bounds = if (useFreeform) computeBounds(context, slotIndex, preset) else null
        return launchInternal(
            context = context,
            packageName = packageName,
            userSerial = userSerial,
            adjacentSplit = true,
            freeformBounds = bounds
        )
    }

    private fun launchInternal(
        context: Context,
        packageName: String,
        userSerial: Long,
        adjacentSplit: Boolean,
        freeformBounds: Rect?
    ): LaunchResult {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        val targetProfile: UserHandle = resolveUserHandle(userManager, launcherApps, userSerial)
        val options = ActivityOptions.makeBasic()

        if (freeformBounds != null) {
            options.setLaunchBounds(freeformBounds)
            applyFreeformWindowingMode(options)
        }

        val activities = try {
            launcherApps.getActivityList(packageName, targetProfile)
        } catch (_: Throwable) {
            emptyList()
        }

        val mySerial = userManager.getSerialNumberForUser(android.os.Process.myUserHandle())
        val isDifferentProfile = userManager.getSerialNumberForUser(targetProfile) != mySerial

        if (isDifferentProfile && activities.isNotEmpty()) {
            return try {
                launcherApps.startMainActivity(
                    activities.first().componentName,
                    targetProfile,
                    freeformBounds,
                    options.toBundle()
                )
                LaunchResult(
                    success = true,
                    message = "Launched secondary profile instance ($packageName).",
                    technicalNote = "Launched via Android LauncherApps across secondary UserHandle."
                )
            } catch (e: Throwable) {
                LaunchResult(
                    success = false,
                    message = "Could not launch secondary profile: ${e.message}",
                    technicalNote = "LauncherApps blocked cross-profile start."
                )
            }
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
            ?: return LaunchResult(
                success = false,
                message = "$packageName is not installed on this device.",
                technicalNote = "PackageManager returned null launch intent."
            )

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (adjacentSplit) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }

        return try {
            context.startActivity(launchIntent, options.toBundle())
            val note = if (freeformBounds != null) {
                "Launched with Freeform bounds $freeformBounds."
            } else if (adjacentSplit) {
                "Launched with FLAG_ACTIVITY_LAUNCH_ADJACENT."
            } else {
                "Launched standard task."
            }
            LaunchResult(
                success = true,
                message = "Launched $packageName.",
                technicalNote = note
            )
        } catch (e: Throwable) {
            // Fallback without ActivityOptions bundle if OEM rejects bounds
            return try {
                context.startActivity(launchIntent)
                LaunchResult(
                    success = true,
                    message = "Launched $packageName (fallback standard mode).",
                    technicalNote = "OEM rejected multi-window ActivityOptions: ${e.message}"
                )
            } catch (inner: Throwable) {
                LaunchResult(
                    success = false,
                    message = "Failed to launch $packageName: ${inner.message}",
                    technicalNote = "Activity start failed."
                )
            }
        }
    }

    private fun resolveUserHandle(
        userManager: UserManager,
        launcherApps: LauncherApps,
        serial: Long
    ): UserHandle {
        if (serial > 0L) {
            for (profile in launcherApps.profiles) {
                if (userManager.getSerialNumberForUser(profile) == serial) {
                    return profile
                }
            }
        }
        return android.os.Process.myUserHandle()
    }

    @Suppress("PrivateApi", "DiscouragedPrivateApi")
    private fun applyFreeformWindowingMode(options: ActivityOptions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // WINDOWING_MODE_FREEFORM = 5 in Android WindowConfiguration
                val method = ActivityOptions::class.java.getMethod(
                    "setLaunchWindowingMode",
                    Int::class.javaPrimitiveType
                )
                method.invoke(options, 5)
            } catch (_: Throwable) {
                // Ignored cleanly if hidden API blocked on this ROM
            }
        }
    }

    fun computeBounds(context: Context, slotIndex: Int, preset: String): Rect {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            width = metrics.bounds.width()
            height = metrics.bounds.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            width = dm.widthPixels
            height = dm.heightPixels
        }

        val pad = (16 * context.resources.displayMetrics.density).toInt()
        return if (preset == PrefsManager.PRESET_LEFT_RIGHT) {
            val halfW = width / 2
            if (slotIndex == 1) {
                Rect(pad, pad * 3, halfW - pad / 2, height - pad * 3)
            } else {
                Rect(halfW + pad / 2, pad * 3, width - pad, height - pad * 3)
            }
        } else {
            val halfH = height / 2
            if (slotIndex == 1) {
                Rect(pad, pad * 3, width - pad, halfH - pad / 2)
            } else {
                Rect(pad, halfH + pad / 2, width - pad, height - pad * 3)
            }
        }
    }
}
