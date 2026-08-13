package com.robeluis.gamebooster

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvRamInfo: TextView
    private lateinit var tvLog: TextView
    private lateinit var containerApps: LinearLayout
    private val checkBoxes = mutableListOf<CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvRamInfo = findViewById(R.id.tvRamInfo)
        tvLog = findViewById(R.id.tvLog)
        containerApps = findViewById(R.id.containerApps)

        updateRamInfo()
        listBackgroundApps()

        findViewById<Button>(R.id.btnBoost).setOnClickListener { boostNow() }
        findViewById<Button>(R.id.btnDnd).setOnClickListener { requestDndAccess() }
        findViewById<Button>(R.id.btnClearCache).setOnClickListener { clearAppCache() }
    }

    private fun updateRamInfo() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val freeMb = info.availMem / (1024 * 1024)
        val totalMb = info.totalMem / (1024 * 1024)
        tvRamInfo.text = "RAM libre: ${freeMb}MB / ${totalMb}MB"
    }

    private fun listBackgroundApps() {
        containerApps.removeAllViews()
        checkBoxes.clear()
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .filter { it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it).toString() }
            .take(25)

        for (app in apps) {
            val cb = CheckBox(this)
            cb.text = pm.getApplicationLabel(app).toString()
            cb.tag = app.packageName
            cb.isChecked = true
            cb.setTextColor(0xFFFFFFFF.toInt())
            containerApps.addView(cb)
            checkBoxes.add(cb)
        }
    }

    private fun boostNow() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var closed = 0

        for (cb in checkBoxes) {
            if (cb.isChecked) {
                val pkg = cb.tag as String
                try {
                    am.killBackgroundProcesses(pkg)
                    closed++
                } catch (e: SecurityException) {
                }
            }
        }

        System.gc()

        updateRamInfo()
        tvLog.text = "✔ $closed apps cerradas en segundo plano.\n✔ RAM actualizada."
        Toast.makeText(this, "Optimización completa", Toast.LENGTH_SHORT).show()
    }

    private fun requestDndAccess() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Activa el acceso en la siguiente pantalla", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        } else {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            tvLog.text = "✔ Modo No Molestar activado."
        }
    }

    private fun clearAppCache() {
        try {
            cacheDir.deleteRecursively()
            tvLog.text = "✔ Caché de la app limpiada."
        } catch (e: Exception) {
            tvLog.text = "⚠ No se pudo limpiar toda la caché."
        }
    }

    override fun onResume() {
        super.onResume()
        updateRamInfo()
    }
}
