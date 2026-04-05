import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var homeNavigationManager: HomeNavigationManager
    private lateinit var videoWatcherInterceptor: VideoWatcherInterceptor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoWatcherInterceptor = VideoWatcherInterceptor()
        homeNavigationManager = HomeNavigationManager()
        initPreferenceListeners()

        loadDefaultFragment()
    }

    private fun loadDefaultFragment() {
        val fragment: Fragment = ForYouFragment() // Use ForYouFragment as the default home fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // Ensure this matches your layout
            .commit()
    }

    private fun initPreferenceListeners() {
        // Setup listeners to dynamically switch feeds based on user preferences
        // Example: 
        // preferenceManager.feedPreference.observe(this) { preference ->
        //     if (preference == "trending") {
        //         homeNavigationManager.selectTrendingFeed()
        //     } else {
        //         homeNavigationManager.selectForYouFeed()
        //     }
        // }
    }
}