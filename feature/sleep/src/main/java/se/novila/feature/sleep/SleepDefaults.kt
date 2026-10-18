package se.novila.feature.sleep

import java.time.LocalTime

/** Where the time pickers open before the user has given a bedtime or a wake time. */
internal val DefaultBedtime: LocalTime = LocalTime.of(23, 0)
internal val DefaultWakeTime: LocalTime = LocalTime.of(7, 0)
