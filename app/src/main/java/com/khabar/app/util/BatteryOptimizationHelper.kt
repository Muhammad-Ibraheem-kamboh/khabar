package com.khabar.app.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } catch (_: Exception) {
            true
        }
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimization(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to standard ignore battery optimization list
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun openOemBackgroundSettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val oemIntents = mutableListOf<Intent>()

        when {
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                oemIntents.add(
                    Intent().setComponent(
                        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                    )
                )
                oemIntents.add(
                    Intent().setComponent(
                        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
                    )
                )
                oemIntents.add(
                    Intent().setComponent(
                        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")
                    )
                )
            }
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                oemIntents.add(
                    Intent().setComponent(
                        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                    )
                )
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                oemIntents.add(
                    Intent().setComponent(
                        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                    )
                )
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                oemIntents.add(
                    Intent().setComponent(
                        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                    )
                )
            }
        }

        // Always add standard battery optimization request as reliable fallback
        oemIntents.add(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )

        for (intent in oemIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
}
