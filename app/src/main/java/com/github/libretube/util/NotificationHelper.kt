package com.github.libretube.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.obj.StreamItem
import com.github.libretube.preferences.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

object NotificationHelper {
    fun enqueueWork(
        context: Context
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val myWorkBuilder = PeriodicWorkRequest.Builder(
            NotificationWorker::class.java,
            1,
            TimeUnit.SECONDS
        )
            .setConstraints(constraints)

        val myWork = myWorkBuilder.build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "NotificationService",
                ExistingPeriodicWorkPolicy.REPLACE,
                myWork
            )
    }

    fun checkForNewStreams(context: Context) {
        val token = PreferenceHelper.getToken()
        var response: List<StreamItem>
        runBlocking {
            val task = async {
                RetrofitInstance.authApi.getFeed(token)
            }
            response = task.await()
      }
        createNotification(
            context,
            response[0].title.toString(),
            response[0].uploaderName.toString()
        )
    }

    fun createNotification(context: Context, title: String, description: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "notification_worker")
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(2, builder.build())
        }

    }

}
