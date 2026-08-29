package com.ankitsudegora.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.*
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
import com.ankitsudegora.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlannedListsScreen(
    plannedLists: List<PlannedListWithItems>,
    categories: List<Category>,
    pendingTransactions: List<TransactionWithCategory>,
    onAddList: (String, Double?, Long?) -> Unit,
    onDeleteList: (PlannedList) -> Unit,
    onDuplicateList: (PlannedListWithItems, String) -> Unit,
    onAddItem: (Long, String, Int, Double) -> Unit,
    onUpdateItem: (PlannedItem) -> Unit,
    onDeleteItem: (PlannedItem) -> Unit,
    onToggleItem: (PlannedItem) -> Unit,
    onGetLastPrice: suspend (String) -> Double?,
    onCheckout: (PlannedListWithItems, String, Long?, Boolean, Long?) -> Unit,
    onUpdateTransactionAmount: (Long, Double) -> Unit,
    onGetTransactionsByLinkedListId: suspend (Long) -> List<Transaction>,
    onUnlinkTransaction: (Long) -> Unit,
    onLinkTransaction: (Long, Long) -> Unit
) {
    var activeListId by remember { mutableStateOf<Long?>(null) }
    
    val activeList = remember(plannedLists, activeListId) {
        plannedLists.find { it.plannedList.id == activeListId }
    }

    if (activeList != null) {
        PlannedDetailScreen(
            listWithItems = activeList,
            categories = categories,
            pendingTransactions = pendingTransactions,
            onAddItem = onAddItem,
            onUpdateItem = onUpdateItem,
            onDeleteItem = onDeleteItem,
            onToggleItem = onToggleItem,
            onGetLastPrice = onGetLastPrice,
            onCheckout = { method, categoryId, carryForward, linkedPendingTxnId ->
                onCheckout(activeList, method, categoryId, carryForward, linkedPendingTxnId)
                activeListId = null
            },
            onUpdateTransactionAmount = onUpdateTransactionAmount,
            onGetTransactionsByLinkedListId = onGetTransactionsByLinkedListId,
            onUnlinkTransaction = onUnlinkTransaction,
            onLinkTransaction = onLinkTransaction,
            onNavigateBack = { activeListId = null }
        )
    } else {
        PlannedMasterScreen(
            plannedLists = plannedLists,
            categories = categories,
            onAddList = onAddList,
            onDeleteList = onDeleteList,
            onDuplicateList = onDuplicateList,
            onSelectList = { listId -> activeListId = listId }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedMasterScreen(
    plannedLists: List<PlannedListWithItems>,
    categories: List<Category>,
    onAddList: (String, Double?, Long?) -> Unit,
    onDeleteList: (PlannedList) -> Unit,
    onDuplicateList: (PlannedListWithItems, String) -> Unit,
    onSelectList: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: My Lists, 1: Completed
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val draftLists = remember(plannedLists, searchQuery) {
        plannedLists.filter { it.plannedList.status != "COMPLETED" && (searchQuery.isBlank() || it.plannedList.name.contains(searchQuery, ignoreCase = true)) }
    }
    val completedLists = remember(plannedLists, searchQuery) {
        plannedLists.filter { it.plannedList.status == "COMPLETED" && (searchQuery.isBlank() || it.plannedList.name.contains(searchQuery, ignoreCase = true)) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearching) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search lists...", color = DarkOnSurfaceVariant) },
                        trailingIcon = {
                            IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkOnSurface)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = BrandLime,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkOnSurface,
                            unfocusedTextColor = DarkOnSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text(
                        text = "Lists",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = DarkOnSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { isSearching = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = DarkOnSurface
                            )
                        }

                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BrandLime)
                                .testTag("add_planned_list_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New List",
                                tint = DarkBackground
                            )
                        }
                    }
                }
            }

            // Segmented Tabs [ My Lists ] vs [ Completed ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(4.dp)
            ) {
                listOf("My Lists" to draftLists.size, "Completed" to completedLists.size).forEachIndexed { index, (label, count) ->
                    val isSel = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) BrandLime else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$label ($count)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSel) DarkBackground else DarkOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentDisplayLists = if (selectedTab == 0) draftLists else completedLists

            if (currentDisplayLists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.PlaylistAddCheck,
                            contentDescription = null,
                            tint = DarkOnSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "No active lists" else "No completed lists",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedTab == 0) "Tap the '+' button above to create a shopping list." else "Completed checklists will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(currentDisplayLists, key = { it.plannedList.id }) { item ->
                        PlannedListCard(
                            listWithItems = item,
                            categories = categories,
                            onClick = { onSelectList(item.plannedList.id) },
                            onDelete = { onDeleteList(item.plannedList) },
                            onClone = {
                                onDuplicateList(item, "${item.plannedList.name} (Copy)")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CreateListDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onCreate = { name, cap, categoryId ->
                onAddList(name, cap, categoryId)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CreateListDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onCreate: (String, Double?, Long?) -> Unit
) {
    var listName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPresetIndex by remember { mutableStateOf(0) }

    val presets = listOf(
        Triple("Groceries", Icons.Default.ShoppingCart, Color(0xFF22C55E)),
        Triple("Dining / BBQ", Icons.Default.Restaurant, Color(0xFFF59E0B)),
        Triple("Home Essentials", Icons.Default.Home, Color(0xFF3B82F6)),
        Triple("Office Supplies", Icons.Default.Work, Color(0xFFA855F7)),
        Triple("Gift Ideas", Icons.Default.CardGiftcard, Color(0xFFEC4899))
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create List",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = DarkOnSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkOnSurfaceVariant)
                    }
                }

                // List Name Input
                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text("List Name", color = DarkOnSurfaceVariant) },
                    placeholder = { Text("e.g. Monthly Groceries", color = DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = BrandLime,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkOnSurface,
                        unfocusedTextColor = DarkOnSurface
                    )
                )

                // Icon & Color Preset Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Icon & Category",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presets.forEachIndexed { index, (label, icon, color) ->
                            val isSel = selectedPresetIndex == index
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) color else color.copy(alpha = 0.2f))
                                    .clickable { selectedPresetIndex = index }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSel) Color.White else color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Description (Optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)", color = DarkOnSurfaceVariant) },
                    placeholder = { Text("e.g. Groceries for the month", color = DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = BrandLime,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkOnSurface,
                        unfocusedTextColor = DarkOnSurface
                    )
                )

                // Create List CTA Button
                Button(
                    onClick = {
                        if (listName.isNotBlank()) {
                            val matchedCat = categories.find { it.name.contains(presets[selectedPresetIndex].first, ignoreCase = true) }
                            onCreate(listName.trim(), null, matchedCat?.id)
                        }
                    },
                    enabled = listName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandLime,
                        contentColor = DarkBackground,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = DarkOnSurfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Create List",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }
    }
}

@Composable
fun PlannedListCard(
    listWithItems: PlannedListWithItems,
    categories: List<Category>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit
) {
    val totalAmount = listWithItems.items.sumOf { it.price * it.quantity }
    val checkedCount = listWithItems.items.count { it.isChecked }
    val totalCount = listWithItems.items.size
    val uncheckedCount = totalCount - checkedCount
    val isCompleted = listWithItems.plannedList.status == "COMPLETED"
    var menuExpanded by remember { mutableStateOf(false) }

    val associatedCat = remember(categories, listWithItems.plannedList.categoryId) {
        categories.find { it.id == listWithItems.plannedList.categoryId }
    }
    val catColor = CategoryColors.getCategoryColor(associatedCat?.name ?: listWithItems.plannedList.name)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Category/List Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(catColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconVector(associatedCat?.iconResName ?: "shopping_cart"),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = listWithItems.plannedList.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalCount items • $uncheckedCount unchecked",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                }
            }

            // 3-dots Menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = DarkOnSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Duplicate List", color = DarkOnSurface) },
                        leadingIcon = { Icon(Icons.Default.CopyAll, contentDescription = null, tint = BrandLime) },
                        onClick = {
                            menuExpanded = false
                            onClone()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete List", color = DarkError) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DarkError) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedDetailScreen(
    listWithItems: PlannedListWithItems,
    categories: List<Category>,
    pendingTransactions: List<TransactionWithCategory>,
    onAddItem: (Long, String, Int, Double) -> Unit,
    onUpdateItem: (PlannedItem) -> Unit,
    onDeleteItem: (PlannedItem) -> Unit,
    onToggleItem: (PlannedItem) -> Unit,
    onGetLastPrice: suspend (String) -> Double?,
    onCheckout: (String, Long?, Boolean, Long?) -> Unit,
    onUpdateTransactionAmount: (Long, Double) -> Unit,
    onGetTransactionsByLinkedListId: suspend (Long) -> List<Transaction>,
    onUnlinkTransaction: (Long) -> Unit,
    onLinkTransaction: (Long, Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf(1) }
    var itemPriceStr by remember { mutableStateOf("") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    
    // We keep track of the specific transaction ID being edited
    var editingTransactionId by remember { mutableStateOf<Long?>(null) }
    var showEditAmountDialog by remember { mutableStateOf(false) }
    var showLinkAlertSelectionDialog by remember { mutableStateOf(false) }

    // Autocomplete estimates
    var estimatedPrice by remember { mutableStateOf<Double?>(null) }

    val listId = listWithItems.plannedList.id
    val isCompleted = listWithItems.plannedList.status == "COMPLETED"

    var linkedTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    LaunchedEffect(listId) {
        linkedTransactions = onGetTransactionsByLinkedListId(listId)
    }

    val totalAmount = listWithItems.items.sumOf { it.price * it.quantity }
    val budgetCap = listWithItems.plannedList.budgetCap

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
                            text = listWithItems.plannedList.name,
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
            } else {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Final Settled Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val displayAmount = if (linkedTransactions.isNotEmpty()) linkedTransactions.sumOf { it.amount } else totalAmount
                                Text(
                                    text = "₹${"%,.2f".format(displayAmount)}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.secondary
                                )
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
                val handleAddItem: () -> Unit = {
                    if (itemName.isNotBlank()) {
                        val trimmed = itemName.trim()
                        val regexWithPrice = Regex("""^(.*?)\s+(\d+(?:\.\d{1,2})?)$""")
                        val match = regexWithPrice.find(trimmed)

                        var finalName = trimmed
                        var finalPrice = itemPriceStr.toDoubleOrNull() ?: estimatedPrice ?: 0.0

                        if (itemPriceStr.isBlank() && match != null) {
                            val potentialName = match.groupValues[1].trim()
                            val potentialPrice = match.groupValues[2].toDoubleOrNull()
                            if (potentialName.isNotEmpty() && potentialPrice != null) {
                                finalName = potentialName
                                finalPrice = potentialPrice
                            }
                        }

                        onAddItem(listId, finalName, itemQty, finalPrice)
                        itemName = ""
                        itemQty = 1
                        itemPriceStr = ""
                    }
                }

                // Add Item Section Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("Quick Item (e.g. Milk 60)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { handleAddItem() }),
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
                                    Icon(Icons.Default.RemoveCircleOutline, null, modifier = Modifier.size(20.dp), tint = DarkOnSurfaceVariant)
                                }
                                Text(
                                    text = itemQty.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface,
                                    modifier = Modifier.widthIn(min = 20.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(
                                    onClick = { itemQty++ },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(20.dp), tint = BrandLime)
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
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { handleAddItem() }),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = handleAddItem,
                                enabled = itemName.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandLime,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontWeight = FontWeight.Bold)
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
                                    tint = BrandLime,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Last paid: ₹${"%,.2f".format(estimatedPrice)} (Auto-applied if price is empty)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandLime
                                )
                            }
                        }
                    }
                }
            }

            // Items Checklist Checklist
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
                        if (isCompleted) {
                            item {
                                LinkedTransactionsSection(
                                    linkedTransactions = linkedTransactions,
                                    onEditTransactionAmount = { txnId ->
                                        editingTransactionId = txnId
                                        showEditAmountDialog = true
                                    },
                                    onUnlinkTransaction = { txnId ->
                                        onUnlinkTransaction(txnId)
                                        linkedTransactions = linkedTransactions.filter { it.id != txnId }
                                    },
                                    onLinkAnotherClick = {
                                        showLinkAlertSelectionDialog = true
                                    }
                                )
                            }
                        }

                        val uncheckedItems = listWithItems.items.filter { !it.isChecked }
                        val checkedItems = listWithItems.items.filter { it.isChecked }

                        if (uncheckedItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "To Buy / Settle",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(uncheckedItems, key = { "item_${it.id}" }) { item ->
                                PlannedItemRow(
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
                                    text = "Checked / Bought",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(checkedItems, key = { "item_${it.id}" }) { item ->
                                PlannedItemRow(
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
            pendingTransactions = pendingTransactions,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { method, categoryId, carryForward, linkedPendingTxnId ->
                onCheckout(method, categoryId, carryForward, linkedPendingTxnId)
                showCheckoutDialog = false
            }
        )
    }

    if (showEditAmountDialog && editingTransactionId != null) {
        var editAmountStr by remember(editingTransactionId) {
            val amt = linkedTransactions.find { it.id == editingTransactionId }?.amount ?: 0.0
            mutableStateOf(if (amt == 0.0) "" else amt.toString())
        }
        AlertDialog(
            onDismissRequest = { showEditAmountDialog = false },
            title = { Text("Edit Linked Settle Amount") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Update the settled cash for this specific transaction. This will modify the transaction in your ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextField(
                        value = editAmountStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                editAmountStr = input
                            }
                        },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newAmount = editAmountStr.toDoubleOrNull() ?: 0.0
                        if (editingTransactionId != null) {
                            onUpdateTransactionAmount(editingTransactionId!!, newAmount)
                            linkedTransactions = linkedTransactions.map {
                                if (it.id == editingTransactionId) it.copy(amount = newAmount) else it
                            }
                        }
                        showEditAmountDialog = false
                    },
                    enabled = editAmountStr.isNotBlank() && (editAmountStr.toDoubleOrNull() ?: 0.0) > 0.0
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAmountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLinkAlertSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showLinkAlertSelectionDialog = false },
            title = { Text("Link Pending Bank Alert") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select a pending transaction to map and link to this completed checklist. This helps group related expenses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (pendingTransactions.isEmpty()) {
                        Text(
                            text = "No pending bank alerts or transactions available to link.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(pendingTransactions, key = { it.transaction.id }) { item ->
                                Surface(
                                    onClick = {
                                        onLinkTransaction(item.transaction.id, listId)
                                        linkedTransactions = linkedTransactions + item.transaction
                                        showLinkAlertSelectionDialog = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.transaction.merchant,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = item.category?.name ?: "Uncategorized",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Text(
                                            text = "₹${item.transaction.amount}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLinkAlertSelectionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PlannedItemRow(
    item: PlannedItem,
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
                
                if (item.price > 0) {
                    Text(
                        text = "${item.quantity} × ₹${"%,.2f".format(item.price)} = ₹${"%,.2f".format(item.quantity * item.price)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (!isCompleted) {
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
    listWithItems: PlannedListWithItems,
    categories: List<Category>,
    pendingTransactions: List<TransactionWithCategory>,
    onDismiss: () -> Unit,
    onConfirm: (paymentMethod: String, categoryId: Long?, carryForward: Boolean, linkedPendingTxnId: Long?) -> Unit
) {
    val checkedItems = listWithItems.items.filter { it.isChecked }
    val unboughtCount = listWithItems.items.count { !it.isChecked }
    val total = checkedItems.sumOf { it.price * it.quantity }

    var paymentMethod by remember { mutableStateOf("UPI") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var carryForward by remember { mutableStateOf(true) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var selectedPendingTxnId by remember { mutableStateOf<Long?>(null) }
    var pendingDropdownExpanded by remember { mutableStateOf(false) }

    val debitCategories = remember(categories) {
        val inflowNames = setOf("salary", "refund", "interest", "other inflow")
        categories.filter { it.name.lowercase() !in inflowNames }
    }

    LaunchedEffect(debitCategories, listWithItems.plannedList.categoryId) {
        if (selectedCategoryId == null) {
            selectedCategoryId = listWithItems.plannedList.categoryId 
                ?: debitCategories.find { it.name.lowercase() == "groceries" }?.id
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

                // Map to Pending transaction dropdown (Optional)
                if (pendingTransactions.isNotEmpty()) {
                    Text(
                        text = "Link to Pending Bank Alert (Optional)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    val selectedPendingTxn = remember(selectedPendingTxnId, pendingTransactions) {
                        pendingTransactions.find { it.transaction.id == selectedPendingTxnId }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { pendingDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedPendingTxn != null) {
                                        "₹${selectedPendingTxn.transaction.amount} from ${selectedPendingTxn.transaction.merchant}"
                                    } else {
                                        "Select alert to map (no duplicate)"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectedPendingTxnId != null) {
                                        IconButton(
                                            onClick = { selectedPendingTxnId = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear selection",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = pendingDropdownExpanded,
                            onDismissRequest = { pendingDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            pendingTransactions.forEach { pt ->
                                DropdownMenuItem(
                                    text = {
                                        Text("₹${pt.transaction.amount} - ${pt.transaction.merchant} (${pt.category?.name ?: "Pending"})")
                                    },
                                    onClick = {
                                        selectedPendingTxnId = pt.transaction.id
                                        pendingDropdownExpanded = false
                                        if (pt.transaction.categoryId != null) {
                                            selectedCategoryId = pt.transaction.categoryId
                                        }
                                        val u = pt.transaction.paymentMethod
                                        if (u == "UPI" || u == "CARD" || u == "CASH") {
                                            paymentMethod = u
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

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
                        onClick = { onConfirm(paymentMethod, selectedCategoryId, carryForward, selectedPendingTxnId) },
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

@Composable
fun LinkedTransactionsSection(
    linkedTransactions: List<Transaction>,
    onEditTransactionAmount: (Long) -> Unit,
    onUnlinkTransaction: (Long) -> Unit,
    onLinkAnotherClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Linked Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = onLinkAnotherClick) {
                    Text(
                        text = "+ Link Alert",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (linkedTransactions.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No ledger transactions linked to this completed list yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                linkedTransactions.forEachIndexed { index, txn ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = txn.merchant,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatter.format(Date(txn.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "₹${"%,.2f".format(txn.amount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { onEditTransactionAmount(txn.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Amount",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onUnlinkTransaction(txn.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Unlink",
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
}



