package com.example.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EncryptedTravelDoc
import com.example.security.TotpManager
import com.example.ui.components.rememberTabiHaptics
import com.example.ui.viewmodel.SyncState
import com.example.ui.viewmodel.TabiMainViewModel
import kotlinx.coroutines.delay

@Composable
fun VaultSecurityTab(mainViewModel: TabiMainViewModel) {
    val haptics = rememberTabiHaptics()
    val context = LocalContext.current
    val docsState by mainViewModel.documentsState.collectAsStateWithLifecycle()
    val offlineDocs by mainViewModel.offlineDocuments.collectAsStateWithLifecycle()
    val userProfile by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val vaultUnlocked by mainViewModel.vaultUnlocked.collectAsStateWithLifecycle()
    val activeTotpSecret by mainViewModel.activeTotpSecret.collectAsStateWithLifecycle()

    val docs = remember(docsState, offlineDocs) {
        when (val s = docsState) {
            is SyncState.Success -> s.data
            else -> offlineDocs.map {
                EncryptedTravelDoc(
                    id = it.id,
                    userId = it.userId,
                    title = it.title,
                    docType = it.docType,
                    encryptedPayload = it.encryptedPayload,
                    ivBase64 = it.ivBase64,
                    saltBase64 = it.saltBase64,
                    keyMode = it.keyMode
                )
            }
        }
    }

    val mfaEnabled = userProfile?.mfaEnabled == true
    var totpInput by remember { mutableStateOf("") }
    var showAddDocDialog by remember { mutableStateOf(false) }
    var inspectingDoc by remember { mutableStateOf<EncryptedTravelDoc?>(null) }
    var setupSecretDisplay by remember { mutableStateOf<String?>(null) }

    // Live TOTP countdown for authenticator preview
    var totpSecondsLeft by remember { mutableIntStateOf(TotpManager.secondsRemaining()) }
    var currentDemoTotp by remember { mutableStateOf("") }
    LaunchedEffect(activeTotpSecret, setupSecretDisplay) {
        val secret = setupSecretDisplay ?: activeTotpSecret
        while (secret != null) {
            totpSecondsLeft = TotpManager.secondsRemaining()
            currentDemoTotp = TotpManager.generateCurrentCode(secret)
            delay(1000L)
        }
    }

    // Google Drive / iCloud Drive / SAF Backup Export & Import
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = mainViewModel.buildBackupExportJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                mainViewModel.showMessage("Encrypted backup exported to cloud/device storage.")
            }.onFailure {
                mainViewModel.showMessage("Export failed: ${it.localizedMessage}")
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: ""
            }.onSuccess { text ->
                if (text.isNotBlank()) mainViewModel.importBackupJson(text)
            }.onFailure {
                mainViewModel.showMessage("Import failed: ${it.localizedMessage}")
            }
        }
    }

    val keyguardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            mainViewModel.unlockVaultViaBiometric()
        }
    }

    val triggerDeviceBiometric = {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && km?.isDeviceSecure == true) {
            val builder = BiometricPrompt.Builder(context)
                .setTitle("Unlock Encrypted Travel Vault")
                .setSubtitle("Verify your identity to decrypt personal documents")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            }
            val prompt = builder.build()
            prompt.authenticate(
                CancellationSignal(),
                context.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        mainViewModel.unlockVaultViaBiometric()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        mainViewModel.showMessage(errString?.toString() ?: "Biometric check cancelled.")
                    }
                }
            )
        } else if (km?.isKeyguardSecure == true) {
            @Suppress("DEPRECATION")
            val intent = km.createConfirmDeviceCredentialIntent("Unlock Vault", "Verify device PIN/pattern")
            if (intent != null) keyguardLauncher.launch(intent)
            else mainViewModel.unlockVaultViaBiometric()
        } else {
            mainViewModel.showMessage("No device lock screen set; enter your 6-digit TOTP code or set a screen lock.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Multi-Factor Security Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Multi-Factor Vault Security (MFA)", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (mfaEnabled) "Google Sign-In + TOTP 2FA Active" else "Enable TOTP 2FA for strict vault lock",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = mfaEnabled,
                            onCheckedChange = { enable ->
                                haptics.toggle()
                                if (enable) {
                                    setupSecretDisplay = mainViewModel.enableTotpMfa()
                                } else {
                                    setupSecretDisplay = null
                                    mainViewModel.disableTotpMfa()
                                }
                            },
                            modifier = Modifier.testTag("mfa_toggle_switch")
                        )
                    }

                    if (mfaEnabled && activeTotpSecret != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Authenticator Key (Base32): ${setupSecretDisplay ?: activeTotpSecret}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Active TOTP Token: $currentDemoTotp (refreshes in ${totpSecondsLeft}s)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (mfaEnabled && !vaultUnlocked) {
                        OutlinedTextField(
                            value = totpInput,
                            onValueChange = { totpInput = it.filter { ch -> ch.isDigit() }.take(6) },
                            label = { Text("Enter 6-digit Authenticator Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    haptics.confirm()
                                    mainViewModel.verifyTotpAndUnlock(totpInput)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify 2FA", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    triggerDeviceBiometric()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Biometric")
                            }
                        }
                    } else if (mfaEnabled && vaultUnlocked) {
                        OutlinedButton(
                            onClick = {
                                haptics.toggle()
                                mainViewModel.lockVault()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Vault Now")
                        }
                    }
                }
            }
        }

        // Cloud Storage Sync (Google Drive / iCloud Drive / SAF Document Providers)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Cloud Storage Sync (Google Drive / Cloud Files)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Export or restore an encrypted backup of your itinerary and vault documents to Google Drive or any installed cloud provider.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                exportBackupLauncher.launch("tabiguide_encrypted_backup.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup")
                        }
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup")
                        }
                    }
                }
            }
        }

        // Encrypted Document Vault List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("E2EE Travel Documents (${docs.size})", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "AES-256-GCM encrypted • Cached offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Button(
                    onClick = {
                        haptics.tap()
                        showAddDocDialog = true
                    },
                    enabled = !mfaEnabled || vaultUnlocked,
                    modifier = Modifier.testTag("add_encrypted_doc_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Encrypt Doc", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (mfaEnabled && !vaultUnlocked) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(
                        "Vault is MFA-locked. Verify your 6-digit TOTP code or device biometric above to view or add encrypted documents.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else if (docs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        "Store passport numbers, JR Pass codes, insurance policies, and hotel PINs with zero-knowledge AES-256-GCM encryption.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(docs, key = { it.id }) { doc ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${doc.docType.uppercase()} • ${doc.keyMode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text(doc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Ciphertext: ${doc.encryptedPayload.take(18)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            haptics.tap()
                            inspectingDoc = doc
                        }) {
                            Icon(Icons.Default.Visibility, contentDescription = "Decrypt and view")
                        }
                        IconButton(onClick = {
                            haptics.toggle()
                            mainViewModel.deleteEncryptedDocument(doc.id)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete document")
                        }
                    }
                }
            }
        }
    }

    if (showAddDocDialog) {
        AddEncryptedDocDialog(
            onDismiss = { showAddDocDialog = false },
            onSave = { title, type, secretBody, passphrase ->
                mainViewModel.saveEncryptedDocument(title, type, secretBody, passphrase)
                showAddDocDialog = false
            }
        )
    }

    if (inspectingDoc != null) {
        DecryptDocumentDialog(
            doc = inspectingDoc!!,
            onDecrypt = { passphrase -> mainViewModel.decryptDocumentPayload(inspectingDoc!!, passphrase) },
            onDismiss = { inspectingDoc = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEncryptedDocDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("Passport") }
    var secretText by remember { mutableStateOf("") }
    var usePassphrase by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }

    val haptics = rememberTabiHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encrypt Travel Document", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Label (e.g. US Passport)") })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Passport", "JR Pass", "Flight", "Hotel", "Insurance").forEach { t ->
                        FilterChip(selected = docType == t, onClick = { haptics.tap(); docType = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(
                    value = secretText,
                    onValueChange = { secretText = it },
                    label = { Text("Sensitive Details (encrypted before upload)") },
                    minLines = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Custom E2EE Passphrase", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = usePassphrase, onCheckedChange = { haptics.toggle(); usePassphrase = it })
                }
                if (usePassphrase) {
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Vault Passphrase (PBKDF2)") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val passOk = !usePassphrase || passphrase.isNotBlank()
                    if (title.isNotBlank() && secretText.isNotBlank() && passOk) {
                        haptics.confirm()
                        onSave(title, docType, secretText, if (usePassphrase) passphrase else null)
                    } else {
                        haptics.error()
                    }
                }
            ) { Text("Encrypt & Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = { haptics.tap(); onDismiss() }) { Text("Cancel", fontWeight = FontWeight.Bold) } }
    )
}

@Composable
private fun DecryptDocumentDialog(
    doc: EncryptedTravelDoc,
    onDecrypt: (String?) -> Result<String>,
    onDismiss: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var plaintext by remember(doc.id) {
        mutableStateOf(
            if (doc.keyMode == "KEYSTORE") onDecrypt(null).getOrNull() else null
        )
    }
    var errorMsg by remember(doc.id) {
        mutableStateOf(
            if (doc.keyMode == "KEYSTORE") onDecrypt(null).exceptionOrNull()?.localizedMessage else null
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${doc.title} (${doc.docType})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (doc.keyMode == "PASSPHRASE" && plaintext == null) {
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Enter Document Passphrase") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Button(
                        onClick = {
                            val res = onDecrypt(passphrase)
                            plaintext = res.getOrNull()
                            errorMsg = res.exceptionOrNull()?.localizedMessage
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Decrypt") }
                }
                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                if (plaintext != null) {
                    HorizontalDivider()
                    Text("Decrypted Content:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                    Text(plaintext.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
