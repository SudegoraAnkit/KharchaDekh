package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Category
import com.example.ui.components.getIconVector

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onAddCategory: (name: String, icon: String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onUpdateCategoryBudget: (Category, Double?) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryName by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("star") }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForBudget by remember { mutableStateOf<Category?>(null) }
    var budgetLimitInput by remember { mutableStateOf("") }

    val iconOptions = listOf(
        "star", "restaurant", "shopping_cart", "home", "directions_car", 
        "shopping_bag", "receipt_long", "movie", "medical_services", 
        "account_balance", "celebration", "local_taxi", "card_membership"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_category_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add General Category")
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Manage Categories & Budgets",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Tap on the Add icon below to create custom tags. Click any category card to set/manage monthly spending budgets.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("categories_grid")
            ) {
                items(categories, key = { "cat_${it.id}" }) { cat ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategoryForBudget = cat
                                budgetLimitInput = cat.budgetLimit?.toString() ?: ""
                                showBudgetDialog = true
                            }
                            .testTag("category_card_${cat.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconVector(cat.iconResName),
                                        contentDescription = cat.name,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    
                                    val limitText = if (cat.budgetLimit != null && cat.budgetLimit > 0) {
                                        "Budget: ₹${"%,.0f".format(cat.budgetLimit)}"
                                    } else {
                                        "No limit set"
                                    }
                                    
                                    Text(
                                        text = limitText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cat.budgetLimit != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            if (cat.isCustom) {
                                IconButton(
                                    onClick = { onDeleteCategory(cat) },
                                    modifier = Modifier.size(32.dp).testTag("delete_cat_btn_${cat.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Create Custom Tag",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Subscriptions, Gifts") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_cat_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text(
                        text = "Pick Icon",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    // Draw a row grid of selectable symbols
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val cols = 5
                        val spacing = 6.dp
                        val w = (maxWidth - (spacing * (cols - 1))) / cols

                        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                            iconOptions.chunked(cols).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                    row.forEach { iconName ->
                                        val isSel = selectedIconName == iconName
                                        val bg = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        val tint = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(w)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bg)
                                                .clickable { selectedIconName = iconName }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getIconVector(iconName),
                                                contentDescription = iconName,
                                                tint = tint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (categoryName.isNotBlank()) {
                            onAddCategory(categoryName.trim(), selectedIconName)
                            categoryName = ""
                            selectedIconName = "star"
                            showAddDialog = false
                        }
                    },
                    enabled = categoryName.isNotBlank()
                ) {
                    Text("Add tag")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBudgetDialog && selectedCategoryForBudget != null) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Text(
                    text = "Spending Limit: ${selectedCategoryForBudget?.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Set a monthly spending limit for this category. Leave blank to remove budget constraint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    OutlinedTextField(
                        value = budgetLimitInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                budgetLimitInput = input
                            }
                        },
                        label = { Text("Budget Limit (₹)") },
                        placeholder = { Text("e.g. 5000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("budget_limit_input_field"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = budgetLimitInput.toDoubleOrNull()
                        onUpdateCategoryBudget(selectedCategoryForBudget!!, limit)
                        showBudgetDialog = false
                    },
                    modifier = Modifier.testTag("save_budget_btn")
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
