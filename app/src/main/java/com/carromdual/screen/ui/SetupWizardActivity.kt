package com.carromdual.screen.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.carromdual.screen.R
import com.carromdual.screen.data.PrefsManager
import com.carromdual.screen.databinding.ActivitySetupWizardBinding
import com.carromdual.screen.utils.CompatibilityChecker
import com.carromdual.screen.utils.MultiWindowHelper
import com.carromdual.screen.utils.PermissionHelper

class SetupWizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupWizardBinding
    private lateinit var prefs: PrefsManager
    private var currentStep = 1
    private val totalSteps = 6

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        renderCurrentStep()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)
        binding = ActivitySetupWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSkipSetup.setOnClickListener { completeSetup() }
        binding.btnWizardBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                renderCurrentStep()
            }
        }
        binding.btnWizardNext.setOnClickListener {
            if (currentStep < totalSteps) {
                currentStep++
                renderCurrentStep()
            } else {
                completeSetup()
            }
        }

        renderCurrentStep()
    }

    override fun onResume() {
        super.onResume()
        renderCurrentStep()
    }

    private fun renderCurrentStep() {
        binding.tvStepCounter.text = getString(R.string.setup_step_counter, currentStep, totalSteps)
        binding.wizardProgress.progress = currentStep
        binding.btnWizardBack.visibility = if (currentStep == 1) View.INVISIBLE else View.VISIBLE
        binding.btnWizardNext.text = if (currentStep == totalSteps) {
            getString(R.string.setup_btn_finish)
        } else {
            getString(R.string.setup_btn_next)
        }

        val report = CompatibilityChecker.runAudit(this)

        binding.btnStepPrimaryAction.visibility = View.GONE
        binding.btnStepSecondaryAction.visibility = View.GONE

        when (currentStep) {
            1 -> {
                binding.ivStepIcon.setImageResource(R.drawable.ic_launcher_foreground)
                binding.tvStepTitle.text = "Welcome to Carrom Dual Screen"
                binding.tvStepSubtitle.text =
                    "A native multitasking controller built for running Carrom Pool alongside a second instance using Android Split-Screen, Freeform Windows, and Floating Controls."
                binding.tvStepStatusTitle.text = "TRANSPARENT ARCHITECTURE"
                binding.tvStepStatusBody.text =
                    "• Uses real Android Multi-Window APIs & Tecno HiOS App Twin / Work Profile detection.\n" +
                        "• Never creates fake boards or dummy games.\n" +
                        "• Zero unnecessary permissions requested."
            }

            2 -> {
                binding.ivStepIcon.setImageResource(R.drawable.ic_compatibility)
                binding.tvStepTitle.text = "Device Compatibility Check"
                binding.tvStepSubtitle.text =
                    "Scanning your phone's Android version, manufacturer profile, and Tecno HiOS multitasking behavior."
                binding.tvStepStatusTitle.text = "${report.manufacturer} ${report.model}"
                binding.tvStepStatusBody.text = buildString {
                    appendLine("Android Version: ${report.androidVersion} (API ${report.apiLevel})")
                    appendLine(
                        "Tecno / Transsion Detected: ${if (report.isTecnoOrTranssion) "Yes (HiOS optimizations ready)" else "No (Standard Android)"}"
                    )
                    if (!report.hiosVersion.isNullOrBlank()) {
                        appendLine("HiOS Build: ${report.hiosVersion}")
                    }
                    append("Battery Unrestricted: ${if (report.isBatteryOptimizationIgnored) "Yes" else "Optimized (Recommended to disable)"}")
                }
                if (!report.isBatteryOptimizationIgnored) {
                    binding.btnStepPrimaryAction.visibility = View.VISIBLE
                    binding.btnStepPrimaryAction.text = "Disable Battery Restriction"
                    binding.btnStepPrimaryAction.setOnClickListener {
                        PermissionHelper.openBatteryOptimizationSettings(this)
                    }
                }
            }

            3 -> {
                binding.ivStepIcon.setImageResource(R.drawable.ic_permissions)
                binding.tvStepTitle.text = "Required Permissions"
                binding.tvStepSubtitle.text =
                    "Only 'Display over other apps' is required for the Floating Window. On Android 13+, notification permission keeps the floating bar alive."
                binding.tvStepStatusTitle.text = "PERMISSION STATUS"
                val hasOverlay = PermissionHelper.hasOverlayPermission(this)
                val hasNotif = PermissionHelper.hasNotificationPermission(this)
                binding.tvStepStatusBody.text =
                    "• Display Over Other Apps: ${if (hasOverlay) "Granted" else "Not Granted"}\n" +
                        "• Notification (Android 13+): ${if (hasNotif) "Granted" else "Optional / Not Granted"}"

                if (!hasOverlay) {
                    binding.btnStepPrimaryAction.visibility = View.VISIBLE
                    binding.btnStepPrimaryAction.text = "Grant Overlay Permission"
                    binding.btnStepPrimaryAction.setOnClickListener {
                        PermissionHelper.openOverlaySettings(this)
                    }
                }
                if (!hasNotif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    binding.btnStepSecondaryAction.visibility = View.VISIBLE
                    binding.btnStepSecondaryAction.text = "Grant Notification Permission"
                    binding.btnStepSecondaryAction.setOnClickListener {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            4 -> {
                binding.ivStepIcon.setImageResource(R.drawable.ic_split_screen)
                binding.tvStepTitle.text = "Android Multi-Window Check"
                binding.tvStepSubtitle.text =
                    "Checking Split-Screen, Multi-Resume, and Freeform Window flags on your device."
                binding.tvStepStatusTitle.text = "MULTI-WINDOW CAPABILITIES"
                binding.tvStepStatusBody.text =
                    "• Split-Screen API (Android 7+): ${if (report.supportsSplitScreen) "Supported" else "Unsupported"}\n" +
                        "• Multi-Resume (Android 10+): ${if (report.supportsMultiResume) "Supported (Both panes active)" else "Focus-only"}\n" +
                        "• Freeform Support: ${if (report.supportsFreeform) "Available" else "Enable in Developer Options"}\n" +
                        "• Force Resizable Flag: ${if (report.forceResizableEnabled) "Enabled" else "Off (Recommended ON for games)"}"

                binding.btnStepPrimaryAction.visibility = View.VISIBLE
                binding.btnStepPrimaryAction.text = "Open Developer Options"
                binding.btnStepPrimaryAction.setOnClickListener {
                    PermissionHelper.openDeveloperOptions(this)
                }
            }

            5 -> {
                binding.ivStepIcon.setImageResource(R.drawable.ic_carrom)
                binding.tvStepTitle.text = "Carrom Pool Installation Check"
                binding.tvStepSubtitle.text =
                    "Verifying com.miniclip.carrom and checking if a second App Twin / Work Profile instance exists."
                binding.tvStepStatusTitle.text = if (report.carromInstalled) {
                    "CARROM POOL DETECTED (v${report.carromVersion})"
                } else {
                    "CARROM POOL NOT FOUND"
                }
                binding.tvStepStatusBody.text = if (report.carromInstalled) {
                    "Detected ${report.detectedCarromInstances} Carrom Pool instance(s).\n" +
                        "Resize Status: ${report.carromResizeModeLabel}\n\n" +
                        if (report.detectedCarromInstances < 2) {
                            "Important: Android & Carrom Pool block running 2 copies of the exact same single APK simultaneously. Enable Tecno App Twin or a Work Profile to run two independent Carrom Pool boards."
                        } else {
                            "Great! Multiple profiles detected for Screen 1 & Screen 2."
                        }
                } else {
                    "Carrom Pool is not installed yet. You can install it from Google Play Store and still explore the dashboard."
                }

                if (!report.carromInstalled) {
                    binding.btnStepPrimaryAction.visibility = View.VISIBLE
                    binding.btnStepPrimaryAction.text = "Install Carrom Pool on Play Store"
                    binding.btnStepPrimaryAction.setOnClickListener {
                        PermissionHelper.openCarromPlayStore(
                            this,
                            PrefsManager.DEFAULT_CARROM_PACKAGE
                        )
                    }
                }
            }

            6 -> {
                binding.ivStepIcon.setImageResource(R.drawable.ic_check_circle)
                binding.tvStepTitle.text = "Setup Complete"
                binding.tvStepSubtitle.text =
                    "Everything is configured. Default Screen 1 and Screen 2 slots have been auto-detected."
                binding.tvStepStatusTitle.text = "READY FOR DASHBOARD"
                binding.tvStepStatusBody.text =
                    "• Carrom Pool: ${if (report.carromInstalled) "Installed" else "Not Installed"}\n" +
                        "• Overlay Permission: ${if (report.hasOverlayPermission) "Granted" else "Pending"}\n" +
                        "• Multi-Window Mode: ${if (report.supportsSplitScreen) "Ready" else "Limited"}"
            }
        }
    }

    private fun completeSetup() {
        val (pkg2, serial2) = MultiWindowHelper.findDefaultScreen2(this)
        if (prefs.screen2UserSerial == -1L) {
            prefs.screen2Package = pkg2
            prefs.screen2UserSerial = serial2
        }
        prefs.isSetupCompleted = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
