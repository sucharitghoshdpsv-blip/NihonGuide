package com.example

import com.example.data.model.GuideContent
import com.example.data.remote.CurrencyRateRepository
import com.example.security.TotpManager
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun totpManager_generatesAndVerifiesSixDigitCode() {
        val secret = TotpManager.generateSecretBase32()
        assertTrue(secret.length >= 16)
        val now = 1_700_000_000_000L
        val code = TotpManager.generateCurrentCode(secret, now)
        assertEquals(6, code.length)
        assertTrue(TotpManager.verifyCode(secret, code, now))
        assertFalse(TotpManager.verifyCode(secret, "000000", now))
    }

    @Test
    fun resolveEtiquetteForLocation_prefersDistrictOverCityAndFallsBackCleanly() {
        val gion = GuideContent.resolveEtiquetteForLocation(
            city = "Kyoto",
            districtOrTitle = "Evening stroll in Gion Hanamikoji",
            notes = "Teahouses"
        )
        assertEquals("kyoto_gion", gion.zoneKey)
        assertTrue(gion.briefTips.isNotEmpty())

        val nara = GuideContent.resolveEtiquetteForLocation(
            city = "Nara",
            districtOrTitle = "Todai-ji",
            notes = ""
        )
        assertEquals("city_nara", nara.zoneKey)

        val custom = GuideContent.resolveEtiquetteForLocation(
            city = "Takayama",
            districtOrTitle = "Old Town"
        )
        assertEquals("Takayama", custom.cityName)
        assertTrue(custom.briefTips.size >= 2)
    }

    @Test
    fun resolveEtiquetteForCoordinates_matchesDistrictAndIgnoresOutOfJapan() {
        // Coordinates near Shibuya Crossing
        val shibuya = GuideContent.resolveEtiquetteForCoordinates(35.6595, 139.7004)
        assertNotNull(shibuya)
        assertEquals("tokyo_shibuya", shibuya?.zoneKey)

        // Coordinates far outside Japan
        val outside = GuideContent.resolveEtiquetteForCoordinates(37.7749, -122.4194)
        assertNull(outside)
    }

    @Test
    fun currencyRateRepository_convertsBidirectionallyAndFormatsCleanly() {
        // Bidirectional conversion
        assertEquals(19.8, CurrencyRateRepository.convertJpyToHome(3000L, 0.0066), 1e-4)
        assertEquals(3030L, CurrencyRateRepository.convertHomeToJpy(20.0, 0.0066))

        // Zero-decimal vs decimal formatting
        assertEquals("$19.80", CurrencyRateRepository.formatHomeCurrency(19.8, "USD"))
        assertEquals("₩9,150", CurrencyRateRepository.formatHomeCurrency(9150.4, "KRW"))
    }

    @Test
    fun currencyRateRepository_resolvesHomeCurrencyExcludingJpy() {
        assertEquals("USD", CurrencyRateRepository.resolveDefaultHomeCurrency(Locale.JAPAN))
        assertEquals("USD", CurrencyRateRepository.resolveDefaultHomeCurrency(Locale.US))
        assertEquals("GBP", CurrencyRateRepository.resolveDefaultHomeCurrency(Locale.UK))
    }
}
