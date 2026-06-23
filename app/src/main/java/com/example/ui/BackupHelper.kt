package com.example.ui

import com.example.data.Slide
import com.example.data.Variable
import com.example.data.NetworkFormula
import com.example.data.NetworkVariable
import com.example.data.SlideWithVariables
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.Exception

@JsonClass(generateAdapter = true)
data class DatabaseBackup(
    val slides: List<BackupSlide>,
    val variables: List<BackupVariable>,
    val activeSlideId: Int? = null,
    val decimalPlaces: Int? = null,
    val isDarkMode: Boolean? = null,
    val adminPassword: String? = null,
    val favoriteSlideIds: List<Int>? = null,
    val calculationHistory: List<BackupCalculationLog>? = null,
    val backupDate: String? = null,
    val version: String? = null
)

@JsonClass(generateAdapter = true)
data class BackupCalculationLog(
    val slideId: Int,
    val timestamp: String,
    val inputs: String,
    val result: String
)

data class RestoreResult(
    val slides: List<Slide>,
    val variables: List<Variable>,
    val activeSlideId: Int?,
    val decimalPlaces: Int?,
    val isDarkMode: Boolean?,
    val adminPassword: String?,
    val favoriteSlideIds: List<Int>?,
    val calculationHistory: List<BackupCalculationLog>?,
    val backupDate: String?,
    val version: String?
)

@JsonClass(generateAdapter = true)
data class BackupSlide(
    val id: Int,
    val title: String,
    val formula: String,
    val resultUnit: String,
    val description: String,
    val category: String
)

@JsonClass(generateAdapter = true)
data class BackupVariable(
    val slideId: Int,
    val symbol: String,
    val name: String,
    val value: Double,
    val unit: String,
    val isRequired: Boolean = true,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0
)

object BackupHelper {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val adapter = moshi.adapter(DatabaseBackup::class.java)

    /**
     * Serializes entities to a JSON String.
     */
    fun createBackupJson(
        slides: List<Slide>, 
        variables: List<Variable>,
        activeSlideId: Int? = null,
        decimalPlaces: Int? = null,
        isDarkMode: Boolean? = null,
        adminPassword: String? = null,
        favoriteSlideIds: List<Int>? = null,
        calculationHistory: List<BackupCalculationLog>? = null,
        backupDate: String? = null,
        version: String? = "1.0"
    ): String {
        val backupSlides = slides.map {
            BackupSlide(
                id = it.id,
                title = it.title,
                formula = it.formula,
                resultUnit = it.resultUnit,
                description = it.description,
                category = it.category
            )
        }
        val backupVars = variables.map {
            BackupVariable(
                slideId = it.slideId,
                symbol = it.symbol,
                name = it.name,
                value = it.value,
                unit = it.unit,
                isRequired = it.isRequired,
                isHidden = it.isHidden,
                sortOrder = it.sortOrder
            )
        }
        val backup = DatabaseBackup(
            slides = backupSlides, 
            variables = backupVars,
            activeSlideId = activeSlideId,
            decimalPlaces = decimalPlaces,
            isDarkMode = isDarkMode,
            adminPassword = adminPassword,
            favoriteSlideIds = favoriteSlideIds,
            calculationHistory = calculationHistory,
            backupDate = backupDate,
            version = version
        )
        return adapter.toJson(backup)
    }

    /**
     * Deserializes JSON String to lists of Entity classes and settings.
     */
    fun restoreFromBackupJson(json: String): RestoreResult? {
        return try {
            val backup = adapter.fromJson(json) ?: return null
            val slides = backup.slides.map {
                Slide(
                    id = it.id,
                    title = it.title,
                    formula = it.formula,
                    resultUnit = it.resultUnit,
                    description = it.description,
                    category = it.category
                )
            }
            val variables = backup.variables.map {
                Variable(
                    id = 0L, // Handled automatically by Room
                    slideId = it.slideId,
                    symbol = it.symbol,
                    name = it.name,
                    value = it.value,
                    unit = it.unit,
                    isRequired = it.isRequired,
                    isHidden = it.isHidden,
                    sortOrder = it.sortOrder
                )
            }
            RestoreResult(
                slides = slides,
                variables = variables,
                activeSlideId = backup.activeSlideId,
                decimalPlaces = backup.decimalPlaces,
                isDarkMode = backup.isDarkMode,
                adminPassword = backup.adminPassword,
                favoriteSlideIds = backup.favoriteSlideIds,
                calculationHistory = backup.calculationHistory,
                backupDate = backup.backupDate,
                version = backup.version
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val cloudFormulaListType = Types.newParameterizedType(List::class.java, NetworkFormula::class.java)
    private val cloudFormulaListAdapter = moshi.adapter<List<NetworkFormula>>(cloudFormulaListType)

    /**
     * Serializes formulas and variables into the standardized format of the future synchronization API.
     */
    fun createCloudBlueprintJson(slides: List<SlideWithVariables>): String {
        val payload = slides.map { swv ->
            NetworkFormula(
                id = swv.slide.id,
                title = swv.slide.title,
                category = swv.slide.category,
                formula = swv.slide.formula,
                resultUnit = swv.slide.resultUnit,
                description = swv.slide.description,
                variables = swv.variables.map { v ->
                    NetworkVariable(
                        symbol = v.symbol,
                        name = v.name,
                        value = v.value,
                        unit = v.unit
                    )
                }
            )
        }
        return cloudFormulaListAdapter.indent("  ").toJson(payload)
    }

    /**
     * Deserializes external JSON schema from API endpoints or dashboard presets.
     */
    fun restoreFromCloudBlueprintJson(json: String): List<NetworkFormula>? {
        return try {
            cloudFormulaListAdapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
