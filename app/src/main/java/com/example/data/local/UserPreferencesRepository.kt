package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tabiguide_prefs")

enum class ThemeMode(val label: String, val japaneseLabel: String) {
    SYSTEM("System", "適応"),
    LIGHT("Washi & Torii", "和紙と鳥居"),
    DARK("Sumi & Lantern", "墨と提灯"),
    SAKURA("Sakura Mist", "桜霞"),
    MATCHA("Matcha Zen", "宇治抹茶"),
    CYBER_TOKYO("Shinjuku Neon", "新宿ネオン")
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val biometricVaultLock: Boolean = false,
    val lastSyncEpochMillis: Long = 0L,
    val homeCurrencyCode: String = "",
    val cachedFxRatesJson: String = "",
    val cachedFxFetchedAtMillis: Long = 0L,
    val cachedFxProvider: String = ""
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BIOMETRIC_VAULT_LOCK = booleanPreferencesKey("biometric_vault_lock")
        val LAST_SYNC_EPOCH = longPreferencesKey("last_sync_epoch")
        val HOME_CURRENCY_CODE = stringPreferencesKey("home_currency_code")
        val CACHED_FX_RATES_JSON = stringPreferencesKey("cached_fx_rates_json")
        val CACHED_FX_FETCHED_AT = longPreferencesKey("cached_fx_fetched_at")
        val CACHED_FX_PROVIDER = stringPreferencesKey("cached_fx_provider")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val modeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = ThemeMode.entries.firstOrNull { it.name == modeName } ?: ThemeMode.SYSTEM
        AppPreferences(
            themeMode = themeMode,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            biometricVaultLock = prefs[Keys.BIOMETRIC_VAULT_LOCK] ?: false,
            lastSyncEpochMillis = prefs[Keys.LAST_SYNC_EPOCH] ?: 0L,
            homeCurrencyCode = prefs[Keys.HOME_CURRENCY_CODE] ?: "",
            cachedFxRatesJson = prefs[Keys.CACHED_FX_RATES_JSON] ?: "",
            cachedFxFetchedAtMillis = prefs[Keys.CACHED_FX_FETCHED_AT] ?: 0L,
            cachedFxProvider = prefs[Keys.CACHED_FX_PROVIDER] ?: ""
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setBiometricVaultLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_VAULT_LOCK] = enabled }
    }

    suspend fun updateLastSync(epochMillis: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_EPOCH] = epochMillis }
    }

    suspend fun setHomeCurrencyCode(code: String) {
        context.dataStore.edit { it[Keys.HOME_CURRENCY_CODE] = code.uppercase() }
    }

    suspend fun saveCachedFxRates(ratesJson: String, fetchedAtMillis: Long, provider: String) {
        context.dataStore.edit {
            it[Keys.CACHED_FX_RATES_JSON] = ratesJson
            it[Keys.CACHED_FX_FETCHED_AT] = fetchedAtMillis
            it[Keys.CACHED_FX_PROVIDER] = provider
        }
    }
}
