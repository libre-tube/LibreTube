package com.github.libretube.ui.views
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.DropDownPreference
import com.github.libretube.R

class SpinnerPreference(context: Context, attrs: AttributeSet): DropDownPreference(context, attrs){

    fun onCreate(parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return inflater.inflate(R.layout.spinner_preference, parent, false)
    }

}
