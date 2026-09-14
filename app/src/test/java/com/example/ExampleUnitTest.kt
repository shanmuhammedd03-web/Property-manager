package com.example

import com.example.util.DateTimeUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testTimeToMinutes() {
    assertEquals(0, DateTimeUtils.timeToMinutes("00:00"))
    assertEquals(600, DateTimeUtils.timeToMinutes("10:00"))
    assertEquals(750, DateTimeUtils.timeToMinutes("12:30"))
    assertEquals(1439, DateTimeUtils.timeToMinutes("23:59"))
  }

  @Test
  fun testDoTimesOverlap_exactSameTime_overlaps() {
    assertTrue(DateTimeUtils.doTimesOverlap("10:00", "12:00", "10:00", "12:00"))
  }

  @Test
  fun testDoTimesOverlap_overlappingRanges_overlaps() {
    // Starts before, ends inside
    assertTrue(DateTimeUtils.doTimesOverlap("09:30", "10:30", "10:00", "12:00"))
    // Starts inside, ends after
    assertTrue(DateTimeUtils.doTimesOverlap("11:30", "12:30", "10:00", "12:00"))
    // Completely inside
    assertTrue(DateTimeUtils.doTimesOverlap("10:30", "11:30", "10:00", "12:00"))
    // Completely encompasses
    assertTrue(DateTimeUtils.doTimesOverlap("09:00", "13:00", "10:00", "12:00"))
  }

  @Test
  fun testDoTimesOverlap_adjacentTimes_doesNotOverlap() {
    // Ends exactly when next starts
    assertFalse(DateTimeUtils.doTimesOverlap("08:00", "10:00", "10:00", "12:00"))
    // Starts exactly when previous ends
    assertFalse(DateTimeUtils.doTimesOverlap("12:00", "14:00", "10:00", "12:00"))
  }

  @Test
  fun testDoTimesOverlap_completelyDisjoint_doesNotOverlap() {
    assertFalse(DateTimeUtils.doTimesOverlap("06:00", "08:00", "10:00", "12:00"))
    assertFalse(DateTimeUtils.doTimesOverlap("14:00", "16:00", "10:00", "12:00"))
  }
}

