package xyz.btcland.libretube

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.math.BigDecimal
import java.math.RoundingMode

class TrendingAdapter(private val trendingFeed: List<Trending>): RecyclerView.Adapter<CustomViewHolder>() {
    override fun getItemCount(): Int {
        return trendingFeed.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.trending_row,parent,false)
        return CustomViewHolder(cell)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val trending = trendingFeed[position]
        holder.v.findViewById<TextView>(R.id.textView_title).text = trending.title
        holder.v.findViewById<TextView>(R.id.textView_channel).text = trending.uploaderName +" • "+ videoViews(trending.views)+" • "+trending.uploadedDate
        val thumbnailImage = holder.v.findViewById<ImageView>(R.id.thumbnail)
        val channelImage = holder.v.findViewById<ImageView>(R.id.channel_image)
        Picasso.get().load(trending.thumbnail).into(thumbnailImage)
        Picasso.get().load(trending.uploaderAvatar).into(channelImage)
    }
}
class CustomViewHolder(val v: View): RecyclerView.ViewHolder(v){

}
fun videoViews(views: Int): String{
    when {
        views<1000 -> {
            return views.toString()
        }
        views in 1000..999999 -> {
            val decimal = BigDecimal(views/1000).setScale(0, RoundingMode.HALF_EVEN)
            return decimal.toString()+"K"
        }
        views in 1000000..10000000 -> {
            val decimal = BigDecimal(views/1000000).setScale(0, RoundingMode.HALF_EVEN)
            return decimal.toString()+"M"
        }
        else -> {
            val decimal = BigDecimal(views/1000000).setScale(0, RoundingMode.HALF_EVEN)
            return decimal.toString()+"M"
        }
    }
}
