import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<VideoItem> videoItems;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public VideoAdapter(Context context, List<VideoItem> videoItems, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.videoItems = videoItems;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item_layout, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem videoItem = videoItems.get(position);
        Glide.with(context).load(videoItem.getThumbnailUrl()).into(holder.thumbnail);
        holder.uploaderInfo.setText(videoItem.getUploaderName());
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(videoItem));
    }

    @Override
    public int getItemCount() {
        return videoItems.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView uploaderInfo;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            uploaderInfo = itemView.findViewById(R.id.uploader_info);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(VideoItem videoItem);
    }
}