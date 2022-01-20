package xyz.btcland.libretube.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import xyz.btcland.libretube.PlayerFragment
import xyz.btcland.libretube.R
import xyz.btcland.libretube.obj.StreamItem
import xyz.btcland.libretube.videoViews

class TrendingAdapter(private val videoFeed: List<StreamItem>): RecyclerView.Adapter<CustomViewHolder>() {
    override fun getItemCount(): Int {
        return videoFeed.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.trending_row,parent,false)
        return CustomViewHolder(cell)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val trending = videoFeed[position]
        holder.v.findViewById<TextView>(R.id.textView_title).text = trending.title
        holder.v.findViewById<TextView>(R.id.textView_channel).text = trending.uploaderName +" • "+ trending.views.videoViews()+" • "+trending.uploadedDate
        val thumbnailImage = holder.v.findViewById<ImageView>(R.id.thumbnail)
        val channelImage = holder.v.findViewById<ImageView>(R.id.channel_image)
        channelImage.setOnClickListener{
            println("channel clicked")
            //TODO
        }
        Picasso.get().load(trending.thumbnail).into(thumbnailImage)
        Picasso.get().load(trending.uploaderAvatar).into(channelImage)
        holder.v.setOnClickListener{
            var bundle = Bundle()
            bundle.putString("videoId",trending.url!!.replace("/watch?v=",""))
            var frag = PlayerFragment()
            frag.arguments = bundle
            val activity = holder.v.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction()
                .remove(PlayerFragment())
                .commit()
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.container, frag)
                .commitNow()
        }
    }
}
class CustomViewHolder(val v: View): RecyclerView.ViewHolder(v){
    init {
    }
}