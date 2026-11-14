package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

data class HomeCurrencyOption(
    val code: String,
    val name: String,
    val symbol: String
)

data class LiveFxSnapshot(
    val baseCurrency: String = "JPY",
    val rates: Map<String, Double>,
    val provider: String,
    val fetchedAtMillis: Long,
    val rawRatesJson: String
)

object CurrencyRateRepository {

    val supportedCurrencies: List<HomeCurrencyOption> = listOf(
        HomeCurrencyOption("USD", "US Dollar", "$"),
        HomeCurrencyOption("EUR", "Euro", "€"),
        HomeCurrencyOption("GBP", "British Pound", "£"),
        HomeCurrencyOption("INR", "Indian Rupee", "₹"),
        HomeCurrencyOption("AUD", "Australian Dollar", "A$"),
        HomeCurrencyOption("CAD", "Canadian Dollar", "C$"),
        HomeCurrencyOption("SGD", "Singapore Dollar", "S$"),
        HomeCurrencyOption("KRW", "South Korean Won", "₩"),
        HomeCurrencyOption("TWD", "New Taiwan Dollar", "NT$"),
        HomeCurrencyOption("THB", "Thai Baht", "฿"),
        HomeCurrencyOption("HKD", "Hong Kong Dollar", "HK$"),
        HomeCurrencyOption("CNY", "Chinese Yuan", "¥"),
        HomeCurrencyOption("NZD", "New Zealand Dollar", "NZ$"),
        HomeCurrencyOption("CHF", "Swiss Franc", "CHF"),
        HomeCurrencyOption("MYR", "Malaysian Ringgit", "RM"),
        HomeCurrencyOption("PHP", "Philippine Peso", "₱"),
        HomeCurrencyOption("VND", "Vietnamese Dong", "₫"),
        HomeCurrencyOption("IDR", "Indonesian Rupiah", "Rp"),
        HomeCurrencyOption("AED", "UAE Dirham", "AED"),
        HomeCurrencyOption("SAR", "Saudi Riyal", "SAR"),
        HomeCurrencyOption("SEK", "Swedish Krona", "kr"),
        HomeCurrencyOption("NOK", "Norwegian Krone", "kr"),
        HomeCurrencyOption("DKK", "Danish Krone", "kr"),
        HomeCurrencyOption("BRL", "Brazilian Real", "R$"),
        HomeCurrencyOption("MXN", "Mexican Peso", "MX$")
    )

    val quickCurrencyCodes: List<String> = listOf(
        "USD", "EUR", "GBP", "INR", "AUD", "CAD", "SGD", "KRW", "TWD", "THB"
    )

    fun findCurrencyOption(code: String): HomeCurrencyOption {
        val upper = code.uppercase(Locale.US)
        return supportedCurrencies.firstOrNull { it.code == upper }
            ?: runCatching {
                val c = Currency.getInstance(upper)
                HomeCurrencyOption(upper, c.displayName, c.symbol)
            }.getOrElse {
                HomeCurrencyOption(upper, upper, upper)
            }
    }

    fun resolveDefaultHomeCurrency(locale: Locale = Locale.getDefault()): String {
        val detected = runCatching { Currency.getInstance(locale)?.currencyCode }.getOrNull()
        if (detected.isNullOrBlank() || detected.equals("JPY", ignoreCase = true)) {
            return "USD"
        }
        return detected.uppercase(Locale.US)
    }

    fun convertJpyToHome(amountJpy: Long, ratePerJpy: Double): Double =
        amountJpy.toDouble() * ratePerJpy

    fun convertJpyToHome(amountJpy: Double, ratePerJpy: Double): Double =
        amountJpy * ratePerJpy

    fun convertHomeToJpy(homeAmount: Double, ratePerJpy: Double): Long {
        if (ratePerJpy <= 0.0 || homeAmount <= 0.0) return 0L
        return (homeAmount / ratePerJpy).roundToLong()
    }

    fun formatHomeCurrency(amount: Double, currencyCode: String): String {
        val upper = currencyCode.uppercase(Locale.US)
        val zeroDecimal = upper in setOf("KRW", "VND", "IDR", "JPY")
        return runCatching {
            val fmt = NumberFormat.getCurrencyInstance(Locale.US)
            fmt.currency = Currency.getInstance(upper)
            fmt.minimumFractionDigits = if (zeroDecimal) 0 else 2
            fmt.maximumFractionDigits = if (zeroDecimal) 0 else 2
            fmt.format(amount)
        }.getOrElse {
            val opt = findCurrencyOption(upper)
            if (zeroDecimal) "${opt.symbol}${amount.roundToLong()} $upper"
            else "${opt.symbol}${"%.2f".format(Locale.US, amount)} $upper"
        }
    }

    fun parseOpenErApiResponse(jsonText: String, nowMillis: Long = System.currentTimeMillis()): LiveFxSnapshot {
        val root = JSONObject(jsonText)
        val resultStatus = root.optString("result", "")
        if (resultStatus.isNotBlank() && !resultStatus.equals("success", ignoreCase = true)) {
            val errorType = root.optString("error-type", "FX provider returned error")
            throw IllegalStateException("FX API error: $errorType")
        }
        val ratesObj = root.getJSONObject("rates")
        val parsed = extractRatesMap(ratesObj)
        require(parsed.isNotEmpty()) { "No exchange rates present in response." }
        return LiveFxSnapshot(
            baseCurrency = root.optString("base_code", "JPY"),
            rates = parsed,
            provider = "ExchangeRate-API",
            fetchedAtMillis = nowMillis,
            rawRatesJson = ratesObj.toString()
        )
    }

    fun parseFrankfurterResponse(jsonText: String, nowMillis: Long = System.currentTimeMillis()): LiveFxSnapshot {
        val root = JSONObject(jsonText)
        val ratesObj = root.getJSONObject("rates")
        val parsed = extractRatesMap(ratesObj)
        require(parsed.isNotEmpty()) { "No exchange rates present in Frankfurter response." }
        return LiveFxSnapshot(
            baseCurrency = root.optString("base", "JPY"),
            rates = parsed,
            provider = "Frankfurter (ECB)",
            fetchedAtMillis = nowMillis,
            rawRatesJson = ratesObj.toString()
        )
    }

    fun parseCachedRatesJson(rawRatesJson: String): Map<String, Double> {
        if (rawRatesJson.isBlank()) return emptyMap()
        return runCatching { extractRatesMap(JSONObject(rawRatesJson)) }.getOrDefault(emptyMap())
    }

    private fun extractRatesMap(ratesObj: JSONObject): Map<String, Double> {
        val out = LinkedHashMap<String, Double>()
        val keys = ratesObj.keys()
        while (keys.hasNext()) {
            val key = keys.next().uppercase(Locale.US)
            val rate = ratesObj.optDouble(key, Double.NaN)
            if (!rate.isNaN() && rate > 0.0) {
                out[key] = rate
            }
        }
        out["JPY"] = 1.0
        return out
    }

    suspend fun fetchLatestJpyRates(): Result<LiveFxSnapshot> = withContext(Dispatchers.IO) {
        val primary = runCatching {
            val body = httpGet("https://open.er-api.com/v6/latest/JPY")
            parseOpenErApiResponse(body)
        }
        if (primary.isSuccess) return@withContext primary

        val secondary = runCatching {
            val body = httpGet("https://api.frankfurter.app/latest?from=JPY")
            parseFrankfurterResponse(body)
        }
        if (secondary.isSuccess) return@withContext secondary

        Result.failure(
            primary.exceptionOrNull()
                ?: secondary.exceptionOrNull()
                ?: IllegalStateException("Unable to reach live FX providers.")
        )
    }

    private fun httpGet(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TabiGuide-Android/1.0")
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code from FX service")
            }
            body
        } finally {
            conn.disconnect()
        }
    }
}
