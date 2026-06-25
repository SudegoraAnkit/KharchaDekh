package com.ankitsudegora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ankitsudegora.data.*
import com.ankitsudegora.ui.components.getIconVector
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroceryScreen(
    groceryLists: List<GroceryListWithItems>,
    categories: List<Category>,
    onAddList: (String, Double?) -> Unit,
    onDeleteList: (GroceryList) -> Unit,
    onDuplicateList: (GroceryListWithItems, String) -> Unit,
    onAddItem: (Long, String, Int, Double) -> Unit,
    onUpdateItem: (GroceryItem) -> Unit,
    onDeleteItem: (GroceryItem) -> Unit,
    onToggleItem: (GroceryItem) -> Unit,
    onGetLastPrice: suspend (String) -> Double?,
    onCheckout: (GroceryListWithItems, String, Long?, Boolean) -> Unit
) {
    var activeListId by remember { mutableStateOf<Long?>(null) }
    
    val activeList = remember(groceryLists, activeListId) {
        groceryLists.find { it.groceryList.id == activeListId }
    }

    if (activeList != null) {
        GroceryDetailScreen(
            listWithItems = activeList,
            categories = categories,
            onAddItem = onAddItem,
            onUpdateItem = onUpdateItem,
            onDeleteItem = onDeleteItem,
            onToggleItem = onToggleItem,
            onGetLastPrice = onGetLastPrice,
            onCheckout = { method, categoryId, carryForward ->
                onCheckout(activeList, method, categoryId, carryForward)
                activeListId = null
            },
            onNavigateBack = { activeListId = null }
        )
    } else {
        GroceryMasterScreen(
            groceryLists = groceryLists,
            onAddList = onAddList,
            onDeleteList = onDeleteList,
            onDuplicateList = onDuplicateList,
            onSelectList = { listId -> activeListId = listId }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryMasterScreen(
    groceryLists: List<GroceryListWithItems>,
    onAddList: (String, Double?) -> Unit,
    onDeleteList: (GroceryList) -> Unit,
    onDuplicateList: (GroceryListWithItems, String) -> Unit,
    onSelectList: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var listName by remember { mutableStateOf("") }
    var budgetCapStr by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_grocery_list_fab")
            ) {
                Icon(Icons.Default.Add, "New List")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Shopping Drafts & Checkpoints",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Build local grocery checklists offline, set budget caps, and convert drafts into paid transactions at checkout.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (groceryLists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No shopping lists created yet.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the '+' button below to start.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(groceryLists, key = { it.groceryList.id }) { item ->
                        GroceryListCard(
                            listWithItems = item,
                            onClick = { onSelectList(item.groceryList.id) },
                            onDelete = { onDeleteList(item.groceryList) },
                            onClone = {
                                onDuplicateList(item, "${item.groceryList.name} (Copy)")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Shopping List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = listName,
                        onValueChange = { listName = it },
                        label = { Text("List Name (e.g., Weekly Groceries)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = budgetCapStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                budgetCapStr = input
                            }
                        },
                        label = { Text("Optional Budget Cap (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listName.isNotBlank()) {
                            val cap = budgetCapStr.toDoubleOrNull()
                            onAddList(listName.trim(), cap)
                            listName = ""
                            budgetCapStr = ""
                            showAddDialog = false
                        }
                    },
                    enabled = listName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GroceryListCard(
    listWithItems: GroceryListWithItems,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit
) {
    val totalAmount = listWithItems.items.sumOf { it.price * it.quantity }
    val checkedAmount = listWithItems.items.filter { it.isChecked }.sumOf { it.price * it.quantity }
    val checkedCount = listWithItems.items.count { it.isChecked }
    val totalCount = listWithItems.items.size
    val isCompleted = listWithItems.groceryList.status == "COMPLETED"

    val date = Date(listWithItems.groceryList.createdTimestamp)
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateString = formatter.format(date)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isCompleted) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = listWithItems.groceryList.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Status badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isCompleted) MaterialTheme.colorScheme.surfaceVariant 
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isCompleted) "Completed" else "Draft",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Row {
                    IconButton(onClick = onClone) {
                        Icon(
                            imageVector = Icons.Default.CopyAll,
                            contentDescription = "Duplicate List",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete List",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Items Checked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "$checkedCount / $totalCount",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isCompleted) "Final Total" else "Running Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${"%,.2f".format(totalAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        if (listWithItems.groceryList.budgetCap != null) {
                            Text(
                                text = " / ₹${listWithItems.groceryList.budgetCap}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryDetailScreen(
    listWithItems: GroceryListWithItems,
    categories: List<Category>,
    onAddItem: (Long, String, Int, Double) -> Unit,
    onUpdateItem: (GroceryItem) -> Unit,
    onDeleteItem: (GroceryItem) -> Unit,
    onToggleItem: (GroceryItem) -> Unit,
    onGetLastPrice: suspend (String) -> Double?,
    onCheckout: (String, Long?, Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf(1) }
    var itemPriceStr by remember { mutableStateOf("") }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    // Autocomplete estimates
    var estimatedPrice by remember { mutableStateOf<Double?>(null) }

    val listId = listWithItems.groceryList.id
    val isCompleted = listWithItems.groceryList.status == "COMPLETED"

    val totalAmount = listWithItems.items.sumOf { it.price * it.quantity }
    val budgetCap = listWithItems.groceryList.budgetCap

    // Calculate budget state colors
    val (footerBgColor, footerTextColor) = remember(totalAmount, budgetCap) {
        if (budgetCap == null) {
            Pair(Color.Transparent, Color.Unspecified)
        } else {
            when {
                totalAmount <= budgetCap * 0.9 -> Pair(Color(0xFFE6F4EA), Color(0xFF137333)) // Light Green
                totalAmount <= budgetCap -> Pair(Color(0xFFFEF7E0), Color(0xFFB06000))       // Light Yellow
                else -> Pair(Color(0xFFFCE8E6), Color(0xFFC5221F))                           // Light Red
            }
        }
    }

    LaunchedEffect(itemName) {
        if (itemName.isNotBlank() && itemName.length >= 2) {
            estimatedPrice = onGetLastPrice(itemName.trim())
        } else {
            estimatedPrice = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = listWithItems.groceryList.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (budgetCap != null) {
                            Text(
                                text = "Budget Cap: ₹${"%,.2f".format(budgetCap)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!isCompleted) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Budget warning message if cap exists
                        if (budgetCap != null && footerBgColor != Color.Transparent) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(footerBgColor)
                                    .padding(vertical = 6.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = when {
                                        totalAmount <= budgetCap * 0.9 -> "✓ Budget status: Under control."
                                        totalAmount <= budgetCap -> "⚠ Budget status: Close to spending cap!"
                                        else -> "🗙 Budget status: Overspent!"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = footerTextColor
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Running Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = "₹${"%,.2f".format(totalAmount)}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = { showCheckoutDialog = true },
                                enabled = listWithItems.items.any { it.isChecked },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .widthIn(min = 140.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Checkout", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isCompleted) {
                // Add Item Section Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("Item Name (e.g., Milk)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            // Quantity selectors
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { if (itemQty > 1) itemQty-- },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, null, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = itemQty.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.widthIn(min = 20.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(
                                    onClick = { itemQty++ },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = itemPriceStr,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        itemPriceStr = input
                                    }
                                },
                                label = { Text("Est. Price (₹)") },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    if (itemName.isNotBlank()) {
                                        val price = itemPriceStr.toDoubleOrNull() ?: estimatedPrice ?: 0.0
                                        onAddItem(listId, itemName.trim(), itemQty, price)
                                        itemName = ""
                                        itemQty = 1
                                        itemPriceStr = ""
                                    }
                                },
                                enabled = itemName.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("Add")
                            }
                        }

                        // Display price estimates lookup helper
                        if (estimatedPrice != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Last paid: ₹${"%,.2f".format(estimatedPrice)} (Tap Add to apply)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Grocery Items Checklist Table
            if (listWithItems.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add items to complete your checklist.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Group unchecked at top, checked at bottom
                    val uncheckedItems = listWithItems.items.filter { !it.isChecked }
                    val checkedItems = listWithItems.items.filter { it.isChecked }

                    if (uncheckedItems.isNotEmpty()) {
                        item {
                            Text(
                                text = "To Buy",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(uncheckedItems, key = { "item_${it.id}" }) { item ->
                            GroceryItemRow(
                                item = item,
                                isCompleted = isCompleted,
                                onToggle = { onToggleItem(item) },
                                onDelete = { onDeleteItem(item) },
                                onUpdatePrice = { price -> onUpdateItem(item.copy(price = price)) },
                                onUpdateQty = { qty -> onUpdateItem(item.copy(quantity = qty)) }
                            )
                        }
                    }

                    if (checkedItems.isNotEmpty()) {
                        item {
                            Text(
                                text = "Checked Off",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(checkedItems, key = { "item_${it.id}" }) { item ->
                            GroceryItemRow(
                                item = item,
                                isCompleted = isCompleted,
                                onToggle = { onToggleItem(item) },
                                onDelete = { onDeleteItem(item) },
                                onUpdatePrice = { price -> onUpdateItem(item.copy(price = price)) },
                                onUpdateQty = { qty -> onUpdateItem(item.copy(quantity = qty)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCheckoutDialog) {
        CheckoutSummaryDialog(
            listWithItems = listWithItems,
            categories = categories,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { method, categoryId, carryForward ->
                onCheckout(method, categoryId, carryForward)
                showCheckoutDialog = false
            }
        )
    }
}

@Composable
fun GroceryItemRow(
    item: GroceryItem,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onUpdatePrice: (Double) -> Unit,
    onUpdateQty: (Int) -> Unit
) {
    var priceStr by remember(item.price) {
        mutableStateOf(if (item.price == 0.0) "" else item.price.toString())
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isCompleted) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("item_check_${item.id}")
                )
            } else {
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (item.isChecked && !isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Subtext showing running multiplication
                if (item.price > 0) {
                    Text(
                        text = "${item.quantity} × ₹${"%,.2f".format(item.price)} = ₹${"%,.2f".format(item.quantity * item.price)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (!isCompleted) {
                // Quantity buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    IconButton(
                        onClick = { if (item.quantity > 1) onUpdateQty(item.quantity - 1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = item.quantity.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(
                        onClick = { onUpdateQty(item.quantity + 1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    }
                }

                // Price entry field
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            priceStr = input
                            onUpdatePrice(input.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    placeholder = { Text("0.00", style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                    modifier = Modifier
                        .width(70.dp)
                        .height(48.dp)
                        .padding(horizontal = 2.dp)
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Display simple qty and total price for historical listings
                Text(
                    text = "₹${"%,.2f".format(item.price * item.quantity)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CheckoutSummaryDialog(
    listWithItems: GroceryListWithItems,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (paymentMethod: String, categoryId: Long?, carryForward: Boolean) -> Unit
) {
    val checkedItems = listWithItems.items.filter { it.isChecked }
    val unboughtCount = listWithItems.items.count { !it.isChecked }
    val total = checkedItems.sumOf { it.price * it.quantity }

    var paymentMethod by remember { mutableStateOf("UPI") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var carryForward by remember { mutableStateOf(true) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val debitCategories = remember(categories) {
        val inflowNames = setOf("salary", "refund", "interest", "other inflow")
        categories.filter { it.name.lowercase() !in inflowNames }
    }

    LaunchedEffect(debitCategories) {
        if (selectedCategoryId == null) {
            selectedCategoryId = debitCategories.find { it.name.lowercase() == "groceries" }?.id
                ?: debitCategories.firstOrNull()?.id
        }
    }

    val selectedCategoryName = remember(selectedCategoryId, debitCategories) {
        debitCategories.find { it.id == selectedCategoryId }?.name ?: "Groceries"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Finalize Purchases & Checkout",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "A ledger transaction of ₹${"%,.2f".format(total)} will be written to your account logs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Select Payment Mode Row
                Text(
                    text = "Select Payment Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "CASH" to Icons.Default.Money,
                        "UPI" to Icons.Default.QrCodeScanner,
                        "CARD" to Icons.Default.CreditCard
                    ).forEach { (method, iconVec) ->
                        val isSel = paymentMethod == method
                        val tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMethod = method }
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = method,
                                    tint = tint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = method,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Select Ledger Category Dropdown
                Text(
                    text = "Ledger Posting Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedCategoryName)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        debitCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Carry forward option
                if (unboughtCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Carry Forward Unbought Items",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Moves $unboughtCount unchecked items to a new draft checklist.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Switch(
                            checked = carryForward,
                            onCheckedChange = { carryForward = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(paymentMethod, selectedCategoryId, carryForward) },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify & Post")
                    }
                }
            }
        }
    }
}
