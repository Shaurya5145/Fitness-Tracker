package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mealName = intent.getStringExtra(EXTRA_MEAL_NAME) ?: "Meal"
        val mealId = intent.getLongExtra(EXTRA_MEAL_ID, -1L)
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
        
        val quotes = listOf(
            "Fuel your body, feed your soul!",
            "Don't skip it, your muscles need it!",
            "Time to eat! You're one step closer to your goals.",
            "Nutrition is 80% of the work. Let's go!",
            "Eat well, train hard, live better."
        )
        val quote = quotes.random()

        showNotification(context, mealName, quote)

        // Reschedule for next day to ensure it triggers perfectly every day
        if (mealId != -1L && hour != -1 && minute != -1) {
            // Because ReminderScheduler automatically adds 1 day if time is in the past,
            // calling this will schedule it beautifully for tomorrow.
            ReminderScheduler.scheduleMealReminder(context, mealId, mealName, hour, minute)
        }
    }

    private fun showNotification(context: Context, mealName: String, quote: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Meal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to eat your meals"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            i,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure a suitable icon is used
            .setContentTitle("Time for $mealName!")
            .setContentText(quote)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(mealName.hashCode(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "meal_reminders_channel"
        const val EXTRA_MEAL_NAME = "extra_meal_name"
        const val EXTRA_MEAL_ID = "extra_meal_id"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
    }
}

object ReminderScheduler {
    fun scheduleMealReminder(context: Context, mealId: Long, mealName: String, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            putExtra(MealReminderReceiver.EXTRA_MEAL_NAME, mealName)
            putExtra(MealReminderReceiver.EXTRA_MEAL_ID, mealId)
            putExtra(MealReminderReceiver.EXTRA_HOUR, hour)
            putExtra(MealReminderReceiver.EXTRA_MINUTE, minute)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mealId.toInt(), // Use mealId as requestCode to update/cancel existing alarms for this meal
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Use exact alarms to ensure it triggers perfectly (Doze/Sleep resilient)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelMealReminder(context: Context, mealId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MealReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mealId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
