package xyz.btcland.libretube

import android.R.attr
import android.R.attr.*
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
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException
import kotlin.math.abs
import com.google.android.exoplayer2.util.MimeTypes
import com.google.common.collect.ImmutableList
import android.app.ActionBar
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
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.PrecomputedTextCompat

import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.util.RepeatModeUtil

import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.ui.TimeBar.OnScrubListener


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PlayerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PlayerFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var videoId: String? = null
    private var param2: String? = null
    private var lastProgress: Float = 0.toFloat()
    private var sId: Int=0
    private var eId: Int=0
    private var paused =false
    private var isFullScreen = false
    private var whichQuality = 0

    private lateinit var exoPlayerView: StyledPlayerView
    private lateinit var motionLayout: SingleViewTouchableMotionLayout
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
        val playerMotionLayout = view.findViewById<SingleViewTouchableMotionLayout>(R.id.playerMotionLayout)
        motionLayout = playerMotionLayout
        exoPlayerView = view.findViewById(R.id.player)
        view.findViewById<TextView>(R.id.textTest).text = videoId
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
            val mainActivity = activity as MainActivity
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()

        }
        view.findViewById<ImageButton>(R.id.close_imageButton).setOnClickListener{
            val mainActivity = activity as MainActivity
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
        view.findViewById<ImageButton>(R.id.fullscreen).setOnClickListener{
            if (!isFullScreen){
                view.findViewById<ScrollView>(R.id.scrollView2).visibility = View.GONE
                view.findViewById<LinearLayout>(R.id.linLayout).visibility = View.GONE
                view.findViewById<TextView>(R.id.textTest).visibility = View.GONE
                view.findViewById<ConstraintLayout>(R.id.main_container).visibility = View.GONE
                with(motionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, -1)
                    enableTransition(R.xml.player_scene,false)
                }
                (activity as MainActivity)?.supportActionBar?.hide()
                isFullScreen=true

            }else{
                view.findViewById<ScrollView>(R.id.scrollView2).visibility = View.VISIBLE
                view.findViewById<LinearLayout>(R.id.linLayout).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.textTest).visibility = View.VISIBLE
                view.findViewById<ConstraintLayout>(R.id.main_container).visibility = View.VISIBLE
                with(motionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                    enableTransition(R.xml.player_scene,true)
                }
                (activity as MainActivity)?.supportActionBar?.show()
                isFullScreen=false
            }

        }

    }


    override fun onStop() {
        super.onStop()
        try {
            exoPlayer.stop()
        }catch (e: Exception){}

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PlayerFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PlayerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }


    private fun fetchJson(view: View) {
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
                            var subtitle = mutableListOf<SubtitleConfiguration>()
                            if(videoInPlayer.subtitles.isNotEmpty()){
                            subtitle?.add(SubtitleConfiguration.Builder(videoInPlayer.subtitles[0].url.toUri())
                                .setMimeType(videoInPlayer.subtitles[0].mimeType) // The correct MIME type (required).
                                .setLanguage(videoInPlayer.subtitles[0].code) // The subtitle language (optional).
                                .build())}
                            val mediaItem: MediaItem = MediaItem.Builder()
                                .setUri(videoInPlayer.hls)
                                .setSubtitleConfigurations(subtitle)
                                .build()
                            exoPlayer = ExoPlayer.Builder(view.context)
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

                            view.findViewById<TextView>(R.id.title_textView).text = videoInPlayer.title

                            view.findViewById<ImageButton>(R.id.quality_select).setOnClickListener{
                                val builder: AlertDialog.Builder? = activity?.let {
                                    AlertDialog.Builder(it)
                                }
                                builder!!.setTitle(R.string.choose_quality_dialog)
                                    .setItems(videosNameArray,
                                        DialogInterface.OnClickListener { _, which ->
                                            // The 'which' argument contains the index position
                                            // of the selected item
                                            //println(which)
                                            whichQuality = which
                                            if(videoInPlayer.subtitles.isNotEmpty()) {
                                                var subtitle =
                                                    mutableListOf<SubtitleConfiguration>()
                                                subtitle?.add(
                                                    SubtitleConfiguration.Builder(videoInPlayer.subtitles[0].url.toUri())
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
                                                    .createMediaSource(fromUri(videoInPlayer.audioStreams[0].url))
                                                if (videoInPlayer.videoStreams[which-1].quality=="720p" || videoInPlayer.videoStreams[which-1].quality=="1080p" || videoInPlayer.videoStreams[which-1].quality=="480p" ){
                                                    audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                                        .createMediaSource(fromUri(videoInPlayer.audioStreams[getMostBitRate(videoInPlayer.audioStreams)].url))
                                                    //println("fuckkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkitttttttttttttttttttttt")
                                                }
                                                val mergeSource: MediaSource = MergingMediaSource(videoSource,audioSource)
                                                exoPlayer.setMediaSource(mergeSource)
                                            }
                                            view.findViewById<TextView>(R.id.quality_text).text=videosNameArray[which]
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
    fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
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

    override fun onResume() {
        super.onResume()
        println("wtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtfwtf")
    }
}