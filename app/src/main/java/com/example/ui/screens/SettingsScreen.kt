package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.Category
import com.example.data.RecurringSchedule
import com.example.ui.components.getIconVector

@Composable
fun SettingsScreen(
    currentHour: Int,
    currentMinute: Int,
    onUpdateTime: (Int, Int) -> Unit,
    onResetOnboarding: () -> Unit,
    onSimulateSms: (body: String) -> Unit,
    recurringSchedules: List<RecurringSchedule>,
    categories: List<Category>,
    onToggleSchedule: (RecurringSchedule) -> Unit,
    onDeleteSchedule: (RecurringSchedule) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Permission state observer
    var hasSmsPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermissions = results.values.all { it }
    }

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
                        tint = if (hasSmsPermissions) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "SMS Tracking Authorization",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = if (hasSmsPermissions) {
                        "Explicit system consent is authorized. Bank and payment alphanumeric senders (e.g. HDFCBK, SBIINB) are matched continuously on-device."
                    } else {
                        "System permissions are missing. Automated expense parser is inactive. Grant permissions below to analyze bank/SMS alerts locally."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!hasSmsPermissions) {
                    Button(
                        onClick = {
                            launcher.launch(
                                arrayOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                                )
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_permissions_btn")
                    ) {
                        Text("Grant System SMS Access")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Authorized",
                            tint = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automated Engine Active",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        )
                    }
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
                    text = "WorkManager schedules a local reminder check daily (default 8:30 PM). If you have unfinalized SMS transactions or log zero entries, we will nudge you gently.",
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
                    text = "No real SMS needed! Tap these bank triggers to simulate parsed SMS receipt locally. Auto-generates critical notifications instantly.",
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
                        onClick = { onSimulateSms("Dear SBI Customer, Rs. 1,200 deposited/credited into Account ...129 via VPA Amazon Cash") },
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
}
