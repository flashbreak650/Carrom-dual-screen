package com.carromdual.screen.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.carromdual.screen.databinding.ActivityHelpSupportBinding
import com.carromdual.screen.utils.PermissionHelper

class HelpSupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpSupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpSupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnHelpOpenDevOptions.setOnClickListener {
            PermissionHelper.openDeveloperOptions(this)
        }
    }
}
