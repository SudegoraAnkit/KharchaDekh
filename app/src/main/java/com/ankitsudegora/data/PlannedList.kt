package com.ankitsudegora.data

import androidx.room.*

@Entity(tableName = "planned_lists")
data class PlannedList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val budgetCap: Double? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val status: String = "DRAFT", // "DRAFT" or "COMPLETED"
    val categoryId: Long? = null
)

@Entity(
    tableName = "planned_items",
    foreignKeys = [
        ForeignKey(
            entity = PlannedList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class PlannedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Int = 1,
    val price: Double = 0.0,
    val isChecked: Boolean = false
)

data class PlannedListWithItems(
    @Embedded val plannedList: PlannedList,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId"
    )
    val items: List<PlannedItem>
)
