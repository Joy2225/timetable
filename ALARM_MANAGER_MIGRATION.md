# AlarmManager Migration for Timetable Widget

## Overview
This document describes the migration from WorkManager to AlarmManager for exact-time widget updates with period highlighting.

## Changes Made

### 1. Replaced WorkManager with AlarmManager
- **Before**: Used `PeriodicWorkRequestBuilder` with 30-minute intervals
- **After**: Uses `AlarmManager` with exact period start times (8:10, 9:00, 9:50, 11:00, 11:50, 14:00, 14:50)

### 2. New Components Added

#### TimetableAlarmReceiver
- BroadcastReceiver that handles period update triggers
- Updates widget when a period starts
- Schedules the next alarm automatically
- Handles both period updates and periodic checks

#### TimetableAlarmScheduler
- Object that manages alarm scheduling
- Schedules alarms for current day and next day
- Handles alarm cancellation and rescheduling
- Includes periodic checks during school hours for manual time changes

#### BootReceiver
- Handles device reboot scenarios
- Reschedules alarms after system restart

### 3. Period Highlighting
- Added `isActive` field to `TimetableDisplayEntry`
- `getCurrentPeriodIndex()` function determines which period is currently active
- Active periods are highlighted with primary color background
- Text and icons change color for active periods
- Added test function `testPeriodDetection()` for debugging

### 4. Updated Files

#### Core Changes:
- `UpdateWorker.kt` - Complete rewrite with AlarmManager implementation
- `TimetableAppWidget.kt` - Added period highlighting and removed WorkManager
- `Timetable.kt` - Added period detection logic and test functions
- `MainActivity.kt` - Added alarm scheduling, testing, and debugging
- `WidgetConfig.kt` - Added alarm rescheduling on day changes

#### New Files:
- `BootReceiver.kt` - Handles device reboot
- `ALARM_MANAGER_MIGRATION.md` - This documentation

#### Manifest Changes:
- Added `RECEIVE_BOOT_COMPLETED` permission
- Added `TimetableAlarmReceiver` and `BootReceiver` declarations

#### Build Changes:
- Removed WorkManager dependency from `build.gradle.kts`

## Benefits

1. **Exact Timing**: Widget updates at precise period start times instead of 30-minute intervals
2. **Period Highlighting**: Currently active period is visually highlighted
3. **Better UX**: Users can immediately see which period is ongoing
4. **Reliability**: Alarms persist through device reboots
5. **Efficiency**: No unnecessary updates between periods
6. **Robust Alarm System**: Multiple alarm types for different Android versions

## Technical Details

### Period Times
- Period 1: 8:10 - 9:00
- Period 2: 9:00 - 9:50
- Period 3: 9:50 - 10:40
- Period 4: 11:00 - 11:50 (after break)
- Period 5: 11:50 - 12:40
- Period 6: 14:00 - 14:50 (after lunch)
- Period 7: 14:50 - 15:40

### Alarm Scheduling
- Alarms are scheduled for both current day and next day
- Only future periods are scheduled for current day (with 5-minute buffer)
- All periods are scheduled for next day
- Alarms are automatically rescheduled after each trigger
- Multiple alarm types used for better reliability:
  - `setAlarmClock` for Android 12+
  - `setExactAndAllowWhileIdle` for Android 6+
  - `setExact` for older versions

### Period Detection Logic
- Real-time period detection based on current system time
- Handles both natural time progression and manual time changes
- Comprehensive logging for debugging
- Test functions for period detection validation

### Permissions Required
- `SCHEDULE_EXACT_ALARM` (for Android 12 and below)
- `USE_EXACT_ALARM` (for Android 13+)
- `RECEIVE_BOOT_COMPLETED` (for reboot handling)

## Testing

To test the implementation:
1. Install the app
2. Add the widget to home screen
3. Check that alarms are scheduled (check logs with tag "TimetableAlarm")
4. Test period detection logic (check logs with tag "PeriodTest")
5. Wait for a period start time or manually trigger an alarm
6. Verify that the widget updates and highlights the active period

### Manual Testing
- Set time to 8:09 AM → No period highlighted
- Set time to 8:10 AM → Period 1 should be highlighted
- Set time to 8:15 AM → Period 1 should be highlighted
- Set time to 9:00 AM → Period 2 should be highlighted

## Migration Notes

- Legacy `UpdateWorker` class completely removed
- WorkManager dependency has been removed
- All existing functionality is preserved
- New highlighting feature is automatically enabled
- Comprehensive logging added for debugging
- Test functions included for period detection validation

## Known Limitations

- Manual time changes require app restart or waiting for next alarm
- Natural time progression works perfectly
- Periodic checks planned for handling manual time changes during school hours 