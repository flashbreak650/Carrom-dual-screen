package com.carromdual.screen.model

enum class CheckStatus {
    PASS,
    WARNING,
    FAIL
}

data class CompatibilityItem(
    val title: String,
    val value: String,
    val detail: String,
    val status: CheckStatus
)

data class AppProfileEntry(
    val label: String,
    val packageName: String,
    val userHandleSerial: Long,
    val isSecondaryProfile: Boolean,
    val isCarromPool: Boolean
)

data class LaunchResult(
    val success: Boolean,
    val message: String,
    val technicalNote: String
)

data class DeviceReport(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val androidVersion: String,
    val apiLevel: Int,
    val isTecnoOrTranssion: Boolean,
    val hiosVersion: String?,
    val hasOverlayPermission: Boolean,
    val isBatteryOptimizationIgnored: Boolean,
    val supportsSplitScreen: Boolean,
    val supportsMultiResume: Boolean,
    val supportsFreeform: Boolean,
    val forceResizableEnabled: Boolean,
    val carromInstalled: Boolean,
    val carromVersion: String?,
    val carromResizeModeLabel: String,
    val userProfileCount: Int,
    val detectedCarromInstances: Int,
    val items: List<CompatibilityItem>
)
