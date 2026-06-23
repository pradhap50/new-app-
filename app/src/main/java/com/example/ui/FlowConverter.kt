package com.example.ui

import java.util.Locale

object FlowConverter {
    // Conversion factors to base unit: LPH
    val LPH_PER_UNIT = mapOf(
        "LPH" to 1.0,
        "LPM" to 60.0,
        "m³/hr" to 1000.0,
        "m³/min" to 60000.0,
        "GPH" to 3.785411784,
        "GPM" to 227.12470704,
        "mL/min" to 0.06,
        "mL/hr" to 0.001
    )

    val units = listOf("LPH", "LPM", "m³/hr", "m³/min", "GPH", "GPM", "mL/min", "mL/hr")

    val unitDescriptions = mapOf(
        "LPH" to "Liters/Hour",
        "LPM" to "Liters/Minute",
        "m³/hr" to "Cubic Meter/Hour",
        "m³/min" to "Cubic Meter/Minute",
        "GPH" to "Gallons/Hour",
        "GPM" to "Gallons/Minute",
        "mL/min" to "Milliliters/Minute",
        "mL/hr" to "Milliliters/Hour"
    )

    fun getLphFactor(unit: String): Double {
        val cleanUnit = unit.trim()
        // Map common flow representations (kg/H, kg/h is treated as LPH/kgH in density 1)
        if (cleanUnit.equals("kg/H", ignoreCase = true) || cleanUnit.equals("kg/h", ignoreCase = true)) {
            return 1.0
        }
        return LPH_PER_UNIT[cleanUnit] ?: 1.0
    }

    fun isFlowUnit(unit: String): Boolean {
        val cleanUnit = unit.trim()
        if (cleanUnit.equals("kg/H", ignoreCase = true) || cleanUnit.equals("kg/h", ignoreCase = true)) {
            return true
        }
        return cleanUnit in LPH_PER_UNIT
    }

    fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        if (value.isNaN() || value.isInfinite()) return value
        val factorFrom = getLphFactor(fromUnit)
        val factorTo = getLphFactor(toUnit)
        val lphVal = value * factorFrom
        return lphVal / factorTo
    }

    fun getEquivalents(value: Double, fromUnit: String): List<Pair<String, Double>> {
        return units.map { toUnit ->
            toUnit to convert(value, fromUnit, toUnit)
        }
    }
}
