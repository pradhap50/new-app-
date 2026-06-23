package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalculatorRepository(private val slideDao: SlideDao) {

    val allSlides: Flow<List<SlideWithVariables>> = slideDao.getAllSlides()
    val allActivityLogs: Flow<List<ActivityLog>> = slideDao.getAllActivityLogs()

    suspend fun insertActivityLog(log: ActivityLog) {
        withContext(Dispatchers.IO) {
            slideDao.insertActivityLog(log)
        }
    }

    suspend fun clearActivityLogs() {
        withContext(Dispatchers.IO) {
            slideDao.clearActivityLogs()
        }
    }

    fun getSlideById(id: Int): Flow<SlideWithVariables?> {
        return slideDao.getSlideById(id)
    }

    suspend fun updateSlide(slide: Slide) {
        withContext(Dispatchers.IO) {
            slideDao.updateSlide(slide)
        }
    }

    suspend fun insertVariable(variable: Variable): Long {
        return withContext(Dispatchers.IO) {
            slideDao.insertVariable(variable)
        }
    }

    suspend fun updateVariable(variable: Variable) {
        withContext(Dispatchers.IO) {
            slideDao.updateVariable(variable)
        }
    }

    suspend fun deleteVariable(variable: Variable) {
        withContext(Dispatchers.IO) {
            slideDao.deleteVariable(variable)
        }
    }

    suspend fun updateSlideAndVariables(slide: Slide, variables: List<Variable>) {
        withContext(Dispatchers.IO) {
            slideDao.updateSlideAndVariables(slide, variables)
        }
    }

    suspend fun deleteSlide(slideId: Int) {
        withContext(Dispatchers.IO) {
            slideDao.deleteSlideAndVariables(slideId)
        }
    }

    suspend fun resetDatabase(slides: List<Slide>, variables: List<Variable>) {
        withContext(Dispatchers.IO) {
            slideDao.resetDatabase(slides, variables)
        }
    }

    suspend fun resetToFactoryDefaults() {
        withContext(Dispatchers.IO) {
            val preparedSlides = mutableListOf<Slide>()
            val preparedVariables = mutableListOf<Variable>()

            // 10 Industry Classical Presets
            val presets = listOf(
                PresetData(
                    id = 1,
                    title = "ASA Calculator",
                    category = "Chemical Dosage",
                    description = "Calculates ASA sizing chemical flow rate in kg/H based on paper machine production rate.",
                    formula = "P * D",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("P", "Production", 20.0, "MT/H"),
                        PresetVar("D", "ASA Dosage", 1.2, "kg/T")
                    )
                ),
                PresetData(
                    id = 2,
                    title = "C-Starch Calculator",
                    category = "Chemical Dosage",
                    description = "Calculates cationic starch solid rate (kg/H) and total starch solution flow rate (kg/H) based on ASA flow rate.\n---outputs---\nC-Starch Solution Flow:(F * R) / (C * 0.01):kg/H",
                    formula = "F * R",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("F", "ASA Flow", 24.0, "kg/H"),
                        PresetVar("R", "ASA : C-Starch Ratio", 2.5, "ratio"),
                        PresetVar("C", "C-Starch Concentration", 3.0, "%")
                    )
                ),
                PresetData(
                    id = 3,
                    title = "AKD Calculator",
                    category = "Chemical Dosage",
                    description = "Calculates Alkylketene Dimer sizing chemical flow rate based on machine production.\n---outputs---\nAKD Wet Product Flow:(P * D) / (C * 0.01):kg/H",
                    formula = "P * D",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("P", "Production", 20.0, "MT/H"),
                        PresetVar("D", "AKD Dosage", 1.5, "kg/T"),
                        PresetVar("C", "AKD Concentration", 15.0, "%")
                    )
                ),
                PresetData(
                    id = 4,
                    title = "PAC Calculator",
                    category = "Chemical Dosage",
                    description = "Calculates PAC active mass flow and commercial liquid solution flow rate.\n---outputs---\nPAC Solution Flow:(P * D) / (C * 0.01):kg/H",
                    formula = "P * D",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("P", "Production", 20.0, "MT/H"),
                        PresetVar("D", "PAC Dosage", 5.0, "kg/T"),
                        PresetVar("C", "PAC Concentration", 10.0, "%")
                    )
                ),
                PresetData(
                    id = 5,
                    title = "Alum Calculator",
                    category = "Chemical Dosage",
                    description = "Calculates Alum dry flow and commercial liquid solution flow rate.\n---outputs---\nAlum Solution Flow:(P * D) / (C * 0.01):kg/H",
                    formula = "P * D",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("P", "Production", 20.0, "MT/H"),
                        PresetVar("D", "Alum Dosage", 8.0, "kg/T"),
                        PresetVar("C", "Alum Concentration", 8.0, "%")
                    )
                ),
                PresetData(
                    id = 6,
                    title = "Bentonite Calculator",
                    category = "Chemical Dosage",
                    description = "Calculates bentonite microparticle active dry flow rate and wet slurry flow rate.\n---outputs---\nBentonite Slurry Flow:(P * D) / (C * 0.01):kg/H",
                    formula = "P * D",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("P", "Production", 20.0, "MT/H"),
                        PresetVar("D", "Bentonite Dosage", 2.0, "kg/T"),
                        PresetVar("C", "Bentonite Concentration", 5.0, "%")
                    )
                ),
                PresetData(
                    id = 7,
                    title = "Defoamer Calculator",
                    category = "Chemical Dosage",
                    description = "Calculates defoamer chemical flow rate in kg/H based on production rate.",
                    formula = "P * D",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("P", "Production", 20.0, "MT/H"),
                        PresetVar("D", "Defoamer Dosage", 0.3, "kg/T")
                    )
                ),
                PresetData(
                    id = 8,
                    title = "Retention Aid Calculator",
                    category = "Chemical Dosage",
                    description = "Calculates retention aid active polymer flow rate.\n---outputs---\nPolymer Preparation Flow:((P * D) / 1000) / (C * 0.01):kg/H",
                    formula = "(P * D) / 1000",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("P", "Production", 20.0, "MT/H"),
                        PresetVar("D", "Retention Aid Dosage", 150.0, "g/T"),
                        PresetVar("C", "Polymer Concentration", 0.1, "%")
                    )
                ),
                PresetData(
                    id = 9,
                    title = "Wet Strength Resin",
                    category = "Chemical Dosage",
                    description = "Calculates Wet Strength Resin active dry solids rate and product wet solution flowrate.\n---outputs---\nWSR Solution Flow:(P * D) / (C * 0.01):kg/H",
                    formula = "P * D",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("P", "Production", 20.0, "MT/H"),
                        PresetVar("D", "WSR Dosage", 10.0, "kg/T"),
                        PresetVar("C", "WSR Concentration", 12.5, "%")
                    )
                ),
                PresetData(
                    id = 10,
                    title = "Dry Strength Resin",
                    category = "Chemical Dosage",
                    description = "Calculates Dry Strength Resin active dry solids and solution feed flow rate.\n---outputs---\nDSR Solution Flow:(P * D) / (C * 0.01):kg/H",
                    formula = "P * D",
                    resultUnit = "kg/H",
                    variables = listOf(
                        PresetVar("P", "Production", 20.0, "MT/H"),
                        PresetVar("D", "DSR Dosage", 6.0, "kg/T"),
                        PresetVar("C", "DSR Concentration", 15.0, "%")
                    )
                )
            )

            for (preset in presets) {
                preparedSlides.add(
                    Slide(
                        id = preset.id,
                        title = preset.title,
                        formula = preset.formula,
                        resultUnit = preset.resultUnit,
                        description = preset.description,
                        category = preset.category
                    )
                )
                for (v in preset.variables) {
                    preparedVariables.add(
                        Variable(
                            slideId = preset.id,
                            symbol = v.symbol,
                            name = v.name,
                            value = v.value,
                            unit = v.unit
                        )
                    )
                }
            }

            for (i in 11..100) {
                preparedSlides.add(
                    Slide(
                        id = i,
                        title = "Formula Slide $i",
                        formula = "A + B",
                        resultUnit = "_",
                        description = "Custom calculator slot for manual paper mill calculations.",
                        category = "Custom"
                    )
                )
                preparedVariables.add(
                    Variable(
                        slideId = i,
                        symbol = "A",
                        name = "Input 1",
                        value = 0.0,
                        unit = "%"
                    )
                )
                preparedVariables.add(
                    Variable(
                        slideId = i,
                        symbol = "B",
                        name = "Input 2",
                        value = 0.0,
                        unit = "kg"
                    )
                )
            }

            slideDao.resetDatabase(preparedSlides, preparedVariables)
        }
    }

    suspend fun checkAndPrepopulate() {
        withContext(Dispatchers.IO) {
            val count = slideDao.getSlidesCount()
            if (count == 0) {
                val preparedSlides = mutableListOf<Slide>()
                val preparedVariables = mutableListOf<Variable>()

                // 10 Industry Classical Presets
                val presets = listOf(
                    PresetData(
                        id = 1,
                        title = "ASA Calculator",
                        category = "Chemical Dosage",
                        description = "Calculates ASA sizing chemical flow rate in kg/H based on paper machine production rate.",
                        formula = "P * D",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("P", "Production", 20.0, "MT/H"),
                            PresetVar("D", "ASA Dosage", 1.2, "kg/T")
                        )
                    ),
                    PresetData(
                        id = 2,
                        title = "C-Starch Calculator",
                        category = "Chemical Dosage",
                        description = "Calculates cationic starch solid rate (kg/H) and total starch solution flow rate (kg/H) based on ASA flow rate.\n---outputs---\nC-Starch Solution Flow:(F * R) / (C * 0.01):kg/H",
                        formula = "F * R",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("F", "ASA Flow", 24.0, "kg/H"),
                            PresetVar("R", "ASA : C-Starch Ratio", 2.5, "ratio"),
                            PresetVar("C", "C-Starch Concentration", 3.0, "%")
                        )
                    ),
                    PresetData(
                        id = 3,
                        title = "AKD Calculator",
                        category = "Chemical Dosage",
                        description = "Calculates Alkylketene Dimer sizing chemical flow rate based on machine production.\n---outputs---\nAKD Wet Product Flow:(P * D) / (C * 0.01):kg/H",
                        formula = "P * D",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("P", "Production", 20.0, "MT/H"),
                            PresetVar("D", "AKD Dosage", 1.5, "kg/T"),
                            PresetVar("C", "AKD Concentration", 15.0, "%")
                        )
                    ),
                    PresetData(
                        id = 4,
                        title = "PAC Calculator",
                        category = "Chemical Dosage",
                        description = "Calculates PAC active mass flow and commercial liquid solution flow rate.\n---outputs---\nPAC Solution Flow:(P * D) / (C * 0.01):kg/H",
                        formula = "P * D",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("P", "Production", 20.0, "MT/H"),
                            PresetVar("D", "PAC Dosage", 5.0, "kg/T"),
                            PresetVar("C", "PAC Concentration", 10.0, "%")
                        )
                    ),
                    PresetData(
                        id = 5,
                        title = "Alum Calculator",
                        category = "Chemical Dosage",
                        description = "Calculates Alum dry flow and commercial liquid solution flow rate.\n---outputs---\nAlum Solution Flow:(P * D) / (C * 0.01):kg/H",
                        formula = "P * D",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("P", "Production", 20.0, "MT/H"),
                            PresetVar("D", "Alum Dosage", 8.0, "kg/T"),
                            PresetVar("C", "Alum Concentration", 8.0, "%")
                        )
                    ),
                    PresetData(
                        id = 6,
                        title = "Bentonite Calculator",
                        category = "Chemical Dosage",
                        description = "Calculates bentonite microparticle active dry flow rate and wet slurry flow rate.\n---outputs---\nBentonite Slurry Flow:(P * D) / (C * 0.01):kg/H",
                        formula = "P * D",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("P", "Production", 20.0, "MT/H"),
                            PresetVar("D", "Bentonite Dosage", 2.0, "kg/T"),
                            PresetVar("C", "Bentonite Concentration", 5.0, "%")
                        )
                    ),
                    PresetData(
                        id = 7,
                        title = "Defoamer Calculator",
                        category = "Chemical Dosage",
                        description = "Calculates defoamer chemical flow rate in kg/H based on production rate.",
                        formula = "P * D",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("P", "Production", 20.0, "MT/H"),
                            PresetVar("D", "Defoamer Dosage", 0.3, "kg/T")
                        )
                    ),
                    PresetData(
                        id = 8,
                        title = "Retention Aid Calculator",
                        category = "Chemical Dosage",
                        description = "Calculates retention aid active polymer flow rate.\n---outputs---\nPolymer Preparation Flow:((P * D) / 1000) / (C * 0.01):kg/H",
                        formula = "(P * D) / 1000",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("P", "Production", 20.0, "MT/H"),
                            PresetVar("D", "Retention Aid Dosage", 150.0, "g/T"),
                            PresetVar("C", "Polymer Concentration", 0.1, "%")
                        )
                    ),
                    PresetData(
                        id = 9,
                        title = "Wet Strength Resin",
                        category = "Chemical Dosage",
                        description = "Calculates Wet Strength Resin active dry solids rate and product wet solution flowrate.\n---outputs---\nWSR Solution Flow:(P * D) / (C * 0.01):kg/H",
                        formula = "P * D",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("P", "Production", 20.0, "MT/H"),
                            PresetVar("D", "WSR Dosage", 10.0, "kg/T"),
                            PresetVar("C", "WSR Concentration", 12.5, "%")
                        )
                    ),
                    PresetData(
                        id = 10,
                        title = "Dry Strength Resin",
                        category = "Chemical Dosage",
                        description = "Calculates Dry Strength Resin active dry solids and solution feed flow rate.\n---outputs---\nDSR Solution Flow:(P * D) / (C * 0.01):kg/H",
                        formula = "P * D",
                        resultUnit = "kg/H",
                        variables = listOf(
                            PresetVar("P", "Production", 20.0, "MT/H"),
                            PresetVar("D", "DSR Dosage", 6.0, "kg/T"),
                            PresetVar("C", "DSR Concentration", 15.0, "%")
                        )
                    )
                )

                // Add Presets
                for (preset in presets) {
                    preparedSlides.add(
                        Slide(
                            id = preset.id,
                            title = preset.title,
                            formula = preset.formula,
                            resultUnit = preset.resultUnit,
                            description = preset.description,
                            category = preset.category
                        )
                    )
                    for (v in preset.variables) {
                        preparedVariables.add(
                            Variable(
                                slideId = preset.id,
                                symbol = v.symbol,
                                name = v.name,
                                value = v.value,
                                unit = v.unit
                            )
                        )
                    }
                }

                // Generatively fill up slides 11 to 100 with blank custom calculators
                for (i in 11..100) {
                    preparedSlides.add(
                        Slide(
                            id = i,
                            title = "Formula Slide $i",
                            formula = "A + B",
                            resultUnit = "_",
                            description = "Custom calculator slot for manual paper mill calculations.",
                            category = "Custom"
                        )
                    )
                    preparedVariables.add(
                        Variable(
                            slideId = i,
                            symbol = "A",
                            name = "Input 1",
                            value = 0.0,
                            unit = "%"
                        )
                    )
                    preparedVariables.add(
                        Variable(
                            slideId = i,
                            symbol = "B",
                            name = "Input 2",
                            value = 0.0,
                            unit = "kg"
                        )
                    )
                }

                slideDao.insertSlides(preparedSlides)
                slideDao.insertVariables(preparedVariables)
            }
        }
    }

    suspend fun insertUserProfile(userProfile: UserProfile) {
        withContext(Dispatchers.IO) {
            slideDao.insertUserProfile(userProfile)
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return withContext(Dispatchers.IO) {
            slideDao.getUserProfile(uid)
        }
    }

    suspend fun getUserProfilesBySyncStatus(status: String): List<UserProfile> {
        return withContext(Dispatchers.IO) {
            slideDao.getUserProfilesBySyncStatus(status)
        }
    }

    suspend fun insertUserActivityLog(log: UserActivityLogEntity) {
        withContext(Dispatchers.IO) {
            slideDao.insertUserActivityLog(log)
        }
    }

    suspend fun getUserActivityLogsBySyncStatus(status: String): List<UserActivityLogEntity> {
        return withContext(Dispatchers.IO) {
            slideDao.getUserActivityLogsBySyncStatus(status)
        }
    }

    suspend fun getPendingUserActivityLogs(): List<UserActivityLogEntity> {
        return withContext(Dispatchers.IO) {
            slideDao.getPendingUserActivityLogs()
        }
    }

    private data class PresetData(
        val id: Int,
        val title: String,
        val category: String,
        val description: String,
        val formula: String,
        val resultUnit: String,
        val variables: List<PresetVar>
    )

    private data class PresetVar(
        val symbol: String,
        val name: String,
        val value: Double,
        val unit: String
    )
}
