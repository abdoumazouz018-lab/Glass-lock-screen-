package com.abdou.glasslock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { enable() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Setup(
                onEnable = { enable() },
                onDisable = { stopService(Intent(this, LockService::class.java)) },
                onTest = { startActivity(Intent(this, LockActivity::class.java)) }
            )
        }
    }

    private fun enable() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startForegroundService(Intent(this, LockService::class.java))
    }
}

@Composable
private fun Setup(onEnable: () -> Unit, onDisable: () -> Unit, onTest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF0E1116)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText("Glass Lock", style = TextStyle(Color.White, 30.sp))
        Spacer(Modifier.height(8.dp))
        BasicText(
            "فعّل الصلاحيات ثم الخدمة. استثنِ التطبيق من توفير البطارية كي لا توقفه سامسونغ.",
            style = TextStyle(Color(0xFFB8C0CC), 15.sp, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(24.dp))
        Btn("تفعيل", Color(0xFF1A6CF0), onEnable)
        Spacer(Modifier.height(12.dp))
        Btn("تجربة الشاشة الآن", Color(0xFF2A313C), onTest)
        Spacer(Modifier.height(12.dp))
        Btn("إيقاف", Color(0xFF2A313C), onDisable)
    }
}

@Composable
private fun Btn(t: String, bg: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick).padding(16.dp),
        contentAlignment = Alignment.Center
    ) { BasicText(t, style = TextStyle(Color.White, 17.sp)) }
}
