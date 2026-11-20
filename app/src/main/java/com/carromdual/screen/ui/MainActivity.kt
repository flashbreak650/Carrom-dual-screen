package com.carromdual.screen.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.carromdual.screen.R
import com.carromdual.screen.data.PrefsManager
import com.carromdual.screen.databinding.ActivityMainBinding
import com.carromdual.screen.service.FloatingWindowService
import com.carromdual.screen.utils.CompatibilityChecker
import com.carromdual.screen.utils.MultiWindowHelper
import com.carromdual.screen.utils.PermissionHelper
import com.carromdual.screen.utils.UiAnimHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager

    private val floatingStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateFloatingTileState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)

        if (!prefs.isSetupCompleted) {
            startActivity(Intent(this, SetupWizardActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTileListeners()
        UiAnimHelper.animateStaggeredEntrance(
            binding.headerContainer,
            binding.statusSummaryCard,
            binding.tileStartDualScreen,
            binding.tileOpenCarrom,
            binding.tileSplitSetup,
            binding.tileFloatingWindow,
            binding.tileWindowSettings,
            binding.tilePermissionManager,
            binding.tileDeviceCompatibility,
            binding.tileHelpSupport
        )
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(FloatingWindowService.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(floatingStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(floatingStateReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(floatingStateReceiver)
        } catch (_: Throwable) {
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboardStatus()
        updateFloatingTileState()
    }

    private fun setupTileListeners() {
        val tiles = listOf(
            binding.tileStartDualScreen,
            binding.tileOpenCarrom,
            binding.tileSplitSetup,
            binding.tileFloatingWindow,
            binding.tileWindowSettings,
            binding.tilePermissionManager,
            binding.tileDeviceCompatibility,
            binding.tileHelpSupport
        )
        tiles.forEach { UiAnimHelper.applyPressScale(it) }

        binding.btnRerunSetup.setOnClickListener {
            startActivity(Intent(this, SetupWizardActivity::class.java))
        }

        // 1. Start Dual Screen
        binding.tileStartDualScreen.setOnClickListener {
            startActivity(Intent(this, DualScreenLauncherActivity::class.java))
        }

        // 2. Open Carrom Pool
        binding.tileOpenCarrom.setOnClickListener {
            val res = MultiWindowHelper.launchSingle(
                this,
                prefs.screen1Package,
                prefs.screen1UserSerial
            )
            if (!res.success) {
                Toast.makeText(this, res.message, Toast.LENGTH_LONG).show()
                PermissionHelper.openCarromPlayStore(this, PrefsManager.DEFAULT_CARROM_PACKAGE)
            }
        }

        // 3. Split Screen Setup
        binding.tileSplitSetup.setOnClickListener {
            val intent = Intent(this, DualScreenLauncherActivity::class.java).apply {
                putExtra(DualScreenLauncherActivity.EXTRA_FOCUS_SPLIT_MODE, true)
            }
            startActivity(intent)
        }

        // 4. Floating Window Toggle
        binding.tileFloatingWindow.setOnClickListener {
            if (FloatingWindowService.isRunning) {
                FloatingWindowService.stop(this)
            } else {
                if (!PermissionHelper.hasOverlayPermission(this)) {
                    Toast.makeText(
                        this,
                        "Grant 'Display over other apps' permission first",
                        Toast.LENGTH_LONG
                    ).show()
                    PermissionHelper.openOverlaySettings(this)
                } else {
                    FloatingWindowService.start(this)
                }
            }
        }

        // 5. Window Settings
        binding.tileWindowSettings.setOnClickListener {
            startActivity(Intent(this, WindowSettingsActivity::class.java))
        }

        // 6. Permission Manager
        binding.tilePermissionManager.setOnClickListener {
            startActivity(Intent(this, PermissionManagerActivity::class.java))
        }

        // 7. Device Compatibility
        binding.tileDeviceCompatibility.setOnClickListener {
            startActivity(Intent(this, DeviceCompatibilityActivity::class.java))
        }

        // 8. Help & Support
        binding.tileHelpSupport.setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }
    }

    private fun refreshDashboardStatus() {
        val report = CompatibilityChecker.runAudit(this)
        val oemTag = if (report.isTecnoOrTranssion) "Tecno / HiOS" else report.manufacturer
        binding.tvDeviceBadge.text = "$oemTag ${report.model} • Android ${report.androidVersion}"

        if (report.carromInstalled && report.detectedCarromInstances >= 2 && report.hasOverlayPermission) {
            binding.tvReadinessPill.text = "DUAL READY"
            binding.tvReadinessPill.setBackgroundResource(R.drawable.bg_status_badge_green)
            binding.tvReadinessPill.setTextColor(ContextCompat.getColor(this, R.color.emerald_neon))
        } else if (report.carromInstalled) {
            binding.tvReadinessPill.text = "PARTIAL"
            binding.tvReadinessPill.setBackgroundResource(R.drawable.bg_status_badge_amber)
            binding.tvReadinessPill.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
        } else {
            binding.tvReadinessPill.text = "NEEDS GAME"
            binding.tvReadinessPill.setBackgroundResource(R.drawable.bg_status_badge_red)
            binding.tvReadinessPill.setTextColor(ContextCompat.getColor(this, R.color.status_error))
        }

        val carromState = if (report.carromInstalled) {
            "Carrom Pool v${report.carromVersion} (${report.detectedCarromInstances} profile instance)"
        } else {
            "Carrom Pool not installed"
        }
        val overlayState = if (report.hasOverlayPermission) "Overlay OK" else "Overlay Needed"
        val splitState = if (report.supportsMultiResume) "Multi-Resume OK" else "Split-Screen Basic"
        binding.tvReadinessDetail.text = "$carromState • $overlayState • $splitState"

        binding.tvCarromTileDesc.text = if (report.carromInstalled) {
            "Installed (v${report.carromVersion})"
        } else {
            "Tap to install from Play Store"
        }
    }

    private fun updateFloatingTileState() {
        if (!::binding.isInitialized) return
        if (FloatingWindowService.isRunning) {
            binding.tvFloatingStateBadge.text = "ACTIVE"
            binding.tvFloatingStateBadge.setBackgroundResource(R.drawable.bg_status_badge_green)
            binding.tvFloatingStateBadge.setTextColor(
                ContextCompat.getColor(this, R.color.emerald_neon)
            )
            binding.tvFloatingTileDesc.text = getString(R.string.tile_floating_window_on)
        } else {
            binding.tvFloatingStateBadge.text = "OFF"
            binding.tvFloatingStateBadge.setBackgroundResource(R.drawable.bg_status_badge_amber)
            binding.tvFloatingStateBadge.setTextColor(
                ContextCompat.getColor(this, R.color.status_warning)
            )
            binding.tvFloatingTileDesc.text = getString(R.string.tile_floating_window_off)
        }
    }
}
