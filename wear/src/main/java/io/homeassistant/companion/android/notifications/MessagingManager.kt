package io.homeassistant.companion.android.notifications

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.homeassistant.companion.android.common.notifications.NotificationData
import io.homeassistant.companion.android.common.notifications.getGroupNotificationBuilder
import io.homeassistant.companion.android.common.notifications.handleChannel
import io.homeassistant.companion.android.common.notifications.handleSmallIcon
import io.homeassistant.companion.android.common.notifications.handleText
import io.homeassistant.companion.android.common.util.cancelGroupIfNeeded
import io.homeassistant.companion.android.common.util.getActiveNotification
import io.homeassistant.companion.android.database.AppDatabase
import io.homeassistant.companion.android.database.notification.NotificationItem
import org.json.JSONObject
import javax.inject.Inject

class MessagingManager @Inject constructor(
    @ApplicationContext val context: Context,
) {

    companion object {
        const val TAG = "MessagingManager"
    }

    fun handleMessage(notificationData: Map<String, String>, source: String) {

        val notificationDao = AppDatabase.getInstance(context).notificationDao()
        val now = System.currentTimeMillis()

        val jsonObject = (notificationData as Map<*, *>?)?.let { JSONObject(it) }
        val notificationRow =
            NotificationItem(0, now, notificationData[NotificationData.MESSAGE].toString(), jsonObject.toString(), source)
        notificationDao.add(notificationRow)

        sendNotification(notificationData, now)
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(data: Map<String, String>, received: Long? = null) {
        val notificationManagerCompat = NotificationManagerCompat.from(context)

        val tag = data["tag"]
        val messageId = tag?.hashCode() ?: received?.toInt() ?: System.currentTimeMillis().toInt()

        var group = data["group"]
        var groupId = 0
        var previousGroup = ""
        var previousGroupId = 0
        if (!group.isNullOrBlank()) {
            group = NotificationData.GROUP_PREFIX + group
            groupId = group.hashCode()
        } else {
            val notification = notificationManagerCompat.getActiveNotification(tag, messageId)
            if (notification != null && notification.isGroup) {
                previousGroup = NotificationData.GROUP_PREFIX + notification.tag
                previousGroupId = previousGroup.hashCode()
            }
        }

        val channelId = handleChannel(context, notificationManagerCompat, data)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)

        handleSmallIcon(context, notificationBuilder, data)

        handleText(notificationBuilder, data)

        notificationManagerCompat.apply {
            Log.d(TAG, "Show notification with tag \"$tag\" and id \"$messageId\"")
            notify(tag, messageId, notificationBuilder.build())
            if (!group.isNullOrBlank()) {
                Log.d(TAG, "Show group notification with tag \"$group\" and id \"$groupId\"")
                notify(group, groupId, getGroupNotificationBuilder(context, channelId, group, data).build())
            } else {
                if (previousGroup.isNotBlank()) {
                    Log.d(
                        TAG,
                        "Remove group notification with tag \"$previousGroup\" and id \"$previousGroupId\""
                    )
                    notificationManagerCompat.cancelGroupIfNeeded(previousGroup, previousGroupId)
                }
            }
        }
    }
}