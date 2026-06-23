package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SlideDao {
    @Transaction
    @Query("SELECT * FROM slides ORDER BY id ASC")
    fun getAllSlides(): Flow<List<SlideWithVariables>>

    @Transaction
    @Query("SELECT * FROM slides WHERE id = :id")
    fun getSlideById(id: Int): Flow<SlideWithVariables?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlide(slide: Slide)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlides(slides: List<Slide>)

    @Update
    suspend fun updateSlide(slide: Slide)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariable(variable: Variable): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariables(variables: List<Variable>)

    @Update
    suspend fun updateVariable(variable: Variable)

    @Delete
    suspend fun deleteVariable(variable: Variable)

    @Query("DELETE FROM variables WHERE slideId = :slideId")
    suspend fun deleteVariablesForSlide(slideId: Int)

    @Transaction
    suspend fun updateSlideAndVariables(slide: Slide, variables: List<Variable>) {
        insertSlide(slide)
        deleteVariablesForSlide(slide.id)
        insertVariables(variables.map { it.copy(id = 0L) }) // Reset autogenerate primary keys to fresh insert
    }

    @Transaction
    suspend fun resetDatabase(slides: List<Slide>, variables: List<Variable>) {
        // Clear existing details
        queryDeleteAllVariables()
        queryDeleteAllSlides()
        
        // Re-insert
        insertSlides(slides)
        insertVariables(variables)
    }

    @Query("DELETE FROM variables")
    suspend fun queryDeleteAllVariables()

    @Query("DELETE FROM slides")
    suspend fun queryDeleteAllSlides()

    @Query("SELECT COUNT(*) FROM slides")
    suspend fun getSlidesCount(): Int

    // Activity Log queries
    @Query("SELECT * FROM activity_logs ORDER BY id DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivityLogs()

    @Query("DELETE FROM slides WHERE id = :slideId")
    suspend fun deleteSlideById(slideId: Int)

    @Transaction
    suspend fun deleteSlideAndVariables(slideId: Int) {
        deleteSlideById(slideId)
        deleteVariablesForSlide(slideId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getUserProfile(uid: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE syncStatus = :status")
    suspend fun getUserProfilesBySyncStatus(status: String): List<UserProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserActivityLog(log: UserActivityLogEntity)

    @Query("SELECT * FROM user_activity_logs WHERE syncStatus = :status")
    suspend fun getUserActivityLogsBySyncStatus(status: String): List<UserActivityLogEntity>

    @Query("SELECT * FROM user_activity_logs WHERE syncStatus != 'synced'")
    suspend fun getPendingUserActivityLogs(): List<UserActivityLogEntity>
}
