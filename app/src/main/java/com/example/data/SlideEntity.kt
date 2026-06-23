package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "slides")
data class Slide(
    @PrimaryKey val id: Int, // 1 to 100
    val title: String,
    val formula: String, // e.g. "(q * C * 60) / P"
    val resultUnit: String, // e.g. "g/t", "%", "mL", "kg"
    val description: String = "",
    val category: String = "Custom" // e.g. "Dosage", "Production", "Dilution", "Custom"
)

@Entity(
    tableName = "variables",
    foreignKeys = [
        ForeignKey(
            entity = Slide::class,
            parentColumns = ["id"],
            childColumns = ["slideId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["slideId"])]
)
data class Variable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val slideId: Int,
    val symbol: String, // e.g. "q", "C", "P"
    val name: String, // e.g. "Chemical Flow", "Concentration"
    val value: Double, // current value
    val unit: String, // e.g. "L/min", "%", "g/L"
    val isRequired: Boolean = true,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0
)

data class SlideWithVariables(
    @Embedded val slide: Slide,
    @Relation(
        parentColumn = "id",
        entityColumn = "slideId"
    )
    val variables: List<Variable>
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: String,
    val userMobile: String,
    val actionType: String, // "LOGIN", "EDIT_FORMULA", "RESET_FORMULA", "BACKUP", "RESTORE", "ADD_SLIDE", "REORDER_SLIDE"
    val description: String
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val uid: String,
    val email: String,
    val role: String,
    val trialStartDate: String,
    val trialEndDate: String,
    val subscriptionStatus: String,
    val accountType: String,
    val createdAt: String,
    val isActive: Boolean,
    val syncStatus: String = "synced"
)

@Entity(tableName = "user_activity_logs")
data class UserActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uid: String,
    val email: String,
    val action: String, // "LOGIN" or "LOGOUT"
    val timestamp: Long,
    val syncStatus: String // "synced", "pending_login", "pending_logout", "pending_activity"
)

