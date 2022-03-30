package com.github.libretube.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.RetrofitInstance
import com.github.libretube.activities.MainActivity
import com.github.libretube.activities.hideKeyboard
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.formatShort
import com.github.libretube.obj.PipedStream
import com.github.libretube.obj.Streams
import com.github.libretube.obj.Subscribe
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.MediaItem.fromUri
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.URLEncoder
import kotlin.math.abs

class PlayerFragment : Fragment() {

    private var isFullScreen = false
    private val logTag = "PlayerFragment"
    private var videoId: String? = null
    private var sId: Int = 0
    private var eId: Int = 0
    private var paused = false
    private var videoType = "MPEG_4"
    private var audioType = "M4A"

    var subtitles = mutableListOf<SubtitleConfiguration>()
    var qualityOptions: Array<String> = arrayOf("Auto")
    var videoStreams: HashMap<String, PipedStream> = hashMapOf()
    var audioStreams: HashMap<String, PipedStream> = hashMapOf()

    var isSubscribed: Boolean = false

    private lateinit var relatedRecView: RecyclerView
    private lateinit var exoPlayerView: StyledPlayerView
    private lateinit var motionLayout: MotionLayout
    private lateinit var exoPlayer: ExoPlayer
    private var mediaSource: MediaSource? = null

    private lateinit var relDownloadVideo: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoId = it.getString("videoId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideKeyboard()
        lifecycleScope.launch {
            val response = initializeData()

            initializeExoPlayer(mediaSource)
            initializePlayerElements(response)

            exoPlayer.play()
        }
        val mainActivity = activity as MainActivity
        val playerMotionLayout = view.findViewById<MotionLayout>(R.id.playerMotionLayout)
        val playImageView = view.findViewById<ImageView>(R.id.play_imageView)

        motionLayout = playerMotionLayout
        exoPlayerView = view.findViewById(R.id.player)
        relDownloadVideo = view.findViewById(R.id.relPlayer_download)
        videoId = videoId!!.replace("/watch?v=", "")

        playerMotionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {}

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
                val mainMotionLayout = mainActivity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                mainMotionLayout.progress = abs(progress)
                eId = endId
                sId = startId
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                println(currentId)
                val mainMotionLayout = mainActivity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                if (currentId == eId) {
                    view.findViewById<ImageButton>(R.id.quality_select).visibility = View.GONE
                    view.findViewById<ImageButton>(R.id.close_imageButton).visibility = View.GONE
                    view.findViewById<TextView>(R.id.quality_text).visibility = View.GONE
                    mainMotionLayout.progress = 1.toFloat()
                } else if (currentId == sId) {
                    view.findViewById<ImageButton>(R.id.quality_select).visibility = View.VISIBLE
                    view.findViewById<ImageButton>(R.id.close_imageButton).visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.quality_text).visibility = View.VISIBLE
                    mainMotionLayout.progress = 0.toFloat()
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {}
        })
        playerMotionLayout.progress = 1.toFloat()
        playerMotionLayout.transitionToStart()

        view.findViewById<TextView>(R.id.player_description).text = videoId
        view.findViewById<ImageView>(R.id.close_imageView).setOnClickListener {
            motionLayout.transitionToEnd()
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
        view.findViewById<ImageButton>(R.id.close_imageButton).setOnClickListener {
            motionLayout.transitionToEnd()
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
        playImageView.setOnClickListener {
            paused = if (paused) {
                playImageView.setImageResource(R.drawable.ic_pause)
                exoPlayer.play()
                false
            } else {
                playImageView.setImageResource(R.drawable.ic_play)
                exoPlayer.pause()
                true
            }
        }
        view.findViewById<ImageButton>(R.id.fullscreen).setOnClickListener {
            // remember to hide everything when new thing added
            val mainContainer = view.findViewById<ConstraintLayout>(R.id.main_container)
            val linLayout = view.findViewById<LinearLayout>(R.id.linLayout)

            if (!isFullScreen) {
                with(motionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, -1)
                    enableTransition(R.id.yt_transition, false)
                }
                mainContainer.isClickable = true
                linLayout.visibility = View.GONE

                mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                isFullScreen = true
            } else {
                with(motionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                    enableTransition(R.id.yt_transition, true)
                }
                mainContainer.isClickable = false
                linLayout.visibility = View.VISIBLE

                mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                isFullScreen = false
            }
        }
        view.findViewById<RecyclerView>(R.id.player_recView).layoutManager = GridLayoutManager(
            view.context,
            resources.getInteger(
                R.integer.grid_items
            )
        )

        relatedRecView = view.findViewById(R.id.player_recView)

        mainActivity.findViewById<FrameLayout>(R.id.container).visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            exoPlayer.stop()
        } catch (e: Exception) {}
    }

    /*
    * Fetches data and processes the result into the UI
    */
    private suspend fun initializeData() = lifecycleScope.async {
        var response = fetchStreams()

        response?.videoStreams?.forEach {
            videoStreams += Pair(it.quality + it.format, it)

            if (!qualityOptions.contains(it.quality!!)) {
                qualityOptions += it.quality!!
                return@forEach
            }

            if (mediaSource == null) {
                val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
                val videoItem: MediaItem = MediaItem.Builder()
                    .setUri(it.url)
                    .setSubtitleConfigurations(subtitles)
                    .build()
                val videoSource: MediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(videoItem)
                var audioSource: MediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(fromUri(response?.audioStreams!![0].url!!))

                mediaSource = MergingMediaSource(videoSource, audioSource)
            }
        }

        response?.audioStreams?.forEach {
            audioStreams += Pair(it.quality + it.format, it)
        }

        response?.subtitles?.forEach {
            var mimeType = it.mimeType ?: return@forEach
            var uri = it.url?.toUri() ?: return@forEach

            subtitles.add(
                SubtitleConfiguration.Builder(uri)
                    .setMimeType(mimeType) // The correct MIME type.
                    .setLanguage(it.code) // The subtitle language.
                    .build()
            )
        }
        return@async response
    }.await()

    private fun initializePlayerElements(response: Streams?) {
        view?.findViewById<TextView>(R.id.quality_text)?.text = "hls"
        view?.findViewById<TextView>(R.id.title_textView)?.text = response?.title
        view?.findViewById<ImageButton>(R.id.quality_select)?.setOnClickListener {
            // Dialog for quality selection
            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)
            }
            builder!!.setTitle(R.string.choose_quality_dialog)
                .setItems(
                    qualityOptions
                ) { _, which ->
                    val currentPos = exoPlayer.currentPosition
                    if (which == 0) {
                        val mediaItem: MediaItem = MediaItem.Builder()
                            .setUri(response?.hls)
                            .setSubtitleConfigurations(subtitles)
                            .build()
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.seekTo(currentPos)
                    } else {
                        val dataSourceFactory: DataSource.Factory =
                            DefaultHttpDataSource.Factory()
                        val videoItem: MediaItem = MediaItem.Builder()
                            .setUri(videoStreams[qualityOptions[which] + videoType]!!.url)
                            .setSubtitleConfigurations(subtitles)
                            .build()
                        val videoSource: MediaSource =
                            DefaultMediaSourceFactory(dataSourceFactory)
                                .createMediaSource(videoItem)

                        val audioItem: MediaItem = MediaItem.Builder()
                            .setUri(response?.audioStreams?.get(getMostBitRate(response.audioStreams))?.url!!)
                            .build()
                        var audioSource: MediaSource =
                            DefaultMediaSourceFactory(dataSourceFactory)
                                .createMediaSource(audioItem)
                        val mergeSource: MediaSource =
                            MergingMediaSource(videoSource, audioSource)
                        exoPlayer.setMediaSource(mergeSource)
                        exoPlayer.seekTo(currentPos)
                    }
                    view?.findViewById<TextView>(R.id.quality_text)?.text =
                        qualityOptions[which]
                }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
        view?.findViewById<TextView>(R.id.player_description)?.text =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(response?.description, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(response?.description)
            }
        view?.findViewById<TextView>(R.id.player_sub)?.text =
            "${response?.views.formatShort()} views • ${response?.uploadDate}"
        view?.findViewById<TextView>(R.id.textLike)?.text = response?.likes.formatShort()

        relatedRecView.adapter = TrendingAdapter(response?.relatedStreams!!)
        val channelImage = view?.findViewById<ImageView>(R.id.player_channelImage)

        Picasso.get().load(response.uploaderAvatar).into(channelImage)
        view?.findViewById<TextView>(R.id.player_channelName)?.text = response.uploader
        view?.findViewById<RelativeLayout>(R.id.player_channel)?.setOnClickListener {

            val activity = view?.context as MainActivity
            val bundle = bundleOf("channel_id" to response.uploaderUrl)
            activity.navController.navigate(R.id.channel, bundle)
            activity.findViewById<MotionLayout>(R.id.mainMotionLayout).transitionToEnd()
            view?.findViewById<MotionLayout>(R.id.playerMotionLayout)?.transitionToEnd()
        }
        val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)

        if (sharedPref?.getString("token", "") != "") {
            val channelId = response.uploaderUrl?.replace("/channel/", "")
            val subButton = view?.findViewById<MaterialButton>(R.id.player_subscribe)
            isSubscribed(subButton!!, channelId!!)
        }
        // share button
        view?.findViewById<RelativeLayout>(R.id.relPlayer_share)?.setOnClickListener {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            var url = "https://piped.kavin.rocks/watch?v=$videoId"
            val instance = sharedPreferences.getString("instance", "https://pipedapi.kavin.rocks")!!
            if (instance != "https://pipedapi.kavin.rocks")
                url += "&instance=${URLEncoder.encode(instance, "UTF-8")}"
            intent.putExtra(Intent.EXTRA_TEXT, url)
            intent.type = "text/plain"
            startActivity(Intent.createChooser(intent, "Share Url To:"))
        }
    }

    private fun initializeExoPlayer(mediaSource: MediaSource?) {
        exoPlayer = ExoPlayer.Builder(requireView().context)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
        exoPlayerView.setShowSubtitleButton(true)
        exoPlayerView.setShowNextButton(false)
        exoPlayerView.setShowPreviousButton(false)
        exoPlayerView.controllerHideOnTouch = true
        exoPlayerView.player = exoPlayer

        mediaSource?.let { exoPlayer.setMediaSource(it) }

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

                exoPlayerView.keepScreenOn = !(
                    playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED ||
                        !playWhenReady
                    )

                if (playWhenReady && playbackState == Player.STATE_READY) {
                    view?.findViewById<ImageView>(R.id.play_imageView)?.setImageResource(R.drawable.ic_pause)
                } else if (playWhenReady) {
                    view?.findViewById<ImageView>(R.id.play_imageView)?.setImageResource(R.drawable.ic_play)
                } else {
                    view?.findViewById<ImageView>(R.id.play_imageView)?.setImageResource(R.drawable.ic_play)
                }
            }
        })

        exoPlayer.prepare()
    }

    private suspend fun fetchStreams(): Streams? {
        return try {
            RetrofitInstance.api.getStreams(videoId!!)
        } catch (e: IOException) {
            println(e)
            Log.e(logTag, "IOException, you might not have internet connection")
            null
        } catch (e: HttpException) {
            Log.e(logTag, "HttpException, unexpected response")
            null
        }
    }
    private fun isSubscribed(button: MaterialButton, channel_id: String) {
        @SuppressLint("ResourceAsColor")
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.isSubscribed(channel_id, sharedPref?.getString("token", "")!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(logTag, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(logTag, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                val colorPrimary = TypedValue()
                (context as Activity).theme.resolveAttribute(
                    android.R.attr.colorPrimary,
                    colorPrimary,
                    true
                )

                val ColorText = TypedValue()
                (context as Activity).theme.resolveAttribute(
                    R.attr.colorOnSurface,
                    ColorText,
                    true
                )

                runOnUiThread {
                    if (response.subscribed == true) {
                        isSubscribed = true
                        button.text = getString(R.string.unsubscribe)
                        button.setTextColor(ColorText.data)
                    }
                    if (response.subscribed != null) {
                        button.setOnClickListener {
                            if (isSubscribed) {
                                unsubscribe(channel_id)
                                button.text = getString(R.string.subscribe)
                                button.setTextColor(colorPrimary.data)
                            } else {
                                subscribe(channel_id)
                                button.text = getString(R.string.unsubscribe)
                                button.setTextColor(colorPrimary.data)
                            }
                        }
                    }
                }
            }
        }
        run()
    }

    private fun subscribe(channel_id: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.subscribe(sharedPref?.getString("token", "")!!, Subscribe(channel_id))
                } catch (e: IOException) {
                    println(e)
                    Log.e(logTag, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(logTag, "HttpException, unexpected response$e")
                    return@launchWhenCreated
                }
                isSubscribed = true
            }
        }
        run()
    }
    private fun unsubscribe(channel_id: String) {
        lifecycleScope.launchWhenCreated {
            try {
                val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                RetrofitInstance.api.unsubscribe(sharedPref?.getString("token", "")!!, Subscribe(channel_id))
            } catch (e: IOException) {
                println(e)
                Log.e(logTag, "IOException, you might not have internet connection")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(logTag, "HttpException, unexpected response")
                return@launchWhenCreated
            }
            isSubscribed = false
        }
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    private fun getMostBitRate(audios: List<PipedStream>): Int {
        var bitrate = 0
        var index = 0
        for ((i, audio) in audios.withIndex()) {
            val q = audio.quality!!.replace(" kbps", "").toInt()
            if (q> bitrate) {
                bitrate = q
                index = i
            }
        }
        return index
    }
}
