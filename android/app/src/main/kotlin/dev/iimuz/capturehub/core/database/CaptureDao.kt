package dev.iimuz.capturehub.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(capture: CaptureEntity): Long

    @Query("UPDATE captures SET status = :status WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: CaptureStatus,
    )

    @Query(
        "SELECT * FROM captures WHERE status IN ('RECEIVED', 'FAILED_WRITE') " +
            "ORDER BY createdAt ASC",
    )
    suspend fun pending(): List<CaptureEntity>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun findById(id: String): CaptureEntity?

    @Query("SELECT * FROM captures ORDER BY createdAt DESC LIMIT 1")
    fun latest(): Flow<CaptureEntity?>
}
