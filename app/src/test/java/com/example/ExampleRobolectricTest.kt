package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.CurrencyRateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TabiGuide", appName)
  }

  @Test
  fun `parses open-er-api and frankfurter payloads and cached rates`() {
    val openErJson = """
      {
        "result": "success",
        "base_code": "JPY",
        "rates": {
          "USD": 0.0066,
          "EUR": 0.0061,
          "KRW": 9.15
        }
      }
    """.trimIndent()
    val openErQuote = CurrencyRateRepository.parseOpenErApiResponse(openErJson, nowMillis = 12345L)
    assertEquals("ExchangeRate-API", openErQuote.provider)
    assertEquals(0.0066, openErQuote.rates["USD"] ?: 0.0, 1e-6)
    assertEquals(1.0, openErQuote.rates["JPY"] ?: 0.0, 1e-6)

    val cachedParsed = CurrencyRateRepository.parseCachedRatesJson(openErQuote.rawRatesJson)
    assertEquals(0.0061, cachedParsed["EUR"] ?: 0.0, 1e-6)

    val frankfurterJson = """
      {
        "amount": 1.0,
        "base": "JPY",
        "date": "2026-04-01",
        "rates": {
          "GBP": 0.0052,
          "AUD": 0.0102
        }
      }
    """.trimIndent()
    val frankQuote = CurrencyRateRepository.parseFrankfurterResponse(frankfurterJson, nowMillis = 67890L)
    assertEquals("Frankfurter (ECB)", frankQuote.provider)
    assertEquals(0.0052, frankQuote.rates["GBP"] ?: 0.0, 1e-6)
    assertTrue(CurrencyRateRepository.parseCachedRatesJson("").isEmpty())
  }
}
