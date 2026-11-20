package com.carromdual.screen.ui

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.carromdual.screen.data.PrefsManager
import com.carromdual.screen.databinding.ActivityWindowSettingsBinding
import com.carromdual.screen.service.FloatingWindowService

class WindowSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWindowSettingsBinding
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)
        binding = ActivityWindowSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        loadValues()
        setupListeners()
    }

    private fun loadValues() {
        val widthProgress = (prefs.floatingWidthDp - 210).coerceIn(0, 170)
        val heightProgress = (prefs.floatingHeightDp - 180).coerceIn(0, 280)
        val opacityProgress = (prefs.floatingOpacityPercent - 45).coerceIn(0, 55)

        binding.seekWidth.progress = widthProgress
        binding.seekHeight.progress = heightProgress
        binding.seekOpacity.progress = opacityProgress

        binding.tvWidthValue.text = "Panel Width: ${210 + widthProgress} dp"
        binding.tvHeightValue.text = "Panel Height: ${180 + heightProgress} dp"
        binding.tvOpacityValue.text = "Opacity: ${45 + opacityProgress}%"

        binding.switchSnapEdge.isChecked = prefs.floatingSnapToEdge
        binding.switchStartMinimized.isChecked = prefs.floatingStartMinimized
    }

    private fun setupListeners() {
        binding.seekWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvWidthValue.text = "Panel Width: ${210 + progress} dp"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.seekHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvHeightValue.text = "Panel Height: ${180 + progress} dp"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvOpacityValue.text = "Opacity: ${45 + progress}%"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnSaveWindowSettings.setOnClickListener {
            prefs.floatingWidthDp = 210 + binding.seekWidth.progress
            prefs.floatingHeightDp = 180 + binding.seekHeight.progress
            prefs.floatingOpacityPercent = 45 + binding.seekOpacity.progress
            prefs.floatingSnapToEdge = binding.switchSnapEdge.isChecked
            prefs.floatingStartMinimized = binding.switchStartMinimized.isChecked

            if (FloatingWindowService.isRunning) {
                val updateIntent = Intent(this, FloatingWindowService::class.java).apply {
                    action = FloatingWindowService.ACTION_UPDATE_SETTINGS
                }
                startService(updateIntent)
            }
            Toast.makeText(this, "Window settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnResetWindowDefaults.setOnClickListener {
            prefs.resetWindowDefaults()
            loadValues()
            if (FloatingWindowService.isRunning) {
                val updateIntent = Intent(this, FloatingWindowService::class.java).apply {
                    action = FloatingWindowService.ACTION_UPDATE_SETTINGS
                }
                startService(updateIntent)
            }
            Toast.makeText(this, "Defaults restored", Toast.LENGTH_SHORT).show()
        }
    }
}
