package town.amrita.timetable.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import java.time.LocalTime
import android.util.Log

@Serializable
data class Timetable(
  val subjects: HashMap<String, Subject>,
  val schedule: HashMap<String, Array<String>>
) {
  val slots: Array<String> = arrayOf(
    "8:10-9:00",
    "9:00-9:50",
    "9:50-10:40",
    "11:00-11:50",
    "11:50-12:40",
    "2:00-2:50",
    "2:50-3:40"
  )
  val labSlots: Array<String> = arrayOf(
    "8:10-10:25",
    "10:50-1:05",
    "1:25-3:40"
  )
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Subject(
  val name: String,
  val code: String,
  val faculty: String,
  val shortName: String = name.split(" ").map({ e -> e[0] }).filter({ e -> e.isUpperCase() })
    .joinToString(separator = "")
)

@Serializable
data class TimetableSpec(
  val year: String,
  val section: String,
  val semester: String
) {
  override fun toString(): String {
    return "${this.year}_${this.section}_${this.semester}"
  }

  companion object {
    fun fromString(string: String): TimetableSpec {
      val sp = string.split("_")
      if (sp.size != 3)
        throw IllegalArgumentException("Invalid spec: $string")

      return TimetableSpec(sp[0], sp[1], sp[2])
    }
  }
}

data class TimetableDisplayEntry(
  val name: String,
  val shortName: String,
  val slot: String,
  val start: Int,
  val end: Int,
  val lab: Boolean,
  val isActive: Boolean = false
)

val FREE_SUBJECT = Subject("Free", "", "")
val UNKNOWN_SUBJECT = Subject("⚠️ Unknown", "", "")

fun getCurrentPeriodIndex(): Int? {
  val now = LocalTime.now()
  val periodTimes = listOf(
    LocalTime.of(8, 10),  // Period 1 start
    LocalTime.of(9, 0),   // Period 2 start
    LocalTime.of(9, 50),  // Period 3 start
    LocalTime.of(11, 0),  // Period 4 start (after break)
    LocalTime.of(11, 50), // Period 5 start
    LocalTime.of(14, 0),  // Period 6 start (after lunch)
    LocalTime.of(14, 50)  // Period 7 start
  )
  
  val periodEndTimes = listOf(
    LocalTime.of(9, 0),   // Period 1 end
    LocalTime.of(9, 50),  // Period 2 end
    LocalTime.of(10, 40), // Period 3 end
    LocalTime.of(11, 50), // Period 4 end
    LocalTime.of(12, 40), // Period 5 end
    LocalTime.of(14, 50), // Period 6 end
    LocalTime.of(15, 40)  // Period 7 end
  )
  
  //android.util.Log.d("PeriodHighlight", "Current time: $now")
  
  for (i in periodTimes.indices) {
    val startTime = periodTimes[i]
    val endTime = periodEndTimes[i]
    //android.util.Log.d("PeriodHighlight", "Checking period $i: $startTime - $endTime")
    
    if (now >= startTime && now < endTime) {
      //android.util.Log.d("PeriodHighlight", "Found active period: $i")
      return i
    }
  }
  
  //android.util.Log.d("PeriodHighlight", "No active period found")
  return null
}

// Test function to verify period detection logic
fun testPeriodDetection(testTime: LocalTime): Int? {
  val periodTimes = listOf(
    LocalTime.of(8, 10),  // Period 1 start
    LocalTime.of(9, 0),   // Period 2 start
    LocalTime.of(9, 50),  // Period 3 start
    LocalTime.of(11, 0),  // Period 4 start (after break)
    LocalTime.of(11, 50), // Period 5 start
    LocalTime.of(14, 0),  // Period 6 start (after lunch)
    LocalTime.of(14, 50)  // Period 7 start
  )
  
  val periodEndTimes = listOf(
    LocalTime.of(9, 0),   // Period 1 end
    LocalTime.of(9, 50),  // Period 2 end
    LocalTime.of(10, 40), // Period 3 end
    LocalTime.of(11, 50), // Period 4 end
    LocalTime.of(12, 40), // Period 5 end
    LocalTime.of(14, 50), // Period 6 end
    LocalTime.of(15, 40)  // Period 7 end
  )
  
  //android.util.Log.d("PeriodTest", "Testing time: $testTime")
  
  for (i in periodTimes.indices) {
    val startTime = periodTimes[i]
    val endTime = periodEndTimes[i]
    
    if (testTime >= startTime && testTime < endTime) {
      //android.util.Log.d("PeriodTest", "Test found active period: $i")
      return i
    }
  }
  
  //android.util.Log.d("PeriodTest", "Test found no active period")
  return null
}

fun buildTimetableDisplay(day: String, timetable: Timetable, showFreePeriods: Boolean = true): List<TimetableDisplayEntry> {
  if (!timetable.schedule.containsKey(day))
    return emptyList()

  val times: MutableList<TimetableDisplayEntry> = mutableListOf()
  var i = 0

  for (x in timetable.schedule[day] ?: arrayOf()) {
    val name = x.removeSuffix("_LAB")
    val isLab = x.endsWith("_LAB")

    val subject = when (name) {
      "FREE" -> FREE_SUBJECT
      in timetable.subjects -> timetable.subjects[name] ?: UNKNOWN_SUBJECT
      else -> UNKNOWN_SUBJECT
    }

    val offset = if (isLab)
      when (i) {
        0 -> 3
        else -> 2
      }
    else 1

    val slot = if (isLab)
      when (i) {
        0 -> timetable.labSlots[0]
        3 -> timetable.labSlots[1]
        5 -> timetable.labSlots[2]
        else -> "⚠️ UNKNOWN"
      }
    else timetable.slots[i]

    if(!(showFreePeriods && subject == FREE_SUBJECT)) {
      val currentPeriodIndex = getCurrentPeriodIndex()
      val isActive = currentPeriodIndex != null && i <= currentPeriodIndex && currentPeriodIndex <= i + offset - 1
      
      times.add(
        TimetableDisplayEntry(
          subject.name,
          subject.shortName,
          slot,
          i,
          i + offset - 1,
          isLab,
          isActive
        )
      )
    }

    i += offset
  }

  return times
}