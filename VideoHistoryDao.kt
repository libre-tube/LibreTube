import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface VideoHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(videoHistory: VideoHistoryEntity)

    @Update
    suspend fun update(videoHistory: VideoHistoryEntity)

    @Query("DELETE FROM video_history WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM video_history ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecent(): List<VideoHistoryEntity>

    @Query("SELECT * FROM video_history")
    suspend fun getAll(): List<VideoHistoryEntity>

    @Query("SELECT * FROM video_history WHERE video_id = :videoId")
    suspend fun getByVideoId(videoId: String): VideoHistoryEntity?

    @Query("DELETE FROM video_history WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM video_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM video_history")
    suspend fun count(): Int
}