package xyz.btcland.libretube

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Player : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        val videoId=intent.getStringExtra("videoId")
        println(videoId)
    }
}