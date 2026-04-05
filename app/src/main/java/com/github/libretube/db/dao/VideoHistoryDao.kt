package com.github.libretube.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface VideoHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(VideoHistory videoHistory);

    @Update
    void update(VideoHistory videoHistory);

    @Delete
    void delete(VideoHistory videoHistory);

    @Query("SELECT * FROM VideoHistory ORDER BY timestamp DESC LIMIT 10")
    List<VideoHistory> getRecent();

    @Query("SELECT * FROM VideoHistory")
    List<VideoHistory> getAll();

    @Query("SELECT * FROM VideoHistory WHERE videoId = :videoId")
    VideoHistory getByVideoId(String videoId);

    @Query("DELETE FROM VideoHistory WHERE timestamp < :timestamp")
    void deleteOlderThan(long timestamp);

    @Query("DELETE FROM VideoHistory")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM VideoHistory")
    int count();
}