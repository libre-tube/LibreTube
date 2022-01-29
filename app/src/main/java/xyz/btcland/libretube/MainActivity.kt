package xyz.btcland.libretube

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.google.android.exoplayer2.ExoPlayer

class MainActivity : AppCompatActivity() {
    lateinit var bottomNavigationView: BottomNavigationView
    lateinit var toolbar: Toolbar
    var f = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)
        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
        val navController = findNavController(R.id.fragment)
        bottomNavigationView.setupWithNavController(navController)

/*        bottomNavigationView.setOnItemSelectedListener {
            println("fuckoff")
            onNavDestinationSelected(it,navController)
        }*/



        toolbar = findViewById(R.id.toolbar)

        toolbar.setOnMenuItemClickListener{
            when (it.itemId){
                R.id.action_search -> {
                    val navController = findNavController(R.id.fragment)
                    navController.popBackStack()
                    navController.navigate(R.id.searchFragment)
                    f = true
                    true
                }
                R.id.action_settings -> {
                    true
                }
            }
            false
        }

    }

    override fun onBackPressed() {
        if (f){
            val navController = findNavController(R.id.fragment)
            navController.popBackStack()
            navController.navigate(R.id.home2)
            f = false
        }else {super.onBackPressed()}
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val orientation = newConfig.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            println("Portrait")
            //findViewById<MotionLayout>(R.id.playerMotionLayout).getTransition(R.id.yt_transition).isEnabled = true
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            println("Landscape")
            window.decorView.apply {
                // Hide both the navigation bar and the status bar.
                // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
                // a general rule, you should design your app to hide the status bar whenever you
                // hide the navigation bar.
                systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
            //findViewById<MotionLayout>(R.id.playerMotionLayout).getTransition(R.id.yt_transition).isEnabled = false

        }
    }
}