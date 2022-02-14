package com.github.libretube

import android.R.attr
import android.R.attr.*
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.source.MediaSource

import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView

import okhttp3.*
import java.io.IOException
import kotlin.math.abs
import com.google.android.exoplayer2.util.MimeTypes
import com.google.common.collect.ImmutableList
import android.app.ActionBar
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.widget.*
import androidx.core.net.toUri
import com.google.android.exoplayer2.C.SELECTION_FLAG_DEFAULT
import com.google.android.exoplayer2.MediaItem.fromUri
import com.google.android.exoplayer2.Player.REPEAT_MODE_ONE
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource

import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import android.widget.PopupWindow
import android.widget.TextView

import android.graphics.drawable.Drawable
import com.google.android.exoplayer2.util.Util
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.Html
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.text.PrecomputedTextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager

import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.util.RepeatModeUtil

import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.ui.TimeBar.OnScrubListener
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.obj.PipedStream
import com.github.libretube.obj.Subscribe
import com.google.android.material.button.MaterialButton


var isFullScreen = false
class PlayerFragment : Fragment() {

    private val TAG = "PlayerFragment"
    private var videoId: String? = null
    private var param2: String? = null
    private var lastProgress: Float = 0.toFloat()
    private var sId: Int=0
    private var eId: Int=0
    private var paused =false
    private var whichQuality = 0

    var isSubscribed: Boolean =false

    private lateinit var relatedRecView: RecyclerView
    private lateinit var exoPlayerView: StyledPlayerView
    private lateinit var motionLayout: MotionLayout
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSource: MediaSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoId = it.getString("videoId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = activity as MainActivity
        mainActivity.findViewById<FrameLayout>(R.id.container).visibility=View.VISIBLE
        val playerMotionLayout = view.findViewById<MotionLayout>(R.id.playerMotionLayout)
        motionLayout = playerMotionLayout
        exoPlayerView = view.findViewById(R.id.player)
        view.findViewById<TextView>(R.id.player_description).text = videoId
        playerMotionLayout.addTransitionListener(object: MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {

            }

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
                val mainActivity = activity as MainActivity
                val mainMotionLayout = mainActivity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                mainMotionLayout.progress = abs(progress)
                eId=endId
                sId=startId

            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                println(currentId)
                val mainActivity = activity as MainActivity
                val mainMotionLayout = mainActivity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                if (currentId==eId) {
                    view.findViewById<ImageButton>(R.id.quality_select).visibility =View.GONE
                    view.findViewById<ImageButton>(R.id.close_imageButton).visibility =View.GONE
                    view.findViewById<TextView>(R.id.quality_text).visibility =View.GONE
                    mainMotionLayout.progress = 1.toFloat()
                }else if(currentId==sId){
                    view.findViewById<ImageButton>(R.id.quality_select).visibility =View.VISIBLE
                    view.findViewById<ImageButton>(R.id.close_imageButton).visibility =View.VISIBLE
                    view.findViewById<TextView>(R.id.quality_text).visibility =View.VISIBLE
                    mainMotionLayout.progress = 0.toFloat()
                }

            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {


            }
        })
        playerMotionLayout.progress=1.toFloat()
        playerMotionLayout.transitionToStart()
        fetchJson(view)
        view.findViewById<ImageView>(R.id.close_imageView).setOnClickListener{
            motionLayout.transitionToEnd()
            val mainActivity = activity as MainActivity
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()

        }
        view.findViewById<ImageButton>(R.id.close_imageButton).setOnClickListener{
            motionLayout.transitionToEnd()
            val mainActivity = activity as MainActivity
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
        val playImageView = view.findViewById<ImageView>(R.id.play_imageView)
        playImageView.setOnClickListener{
            paused = if(paused){
                playImageView.setImageResource(R.drawable.ic_pause)
                exoPlayer.play()
                false
            }else {
                playImageView.setImageResource(R.drawable.ic_play)
                exoPlayer.pause()
                true
            }
        }
        //FullScreen button trigger
        view.findViewById<ImageButton>(R.id.fullscreen).setOnClickListener{
            //remember to hide everything when new shit added
            if (!isFullScreen){
                with(motionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, -1)
                    enableTransition(R.id.yt_transition,false)
                }
                view.findViewById<ConstraintLayout>(R.id.main_container).isClickable =true
                view.findViewById<LinearLayout>(R.id.linLayout).visibility=View.GONE
                val mainActivity = activity as MainActivity
                mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                isFullScreen=true

            }else{
                with(motionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                    enableTransition(R.id.yt_transition,true)
                }
                view.findViewById<ConstraintLayout>(R.id.main_container).isClickable =false
                view.findViewById<LinearLayout>(R.id.linLayout).visibility=View.VISIBLE
                val mainActivity = activity as MainActivity
                mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                isFullScreen=false
            }

        }
        relatedRecView = view.findViewById(R.id.player_recView)
        relatedRecView.layoutManager = GridLayoutManager(view.context, resources.getInteger(R.integer.grid_items))


    }


    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            exoPlayer.stop()
        }catch (e: Exception){}
    }


    private fun fetchJson(view: View) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getStreams(videoId!!)
                } catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                var videosNameArray: Array<CharSequence> = arrayOf()
                videosNameArray += "HLS"
                for (vid in response.videoStreams!!){
                    val name = vid.quality +" "+ vid.format
                    videosNameArray += name
                }
                runOnUiThread {
                    var subtitle = mutableListOf<SubtitleConfiguration>()
                    if(response.subtitles!!.isNotEmpty()){
                        subtitle?.add(SubtitleConfiguration.Builder(response.subtitles!![0].url!!.toUri())
                            .setMimeType(response.subtitles!![0].mimeType!!) // The correct MIME type (required).
                            .setLanguage(response.subtitles!![0].code) // The subtitle language (optional).
                            .build())}
                    val mediaItem: MediaItem = MediaItem.Builder()
                        .setUri(response.hls)
                        .setSubtitleConfigurations(subtitle)
                        .build()
                    exoPlayer = ExoPlayer.Builder(view.context)
                        .setSeekBackIncrementMs(5000)
                        .setSeekForwardIncrementMs(5000)
                        .build()
                    exoPlayerView.setShowSubtitleButton(true)
                    exoPlayerView.setShowNextButton(false)
                    exoPlayerView.setShowPreviousButton(false)
                    //exoPlayerView.controllerShowTimeoutMs = 1500
                    exoPlayerView.controllerHideOnTouch = true
                    exoPlayerView.player = exoPlayer
                    exoPlayer.setMediaItem(mediaItem)
                    ///exoPlayer.getMediaItemAt(5)
                    exoPlayer.prepare()
                    exoPlayer.play()

                    view.findViewById<TextView>(R.id.title_textView).text = response.title

                    view.findViewById<ImageButton>(R.id.quality_select).setOnClickListener{
                        //Dialog for quality selection
                        val builder: AlertDialog.Builder? = activity?.let {
                            AlertDialog.Builder(it)
                        }
                        builder!!.setTitle(R.string.choose_quality_dialog)
                            .setItems(videosNameArray,
                                DialogInterface.OnClickListener { _, which ->
                                    whichQuality = which
                                    if(response.subtitles!!.isNotEmpty()) {
                                        var subtitle =
                                            mutableListOf<SubtitleConfiguration>()
                                        subtitle?.add(
                                            SubtitleConfiguration.Builder(response.subtitles!![0].url!!.toUri())
                                                .setMimeType(response.subtitles!![0].mimeType!!) // The correct MIME type (required).
                                                .setLanguage(response.subtitles!![0].code) // The subtitle language (optional).
                                                .build()
                                        )
                                    }
                                    if(which==0){
                                        val mediaItem: MediaItem = MediaItem.Builder()
                                            .setUri(response.hls)
                                            .setSubtitleConfigurations(subtitle)
                                            .build()
                                        exoPlayer.setMediaItem(mediaItem)
                                    }else{
                                        val dataSourceFactory: DataSource.Factory =
                                            DefaultHttpDataSource.Factory()
                                        val videoItem: MediaItem = MediaItem.Builder()
                                            .setUri(response.videoStreams[which-1].url)
                                            .setSubtitleConfigurations(subtitle)
                                            .build()
                                        val videoSource: MediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                                            .createMediaSource(videoItem)
                                        var audioSource: MediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                                            .createMediaSource(fromUri(response.audioStreams!![0].url!!))
                                        if (response.videoStreams[which-1].quality=="720p" || response.videoStreams[which-1].quality=="1080p" || response.videoStreams[which-1].quality=="480p" ){
                                            audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                                .createMediaSource(fromUri(response.audioStreams!![getMostBitRate(response.audioStreams)].url!!))
                                        }
                                        val mergeSource: MediaSource = MergingMediaSource(videoSource,audioSource)
                                        exoPlayer.setMediaSource(mergeSource)
                                    }
                                    view.findViewById<TextView>(R.id.quality_text).text=videosNameArray[which]
                                })
                        val dialog: AlertDialog? = builder?.create()
                        dialog?.show()
                    }
                    //Listener for play and pause icon change
                    exoPlayer!!.addListener(object : com.google.android.exoplayer2.Player.Listener {
                        override fun onPlayerStateChanged(playWhenReady: Boolean,playbackState: Int) {

                            exoPlayerView.keepScreenOn = !(playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED ||
                                    !playWhenReady)

                            if (playWhenReady && playbackState == Player.STATE_READY) {
                                // media actually playing
                                view.findViewById<ImageView>(R.id.play_imageView).setImageResource(R.drawable.ic_pause)
                            } else if (playWhenReady) {
                                // might be idle (plays after prepare()),
                                // buffering (plays when data available)
                                // or ended (plays when seek away from end)
                                view.findViewById<ImageView>(R.id.play_imageView).setImageResource(R.drawable.ic_play)
                            } else {
                                // player paused in any state
                                view.findViewById<ImageView>(R.id.play_imageView).setImageResource(R.drawable.ic_play)
                            }
                        }
                    })
                    relatedRecView.adapter = TrendingAdapter(response.relatedStreams!!)
                    view.findViewById<TextView>(R.id.player_description).text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(response.description, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        Html.fromHtml(response.description)
                    }
                    view.findViewById<TextView>(R.id.player_sub).text = response.views.videoViews() + " views • "+response.uploadDate
                    view.findViewById<TextView>(R.id.textLike).text = response.likes.videoViews()
                    val channelImage = view.findViewById<ImageView>(R.id.player_channelImage)
                    Picasso.get().load(response.uploaderAvatar).into(channelImage)
                    view.findViewById<TextView>(R.id.player_channelName).text=response.uploader
                    view.findViewById<RelativeLayout>(R.id.player_channel).setOnClickListener {

                        val activity = view.context as MainActivity
                        val bundle = bundleOf("channel_id" to response.uploaderUrl)
                        activity.navController.navigate(R.id.channel,bundle)
                        activity.findViewById<MotionLayout>(R.id.mainMotionLayout).transitionToEnd()
                        view.findViewById<MotionLayout>(R.id.playerMotionLayout).transitionToEnd()
                    }
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    if(sharedPref?.getString("token","")!=""){
                        val channelId = response.uploaderUrl?.replace("/channel/","")
                        val subButton = view.findViewById<MaterialButton>(R.id.player_subscribe)
                        isSubscribed(subButton, channelId!!)
                    }
                }
            }

        }
        run()

    }

    private fun isSubscribed(button: MaterialButton, channel_id: String){
        @SuppressLint("ResourceAsColor")
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.isSubscribed(channel_id,sharedPref?.getString("token","")!!)
                }catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                runOnUiThread {
                    if (response.subscribed==true){
                        isSubscribed=true
                        button.text=getString(R.string.unsubscribe)
                        button.setTextColor(R.attr.colorPrimaryDark)
                    }
                    if(response.subscribed!=null){
                    button.setOnClickListener {
                        if(isSubscribed){
                            unsubscribe(channel_id)
                            button.text=getString(R.string.subscribe)
                            button.setTextColor(resources.getColor(R.color.md_theme_light_primary))

                        }else{
                            subscribe(channel_id)
                            button.text=getString(R.string.unsubscribe)
                            button.setTextColor(R.attr.colorPrimaryDark)
                        }
                    }}
                }
            }
        }
        run()
    }

    private fun subscribe(channel_id: String){
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.subscribe(sharedPref?.getString("token","")!!, Subscribe(channel_id))
                }catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response$e")
                    return@launchWhenCreated
                }
                isSubscribed=true
            }
        }
        run()
    }
    private fun unsubscribe(channel_id: String){
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.unsubscribe(sharedPref?.getString("token","")!!, Subscribe(channel_id))
                }catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                isSubscribed=false
            }
        }
        run()
    }


    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

     private fun getMostBitRate(audios: List<PipedStream>):Int{
        var bitrate =0
        var index = 0
        for ((i, audio) in audios.withIndex()){
            val q = audio.quality!!.replace(" kbps","").toInt()
            if (q>bitrate){
                bitrate=q
                index = i
            }
        }
        return index
    }

    override fun onResume() {
        super.onResume()
    }

}