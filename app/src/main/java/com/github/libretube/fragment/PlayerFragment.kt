package com.github.libretube.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.model.PipedStream
import com.github.libretube.model.Subscribe
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.MediaItem.fromUri
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso
import retrofit2.HttpException
import java.io.IOException
import java.net.URLEncoder
import kotlin.math.abs


var isFullScreen = false
private const val TAG = "PlayerFragment"

class PlayerFragment : Fragment() {

    private var videoId: String? = null
    private var param2: String? = null
    private var lastProgress: Float = 0.toFloat()
    private var sId: Int = 0
    private var eId: Int = 0
    private var paused = false
    private var whichQuality = 0

    var isSubscribed: Boolean = false

    private lateinit var relatedRecView: RecyclerView
    private lateinit var exoPlayerView: StyledPlayerView
    private lateinit var motionLayout: MotionLayout
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSource: MediaSource

    private lateinit var relDownloadVideo: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoId = it.getString("videoId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = activity as MainActivity
        val playerMotionLayout = view.findViewById<MotionLayout>(R.id.playerMotionLayout)

        hideKeyboard()
        videoId = videoId!!.replace("/watch?v=", "")
        relDownloadVideo = view.findViewById(R.id.relPlayer_download)
        mainActivity.findViewById<FrameLayout>(R.id.container).visibility = View.VISIBLE
        motionLayout = playerMotionLayout
        exoPlayerView = view.findViewById(R.id.player)
        view.findViewById<TextView>(R.id.player_description).text = videoId
        playerMotionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
            ) {
                // no op
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float,
            ) {
                val mainMotionLayout =
                    mainActivity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                mainMotionLayout.progress = abs(progress)
                eId = endId
                sId = startId

            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                println(currentId)
                val mainMotionLayout =
                    mainActivity.findViewById<MotionLayout>(R.id.mainMotionLayout)
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
                progress: Float,
            ) {
                // no op
            }
        })
        playerMotionLayout.progress = 1.toFloat()
        playerMotionLayout.transitionToStart()
        fetchJson(view)
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
        val playImageView = view.findViewById<ImageView>(R.id.play_imageView)
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
        //FullScreen button trigger
        view.findViewById<ImageButton>(R.id.fullscreen).setOnClickListener {
            //remember to hide everything when new thing added
            if (!isFullScreen) {
                with(motionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, -1)
                    enableTransition(R.id.yt_transition, false)
                }
                view.findViewById<ConstraintLayout>(R.id.main_container).isClickable = true
                view.findViewById<LinearLayout>(R.id.linLayout).visibility = View.GONE
                mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                isFullScreen = true

            } else {
                with(motionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                    enableTransition(R.id.yt_transition, true)
                }
                view.findViewById<ConstraintLayout>(R.id.main_container).isClickable = false
                view.findViewById<LinearLayout>(R.id.linLayout).visibility = View.VISIBLE
                mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                isFullScreen = false
            }

        }
        relatedRecView = view.findViewById(R.id.player_recView)
        relatedRecView.layoutManager =
            GridLayoutManager(view.context, resources.getInteger(R.integer.grid_items))
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            exoPlayer.stop()
        } catch (e: Exception) {
            // TODO: Handle exception
        }
    }

    private fun fetchJson(view: View) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getStreams(videoId!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                var videosNameArray: Array<CharSequence> = arrayOf()
                videosNameArray += "HLS"
                for (vid in response.videoStreams!!) {
                    val name = vid.quality + " " + vid.format
                    videosNameArray += name
                }
                runOnUiThread {
                    var subtitle = mutableListOf<SubtitleConfiguration>()
                    if (response.subtitles!!.isNotEmpty()) {
                        subtitle?.add(SubtitleConfiguration.Builder(response.subtitles!![0].url!!.toUri())
                            .setMimeType(response.subtitles!![0].mimeType!!) // The correct MIME type (required).
                            .setLanguage(response.subtitles!![0].code) // The subtitle language (optional).
                            .build())
                    }

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
                    val sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                    val defres = sharedPreferences.getString("default_res", "")!!
                    when {
                        defres != "" -> {
                            run lit@{
                                response.videoStreams!!.forEachIndexed { index, pipedStream ->
                                    if (pipedStream.quality!!.contains(defres)) {
                                        val dataSourceFactory: DataSource.Factory =
                                            DefaultHttpDataSource.Factory()
                                        val videoItem: MediaItem = MediaItem.Builder()
                                            .setUri(response.videoStreams[index].url)
                                            .setSubtitleConfigurations(subtitle)
                                            .build()
                                        val videoSource: MediaSource =
                                            DefaultMediaSourceFactory(dataSourceFactory)
                                                .createMediaSource(videoItem)
                                        var audioSource: MediaSource =
                                            DefaultMediaSourceFactory(dataSourceFactory)
                                                .createMediaSource(fromUri(response.audioStreams!![0].url!!))
                                        if (response.videoStreams[index].quality == "720p" || response.videoStreams[index].quality == "1080p" || response.videoStreams[index].quality == "480p") {
                                            audioSource =
                                                ProgressiveMediaSource.Factory(dataSourceFactory)
                                                    .createMediaSource(fromUri(response.audioStreams!![getMostBitRate(
                                                        response.audioStreams)].url!!))
                                        }
                                        val mergeSource: MediaSource =
                                            MergingMediaSource(videoSource, audioSource)
                                        exoPlayer.setMediaSource(mergeSource)
                                        view.findViewById<TextView>(R.id.quality_text).text =
                                            videosNameArray[index + 1]
                                        return@lit
                                    }
                                }
                            }

                        }
                        response.hls != null -> {
                            val mediaItem: MediaItem = MediaItem.Builder()
                                .setUri(response.hls)
                                .setSubtitleConfigurations(subtitle)
                                .build()
                            exoPlayer.setMediaItem(mediaItem)
                        }
                        else -> {
                            val dataSourceFactory: DataSource.Factory =
                                DefaultHttpDataSource.Factory()
                            val videoItem: MediaItem = MediaItem.Builder()
                                .setUri(response.videoStreams[0].url)
                                .setSubtitleConfigurations(subtitle)
                                .build()
                            val videoSource: MediaSource =
                                DefaultMediaSourceFactory(dataSourceFactory)
                                    .createMediaSource(videoItem)
                            var audioSource: MediaSource =
                                DefaultMediaSourceFactory(dataSourceFactory)
                                    .createMediaSource(fromUri(response.audioStreams!![0].url!!))
                            if (response.videoStreams[0].quality == "720p" || response.videoStreams[0].quality == "1080p" || response.videoStreams[0].quality == "480p") {
                                audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                    .createMediaSource(fromUri(response.audioStreams!![getMostBitRate(
                                        response.audioStreams)].url!!))
                            }
                            val mergeSource: MediaSource =
                                MergingMediaSource(videoSource, audioSource)
                            exoPlayer.setMediaSource(mergeSource)
                            view.findViewById<TextView>(R.id.quality_text).text = videosNameArray[1]
                        }
                    }

                    ///exoPlayer.getMediaItemAt(5)
                    exoPlayer.prepare()
                    exoPlayer.play()

                    view.findViewById<TextView>(R.id.title_textView).text = response.title

                    view.findViewById<ImageButton>(R.id.quality_select).setOnClickListener {
                        //Dialog for quality selection
                        val builder: AlertDialog.Builder? = activity?.let {
                            AlertDialog.Builder(it)
                        }
                        builder!!.setTitle(R.string.choose_quality_dialog)
                            .setItems(videosNameArray,
                                DialogInterface.OnClickListener { _, which ->
                                    whichQuality = which
                                    if (response.subtitles.isNotEmpty()) {
                                        val subtitle =
                                            mutableListOf<SubtitleConfiguration>()
                                        subtitle.add(
                                            SubtitleConfiguration.Builder(response.subtitles[0].url!!.toUri())
                                                .setMimeType(response.subtitles[0].mimeType!!) // The correct MIME type (required).
                                                .setLanguage(response.subtitles[0].code) // The subtitle language (optional).
                                                .build()
                                        )
                                    }
                                    if (which == 0) {
                                        val mediaItem: MediaItem = MediaItem.Builder()
                                            .setUri(response.hls)
                                            .setSubtitleConfigurations(subtitle)
                                            .build()
                                        exoPlayer.setMediaItem(mediaItem)
                                    } else {
                                        val dataSourceFactory: DataSource.Factory =
                                            DefaultHttpDataSource.Factory()
                                        val videoItem: MediaItem = MediaItem.Builder()
                                            .setUri(response.videoStreams[which - 1].url)
                                            .setSubtitleConfigurations(subtitle)
                                            .build()
                                        val videoSource: MediaSource =
                                            DefaultMediaSourceFactory(dataSourceFactory)
                                                .createMediaSource(videoItem)
                                        var audioSource: MediaSource =
                                            DefaultMediaSourceFactory(dataSourceFactory)
                                                .createMediaSource(fromUri(response.audioStreams!![0].url!!))
                                        if (response.videoStreams[which - 1].quality == "720p" || response.videoStreams[which - 1].quality == "1080p" || response.videoStreams[which - 1].quality == "480p") {
                                            audioSource =
                                                ProgressiveMediaSource.Factory(dataSourceFactory)
                                                    .createMediaSource(
                                                        fromUri(
                                                            response.audioStreams[getMostBitRate(
                                                                response.audioStreams
                                                            )].url!!
                                                        )
                                                    )
                                        }
                                        val mergeSource: MediaSource =
                                            MergingMediaSource(videoSource, audioSource)
                                        exoPlayer.setMediaSource(mergeSource)
                                    }
                                    view.findViewById<TextView>(R.id.quality_text).text =
                                        videosNameArray[which]
                                })
                        val dialog: AlertDialog = builder.create()
                        dialog.show()
                    }
                    //Listener for play and pause icon change
                    exoPlayer.addListener(object : com.google.android.exoplayer2.Player.Listener {
                        override fun onPlayerStateChanged(
                            playWhenReady: Boolean,
                            playbackState: Int,
                        ) {

                            exoPlayerView.keepScreenOn =
                                !(playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED ||
                                        !playWhenReady)

                            if (playWhenReady && playbackState == Player.STATE_READY) {
                                // media actually playing
                                view.findViewById<ImageView>(R.id.play_imageView)
                                    .setImageResource(R.drawable.ic_pause)
                            } else if (playWhenReady) {
                                // might be idle (plays after prepare()),
                                // buffering (plays when data available)
                                // or ended (plays when seek away from end)
                                view.findViewById<ImageView>(R.id.play_imageView)
                                    .setImageResource(R.drawable.ic_play)
                            } else {
                                // player paused in any state
                                view.findViewById<ImageView>(R.id.play_imageView)
                                    .setImageResource(R.drawable.ic_play)
                            }
                        }
                    })
                    relatedRecView.adapter = TrendingAdapter(response.relatedStreams!!)
                    view.findViewById<TextView>(R.id.player_description).text =
                        if (SDK_INT >= Build.VERSION_CODES.N) {
                            Html.fromHtml(response.description, Html.FROM_HTML_MODE_COMPACT)
                        } else {
                            Html.fromHtml(response.description)
                        }
                    view.findViewById<TextView>(R.id.player_sub).text =
                        response.views.formatShort() + " views • " + response.uploadDate
                    view.findViewById<TextView>(R.id.textLike).text = response.likes.formatShort()
                    val channelImage = view.findViewById<ImageView>(R.id.player_channelImage)
                    Picasso.get().load(response.uploaderAvatar).into(channelImage)
                    view.findViewById<TextView>(R.id.player_channelName).text = response.uploader
                    view.findViewById<RelativeLayout>(R.id.player_channel).setOnClickListener {

                        val activity = view.context as MainActivity
                        val bundle = bundleOf("channel_id" to response.uploaderUrl)
                        activity.navController.navigate(R.id.channel, bundle)
                        activity.findViewById<MotionLayout>(R.id.mainMotionLayout).transitionToEnd()
                        view.findViewById<MotionLayout>(R.id.playerMotionLayout).transitionToEnd()
                    }
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    if (sharedPref?.getString("token", "") != "") {
                        val channelId = response.uploaderUrl?.replace("/channel/", "")
                        val subButton = view.findViewById<MaterialButton>(R.id.player_subscribe)
                        isSubscribed(subButton, channelId!!)
                    }
                    //share button
                    view.findViewById<RelativeLayout>(R.id.relPlayer_share).setOnClickListener {
                        val sharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                        val intent = Intent()
                        intent.action = Intent.ACTION_SEND
                        var url = "https://piped.kavin.rocks/watch?v=$videoId"
                        val instance = sharedPreferences.getString("instance",
                            "https://pipedapi.kavin.rocks")!!
                        if (instance != "https://pipedapi.kavin.rocks")
                            url += "&instance=${URLEncoder.encode(instance, "UTF-8")}"
                        intent.putExtra(Intent.EXTRA_TEXT, url)
                        intent.type = "text/plain"
                        startActivity(Intent.createChooser(intent, "Share Url To:"))
                    }
                    //check if livestream
                    if (response.duration!! > 0) {
                        //download clicked
                        relDownloadVideo.setOnClickListener {
                            if (!IS_DOWNLOAD_RUNNING) {
                                val mainActivity = activity as MainActivity
                                Log.e(TAG, "download button clicked!")
                                if (SDK_INT >= Build.VERSION_CODES.R) {
                                    Log.d("myz", "" + SDK_INT)
                                    if (!Environment.isExternalStorageManager()) {
                                        ActivityCompat.requestPermissions(
                                            mainActivity, arrayOf(
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.MANAGE_EXTERNAL_STORAGE
                                            ), 1
                                        ) //permission request code is just an int
                                    }
                                } else {
                                    if (ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        ActivityCompat.requestPermissions(
                                            mainActivity,
                                            arrayOf(
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                                            ),
                                            1
                                        )
                                    }
                                }
                                val vidName = arrayListOf<String>()
                                val vidUrl = arrayListOf<String>()
                                val audioName = arrayListOf<String>()
                                val audioUrl = arrayListOf<String>()
                                val newFragment = DownloadDialogFragment()
                                val bundle = Bundle()

                                vidName.add("No video")
                                vidUrl.add("")
                                for (vid in response.videoStreams) {
                                    val name = vid.quality + " " + vid.format
                                    vidName.add(name)
                                    vidUrl.add(vid.url!!)
                                }
                                audioName.add("No audio")
                                audioUrl.add("")
                                for (audio in response.audioStreams!!) {
                                    val name = audio.quality + " " + audio.format
                                    audioName.add(name)
                                    audioUrl.add(audio.url!!)
                                }
                                bundle.putStringArrayList("videoName", vidName)
                                bundle.putStringArrayList("videoUrl", vidUrl)
                                bundle.putStringArrayList("audioName", audioName)
                                bundle.putStringArrayList("audioUrl", audioUrl)
                                bundle.putString("videoId", videoId)
                                bundle.putInt("duration", response.duration)
                                newFragment.arguments = bundle
                                newFragment.show(childFragmentManager, "Download")
                            } else {
                                Toast.makeText(context, R.string.dlisinprogress, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } else {
                        Toast.makeText(context, R.string.cannotDownload, Toast.LENGTH_SHORT).show()
                    }
                    if (response.hls != null) {
                        view.findViewById<RelativeLayout>(R.id.relPlayer_vlc).setOnClickListener {
                            exoPlayer.pause()
                            try {
                                val vlcRequestCode = 42
                                val uri: Uri = Uri.parse(response.hls)
                                val vlcIntent = Intent(Intent.ACTION_VIEW)
                                vlcIntent.setPackage("org.videolan.vlc")
                                vlcIntent.setDataAndTypeAndNormalize(uri, "video/*")
                                vlcIntent.putExtra("title", response.title)
                                vlcIntent.putExtra("from_start", false)
                                vlcIntent.putExtra("position", exoPlayer.currentPosition)
                                startActivityForResult(vlcIntent, vlcRequestCode)
                            } catch (e: Exception) {
                                Toast.makeText(context, R.string.vlcerror, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
        run()
    }

    private fun isSubscribed(button: MaterialButton, channelId: String) {
        @SuppressLint("ResourceAsColor")
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.isSubscribed(
                        channelId,
                        sharedPref?.getString("token", "")!!
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                val colorPrimary = TypedValue()
                (context as Activity).theme.resolveAttribute(
                    android.R.attr.colorPrimary,
                    colorPrimary,
                    true
                )

                val colorText = TypedValue()
                (context as Activity).theme.resolveAttribute(
                    R.attr.colorOnSurface,
                    colorText,
                    true
                )

                runOnUiThread {
                    if (response.subscribed == true) {
                        isSubscribed = true
                        button.text = getString(R.string.unsubscribe)
                        button.setTextColor(colorText.data)
                    }
                    if (response.subscribed != null) {
                        button.setOnClickListener {
                            if (isSubscribed) {
                                unsubscribe(channelId)
                                button.text = getString(R.string.subscribe)
                                button.setTextColor(colorPrimary.data)

                            } else {
                                subscribe(channelId)
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
                    RetrofitInstance.api.subscribe(
                        sharedPref?.getString("token", "")!!,
                        Subscribe(channel_id)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response$e")
                    return@launchWhenCreated
                }
                isSubscribed = true
            }
        }
        run()
    }

    private fun unsubscribe(channel_id: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.unsubscribe(
                        sharedPref?.getString("token", "")!!,
                        Subscribe(channel_id)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                isSubscribed = false
            }
        }
        run()
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
            if (q > bitrate) {
                bitrate = q
                index = i
            }
        }
        return index
    }
}
