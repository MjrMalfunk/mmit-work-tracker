package com.midwestmanagedit.tracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.midwestmanagedit.tracker.TrackerViewModel
import com.midwestmanagedit.tracker.data.ShiftMaterialUsageEntity
import com.midwestmanagedit.tracker.inventory.searchInventory
import com.midwestmanagedit.tracker.inventory.validateMaterialQuantity
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.Date
import java.util.Locale

private fun number(value: Double) = DecimalFormat("0.###").format(value)
private fun syncTime(time: Long) = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time))

@Composable
fun InventoryConnectionCard(vm: TrackerViewModel) {
    val state by vm.inventoryCatalog.collectAsState()
    var showConnection by remember { mutableStateOf(false) }
    val importCatalog = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importInventoryCatalog(uri)
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Inventory catalog", fontWeight = FontWeight.Bold)
            Text(if (state.syncedAtEpochMs == null) "Connect to OPS V2 or import a catalog downloaded from its Inventory panel."
                else "${state.items.size} items saved on this phone · last synced ${syncTime(state.syncedAtEpochMs!!)}")
            Text("Search works offline. Stock is last reported; OPS checks it when the outing is imported.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showConnection = true }, enabled = !state.busy) {
                    Text(if (state.authenticated) "Connection" else "Connect to OPS V2")
                }
                OutlinedButton(onClick = { vm.refreshInventory(force = true) }, enabled = state.authenticated && !state.busy) {
                    Text(if (state.busy) "Refreshing…" else "Refresh stock")
                }
            }
            TextButton(onClick = { importCatalog.launch(arrayOf("application/json", "text/plain")) }, enabled = !state.busy) {
                Text("Import catalog JSON")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (showConnection) {
        var serverUrl by rememberSaveable { mutableStateOf(state.serverUrl.takeIf { it.startsWith("https://") }.orEmpty()) }
        var email by rememberSaveable { mutableStateOf(state.email) }
        // Credentials deliberately use ephemeral state, not rememberSaveable or preferences.
        var password by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!state.busy) showConnection = false },
            title = { Text("Connect to OPS V2") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use the HTTPS server address reachable from this phone. Your desktop's 127.0.0.1 address is local to that computer.")
                    OutlinedTextField(serverUrl, { serverUrl = it }, label = { Text("OPS V2 server address") },
                        placeholder = { Text("https://v2.example.com") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), enabled = !state.busy)
                    OutlinedTextField(email, { email = it }, label = { Text("OPS V2 email") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), enabled = !state.busy)
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), enabled = !state.busy)
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("Verification code") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        enabled = !state.busy)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !state.busy && serverUrl.isNotBlank() && email.isNotBlank() && password.isNotEmpty() && code.length == 6,
                    onClick = {
                        vm.connectInventory(serverUrl, email, password, code) { showConnection = false }
                        password = ""; code = ""
                    }) { Text(if (state.busy) "Connecting…" else "Sign in and sync") }
            },
            dismissButton = {
                Row {
                    if (state.serverUrl.isNotBlank()) TextButton(enabled = !state.busy, onClick = {
                        vm.disconnectInventory(); showConnection = false
                    }) { Text("Disconnect") }
                    TextButton(enabled = !state.busy, onClick = { showConnection = false }) { Text("Close") }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldNationMaterialsCard(materials: List<ShiftMaterialUsageEntity>, vm: TrackerViewModel) {
    val catalog by vm.inventoryCatalog.collectAsState()
    val saving by vm.savingMaterial.collectAsState()
    val focus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSku by rememberSaveable { mutableStateOf<String?>(null) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var draftAccount by rememberSaveable { mutableStateOf(catalog.accountKey) }
    val selected = catalog.items.firstOrNull { it.sku == selectedSku }
    val matches = remember(catalog.items, query, selectedSku) {
        searchInventory(catalog.items, if (selectedSku == null) query else "")
    }
    val amount = quantity.toDoubleOrNull()
    val alreadyRecorded = materials.firstOrNull { it.sku == selectedSku }?.quantity ?: 0.0
    val quantityValid = amount?.let { runCatching { validateMaterialQuantity(it, alreadyRecorded) }.isSuccess } == true
    LaunchedEffect(catalog.accountKey) {
        if (draftAccount != catalog.accountKey) {
            query = ""; selectedSku = null; quantity = ""; notes = ""; expanded = false
            draftAccount = catalog.accountKey
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Materials used", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Choose an inventory item, then record what you used. It will be attached to this work order in the JSON export.")
            materials.forEach { material ->
                val unit = catalog.items.firstOrNull { it.sku == material.sku }?.unit.orEmpty()
                Text("${material.sku} · ${number(material.quantity)} $unit${material.notes?.let { " · $it" } ?: ""}")
            }
            if (catalog.items.isEmpty()) Text(
                if (catalog.syncedAtEpochMs == null) "Connect to OPS V2 or import a catalog JSON using the controls above."
                else "No active inventory items are available. Refresh the catalog after adding stock in OPS V2.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExposedDropdownMenuBox(expanded = expanded && catalog.items.isNotEmpty() && !saving,
                onExpandedChange = { expanded = it }) {
                OutlinedTextField(value = query, onValueChange = { query = it; selectedSku = null; expanded = true },
                    label = { Text("Search inventory by SKU or description") }, singleLine = true,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(), enabled = catalog.items.isNotEmpty() && !saving,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    supportingText = {
                        Text(selected?.let { "Last reported stock: ${number(it.quantityOnHand)} ${it.unit}" }
                            ?: "Type to search, then tap an item to select its exact SKU.")
                    })
                ExposedDropdownMenu(expanded = expanded && catalog.items.isNotEmpty() && !saving,
                    onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                    if (matches.isEmpty()) DropdownMenuItem(text = { Text("No matching items. Try another search or refresh stock.") },
                        onClick = {}, enabled = false)
                    matches.forEach { item ->
                        DropdownMenuItem(text = {
                            Column {
                                Text(item.sku, fontWeight = FontWeight.Bold)
                                Text(item.description)
                                Text("${number(item.quantityOnHand)} ${item.unit} last reported", style = MaterialTheme.typography.bodySmall)
                            }
                        }, onClick = { selectedSku = item.sku; query = "${item.sku} · ${item.description}"; expanded = false })
                    }
                }
            }
            val quantityLabel = when (selected?.unit?.lowercase(Locale.ROOT)) {
                "ft", "foot", "feet" -> "Feet used"
                "each", "ea" -> "Quantity used (each)"
                null -> "Quantity used"
                else -> "Quantity used (${selected.unit})"
            }
            OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text(quantityLabel) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = quantity.isNotEmpty() && !quantityValid,
                supportingText = { Text("Up to three decimal places; total per SKU cannot exceed 10,000.") })
            if (alreadyRecorded > 0) Text("Already recorded this outing: ${number(alreadyRecorded)} ${selected?.unit.orEmpty()}")
            if (selected != null && amount != null && amount + alreadyRecorded > selected.quantityOnHand) Text(
                "This exceeds last-reported stock. OPS V2 checks availability when you import.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Material note (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
            Button(enabled = selected != null && quantityValid && !saving, modifier = Modifier.fillMaxWidth(), onClick = {
                vm.addFieldNationMaterial(selected!!.sku, amount!!, notes) {
                    query = ""; selectedSku = null; quantity = ""; notes = ""; expanded = false
                    focus.clearFocus()
                }
            }) { Text(if (saving) "Saving…" else "ADD MATERIAL USE") }
        }
    }
}
