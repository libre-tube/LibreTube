import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "video_history")
public class VideoHistoryEntity {

    @PrimaryKey
    public String videoId;
    public String title;
    public String uploaderId;
    public String uploaderName;
    public int duration;
    public String thumbnailUrl;
    public String[] categories;
    public long watchedAt;
    public int watchCount;
    public int ratingPositive;
    public int ratingNegative;

    // Constructor, getters and setters can be added here
}