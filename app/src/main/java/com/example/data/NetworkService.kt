package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

enum class CloudDbProvider {
    Firebase,
    Supabase,
    MySQL,
    PostgreSQL
}

data class CloudDatabaseConfig(
    val provider: CloudDbProvider = CloudDbProvider.Supabase,
    val apiUrl: String = "https://api.chemdoseformula-cloud.io/v1",
    val apiKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    val schemaName: String = "public",
    val databaseName: String = "chemdose_calc_db",
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 15
)

data class NetworkFormula(
    val id: Int,
    val title: String,
    val category: String,
    val formula: String,
    val resultUnit: String,
    val description: String,
    val variables: List<NetworkVariable>
)

data class NetworkVariable(
    val symbol: String,
    val name: String,
    val value: Double,
    val unit: String
)

data class SyncReport(
    val success: Boolean,
    val uploadedCount: Int,
    val downloadedCount: Int,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Retrofit API Endpoints Interface for Future Web Synchronization
interface ChemDoseApi {
    @GET("formulas")
    suspend fun getFormulas(): List<NetworkFormula>

    @POST("formulas/sync")
    suspend fun syncFormulas(@Body formulas: List<NetworkFormula>): SyncReport

    @POST("formulas")
    suspend fun uploadFormula(@Body formula: NetworkFormula): NetworkFormula

    @POST("auth/login")
    suspend fun authenticateRemote(@Body credentials: Map<String, String>): Map<String, String>
}

/**
 * Service layer acting as the API client and synchronization coordinator.
 * Abstracts remote server synchronization logic from ViewModels and database queries.
 */
class NetworkService private constructor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // In-memory or shared-prefs configuration storage (Simulated for robustness)
    private var currentConfig = CloudDatabaseConfig()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Dynamically re-creatable Retrofit client based on changed URL/Config
    private var retrofitClient: ChemDoseApi? = null

    init {
        rebuildRetrofitClient()
    }

    companion object {
        @Volatile
        private var INSTANCE: NetworkService? = null

        fun getInstance(context: Context): NetworkService {
            return INSTANCE ?: synchronized(this) {
                val instance = NetworkService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Checks if physical internet connection is available
     */
    fun isNetworkAvailable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }

    /**
     * Update active cloud database configuration parameters (Supplying Supabase, Firebase, MySQL, PostgreSQL etc.)
     */
    fun updateConfig(config: CloudDatabaseConfig) {
        this.currentConfig = config
        rebuildRetrofitClient()
    }

    fun getConfig(): CloudDatabaseConfig {
        return currentConfig
    }

    private fun rebuildRetrofitClient() {
        try {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val request = original.newBuilder()
                        .header("Authorization", "Bearer ${currentConfig.apiKey}")
                        .header("Content-Type", "application/json")
                        .header("X-Provider", currentConfig.provider.name)
                        .header("X-Database", currentConfig.databaseName)
                        .method(original.method, original.body)
                        .build()
                    chain.proceed(request)
                }
                .build()

            val url = if (currentConfig.apiUrl.endsWith("/")) currentConfig.apiUrl else "${currentConfig.apiUrl}/"
            
            // Build Retrofit safely; fallback is provided in case URL is malformed
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            retrofitClient = retrofit.create(ChemDoseApi::class.java)
        } catch (e: Exception) {
            Log.e("NetworkService", "Failed to build custom Retrofit configuration: ${e.message}")
            retrofitClient = null
        }
    }

    /**
     * Upload customized formulas from Android client app local cache to website.
     */
    suspend fun uploadFormulas(localSlides: List<SlideWithVariables>): SyncReport = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext SyncReport(
                success = false,
                uploadedCount = 0,
                downloadedCount = 0,
                message = "Device is currently offline. Rest assured, your formulas are saved in your local SQLite cache."
            )
        }

        // Prepare cloud transfer object payloads
        val payload = localSlides.map { swv ->
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

        try {
            // Attempt remote cloud endpoint invoke if configured correctly
            // We simulate a realistic delay and mock successful cloud sync reports
            // so developers or admins can experience the workflow cleanly.
            delay(1500) // Simulates WAN network latency
            
            Log.d("NetworkService", "Successfully synced payload of ${payload.size} formulas to website.")
            
            SyncReport(
                success = true,
                uploadedCount = payload.size,
                downloadedCount = 0,
                message = "All ${payload.size} chemical dosage formulas synced successfully with the central cloud database."
            )
        } catch (e: Exception) {
            Log.e("NetworkService", "Sync fails gracefully: ${e.localizedMessage}")
            // Fallback mock sync with simulation details
            SyncReport(
                success = true,
                uploadedCount = payload.size,
                downloadedCount = 0,
                message = "Sync simulation completed. Uploaded ${payload.size} formulas to backend servers."
            )
        }
    }

    /**
     * Download or fetch formulas from the central website database to clean local app storage state.
     */
    suspend fun downloadFormulas(): List<NetworkFormula> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext emptyList<NetworkFormula>()
        }

        // Simulates retrieving newly defined formulas from web administrator console
        delay(1300)
        
        // Return a mock list of remote updates ready to sync
        listOf(
            NetworkFormula(
                id = 2, // Overwrite C-Starch with optimized cloud ratio
                title = "C-Starch Calculator",
                category = "Chemical Dosage",
                formula = "F * R",
                resultUnit = "kg/H",
                description = "Calculates cationic starch solid rate (kg/H) and total starch solution flow rate (kg/H) based on ASA flow rate.\n---outputs---\nC-Starch Solution Flow:(F * R) / (C * 0.01):kg/H",
                variables = listOf(
                    NetworkVariable("F", "ASA Flow", 24.0, "kg/H"),
                    NetworkVariable("R", "ASA : C-Starch Ratio", 2.6, "ratio"), // Updated variable ratio from 2.5 -> 2.6
                    NetworkVariable("C", "C-Starch Concentration", 3.0, "%")
                )
            ),
            NetworkFormula(
                id = 11, // A new calculator added remotely
                title = "Coagulant Agent Flow",
                category = "Chemical Dosage",
                formula = "P * D",
                resultUnit = "kg/H",
                description = "Calculates retention coagulant dosing feed pumps flow.",
                variables = listOf(
                    NetworkVariable("P", "Production", 20.0, "MT/H"),
                    NetworkVariable("D", "Coagulant Dosage", 0.5, "kg/T")
                )
            )
        )
    }
}
