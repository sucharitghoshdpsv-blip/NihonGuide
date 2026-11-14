package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "offline_map_regions")
data class OfflineMapRegionEntity(
    @PrimaryKey val regionId: String,
    val name: String,
    val japaneseName: String,
    val description: String,
    val downloadedAtMillis: Long,
    val sizeMb: Double,
    val isDownloaded: Boolean
)

@Entity(tableName = "offline_documents")
data class OfflineDocumentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val docType: String,
    val encryptedPayload: String,
    val ivBase64: String,
    val saltBase64: String,
    val keyMode: String,
    val cachedAtMillis: Long
)

@Entity(tableName = "offline_itinerary")
data class OfflineItineraryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dayNumber: Long,
    val timeSlot: String,
    val title: String,
    val city: String,
    val category: String,
    val notes: String,
    val estimatedCostJpy: Long,
    val completed: Boolean
)

@Entity(tableName = "offline_expenses")
data class OfflineExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val amountJpy: Long,
    val category: String,
    val dayNumber: Long,
    val notes: String
)

@Dao
interface TabiOfflineDao {
    @Query("SELECT * FROM offline_map_regions ORDER BY name ASC")
    fun observeMapRegions(): Flow<List<OfflineMapRegionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMapRegions(regions: List<OfflineMapRegionEntity>)

    @Query("UPDATE offline_map_regions SET isDownloaded = :downloaded, downloadedAtMillis = :timestamp WHERE regionId = :regionId")
    suspend fun setRegionDownloaded(regionId: String, downloaded: Boolean, timestamp: Long)

    @Query("SELECT * FROM offline_documents WHERE userId = :userId ORDER BY cachedAtMillis DESC")
    fun observeOfflineDocuments(userId: String): Flow<List<OfflineDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfflineDocuments(docs: List<OfflineDocumentEntity>)

    @Query("DELETE FROM offline_documents WHERE id = :id")
    suspend fun deleteOfflineDocument(id: String)

    @Query("SELECT * FROM offline_itinerary WHERE userId = :userId ORDER BY dayNumber ASC, timeSlot ASC")
    fun observeOfflineItinerary(userId: String): Flow<List<OfflineItineraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfflineItinerary(items: List<OfflineItineraryEntity>)

    @Query("DELETE FROM offline_itinerary WHERE userId = :userId")
    suspend fun clearOfflineItineraryForUser(userId: String)

    @Query("SELECT * FROM offline_expenses WHERE userId = :userId")
    fun observeOfflineExpenses(userId: String): Flow<List<OfflineExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfflineExpenses(expenses: List<OfflineExpenseEntity>)

    @Query("DELETE FROM offline_expenses WHERE userId = :userId")
    suspend fun clearOfflineExpensesForUser(userId: String)
}

@Database(
    entities = [
        OfflineMapRegionEntity::class,
        OfflineDocumentEntity::class,
        OfflineItineraryEntity::class,
        OfflineExpenseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TabiDatabase : RoomDatabase() {
    abstract fun offlineDao(): TabiOfflineDao

    companion object {
        @Volatile
        private var INSTANCE: TabiDatabase? = null

        fun getInstance(context: Context): TabiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TabiDatabase::class.java,
                    "tabiguide_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
