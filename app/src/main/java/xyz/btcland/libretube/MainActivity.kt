package xyz.btcland.libretube

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.google.android.exoplayer2.ExoPlayer

class MainActivity : AppCompatActivity() {
    lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
        val navController = findNavController(R.id.fragment)
        bottomNavigationView.setupWithNavController(navController)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId){
        R.id.action_search -> {
            val navController = findNavController(R.id.fragment)
            navController.popBackStack()
            navController.navigate(R.id.searchFragment)
            //bottomNavigationView.clearFocus()
            //val navController = findNavController(R.id.fragment)
            //navController.navigate(R.id.searchFragment)
            //navController.navigate(R.id.home2)
            true
        }
        R.id.action_settings -> {

            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar,menu)
        return true
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