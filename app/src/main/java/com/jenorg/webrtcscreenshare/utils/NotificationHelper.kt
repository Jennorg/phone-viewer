package com.jenorg.webrtcscreenshare.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.codewithkael.webrtcscreenshare.ui.MainActivity

class NotificationHelper(context: Context) {
  
  private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  private val notificationChannelId = "12345" // You should use a unique ID for your channel
  
  init {
    createNotificationChannel() // Create the notification channel
  }
  
  fun buildBasicNotification(
    context: Context,
    title: String,
    message: String,
    smallIcon: Int,
    channelId: String = notificationChannelId
  ): Notification {
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
      .setContentTitle(title)
      .setContentText(message)
      .setSmallIcon(smallIcon) // Replace with your icon resource
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Adjust priority as needed
    
    return notificationBuilder.build()
  }
  
  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "My Notification Channel"
      val descriptionText = "Channel for basic notifications"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(notificationChannelId, name, importance).apply {
        description = descriptionText
      }
      notificationManager.createNotificationChannel(channel)
    }
  }
}
