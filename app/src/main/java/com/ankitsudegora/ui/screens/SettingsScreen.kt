package com.ankitsudegora.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ankitsudegora.data.Category
import com.ankitsudegora.data.RecurringSchedule
import com.ankitsudegora.data.CreditCard
import androidx.compose.foundation.isSystemInDarkTheme
import com.ankitsudegora.ui.components.getIconVector

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
    onDeleteCreditCard: (CreditCard) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun isNotificationServiceEnabled(ctx: Context): Boolean {
        val pkgName = ctx.packageName
        val flat = android.provider.Settings.Secure.getString(
            ctx.contentResolver,
            "enabled_notification_listeners"
        )
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = android.content.ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    return true
                }
            }
        }
        return false
    }

    // Permission state observer
    var hasNotificationAccess by remember {
        mutableStateOf(isNotificationServiceEnabled(context))
    }

    // Observe lifecycle to refresh state when returning to the app
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        context.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    // Theme-compliant success green colors (Green 400/900 for dark mode, Green 600/100 for light mode)
    val isDark = isSystemInDarkTheme()
    val successColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    val successBgColor = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)

    // Permission dialog state
    var showPermissionExplanationDialog by remember { mutableStateOf(false) }

    // Reminder state variables
    var showTimePickerDiag by remember { mutableStateOf(false) }
    var inputHour by remember { mutableStateOf(currentHour) }
    var inputMinute by remember { mutableStateOf(currentMinute) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "App Settings & Control",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Trust Banner: 100% Offline & Private
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Privacy Verified",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "100% Offline & Private",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "KharchaDekh stores and processes all transaction data strictly on your device. SMS parsing and notifications are handled completely offline. No accounts are required, and no records ever leave your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Segment 1: System Permissions Compliance Status
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Permission Settings",
                        tint = if (hasNotificationAccess) successColor else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Notification Access Authorization",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = if (hasNotificationAccess) {
                        "Explicit system consent is authorized. App notifications from banking and payment apps (e.g. Google Pay, PhonePe, Paytm) are matched continuously on-device."
                    } else {
                        "System permissions are missing. Automated expense parser is inactive. Grant notification listener access below to analyze transaction alerts locally."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!hasNotificationAccess) {
                    Button(
                        onClick = {
                            showPermissionExplanationDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_permissions_btn")
                    ) {
                        Text("Grant Notification Access")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(successBgColor)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Authorized",
                            tint = successColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automated Engine Active",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = successColor
                            )
                        )
                    }
                }
            }
        }

        // Profile Personalization Card
        var profileName by remember(userName) { mutableStateOf(userName) }
        var isEditingName by remember { mutableStateOf(false) }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Name",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "User Profile Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (isEditingName) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text("Profile Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onUpdateUserName(profileName)
                                isEditingName = false
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current User Name",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = { isEditingName = true }) {
                            Text("Edit Name")
                        }
                    }
                }
            }
        }

        // Budget Goals Planner Card
        var incomeInput by remember(monthlyIncome) { mutableStateOf(if (monthlyIncome > 0) monthlyIncome.toInt().toString() else "") }
        var savingsInput by remember(savingsTargetPct) { mutableStateOf(savingsTargetPct.toString()) }
        var spendingInput by remember(spendingTargetPct) { mutableStateOf(spendingTargetPct.toString()) }
        var isEditingGoals by remember { mutableStateOf(false) }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Budget Planner",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Budget & Savings Planner",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Establish your financial boundaries. Setting a monthly income and target percentages helps monitor active progress directly on the feed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isEditingGoals) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = incomeInput,
                            onValueChange = { incomeInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Monthly Income (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = savingsInput,
                                onValueChange = { savingsInput = it.filter { char -> char.isDigit() } },
                                label = { Text("Savings Target (%)") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = spendingInput,
                                onValueChange = { spendingInput = it.filter { char -> char.isDigit() } },
                                label = { Text("Spending Cap (%)") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                val inc = incomeInput.toDoubleOrNull() ?: 0.0
                                val sav = savingsInput.toIntOrNull() ?: 20
                                val spnd = spendingInput.toIntOrNull() ?: 50
                                onUpdateBudgetGoals(inc, sav, spnd)
                                isEditingGoals = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Goals")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Monthly Income: " + (if (monthlyIncome > 0) "₹%,.0f".format(monthlyIncome) else "Not set"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Target Splits: Savings ${savingsTargetPct}% | Spending Max ${spendingTargetPct}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { isEditingGoals = true }) {
                            Text("Adjust Goals")
                        }
                    }
                }
            }
        }

        // Billing Cycle Start Day card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Billing Cycle Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Billing Cycle Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Select the day of the month your pay cycle starts. Budgets and Safe to Spend calculations will compute from this date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Day: $billingCycleStartDay",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(60.dp)
                    )

                    Slider(
                        value = billingCycleStartDay.toFloat(),
                        onValueChange = { onUpdateBillingCycleStartDay(it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 29,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Current Cycle: Day $billingCycleStartDay of this month to Day ${if (billingCycleStartDay == 1) 30 else billingCycleStartDay - 1} of next month (approx).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Category Management Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Manage Categories",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Category Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Configure custom ledger categories, set budgets, and choose custom icons.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onNavigateToCategories,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Categories")
                }
            }
        }

        // Credit Card Management Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "Manage Credit Cards",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Credit Card Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Add and manage your credit cards. These cards can be linked to your expense ledger and used for repayment tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var cardNameInput by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cardNameInput,
                        onValueChange = { cardNameInput = it },
                        placeholder = { Text("e.g. HDFC Regalia") },
                        label = { Text("Credit Card Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (cardNameInput.isNotBlank()) {
                                onAddCreditCard(cardNameInput.trim())
                                cardNameInput = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }

                if (creditCards.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Registered Cards",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        creditCards.forEach { card ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = card.cardName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCreditCard(card) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Data Backup & Cloud Sync Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Backup and Sync",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Backup & Restore (Cloud / Local)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Secure your ledger records. Use local file storage or link directly to Google Drive, OneDrive, or Dropbox folders synced on your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onBackupDatabase,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup")
                    }

                    OutlinedButton(
                        onClick = onRestoreDatabase,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Backup at Night",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Saves database snapshot to secure files automatically during daily night reminder check.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoBackupNight,
                        onCheckedChange = onUpdateAutoBackupNight,
                        modifier = Modifier.testTag("auto_backup_night_switch")
                    )
                }
            }
        }

        // Segment 2: Custom Retention Reminders
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Reminders Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Daily Evening Reminders",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "WorkManager schedules a local reminder check daily (default 8:30 PM). If you have unfinalized transaction alerts or log zero entries, we will nudge you gently.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { showTimePickerDiag = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reminder Scheduled Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "%02d:%02d %s".format(
                                if (currentHour % 12 == 0) 12 else currentHour % 12,
                                currentMinute,
                                if (currentHour >= 12) "PM" else "AM"
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Change Time",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Segment 3: Developer Testing Suite
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Diagnostic Settings",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Interactive Test Simulation Panel",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "No real notifications needed! Tap these triggers to simulate transaction notifications locally. Auto-generates critical notifications instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSimulateSms("Spent Rs. 350 at Swiggy via HDFC Debit Card") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simulate Swiggy Spend (HDFC: ₹350)")
                    }

                    Button(
                        onClick = { onSimulateSms("Urgent transaction: Rs. 14,500.00 debited to Rent VPA 9876543211@paytm") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simulate Rent Debit (Paytm: ₹14,500)")
                    }

                    Button(
                        onClick = { onSimulateSms("Dear Customer, Rs. 1,200 deposited/credited into Account ...129 via VPA Amazon Cash") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simulate Amazon Refund Credit (SBI: ₹1,200)")
                    }
                }
            }
        }

        // Segment 4: Scheduled Payments (Recurring Schedules Management)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth().testTag("recurring_schedules_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Recurring schedules",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Active Scheduled Payments",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Configure and coordinate regular transactions (e.g., monthly rent, standard subscriptions). The core daemon spawns pending reviews once schedules hit trigger thresholds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (recurringSchedules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active schedules logged yet. Toggle \"Designate as Recurring\" when reconciling or logging any transaction.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recurringSchedules.forEach { s ->
                            val linkedCat = categories.find { it.id == s.categoryId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (s.isActive) MaterialTheme.colorScheme.primaryContainer 
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconVector(linkedCat?.iconResName ?: "star"),
                                            contentDescription = null,
                                            tint = if (s.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.merchant,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (s.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${s.frequency} • ₹${"%,.2f".format(s.amount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Switch(
                                        checked = s.isActive,
                                        onCheckedChange = { onToggleSchedule(s) },
                                        modifier = Modifier.scale(0.8f).testTag("toggle_schedule_${s.id}")
                                    )
                                    IconButton(
                                        onClick = { onDeleteSchedule(s) },
                                        modifier = Modifier.size(32.dp).testTag("delete_schedule_${s.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Schedule",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature Guide card
        var showFeatureGuide by remember { mutableStateOf(false) }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFeatureGuide = !showFeatureGuide }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Feature Guide",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Quick walkthroughs for every feature",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showFeatureGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showFeatureGuide) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(
                    visible = showFeatureGuide,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        FeatureGuideItem(
                            icon = Icons.Default.Notifications,
                            title = "SMS Auto-Capture",
                            purpose = "Automatically reads bank SMS alerts to log transactions on-device.",
                            steps = listOf(
                                "Go to Settings → Enable Notification Access",
                                "Grant notification permission for KharchaDekh",
                                "Bank alerts will auto-appear as pending transactions in Feed"
                            )
                        )
                        FeatureGuideItem(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            title = "Feed, Stats & Calendar",
                            purpose = "View live transaction feed, interactive expense charts, and a monthly calendar view — all in one swipeable tab.",
                            steps = listOf(
                                "Tap Feed at bottom → Swipe between Feed, Stats, Calendar tabs",
                                "Stats shows a doughnut chart and 6-month trend bars",
                                "Calendar lets you browse and export daily statements"
                            )
                        )
                        FeatureGuideItem(
                            icon = Icons.Default.Edit,
                            title = "Enrich & Categorize",
                            purpose = "Finalize pending SMS transactions by assigning category, merchant, and amount.",
                            steps = listOf(
                                "Tap a pending transaction card in Feed",
                                "Select category, enter merchant name, adjust amount",
                                "Optionally link to a planned checklist or refund",
                                "Tap Confirm to save to your ledger"
                            )
                        )
                        FeatureGuideItem(
                            icon = Icons.Default.Checklist,
                            title = "Planned Checklists",
                            purpose = "Plan grocery or shopping lists with budgets. Checkout maps to bank alerts to avoid double entries.",
                            steps = listOf(
                                "Tap Lists tab → Add a new checklist with a budget cap",
                                "Add items with name, quantity, and price",
                                "Check off items as you shop",
                                "Tap Checkout → optionally link to a pending bank alert",
                                "Edit the settled total anytime after checkout"
                            )
                        )
                        FeatureGuideItem(
                            icon = Icons.Default.CreditCard,
                            title = "Credit Card Tracking",
                            purpose = "Track credit card spends separately and log repayments against outstanding bills.",
                            steps = listOf(
                                "Tap Cards tab → Add a credit card by name",
                                "Mark debit transactions as 'Paid via Credit Card' during enrichment",
                                "Tap Repay on a card to log bill payments and settle dues"
                            )
                        )
                        FeatureGuideItem(
                            icon = Icons.Default.Autorenew,
                            title = "Recurring Payments",
                            purpose = "Auto-log recurring expenses like rent, subscriptions, or EMIs on schedule.",
                            steps = listOf(
                                "During enrichment, toggle 'Recurring' and pick a frequency",
                                "Manage all schedules in Settings → Recurring section",
                                "Toggle active/inactive or delete schedules anytime"
                            )
                        )
                        FeatureGuideItem(
                            icon = Icons.Default.Savings,
                            title = "Budget Goals",
                            purpose = "Set monthly income, savings target, and spending cap to track discretionary spending.",
                            steps = listOf(
                                "Go to Settings → Budget Goals",
                                "Enter monthly income, savings %, and spending %",
                                "Feed will show daily allowance and budget utilization"
                            )
                        )
                        FeatureGuideItem(
                            icon = Icons.Default.BackupTable,
                            title = "Backup & Restore",
                            purpose = "Export your entire ledger as a secure ZIP backup. Restore from any previous backup file.",
                            steps = listOf(
                                "Settings → Tap Backup to save a .zip file",
                                "Settings → Tap Restore and pick a backup file",
                                "Auto-backup runs nightly if enabled"
                            )
                        )
                    }
                }
            }
        }

        // Share App card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share App",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Share KharchaDekh",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "Help your friends organize their transactions! Share this secure, 100% offline, privacy-first ledger app with them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "Hey! 🚀 Take control of your money with KharchaDekh — a premium, 100% offline & secure expense manager. It reads SMS bank alerts fully on-device with zero internet required. No ads, no tracking, complete privacy! 🛡️💰\n\nDownload it on Google Play: https://play.google.com/store/apps/details?id=com.ankitsudegora"
                            )
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share KharchaDekh with friends"))
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share with Friends")
                }
            }
        }

        // Segment 5: Security & Privacy Flush (Erase data)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DPDP Safety Wiping Suite",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "If you wish to remove your credentials or completely reset the application logic to default, trigger a total reset below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(
                    onClick = onResetOnboarding,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_consent_btn")
                ) {
                    Text("Decline Consent & Flush All Ledger States")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Made with 💝 by Ankit Sudegora",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Version v1.1.2.3 • Secure Ledger",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Modal Time Picker Dialog representation
    if (showTimePickerDiag) {
        AlertDialog(
            onDismissRequest = { showTimePickerDiag = false },
            title = { Text("Configure Reminder Hour") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Slide hour and minute selectors for evening reviews:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hour (24h format): $inputHour", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = inputHour.toFloat(),
                                onValueChange = { inputHour = it.toInt() },
                                valueRange = 0f..23f,
                                steps = 24
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Minute: $inputMinute", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = inputMinute.toFloat(),
                                onValueChange = { inputMinute = it.toInt() },
                                valueRange = 0f..59f,
                                steps = 60
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTime(inputHour, inputMinute)
                        showTimePickerDiag = false
                    }
                ) {
                    Text("Save Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDiag = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Permission Explanation Dialog
    if (showPermissionExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Notification Access Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "KharchaDekh parses incoming transactional SMS notifications from banks and payment apps (e.g. Google Pay, PhonePe, Paytm) completely locally on your device to automate expense tracking.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "To enable this core functionality, you will be redirected to the system settings page to toggle on 'KharchaDekh' under Notification Access.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "🔒 Your data is 100% secure, offline, processed on-device, and never leaves your phone.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionExplanationDialog = false
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionExplanationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FeatureGuideItem(
    icon: ImageVector,
    title: String,
    purpose: String,
    steps: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = purpose,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 40.dp, top = 4.dp)
                ) {
                    steps.forEachIndexed { index, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
