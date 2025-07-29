package town.amrita.timetable.widget

import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.os.Build
import kotlinx.coroutines.flow.first
import town.amrita.timetable.models.updateDay
import town.amrita.timetable.utils.TODAY
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import android.util.Log

class TimetableAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == "town.amrita.timetable.PERIOD_UPDATE") {
      Log.d("TimetableAlarm", "Period update triggered")
      // Update the widget when a period starts
      kotlinx.coroutines.runBlocking {
        context.updateDay(TODAY)
      }
      // Schedule the next alarm
      TimetableAlarmScheduler.scheduleNextPeriodAlarm(context)
    }
  }
}

object TimetableAlarmScheduler {
  private val PERIOD_TIMES = listOf(
    LocalTime.of(8, 10),  // Period 1 start
    LocalTime.of(9, 0),   // Period 2 start
    LocalTime.of(9, 50),  // Period 3 start
    LocalTime.of(11, 0),  // Period 4 start (after break)
    LocalTime.of(11, 50), // Period 5 start
    LocalTime.of(14, 0),  // Period 6 start (after lunch)
    LocalTime.of(14, 50)  // Period 7 start
  )

  fun schedulePeriodAlarms(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    // Cancel any existing alarms
    cancelAllAlarms(context)
    
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    
    Log.d("TimetableAlarm", "Scheduling alarms for ${today} at ${now}")
    
    // Schedule alarms for each period time today
    PERIOD_TIMES.forEachIndexed { index, time ->
      val periodStart = LocalDateTime.of(today, time)
      
      // Schedule if the period hasn't started yet today, or if it's within the next 5 minutes
      val timeUntilPeriod = java.time.Duration.between(now, periodStart)
      if (periodStart.isAfter(now) || timeUntilPeriod.toMinutes() >= -5) {
        scheduleAlarm(context, alarmManager, periodStart, index)
        Log.d("TimetableAlarm", "Scheduled alarm for period ${index + 1} at ${periodStart}")
      } else {
        Log.d("TimetableAlarm", "Skipped period ${index + 1} at ${periodStart} (already passed)")
      }
    }
    
    // Schedule alarms for tomorrow
    val tomorrow = today.plusDays(1)
    PERIOD_TIMES.forEachIndexed { index, time ->
      val periodStart = LocalDateTime.of(tomorrow, time)
      scheduleAlarm(context, alarmManager, periodStart, index)
      Log.d("TimetableAlarm", "Scheduled alarm for tomorrow period ${index + 1} at ${periodStart}")
    }
  }
  
  private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, periodStart: LocalDateTime, periodIndex: Int) {
    val intent = Intent(context, TimetableAlarmReceiver::class.java).apply {
      action = "town.amrita.timetable.PERIOD_UPDATE"
      putExtra("period_index", periodIndex)
    }
    
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      periodIndex,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    val triggerTime = periodStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val currentTime = System.currentTimeMillis()
    val delayMs = triggerTime - currentTime
    
    Log.d("TimetableAlarm", "Setting alarm for period ${periodIndex + 1}: trigger at ${periodStart} (in ${delayMs}ms)")
    
    // Try multiple alarm types for better reliability
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // For Android 12+, use setAlarmClock for better reliability
        val info = android.app.AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        alarmManager.setAlarmClock(info, pendingIntent)
        Log.d("TimetableAlarm", "Used setAlarmClock for period ${periodIndex + 1}")
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Log.d("TimetableAlarm", "Used setExactAndAllowWhileIdle for period ${periodIndex + 1}")
      } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Log.d("TimetableAlarm", "Used setExact for period ${periodIndex + 1}")
      }
      
      Log.d("TimetableAlarm", "Alarm set successfully for period ${periodIndex + 1}")
    } catch (e: Exception) {
      Log.e("TimetableAlarm", "Failed to set alarm for period ${periodIndex + 1}: ${e.message}")
    }
  }
  
  fun scheduleNextPeriodAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    
    // Find the next period time
    val nextPeriodTime = PERIOD_TIMES.find { time ->
      LocalDateTime.of(today, time).isAfter(now)
    }
    
    if (nextPeriodTime != null) {
      val periodIndex = PERIOD_TIMES.indexOf(nextPeriodTime)
      val periodStart = LocalDateTime.of(today, nextPeriodTime)
      scheduleAlarm(context, alarmManager, periodStart, periodIndex)
    } else {
      // If no more periods today, schedule for tomorrow
      val tomorrow = today.plusDays(1)
      val periodStart = LocalDateTime.of(tomorrow, PERIOD_TIMES[0])
      scheduleAlarm(context, alarmManager, periodStart, 0)
    }
  }
  
  fun cancelAllAlarms(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    PERIOD_TIMES.forEachIndexed { index, _ ->
      val intent = Intent(context, TimetableAlarmReceiver::class.java).apply {
        action = "town.amrita.timetable.PERIOD_UPDATE"
      }
      
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        index,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
      )
      
      pendingIntent?.let {
        alarmManager.cancel(it)
        it.cancel()
      }
    }
  }
}

// Legacy UpdateWorker removed - using AlarmManager instead