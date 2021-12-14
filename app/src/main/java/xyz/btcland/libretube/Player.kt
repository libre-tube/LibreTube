package xyz.btcland.libretube

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException

class Player : Activity() {

    private lateinit var exoPlayerView: StyledPlayerView
    private lateinit var motionLayout: SingleViewTouchableMotionLayout
    private lateinit var exoPlayer: ExoPlayer
    private var videoId: String? =null
    private var seekTo: Long? = 0
    private var whichQuality: Int? = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        seekTo = intent.getStringExtra("seekTo")?.toLong()
        whichQuality = intent.getStringExtra("quality")?.toInt()
        videoId=intent.getStringExtra("videoId")
        exoPlayerView = findViewById(R.id.fullscreen_player)
        fetchJson(this)
    }



    private fun fetchJson(context: Context) {
        val client = OkHttpClient()

        fun run() {
            val request = Request.Builder()
                .url("https://pipedapi.kavin.rocks/streams/$videoId")
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val body = response.body!!.string()
                        println(body)
                        val gson = GsonBuilder().create()
                        val videoInPlayer = gson.fromJson(body, VideoInPlayer::class.java)
                        var videosNameArray: Array<CharSequence> = arrayOf()
                        videosNameArray += "HLS"
                        for (vids in videoInPlayer.videoStreams){
                            val name = vids.quality +" "+ vids.format
                            videosNameArray += name
                        }
                        runOnUiThread {
                            exoPlayer = ExoPlayer.Builder(context)
                                .build()
                            var subtitle = mutableListOf<MediaItem.SubtitleConfiguration>()
                            if(videoInPlayer.subtitles.isNotEmpty()){
                                subtitle?.add(
                                    MediaItem.SubtitleConfiguration.Builder(videoInPlayer.subtitles[0].url.toUri())
                                    .setMimeType(videoInPlayer.subtitles[0].mimeType) // The correct MIME type (required).
                                    .setLanguage(videoInPlayer.subtitles[0].code) // The subtitle language (optional).
                                    .build())}
                            if(whichQuality==0){
                                val mediaItem: MediaItem = MediaItem.Builder()
                                    .setUri(videoInPlayer.hls)
                                    .setSubtitleConfigurations(subtitle)
                                    .build()
                                exoPlayer.setMediaItem(mediaItem)
                            }else{
                                val dataSourceFactory: DataSource.Factory =
                                    DefaultHttpDataSource.Factory()
                                val videoItem: MediaItem = MediaItem.Builder()
                                    .setUri(videoInPlayer.videoStreams[whichQuality!!-1].url)
                                    .setSubtitleConfigurations(subtitle)
                                    .build()
                                val videoSource: MediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                                    .createMediaSource(videoItem)
                                var audioSource: MediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                                    .createMediaSource(
                                        MediaItem.fromUri(
                                            videoInPlayer.audioStreams[0].url
                                        )
                                    )
                                if (videoInPlayer.videoStreams[whichQuality!!-1].quality=="720p" || videoInPlayer.videoStreams[whichQuality!!-1].quality=="1080p" || videoInPlayer.videoStreams[whichQuality!!-1].quality=="480p" ){
                                    audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                        .createMediaSource(
                                            MediaItem.fromUri(
                                                videoInPlayer.audioStreams[getMostBitRate(
                                                    videoInPlayer.audioStreams
                                                )].url
                                            )
                                        )
                                    //println("fuckkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkitttttttttttttttttttttt")
                                }
                                val mergeSource: MediaSource = MergingMediaSource(videoSource,audioSource)
                                exoPlayer.setMediaSource(mergeSource)
                            }
                            findViewById<TextView>(R.id.quality_text).text=videosNameArray[whichQuality!!]

                            exoPlayerView.setShowSubtitleButton(true)
                            exoPlayerView.setShowNextButton(false)
                            exoPlayerView.setShowPreviousButton(false)
                            exoPlayerView.controllerShowTimeoutMs = 1500
                            exoPlayerView.controllerHideOnTouch = true
                            exoPlayerView.player = exoPlayer

                            ///exoPlayer.getMediaItemAt(5)
                            exoPlayer.prepare()
                            exoPlayer.play()
                            exoPlayer.seekTo(seekTo!!)
                            findViewById<ImageButton>(R.id.quality_select).setOnClickListener{
                                val builder: AlertDialog.Builder? = let {
                                    AlertDialog.Builder(context)
                                }
                                builder!!.setTitle(R.string.choose_quality_dialog)
                                    .setItems(videosNameArray,
                                        DialogInterface.OnClickListener { _, which ->
                                            // The 'which' argument contains the index position
                                            // of the selected item
                                            //println(which)
                                            if(videoInPlayer.subtitles.isNotEmpty()) {
                                                var subtitle =
                                                    mutableListOf<MediaItem.SubtitleConfiguration>()
                                                subtitle?.add(
                                                    MediaItem.SubtitleConfiguration.Builder(videoInPlayer.subtitles[0].url.toUri())
                                                        .setMimeType(videoInPlayer.subtitles[0].mimeType) // The correct MIME type (required).
                                                        .setLanguage(videoInPlayer.subtitles[0].code) // The subtitle language (optional).
                                                        .build()
                                                )
                                            }
                                            if(which==0){
                                                val mediaItem: MediaItem = MediaItem.Builder()
                                                    .setUri(videoInPlayer.hls)
                                                    .setSubtitleConfigurations(subtitle)
                                                    .build()
                                                exoPlayer.setMediaItem(mediaItem)
                                            }else{
                                                val dataSourceFactory: DataSource.Factory =
                                                    DefaultHttpDataSource.Factory()
                                                val videoItem: MediaItem = MediaItem.Builder()
                                                    .setUri(videoInPlayer.videoStreams[which-1].url)
                                                    .setSubtitleConfigurations(subtitle)
                                                    .build()
                                                val videoSource: MediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                                                    .createMediaSource(videoItem)
                                                var audioSource: MediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                                                    .createMediaSource(
                                                        MediaItem.fromUri(
                                                            videoInPlayer.audioStreams[0].url
                                                        )
                                                    )
                                                if (videoInPlayer.videoStreams[which-1].quality=="720p" || videoInPlayer.videoStreams[which-1].quality=="1080p" || videoInPlayer.videoStreams[which-1].quality=="480p" ){
                                                    audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                                        .createMediaSource(
                                                            MediaItem.fromUri(
                                                                videoInPlayer.audioStreams[getMostBitRate(
                                                                    videoInPlayer.audioStreams
                                                                )].url
                                                            )
                                                        )
                                                    //println("fuckkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkitttttttttttttttttttttt")
                                                }
                                                val mergeSource: MediaSource = MergingMediaSource(videoSource,audioSource)
                                                exoPlayer.setMediaSource(mergeSource)
                                            }
                                            findViewById<TextView>(R.id.quality_text).text=videosNameArray[which]
                                        })
                                val dialog: AlertDialog? = builder?.create()
                                dialog?.show()
                            }
                        }
                    }


                }
            })
        }
        run()

    }
    fun getMostBitRate(audios: List<Stream>):Int{
        var bitrate =0
        var index = 0
        for ((i, audio) in audios.withIndex()){
            val q = audio.quality.replace(" kbps","").toInt()
            if (q>bitrate){
                bitrate=q
                index = i
            }
        }
        return index
    }

    override fun onStop() {
        super.onStop()
        myApp.seekTo=exoPlayer.currentPosition
        exoPlayer.stop()
    }
}




