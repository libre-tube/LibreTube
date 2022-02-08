package com.github.libretube

import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.material.color.DynamicColors
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    lateinit var bottomNavigationView: BottomNavigationView
    lateinit var toolbar: Toolbar
    lateinit var navController : NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        RetrofitInstance.url=sharedPreferences.getString("instance", "https://pipedapi.kavin.rocks/")!!

        DynamicColors.applyToActivitiesIfAvailable(application)


        setContentView(R.layout.activity_main)
        bottomNavigationView = findViewById(R.id.bottomNav)


        navController = findNavController(R.id.fragment)
        bottomNavigationView.setupWithNavController(navController)





        toolbar = findViewById(R.id.toolbar)
        val hexColor = String.format("#%06X", 0xFFFFFF and 0xcc322d)
        val appName = HtmlCompat.fromHtml(
            "Libre<span  style='color:$hexColor';>Tube</span>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        toolbar.title= appName

        toolbar.setNavigationOnClickListener{
            //settings fragment stuff
            navController.navigate(R.id.settings)
            true
        }

        toolbar.setOnMenuItemClickListener{
            when (it.itemId){
                R.id.action_search -> {
                    navController.navigate(R.id.searchFragment)
                    true
                }
            }
            false
        }

    }

    override fun onBackPressed() {
        try{
            val mainMotionLayout = findViewById<MotionLayout>(R.id.mainMotionLayout)
            if (mainMotionLayout.progress == 0.toFloat()){
                mainMotionLayout.transitionToEnd()
                findViewById<MotionLayout>(R.id.playerMotionLayout).transitionToEnd()
            }else{
                navController.popBackStack()
                if (navController.currentBackStackEntry == null){
                    finish()
                }}
        }catch (e: Exception){
            navController.popBackStack()
            if (navController.currentBackStackEntry == null){
                finish()
            }
        }


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
