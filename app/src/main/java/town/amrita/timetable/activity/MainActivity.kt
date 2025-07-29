package town.amrita.timetable.activity

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import town.amrita.timetable.models.widgetConfig
import town.amrita.timetable.models.refreshWidget
import town.amrita.timetable.ui.RootScreen
import town.amrita.timetable.ui.TimetableTheme
import town.amrita.timetable.widget.TimetableAlarmScheduler
import android.util.Log

val LocalWidgetId = staticCompositionLocalOf { AppWidgetManager.INVALID_APPWIDGET_ID }

class MainActivity : ComponentActivity() {
  var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    appWidgetId = intent?.extras?.getInt(
      AppWidgetManager.EXTRA_APPWIDGET_ID,
      AppWidgetManager.INVALID_APPWIDGET_ID
    ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    // Schedule alarms for exact period updates
    TimetableAlarmScheduler.schedulePeriodAlarms(this)
    
    // For testing: Log current period detection
    val currentPeriod = town.amrita.timetable.models.getCurrentPeriodIndex()
    //android.util.Log.d("MainActivity", "Current period at startup: $currentPeriod")
    
    // For testing: Test period detection at 8:10 AM
    val testTime = java.time.LocalTime.of(8, 10)
    //android.util.Log.d("MainActivity", "Testing period detection at $testTime")
    val testResult = town.amrita.timetable.models.testPeriodDetection(testTime)
    //android.util.Log.d("MainActivity", "Test result for 8:10 AM: $testResult (should be 0)")
    
    // Test at 8:15 AM
    val testTime2 = java.time.LocalTime.of(8, 15)
    val testResult2 = town.amrita.timetable.models.testPeriodDetection(testTime2)
    //android.util.Log.d("MainActivity", "Test result for 8:15 AM: $testResult2 (should be 0)")
    
    // Test at 9:00 AM
    val testTime3 = java.time.LocalTime.of(9, 0)
    val testResult3 = town.amrita.timetable.models.testPeriodDetection(testTime3)
    //android.util.Log.d("MainActivity", "Test result for 9:00 AM: $testResult3 (should be 1)")
    
    // For testing: Force refresh widget after 5 seconds to test highlighting
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
      //android.util.Log.d("MainActivity", "Forcing widget refresh for testing")
      kotlinx.coroutines.runBlocking {
        this@MainActivity.refreshWidget()
      }
    }, 5000)
    
    // For testing: Manually trigger alarm after 10 seconds to test alarm functionality
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
      //android.util.Log.d("MainActivity", "Manually triggering alarm for testing")
      val intent = android.content.Intent(this@MainActivity, town.amrita.timetable.widget.TimetableAlarmReceiver::class.java).apply {
        action = "town.amrita.timetable.PERIOD_UPDATE"
        putExtra("period_index", 0)
      }
      sendBroadcast(intent)
    }, 10000)

    setContent {
      TimetableTheme {
        CompositionLocalProvider(LocalWidgetId provides appWidgetId) {
          RootScreen()
        }
      }
    }
  }

  override fun finish() {
    val currentFileKey = runBlocking { widgetConfig.data.first() }.file
    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && currentFileKey != null) {
      val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      setResult(RESULT_OK, resultValue)
    }
    super.finish()
  }
}