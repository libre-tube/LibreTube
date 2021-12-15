package xyz.btcland.libretube

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.exoplayer2.ExoPlayer

class MainActivity : AppCompatActivity() {
    lateinit var exoPlayer:ExoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
        val navController = findNavController(R.id.fragment)
        bottomNavigationView.setupWithNavController(navController)

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val orientation = newConfig.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            println("Portrait")
            //findViewById<MotionLayout>(R.id.playerMotionLayout).getTransition(R.id.yt_transition).isEnabled = true
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            println("Landscape")
            //findViewById<MotionLayout>(R.id.playerMotionLayout).getTransition(R.id.yt_transition).isEnabled = false

        }
    }
}