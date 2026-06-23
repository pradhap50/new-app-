package com.example

import com.example.data.Slide
import com.example.data.Variable
import com.example.data.NetworkFormula
import com.example.data.NetworkVariable
import com.example.data.SlideWithVariables
import com.example.ui.BackupHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testBackupAndRestoreSerialization() {
    val testSlides = listOf(
      Slide(
        id = 42,
        title = "Test Paper Formula",
        category = "General",
        formula = "FLOW * DENSITY",
        resultUnit = "kg/hr",
        description = "Verifying JSON Serialization flow"
      )
    )
    val testVariables = listOf(
      Variable(
        id = 1L,
        slideId = 42,
        symbol = "FLOW",
        name = "Flow rate",
        value = 150.0,
        unit = "LPH"
      ),
      Variable(
        id = 2L,
        slideId = 42,
        symbol = "DENSITY",
        name = "Stock density",
        value = 1.05,
        unit = "kg/L"
      )
    )

    // Generate backup JSON block
    val backupJson = BackupHelper.createBackupJson(
      slides = testSlides,
      variables = testVariables,
      activeSlideId = 42,
      decimalPlaces = 3,
      isDarkMode = true
    )

    assertNotNull(backupJson)
    assertTrue(backupJson.contains("Test Paper Formula"))
    assertTrue(backupJson.contains("FLOW * DENSITY"))

    // Deserialize / Restore back
    val restoreResult = BackupHelper.restoreFromBackupJson(backupJson)
    assertNotNull(restoreResult)
    assertEquals(1, restoreResult!!.slides.size)
    assertEquals(2, restoreResult.variables.size)
    assertEquals("Test Paper Formula", restoreResult.slides[0].title)
    assertEquals(42, restoreResult.activeSlideId)
    assertEquals(3, restoreResult.decimalPlaces)
    assertTrue(restoreResult.isDarkMode == true)
  }

  @Test
  fun testCloudBlueprintSerialization() {
    val slideWithVariables = SlideWithVariables(
      slide = Slide(
        id = 10,
        title = "Cloud Sync Test Node",
        category = "Advanced",
        formula = "X + Y",
        resultUnit = "meters",
        description = "Validating network transfer layer blueprint"
      ),
      variables = listOf(
        Variable(101L, 10, "X", "First parameter", 4.2, "m"),
        Variable(102L, 10, "Y", "Second parameter", 9.8, "m")
      )
    )

    val jsonPayload = BackupHelper.createCloudBlueprintJson(listOf(slideWithVariables))
    assertNotNull(jsonPayload)
    assertTrue(jsonPayload.contains("Cloud Sync Test Node"))

    val decodedList = BackupHelper.restoreFromCloudBlueprintJson(jsonPayload)
    assertNotNull(decodedList)
    assertEquals(1, decodedList!!.size)
    val schema = decodedList[0]
    assertEquals("Cloud Sync Test Node", schema.title)
    assertEquals(2, schema.variables.size)
    assertEquals("X", schema.variables[0].symbol)
    assertEquals(4.2, schema.variables[0].value, 0.001)
  }

  @Test
  fun testFormulaSerializerAndDeserializer() {
    val tokens = listOf(
      com.example.ui.FormulaToken("variable", "X"),
      com.example.ui.FormulaToken("operator", "+"),
      com.example.ui.FormulaToken("constant", "3.14")
    )
    val serialized = com.example.ui.FormulaSerializer.serialize(tokens)
    assertNotNull(serialized)
    assertTrue(serialized.contains("\"type\":\"variable\""))
    assertTrue(serialized.contains("\"value\":\"X\""))

    val deserialized = com.example.ui.FormulaSerializer.deserialize(serialized)
    assertNotNull(deserialized)
    assertEquals(3, deserialized!!.size)
    assertEquals("variable", deserialized[0].type)
    assertEquals("X", deserialized[0].value)
    assertEquals("operator", deserialized[1].type)
    assertEquals("+", deserialized[1].value)
    assertEquals("constant", deserialized[2].type)
    assertEquals("3.14", deserialized[2].value)
  }
}
