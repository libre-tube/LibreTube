package xyz.btcland.libretube

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource

import com.google.android.exoplayer2.ui.PlayerView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException
import kotlin.math.abs

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


    private lateinit var exoPlayerView: PlayerView
    private lateinit var motionLayout: SingleViewTouchableMotionLayout
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSource: MediaSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoId = it.getString("videoId")
            param2 = it.getString(ARG_PARAM2)
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
                    mainMotionLayout.progress = 1.toFloat()
                }else if(currentId==sId){
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
            println("wtf?")
            val mainActivity = activity as MainActivity
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
            mainActivity.findViewById<FrameLayout>(R.id.container).layoutParams=ViewGroup.LayoutParams(0,0)

        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer.stop()
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

    private fun initPlayer(view: View,url: String){
        exoPlayer = ExoPlayer.Builder(view.context).build()
        exoPlayerView.player = exoPlayer
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.play()
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
                        runOnUiThread {
                            initPlayer(view,videoInPlayer.hls)
                            view.findViewById<TextView>(R.id.title_textView).text = videoInPlayer.title
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
}