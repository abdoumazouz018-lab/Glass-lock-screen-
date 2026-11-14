package com.abdou.glasslock

import android.app.KeyguardManager
import android.content.*
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class LockActivity : ComponentActivity() {
    private val present = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { finish() }
    }
    private var torchOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        registerReceiver(present, IntentFilter(Intent.ACTION_USER_PRESENT))
        setContent {
            BackHandler(true) {}
            LockScreen(onUnlock = { unlock() }, onTorch = { torch(it) }, onCamera = { camera() })
        }
    }

    private fun unlock() {
        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { finish() }
            })
    }

    private fun torch(on: Boolean) {
        try {
            val cm = getSystemService(CameraManager::class.java)
            cm.setTorchMode(cm.cameraIdList[0], on)
            torchOn = on
        } catch (_: Exception) {}
    }

    private fun camera() {
        try {
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            try { startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        unregisterReceiver(present)
        super.onDestroy()
    }
}
