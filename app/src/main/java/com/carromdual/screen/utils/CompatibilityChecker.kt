package com.carromdual.screen.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import com.carromdual.screen.data.PrefsManager
import com.carromdual.screen.model.CheckStatus
import com.carromdual.screen.model.CompatibilityItem
import com.carromdual.screen.model.DeviceReport
import java.util.Locale

object CompatibilityChecker {

    fun runAudit(context: Context): DeviceReport {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val model = Build.MODEL.orEmpty()
        val androidVersion = Build.VERSION.RELEASE ?: "Unknown"
        val apiLevel = Build.VERSION.SDK_INT

        val combined = "$manufacturer $brand ${Build.DISPLAY}".lowercase(Locale.ROOT)
        val isTecnoOrTranssion = combined.contains("tecno") ||
            combined.contains("infinix") ||
            combined.contains("itel") ||
            combined.contains("hios") ||
            combined.contains("transsion")

        val hiosVersion = readSystemProperty("ro.os.hios.version")
            ?: readSystemProperty("ro.tranos.version")

        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        val batteryIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)

        val supportsSplitScreen = apiLevel >= Build.VERSION_CODES.N
        val supportsMultiResume = apiLevel >= Build.VERSION_CODES.Q

        val pm = context.packageManager
        val freeformFeature = pm.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
        val devFreeform = readGlobalSettingInt(context, "enable_freeform_support") == 1
        val forceResizable = readGlobalSettingInt(context, "force_resizable_activities") == 1
        val supportsFreeform = freeformFeature || devFreeform || isTecnoOrTranssion

        var carromInstalled = false
        var carromVersion: String? = null
        var carromResizeLabel = "Not installed"

        try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    PrefsManager.DEFAULT_CARROM_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(PrefsManager.DEFAULT_CARROM_PACKAGE, 0)
            }
            carromInstalled = true
            carromVersion = pkgInfo.versionName ?: "Installed"
            carromResizeLabel = inspectCarromResizeMode(pm)
        } catch (_: PackageManager.NameNotFoundException) {
            carromInstalled = false
        }

        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val profiles = launcherApps.profiles
        var carromInstances = 0
        for (profile in profiles) {
            val acts = try {
                launcherApps.getActivityList(PrefsManager.DEFAULT_CARROM_PACKAGE, profile)
            } catch (_: Exception) {
                emptyList()
            }
            if (acts.isNotEmpty()) {
                carromInstances++
            }
        }
        val profileCount = profiles.size.coerceAtLeast(userManager.userProfiles.size)

        val items = buildList {
            add(
                CompatibilityItem(
                    title = "Device & OS",
                    value = "$manufacturer $model • Android $androidVersion (API $apiLevel)",
                    detail = if (isTecnoOrTranssion) {
                        "Tecno / Transsion HiOS detected${if (!hiosVersion.isNullOrBlank()) " ($hiosVersion)" else ""}. Enable App Twin & Lightning Multi-Window in HiOS Settings for best dual-instance support."
                    } else {
                        "Standard Android multi-window behavior applies."
                    },
                    status = if (apiLevel >= Build.VERSION_CODES.Q) CheckStatus.PASS else CheckStatus.WARNING
                )
            )

            add(
                CompatibilityItem(
                    title = "Carrom Pool Installation",
                    value = if (carromInstalled) "Installed (v$carromVersion)" else "Not Installed",
                    detail = if (carromInstalled) {
                        "Found ${PrefsManager.DEFAULT_CARROM_PACKAGE}. Activity resize status: $carromResizeLabel."
                    } else {
                        "Install Carrom Pool (${PrefsManager.DEFAULT_CARROM_PACKAGE}) from Google Play Store."
                    },
                    status = if (carromInstalled) CheckStatus.PASS else CheckStatus.FAIL
                )
            )

            add(
                CompatibilityItem(
                    title = "Dual / Cloned Carrom Instances",
                    value = "$carromInstances instance(s) across $profileCount profile(s)",
                    detail = when {
                        carromInstances >= 2 ->
                            "Multiple Carrom Pool profiles detected (App Twin / Work Profile). You can launch Screen 1 and Screen 2 as two truly separate accounts!"
                        carromInstances == 1 ->
                            "Only 1 Carrom Pool instance detected. Android & Unity games normally block running two tasks from a single APK. Enable Tecno App Twin (Dual Apps) or Work Profile to create a second Carrom Pool instance."
                        else ->
                            "No Carrom Pool profile detected yet."
                    },
                    status = when {
                        carromInstances >= 2 -> CheckStatus.PASS
                        carromInstances == 1 -> CheckStatus.WARNING
                        else -> CheckStatus.FAIL
                    }
                )
            )

            add(
                CompatibilityItem(
                    title = "Split-Screen & Multi-Resume",
                    value = if (supportsMultiResume) "Supported (Android 10+ Multi-Resume)" else "Split-Screen Only (Single Active Focus)",
                    detail = if (supportsMultiResume) {
                        "Android OS isolates touch inputs per pane and allows both split windows to stay resumed simultaneously."
                    } else {
                        "Android 8–9 pauses the unfocused pane when you tap the other window."
                    },
                    status = if (supportsMultiResume) CheckStatus.PASS else CheckStatus.WARNING
                )
            )

            add(
                CompatibilityItem(
                    title = "Freeform / Floating App Windows",
                    value = when {
                        freeformFeature || devFreeform -> "Freeform Enabled"
                        isTecnoOrTranssion -> "HiOS Multi-Window Available"
                        else -> "Disabled in System Settings"
                    },
                    detail = if (forceResizable) {
                        "Force activities to be resizable is ON. Non-resizable games can be forced into split/freeform."
                    } else {
                        "If Carrom Pool refuses to enter Split-Screen or Freeform, enable 'Force activities to be resizable' and 'Enable freeform windows' in Developer Options."
                    },
                    status = if (freeformFeature || devFreeform || forceResizable) CheckStatus.PASS else CheckStatus.WARNING
                )
            )

            add(
                CompatibilityItem(
                    title = "Overlay Permission (Floating Control)",
                    value = if (hasOverlay) "Granted" else "Not Granted",
                    detail = if (hasOverlay) {
                        "Floating control window can be moved, resized, minimized, and maximized over Carrom Pool."
                    } else {
                        "Grant 'Display over other apps' in Permission Manager to enable the Floating Window controller."
                    },
                    status = if (hasOverlay) CheckStatus.PASS else CheckStatus.WARNING
                )
            )

            add(
                CompatibilityItem(
                    title = "Background / Battery Restriction",
                    value = if (batteryIgnored) "Unrestricted" else "Battery Optimized",
                    detail = if (batteryIgnored) {
                        "Floating Window Service will not be aggressively killed by Android or Tecno Phone Master."
                    } else {
                        "Tecno HiOS may close background services during gaming. Disable battery optimization for Carrom Dual Screen."
                    },
                    status = if (batteryIgnored) CheckStatus.PASS else CheckStatus.WARNING
                )
            )
        }

        return DeviceReport(
            manufacturer = manufacturer,
            model = model,
            brand = brand,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            isTecnoOrTranssion = isTecnoOrTranssion,
            hiosVersion = hiosVersion,
            hasOverlayPermission = hasOverlay,
            isBatteryOptimizationIgnored = batteryIgnored,
            supportsSplitScreen = supportsSplitScreen,
            supportsMultiResume = supportsMultiResume,
            supportsFreeform = supportsFreeform,
            forceResizableEnabled = forceResizable,
            carromInstalled = carromInstalled,
            carromVersion = carromVersion,
            carromResizeModeLabel = carromResizeLabel,
            userProfileCount = profileCount,
            detectedCarromInstances = carromInstances,
            items = items
        )
    }

    private fun inspectCarromResizeMode(pm: PackageManager): String {
        return try {
            val launchIntent = pm.getLaunchIntentForPackage(PrefsManager.DEFAULT_CARROM_PACKAGE)
                ?: return "No launcher activity"
            val resolveInfo = pm.resolveActivity(launchIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?: return "Unknown"
            val activityInfo: ActivityInfo = resolveInfo.activityInfo
            val field = ActivityInfo::class.java.getDeclaredField("resizeMode")
            field.isAccessible = true
            val mode = field.getInt(activityInfo)
            if (mode == 0) {
                "Locked (non-resizable; use Developer Options -> Force Resizable)"
            } else {
                "Resizable (mode=$mode)"
            }
        } catch (_: Throwable) {
            "Managed by system/OEM policy"
        }
    }

    private fun readGlobalSettingInt(context: Context, key: String): Int {
        return try {
            Settings.Global.getInt(context.contentResolver, key, 0)
        } catch (_: Throwable) {
            0
        }
    }

    @SuppressLint("PrivateApi")
    private fun readSystemProperty(key: String): String? {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val method = cls.getMethod("get", String::class.java)
            val result = method.invoke(null, key) as? String
            if (result.isNullOrBlank()) null else result.trim()
        } catch (_: Throwable) {
            null
        }
    }
}
