package io.homeassistant.companion.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.dagger.GraphComponentAccessor
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.util.cancelGroupIfNeeded
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

class NotificationDeleteReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_DATA = "EXTRA_DATA"
        const val EXTRA_NOTIFICATION_GROUP = "EXTRA_NOTIFICATION_GROUP"
        const val EXTRA_NOTIFICATION_GROUP_ID = "EXTRA_NOTIFICATION_GROUP_ID"
        const val TAG = "NotifDeleteReceiver"
    }

    @Inject
    lateinit var integrationRepository: IntegrationRepository

    override fun onReceive(context: Context, intent: Intent) {

        DaggerServiceComponent.builder()
            .appComponent((context.applicationContext as GraphComponentAccessor).appComponent)
            .build()
            .inject(this)

        @SuppressWarnings("unchecked")
        val hashData = intent.getSerializableExtra(EXTRA_DATA) as HashMap<String, *>
        val group = intent.getStringExtra(EXTRA_NOTIFICATION_GROUP)
        val groupId = intent.getIntExtra(EXTRA_NOTIFICATION_GROUP_ID, -1)

        val notificationManagerCompat = NotificationManagerCompat.from(context)

        // Cancel any left empty group of the notification, if needed
        // This maybe the case if timeoutAfter has deleted the notification
        // Then only the empty group is left and needs to be cancelled
        notificationManagerCompat.cancelGroupIfNeeded(group, groupId)

        runBlocking {
            try {
                integrationRepository.fireEvent("mobile_app_notification_dismissed", hashData)
                Log.d(TAG, "Notification dismiss event successful!")
            } catch (e: Exception) {
                Log.e(TAG, "Issue sending event to Home Assistant", e)
                Toast.makeText(
                    context,
                    R.string.notification_dismiss_failure,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
