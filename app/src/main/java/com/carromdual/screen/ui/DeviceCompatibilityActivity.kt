package com.carromdual.screen.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.carromdual.screen.R
import com.carromdual.screen.databinding.ActivityDeviceCompatibilityBinding
import com.carromdual.screen.model.CheckStatus
import com.carromdual.screen.utils.CompatibilityChecker

class DeviceCompatibilityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceCompatibilityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceCompatibilityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefreshAudit.setOnClickListener { renderAudit() }

        renderAudit()
    }

    override fun onResume() {
        super.onResume()
        renderAudit()
    }

    private fun renderAudit() {
        val report = CompatibilityChecker.runAudit(this)

        val oemNote = if (report.isTecnoOrTranssion) {
            "Tecno / Transsion HiOS Device Detected"
        } else {
            "${report.manufacturer} Android Device"
        }
        binding.tvCompatHeaderTitle.text = "$oemNote (${report.model})"
        binding.tvCompatHeaderSummary.text =
            "Android ${report.androidVersion} (API ${report.apiLevel}) • " +
                "${report.detectedCarromInstances} Carrom Pool instance(s) across ${report.userProfileCount} profile(s)."

        binding.compatItemsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (item in report.items) {
            val row = inflater.inflate(
                R.layout.item_compatibility_row,
                binding.compatItemsContainer,
                false
            )
            val icon: ImageView = row.findViewById(R.id.ivCompatStatusIcon)
            val tvTitle: TextView = row.findViewById(R.id.tvCompatTitle)
            val tvValue: TextView = row.findViewById(R.id.tvCompatValue)
            val tvDetail: TextView = row.findViewById(R.id.tvCompatDetail)

            tvTitle.text = item.title
            tvValue.text = item.value
            tvDetail.text = item.detail

            when (item.status) {
                CheckStatus.PASS -> {
                    icon.setImageResource(R.drawable.ic_check_circle)
                    tvValue.setTextColor(ContextCompat.getColor(this, R.color.emerald_neon))
                }
                CheckStatus.WARNING -> {
                    icon.setImageResource(R.drawable.ic_warning)
                    tvValue.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                }
                CheckStatus.FAIL -> {
                    icon.setImageResource(R.drawable.ic_error_circle)
                    tvValue.setTextColor(ContextCompat.getColor(this, R.color.status_error))
                }
            }

            binding.compatItemsContainer.addView(row)
        }
    }
}
