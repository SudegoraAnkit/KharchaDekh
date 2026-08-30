package com.ankitsudegora.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.CreditCard
import com.ankitsudegora.data.RecurringSchedule
import com.ankitsudegora.ui.components.getCurrencySymbol
import com.ankitsudegora.ui.theme.*

@Composable
fun SettingsScreen(
    userName: String,
    onUpdateUserName: (String) -> Unit,
    currentHour: Int,
    currentMinute: Int,
    onUpdateTime: (Int, Int) -> Unit,
    onResetOnboarding: () -> Unit,
    onSimulateSms: (body: String) -> Unit,
    onBackupDatabase: () -> Unit,
    onRestoreDatabase: () -> Unit,
    recurringSchedules: List<RecurringSchedule>,
    categories: List<Category>,
    onToggleSchedule: (RecurringSchedule) -> Unit,
    onDeleteSchedule: (RecurringSchedule) -> Unit,
    monthlyIncome: Double,
    savingsTargetPct: Int,
    spendingTargetPct: Int,
    onUpdateBudgetGoals: (Double, Int, Int) -> Unit,
    autoBackupNight: Boolean,
    onUpdateAutoBackupNight: (Boolean) -> Unit,
    billingCycleStartDay: Int,
    onUpdateBillingCycleStartDay: (Int) -> Unit,
    onNavigateToCategories: () -> Unit,
    creditCards: List<CreditCard>,
    onAddCreditCard: (String) -> Unit,
    onDeleteCreditCard: (CreditCard) -> Unit,
    isMultiCurrencyEnabled: Boolean,
    onToggleMultiCurrency: (Boolean) -> Unit,
    primaryCurrency: String,
    onUpdatePrimaryCurrency: (String) -> Unit,
    onNavigateToCreditCards: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Dialog state controllers
    var showProfileDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var showSimulateSmsDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // TimePicker Dialog for daily review reminder
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute -> onUpdateTime(hourOfDay, minute) },
            currentHour,
            currentMinute,
            false
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Top Header: Settings + Subtitle + Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = DarkOnSurface
                )
                Text(
                    text = "Manage your account, preferences & more",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant
                )
            }

            IconButton(
                onClick = { Toast.makeText(context, "Search settings", Toast.LENGTH_SHORT).show() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = DarkOnSurface, modifier = Modifier.size(20.dp))
            }
        }

        // 2. Security & Privacy Banner (Matches Mockup)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF064E3B).copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyDialog = true }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Your data is safe with us", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
                        Text("We use bank-level security to keep your data private and protected.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = DarkOnSurfaceVariant)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Privacy & Security", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = BrandLime)
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = BrandLime, modifier = Modifier.size(12.dp))
                }
            }
        }

        // 3. User Profile Card (Matches Mockup)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProfileDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(userName.ifBlank { "Ankit Rai" }, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
                        Text("Local Encrypted Profile", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceVariant)
                    }
                }

                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = DarkOnSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }

        // 4. PREFERENCES Group Card (Matches Mockup)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PREFERENCES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = DarkOnSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsNavigationItem(
                        icon = Icons.Default.Palette,
                        iconTint = Color(0xFF10B981),
                        title = "App Preferences",
                        subtitle = "Themes, dark mode, animation speeds",
                        onClick = { Toast.makeText(context, "Dark Theme active", Toast.LENGTH_SHORT).show() }
                    )
                    SettingsItemDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.Notifications,
                        iconTint = Color(0xFF10B981),
                        title = "Notifications",
                        subtitle = "Daily review at ${"%02d:%02d".format(currentHour, currentMinute)}",
                        onClick = { timePickerDialog.show() }
                    )
                    SettingsItemDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.CurrencyRupee,
                        iconTint = Color(0xFF10B981),
                        title = "Currency & Region",
                        subtitle = "$primaryCurrency (${getCurrencySymbol(primaryCurrency)}) • Multi-Currency",
                        onClick = { showCurrencyDialog = true }
                    )
                    SettingsItemDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.Category,
                        iconTint = Color(0xFF10B981),
                        title = "Category Management",
                        subtitle = "Manage ${categories.size} categories & icons",
                        onClick = onNavigateToCategories
                    )
                    SettingsItemDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.PieChart,
                        iconTint = Color(0xFF10B981),
                        title = "Budget Settings",
                        subtitle = "Monthly budget: ${getCurrencySymbol(primaryCurrency)}${"%,.0f".format(monthlyIncome)}",
                        onClick = { showBudgetDialog = true }
                    )
                }
            }
        }

        // 5. ACCOUNTS & DATA Group Card (Matches Mockup)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ACCOUNTS & DATA",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = DarkOnSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsNavigationItem(
                        icon = Icons.Default.AccountBalance,
                        iconTint = Color(0xFF10B981),
                        title = "Bank Accounts",
                        subtitle = "Manage linked accounts & SMS sync",
                        onClick = { showSimulateSmsDialog = true }
                    )
                    SettingsItemDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.CreditCard,
                        iconTint = Color(0xFF10B981),
                        title = "Credit Card Management",
                        subtitle = "View ${creditCards.size} cards, accumulated dues & billing cycle",
                        onClick = onNavigateToCreditCards
                    )
                    SettingsItemDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.CloudUpload,
                        iconTint = Color(0xFF10B981),
                        title = "Backup & Restore",
                        subtitle = "Local & cloud JSON backup (${if (autoBackupNight) "Auto on" else "Manual"})",
                        onClick = { showBackupOptionsDialog = true }
                    )
                    SettingsItemDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.FileDownload,
                        iconTint = Color(0xFF10B981),
                        title = "Export Data",
                        subtitle = "Download reports as CSV or PDF",
                        onClick = { Toast.makeText(context, "Export CSV from Activity Ledger", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }

        // 6. SUPPORT & ABOUT Group Card (Matches Mockup)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SUPPORT & ABOUT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = DarkOnSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsNavigationItem(
                        icon = Icons.AutoMirrored.Filled.Help,
                        iconTint = Color(0xFF10B981),
                        title = "Help & Support",
                        subtitle = "FAQs, guides & contact us",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SudegoraAnkit/KharchaDekh"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to open link in browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SettingsItemDivider()
                    SettingsNavigationItem(
                        icon = Icons.AutoMirrored.Filled.Article,
                        iconTint = Color(0xFF10B981),
                        title = "What's New",
                        subtitle = "Version 2.0 (19) Release Notes",
                        badge = "NEW",
                        onClick = { showWhatsNewDialog = true }
                    )
                    SettingsItemDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.Info,
                        iconTint = Color(0xFF10B981),
                        title = "About KharchaDekh",
                        subtitle = "v2.0.0.0 • Offline First Finance",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }

        // 7. Sign Out / Reset Data Card (Red Accent)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFEF4444).copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetConfirmDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Reset App Data", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFEF4444))
                        Text("Clear local cache & reset onboarding", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceVariant)
                    }
                }

                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            }
        }

        // 8. Footer (Matches Mockup)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔒 100% Secure • Your data is always private 🍃", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialog: Edit Profile Name
    if (showProfileDialog) {
        var nameInput by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Edit Profile Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateUserName(nameInput.trim())
                        showProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = DarkBackground)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("Cancel", color = DarkOnSurfaceVariant) }
            },
            containerColor = DarkSurface
        )
    }

    // Dialog: Currency Selector
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Primary Currency", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("INR" to "₹ Indian Rupee", "USD" to "$ US Dollar", "EUR" to "€ Euro", "GBP" to "£ British Pound", "AED" to "د.إ UAE Dirham").forEach { (code, label) ->
                        val isSel = primaryCurrency == code
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) BrandLimeContainer else DarkSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdatePrimaryCurrency(code)
                                    showCurrencyDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = if (isSel) BrandOnLimeContainer else DarkOnSurface)
                                if (isSel) Icon(Icons.Default.Check, null, tint = BrandOnLimeContainer, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text("Close", color = BrandLime) } },
            containerColor = DarkSurface
        )
    }

    // Dialog: Budget Settings
    if (showBudgetDialog) {
        var budgetInput by remember { mutableStateOf(if (monthlyIncome > 0) monthlyIncome.toInt().toString() else "30000") }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Monthly Budget Limit", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Target Spend Cap (${getCurrencySymbol(primaryCurrency)})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = budgetInput.toDoubleOrNull() ?: 30000.0
                        onUpdateBudgetGoals(cap, savingsTargetPct, spendingTargetPct)
                        showBudgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = DarkBackground)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel", color = DarkOnSurfaceVariant) } },
            containerColor = DarkSurface
        )
    }

    // Dialog: Backup & Restore Options (M6)
    if (showBackupOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showBackupOptionsDialog = false },
            title = { Text("Backup & Restore Data", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Export a full encrypted backup of your database, or restore from a previously exported .zip backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                    Button(
                        onClick = {
                            showBackupOptionsDialog = false
                            onBackupDatabase()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = DarkBackground)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export / Backup Database (.zip)", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showBackupOptionsDialog = false
                            onRestoreDatabase()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import / Restore Database (.zip)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBackupOptionsDialog = false }) { Text("Cancel", color = DarkOnSurfaceVariant) }
            },
            containerColor = DarkSurface
        )
    }

    // Dialog: SMS Simulator
    if (showSimulateSmsDialog) {
        var smsBodyInput by remember { mutableStateOf("Paid Rs. 450 to Swiggy using UPI on 30-Aug-2026.") }
        AlertDialog(
            onDismissRequest = { showSimulateSmsDialog = false },
            title = { Text("Simulate Bank SMS", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Test the offline semantic parser with custom SMS formats:", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceVariant)
                    OutlinedTextField(
                        value = smsBodyInput,
                        onValueChange = { smsBodyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSimulateSms(smsBodyInput.trim())
                        showSimulateSmsDialog = false
                        Toast.makeText(context, "SMS sent to parser!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = DarkBackground)
                ) {
                    Text("Parse & Log", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showSimulateSmsDialog = false }) { Text("Cancel", color = DarkOnSurfaceVariant) } },
            containerColor = DarkSurface
        )
    }

    // Dialog: What's New
    if (showWhatsNewDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsNewDialog = false },
            title = { Text("What's New in v2.0", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Low-Friction 1-Tap Expense Review & Entry", style = MaterialTheme.typography.bodyMedium)
                    Text("• Credit Card Accumulated Dues & Statement Tracking", style = MaterialTheme.typography.bodyMedium)
                    Text("• Dynamic Time-Aware Greetings (Morning, Afternoon, Evening, Night)", style = MaterialTheme.typography.bodyMedium)
                    Text("• Interactive Spending Overview Bezier Line Charts", style = MaterialTheme.typography.bodyMedium)
                    Text("• 100% Offline & On-Device Financial Security", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { showWhatsNewDialog = false }) { Text("Awesome!", color = BrandLime) } },
            containerColor = DarkSurface
        )
    }

    // Dialog: About KharchaDekh
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About KharchaDekh", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("KharchaDekh Version 2.0.0.0 (Build 19)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Offline-first expense tracking and AI-assisted financial clarity built for India.", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close", color = BrandLime) } },
            containerColor = DarkSurface
        )
    }

    // Dialog: Privacy & Security
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("100% Private & Secure", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "All financial transactions, bank notifications, category budgets, and credit card statements are parsed and stored purely locally on your device in an encrypted Room SQLite database. No data ever leaves your device without your explicit export command.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkOnSurface
                )
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Got it", color = BrandLime) } },
            containerColor = DarkSurface
        )
    }

    // Dialog: Reset App Data Confirmation
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset App Data?", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
            text = {
                Text("This will reset your onboarding preferences and profile settings. Existing transaction records will be retained.", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetOnboarding()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                ) {
                    Text("Confirm Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel", color = DarkOnSurfaceVariant) } },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun SettingsNavigationItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }

            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = DarkOnSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceVariant, maxLines = 1)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandLime
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                        color = DarkBackground,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = DarkOnSurfaceVariant, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun SettingsItemDivider() {
    HorizontalDivider(
        color = DarkBorder.copy(alpha = 0.5f),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
