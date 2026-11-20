package com.carromdual.screen.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.carromdual.screen.R
import com.carromdual.screen.databinding.ActivityPermissionManagerBinding
import com.carromdual.screen.service.DualScreenAccessibilityService
import com.carromdual.screen.utils.PermissionHelper

class PermissionManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionManagerBinding

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshStatuses()
        Toast.makeText(
            this,
            if (granted) "Notification permission granted" else "Notification permission denied",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnGrantOverlay.setOnClickListener {
            PermissionHelper.openOverlaySettings(this)
        }

        binding.btnGrantNotif.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Toast.makeText(
                    this,
                    "Notification permission is automatic below Android 13",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnBatterySettings.setOnClickListener {
            PermissionHelper.openBatteryOptimizationSettings(this)
        }

        binding.btnTecnoAutoStart.setOnClickListener {
            PermissionHelper.openTecnoBackgroundSettings(this)
        }

        binding.btnOpenDevOptions.setOnClickListener {
            PermissionHelper.openDeveloperOptions(this)
        }

        binding.btnEnableAutoSplit.setOnClickListener {
            PermissionHelper.openAccessibilitySettings(this)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
    }

    private fun refreshStatuses() {
        val overlayOk = PermissionHelper.hasOverlayPermission(this)
        val notifOk = PermissionHelper.hasNotificationPermission(this)
        val batteryOk = PermissionHelper.isIgnoringBatteryOptimizations(this)
        val autoSplitOk = DualScreenAccessibilityService.isEnabled()

        if (overlayOk) {
            binding.tvOverlayStatus.text = "Granted"
            binding.tvOverlayStatus.setBackgroundResource(R.drawable.bg_status_badge_green)
            binding.tvOverlayStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_neon))
        } else {
            binding.tvOverlayStatus.text = "Required"
            binding.tvOverlayStatus.setBackgroundResource(R.drawable.bg_status_badge_amber)
            binding.tvOverlayStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
        }

        if (notifOk) {
            binding.tvNotifStatus.text = "Granted"
            binding.tvNotifStatus.setBackgroundResource(R.drawable.bg_status_badge_green)
            binding.tvNotifStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_neon))
        } else {
            binding.tvNotifStatus.text = "Recommended"
            binding.tvNotifStatus.setBackgroundResource(R.drawable.bg_status_badge_amber)
            binding.tvNotifStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
        }

        if (batteryOk) {
            binding.tvBatteryStatus.text = "Unrestricted"
            binding.tvBatteryStatus.setBackgroundResource(R.drawable.bg_status_badge_green)
            binding.tvBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_neon))
        } else {
            binding.tvBatteryStatus.text = "Optimized"
            binding.tvBatteryStatus.setBackgroundResource(R.drawable.bg_status_badge_amber)
            binding.tvBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
        }

        if (autoSplitOk) {
            binding.tvAutoSplitStatus.text = "Enabled"
            binding.tvAutoSplitStatus.setBackgroundResource(R.drawable.bg_status_badge_green)
            binding.tvAutoSplitStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_neon))
        } else {
            binding.tvAutoSplitStatus.text = "Optional"
            binding.tvAutoSplitStatus.setBackgroundResource(R.drawable.bg_status_badge_amber)
            binding.tvAutoSplitStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
        }
    }
}
