package com.terxdesk.droid

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.terxdesk.droid.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTerminal.setOnClickListener {
            startActivity(Intent(this, TerminalActivity::class.java))
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                binding.permissionStatus.text = "Storage: Not granted"
                binding.btnGrantPermission.setOnClickListener {
                    val uri = android.net.Uri.parse("package:$packageName")
                    startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri))
                }
                binding.btnGrantPermission.visibility = View.VISIBLE
            } else {
                binding.permissionStatus.text = "Storage: Granted"
                binding.btnGrantPermission.visibility = View.GONE
            }
        } else {
            binding.permissionStatus.text = "Storage: OK"
            binding.btnGrantPermission.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
}
