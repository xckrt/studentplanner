package com.xckrt.studentplanner.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings

class AudioController(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    fun hasPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }
    fun requestPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    fun mutePhone() {
        if (hasPermission()) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }
    }
    fun unmutePhone() {
        if (hasPermission()) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }
}