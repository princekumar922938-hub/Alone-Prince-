package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Kharcha", appName)
  }

  @Test
  fun `verify indian currency formatting with grouping`() {
    // 12500000 paise = ₹1,25,000.00
    assertEquals("₹1,25,000.00", CurrencyUtils.formatInr(12500000L))
    // 5000 paise = ₹50.00
    assertEquals("₹50.00", CurrencyUtils.formatInr(5000L))
    // 100000 paise = ₹1,000.00
    assertEquals("₹1,000.00", CurrencyUtils.formatInr(100000L))
    // 1000000000 paise = ₹1,00,00,000.00 (1 crore)
    assertEquals("₹1,00,00,000.00", CurrencyUtils.formatInr(1000000000L))
  }

  @Test
  fun `verify amount parsing to paise`() {
    assertEquals(125000L, CurrencyUtils.parseAmountToPaise("1250"))
    assertEquals(125050L, CurrencyUtils.parseAmountToPaise("1250.50"))
    assertEquals(125075L, CurrencyUtils.parseAmountToPaise("1,250.75"))
  }

  @Test
  fun `verify indian date formatting`() {
    val epochDay = DateUtils.todayEpochDay()
    val formatted = DateUtils.formatDate(epochDay)
    // Matches DD/MM/YYYY pattern
    assertNotNull(formatted)
    assertEquals(3, formatted.split("/").size)
  }
}
