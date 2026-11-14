package com.abdou.glasslock

import android.app.*
import android.content.*
import android.os.IBinder

class LockService : Service() {
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            c.startActivity(
                Intent(c, LockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("lock", "Glass Lock", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "lock")
            .setContentTitle("Glass Lock")
            .setContentText("Active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
        startForeground(1, n)
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() {
        unregisterReceiver(screenOff)
        super.onDestroy()
    }
}
