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
            slideDao.resetDatabase(emptyList(), emptyList())
        }
    }

    suspend fun checkAndPrepopulate() {
        // Do NOT include any preloaded/default formulas in the application.
        // On first installation, the formula database must be completely empty.
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
}
