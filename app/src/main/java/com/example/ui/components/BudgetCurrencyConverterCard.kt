package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.CurrencyRateRepository
import com.example.ui.viewmodel.FxRateState
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetCurrencyConverterCard(
    fxState: FxRateState,
    onSelectHomeCurrency: (String) -> Unit,
    onRefreshRates: () -> Unit,
    onLogConvertedAmount: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberTabiHaptics()
    var jpyToHome by remember { mutableStateOf(true) }
    var amountInput by remember { mutableStateOf("1000") }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    val homeOption = remember(fxState.homeCurrency) {
        CurrencyRateRepository.findCurrencyOption(fxState.homeCurrency)
    }
    val ratePerJpy = fxState.activeRatePerJpy

    val quickCodes = remember(fxState.homeCurrency) {
        val base = CurrencyRateRepository.quickCurrencyCodes
        if (fxState.homeCurrency in base) base else listOf(fxState.homeCurrency) + base
    }

    val numericInput = amountInput.toDoubleOrNull() ?: 0.0
    val convertedJpyLong: Long = remember(numericInput, jpyToHome, ratePerJpy) {
        if (jpyToHome) numericInput.roundToLong().coerceAtLeast(0L)
        else if (ratePerJpy != null && ratePerJpy > 0.0) CurrencyRateRepository.convertHomeToJpy(numericInput, ratePerJpy)
        else 0L
    }
    val convertedResultLabel: String = remember(numericInput, jpyToHome, ratePerJpy, fxState.homeCurrency) {
        if (ratePerJpy == null || ratePerJpy <= 0.0) {
            "—"
        } else if (jpyToHome) {
            CurrencyRateRepository.formatHomeCurrency(
                CurrencyRateRepository.convertJpyToHome(numericInput, ratePerJpy),
                fxState.homeCurrency
            )
        } else {
            "¥${CurrencyRateRepository.convertHomeToJpy(numericInput, ratePerJpy)}"
        }
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "JPY ↔ ${fxState.homeCurrency} Real-Time Converter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val statusText = when {
                        fxState.isRefreshing -> "Fetching live market rate..."
                        ratePerJpy != null && fxState.fetchedAtMillis > 0L -> {
                            val timeStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(fxState.fetchedAtMillis))
                            val modeTag = if (fxState.isLiveSession) "Live" else "Cached"
                            val src = fxState.provider.ifBlank { "FX" }
                            "$modeTag • $src • $timeStr"
                        }
                        fxState.errorMessage != null -> "Offline • Tap refresh for live rate"
                        else -> "Waiting for live exchange rate..."
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (fxState.isLiveSession) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = {
                    haptics.tap()
                    onRefreshRates()
                },
                enabled = !fxState.isRefreshing,
                modifier = Modifier.testTag("refresh_fx_rates_button")
            ) {
                if (fxState.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh live JPY exchange rates",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Home Currency Selector
        Text(
            text = "HOME CURRENCY (${homeOption.name})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(quickCodes, key = { it }) { code ->
                FilterChip(
                    selected = fxState.homeCurrency == code,
                    onClick = {
                        haptics.tap()
                        onSelectHomeCurrency(code)
                    },
                    label = { Text(code, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("currency_chip_$code")
                )
            }
            item {
                AssistChip(
                    onClick = {
                        haptics.tap()
                        showCurrencyPicker = true
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("More") },
                    modifier = Modifier.testTag("more_currencies_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input + Direction Swap Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = amountInput,
                onValueChange = { raw ->
                    val filtered = buildString {
                        var dotSeen = false
                        for (ch in raw) {
                            if (ch.isDigit()) append(ch)
                            else if (!jpyToHome && ch == '.' && !dotSeen) {
                                append(ch)
                                dotSeen = true
                            }
                        }
                    }
                    amountInput = filtered.take(10)
                },
                label = {
                    Text(if (jpyToHome) "Amount in Japanese Yen (¥ JPY)" else "Amount in ${fxState.homeCurrency} (${homeOption.symbol})")
                },
                prefix = {
                    Text(if (jpyToHome) "¥ " else "${homeOption.symbol} ", fontWeight = FontWeight.Bold)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (jpyToHome) KeyboardType.Number else KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("currency_converter_input")
            )

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("swap_currency_direction_button")
            ) {
                IconButton(
                    onClick = {
                        haptics.toggle()
                        val nextJpyToHome = !jpyToHome
                        if (ratePerJpy != null && ratePerJpy > 0.0 && numericInput > 0.0) {
                            amountInput = if (nextJpyToHome) {
                                CurrencyRateRepository.convertHomeToJpy(numericInput, ratePerJpy).toString()
                            } else {
                                "%.2f".format(Locale.US, CurrencyRateRepository.convertJpyToHome(numericInput, ratePerJpy))
                            }
                        }
                        jpyToHome = nextJpyToHome
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap JPY and ${fxState.homeCurrency}"
                    )
                }
            }
        }

        if (jpyToHome) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(500L, 1000L, 3000L, 5000L, 10000L).forEach { preset ->
                    AssistChip(
                        onClick = {
                            haptics.tap()
                            amountInput = preset.toString()
                        },
                        label = { Text("¥$preset") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Conversion Result Surface
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (ratePerJpy != null && ratePerJpy > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (jpyToHome) "ESTIMATED IN ${fxState.homeCurrency}" else "EQUIVALENT IN JPY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = convertedResultLabel,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.testTag("currency_converted_output")
                            )
                        }
                        if (convertedJpyLong > 0L) {
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    onLogConvertedAmount(convertedJpyLong)
                                },
                                modifier = Modifier.testTag("log_converted_expense_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log ¥$convertedJpyLong")
                            }
                        }
                    }

                    HorizontalDivider()
                    val jpyPerOneHome = 1.0 / ratePerJpy
                    Text(
                        text = "1,000 JPY = ${CurrencyRateRepository.formatHomeCurrency(ratePerJpy * 1000.0, fxState.homeCurrency)}  •  1 ${fxState.homeCurrency} = ¥${"%.1f".format(Locale.US, jpyPerOneHome)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = fxState.errorMessage
                            ?: "Connect to the internet and tap Refresh to fetch live JPY → ${fxState.homeCurrency} exchange rates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showCurrencyPicker) {
        HomeCurrencyPickerDialog(
            selectedCode = fxState.homeCurrency,
            onDismiss = {
                haptics.tap()
                showCurrencyPicker = false
            },
            onSelect = { code ->
                haptics.confirm()
                onSelectHomeCurrency(code)
                showCurrencyPicker = false
            }
        )
    }
}

@Composable
private fun HomeCurrencyPickerDialog(
    selectedCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val haptics = rememberTabiHaptics()
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) CurrencyRateRepository.supportedCurrencies
        else CurrencyRateRepository.supportedCurrencies.filter {
            it.code.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Home Currency", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search code or currency (e.g. USD, INR, EUR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.code }) { option ->
                        val picked = option.code == selectedCode
                        Surface(
                            color = if (picked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            contentColor = if (picked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.tap()
                                    onSelect(option.code)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${option.code} (${option.symbol})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(option.name, style = MaterialTheme.typography.bodySmall)
                                }
                                if (picked) {
                                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
