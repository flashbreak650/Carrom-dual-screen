package com.carromdual.screen.ui

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.carromdual.screen.R
import com.carromdual.screen.data.PrefsManager
import com.carromdual.screen.databinding.ActivityDualScreenBinding
import com.carromdual.screen.model.AppProfileEntry
import com.carromdual.screen.service.FloatingWindowService
import com.carromdual.screen.utils.CompatibilityChecker
import com.carromdual.screen.utils.MultiWindowHelper
import com.carromdual.screen.utils.PermissionHelper

class DualScreenLauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDualScreenBinding
    private lateinit var prefs: PrefsManager
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)
        binding = ActivityDualScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (prefs.screen2UserSerial == -1L) {
            val (pkg2, serial2) = MultiWindowHelper.findDefaultScreen2(this)
            prefs.screen2Package = pkg2
            prefs.screen2UserSerial = serial2
        }

        binding.btnBack.setOnClickListener { finish() }

        if (intent.getBooleanExtra(EXTRA_FOCUS_SPLIT_MODE, false)) {
            prefs.launchMode = PrefsManager.MODE_SPLIT
        }

        bindControls()
        refreshSlotLabels()
        refreshInstanceAuditBanner()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun bindControls() {
        if (prefs.launchMode == PrefsManager.MODE_FREEFORM) {
            binding.rbFreeformMode.isChecked = true
        } else {
            binding.rbSplitMode.isChecked = true
        }

        if (prefs.splitLayoutPreset == PrefsManager.PRESET_LEFT_RIGHT) {
            binding.rbLeftRight.isChecked = true
        } else {
            binding.rbTopBottom.isChecked = true
        }

        binding.rgLaunchMode.setOnCheckedChangeListener { _, checkedId ->
            prefs.launchMode = if (checkedId == R.id.rbFreeformMode) {
                PrefsManager.MODE_FREEFORM
            } else {
                PrefsManager.MODE_SPLIT
            }
        }

        binding.rgOrientationPreset.setOnCheckedChangeListener { _, checkedId ->
            prefs.splitLayoutPreset = if (checkedId == R.id.rbLeftRight) {
                PrefsManager.PRESET_LEFT_RIGHT
            } else {
                PrefsManager.PRESET_TOP_BOTTOM
            }
        }

        binding.btnPickScreen1.setOnClickListener { showAppPickerDialog(slot = 1) }
        binding.btnPickScreen2.setOnClickListener { showAppPickerDialog(slot = 2) }

        binding.btnLaunchScreen1.setOnClickListener {
            val res = MultiWindowHelper.launchInMultiWindow(
                context = this,
                packageName = prefs.screen1Package,
                userSerial = prefs.screen1UserSerial,
                useFreeform = prefs.launchMode == PrefsManager.MODE_FREEFORM,
                slotIndex = 1,
                preset = prefs.splitLayoutPreset
            )
            binding.tvLaunchStatusOutput.text = "${res.message}\n${res.technicalNote}"
        }

        binding.btnLaunchScreen2.setOnClickListener {
            val res = MultiWindowHelper.launchInMultiWindow(
                context = this,
                packageName = prefs.screen2Package,
                userSerial = prefs.screen2UserSerial,
                useFreeform = prefs.launchMode == PrefsManager.MODE_FREEFORM,
                slotIndex = 2,
                preset = prefs.splitLayoutPreset
            )
            binding.tvLaunchStatusOutput.text = "${res.message}\n${res.technicalNote}"
        }

        binding.btnStartDualSequence.setOnClickListener {
            startDualSequence()
        }

        binding.btnToggleFloatingHere.setOnClickListener {
            if (!PermissionHelper.hasOverlayPermission(this)) {
                Toast.makeText(this, "Grant Overlay permission first", Toast.LENGTH_LONG).show()
                PermissionHelper.openOverlaySettings(this)
            } else if (FloatingWindowService.isRunning) {
                FloatingWindowService.stop(this)
                binding.tvLaunchStatusOutput.text = "Floating Control Bar stopped."
            } else {
                FloatingWindowService.start(this)
                binding.tvLaunchStatusOutput.text = "Floating Control Bar started."
            }
        }
    }

    private fun startDualSequence() {
        val useFreeform = prefs.launchMode == PrefsManager.MODE_FREEFORM
        val samePackageSameUser = prefs.screen1Package == prefs.screen2Package &&
            prefs.screen1UserSerial == prefs.screen2UserSerial

        if (samePackageSameUser) {
            binding.tvLaunchStatusOutput.text =
                "Warning: Screen 1 and Screen 2 point to the exact same single Carrom Pool profile. " +
                    "Android/Unity typically blocks 2 simultaneous tasks of the same APK unless you select a Tecno App Twin or Work Profile instance for Screen 2. Attempting launch…"
        }

        val res1 = MultiWindowHelper.launchInMultiWindow(
            context = this,
            packageName = prefs.screen1Package,
            userSerial = prefs.screen1UserSerial,
            useFreeform = useFreeform,
            slotIndex = 1,
            preset = prefs.splitLayoutPreset
        )

        handler.postDelayed({
            val res2 = MultiWindowHelper.launchInMultiWindow(
                context = this,
                packageName = prefs.screen2Package,
                userSerial = prefs.screen2UserSerial,
                useFreeform = useFreeform,
                slotIndex = 2,
                preset = prefs.splitLayoutPreset
            )
            binding.tvLaunchStatusOutput.text =
                "Screen 1: ${res1.message} (${res1.technicalNote})\n" +
                    "Screen 2: ${res2.message} (${res2.technicalNote})"
        }, 500L)
    }

    private fun refreshSlotLabels() {
        val apps = MultiWindowHelper.getAvailableApps(this)
        val s1 = apps.firstOrNull {
            it.packageName == prefs.screen1Package && it.userHandleSerial == prefs.screen1UserSerial
        }
        val s2 = apps.firstOrNull {
            it.packageName == prefs.screen2Package && it.userHandleSerial == prefs.screen2UserSerial
        }

        binding.tvScreen1Label.text = s1?.label ?: prefs.screen1Package
        binding.tvScreen1Pkg.text = "${prefs.screen1Package} • Profile #${prefs.screen1UserSerial}"

        binding.tvScreen2Label.text = s2?.label ?: prefs.screen2Package
        binding.tvScreen2Pkg.text = "${prefs.screen2Package} • Profile #${prefs.screen2UserSerial}"
    }

    private fun refreshInstanceAuditBanner() {
        val report = CompatibilityChecker.runAudit(this)
        binding.tvInstanceAuditNote.text = when {
            report.detectedCarromInstances >= 2 ->
                "Detected ${report.detectedCarromInstances} separate Carrom Pool instances (Primary + App Twin / Work Profile). True dual-screen Carrom Pool is supported on this device!"
            report.carromInstalled ->
                "Only 1 Carrom Pool APK is installed. Android & Carrom Pool do not permit cloning a single Unity APK natively from outside. To run two Carrom Pool boards simultaneously, enable Tecno App Twin or a Work Profile and pick it in Screen 2."
            else ->
                "Carrom Pool (${PrefsManager.DEFAULT_CARROM_PACKAGE}) is not installed. Install it or pick another app for testing."
        }
    }

    private fun showAppPickerDialog(slot: Int) {
        val apps = MultiWindowHelper.getAvailableApps(this)
        if (apps.isEmpty()) {
            Toast.makeText(this, "No launchable apps found", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_picker, null)
        val tvTitle: TextView = dialogView.findViewById(R.id.tvPickerTitle)
        val rv: RecyclerView = dialogView.findViewById(R.id.rvAppPicker)
        tvTitle.text = if (slot == 1) "Choose Screen 1 App" else "Choose Screen 2 App / Twin"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = AppPickerAdapter(apps) { picked ->
            if (slot == 1) {
                prefs.screen1Package = picked.packageName
                prefs.screen1UserSerial = picked.userHandleSerial
            } else {
                prefs.screen2Package = picked.packageName
                prefs.screen2UserSerial = picked.userHandleSerial
            }
            refreshSlotLabels()
            dialog.dismiss()
        }

        dialog.show()
    }

    private class AppPickerAdapter(
        private val items: List<AppProfileEntry>,
        private val onClick: (AppProfileEntry) -> Unit
    ) : RecyclerView.Adapter<AppPickerAdapter.Holder>() {

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvAppName)
            val tvPkg: TextView = view.findViewById(R.id.tvAppPkg)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_picker, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.label
            holder.tvPkg.text = "${item.packageName} • Profile #${item.userHandleSerial}"
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val EXTRA_FOCUS_SPLIT_MODE = "extra_focus_split_mode"
    }
}
