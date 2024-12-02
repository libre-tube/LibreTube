package com.github.libretube.enums

import androidx.annotation.StringRes
import com.github.libretube.R

enum class ImportFormat(@StringRes val value: Int) {
    NEWPIPE(R.string.import_format_newpipe),
    FREETUBE(R.string.import_format_freetube),
    YOUTUBECSV(R.string.import_format_youtube_csv),
    YOUTUBEJSON(R.string.youtube),
    PIPED(R.string.import_format_piped),
    URLSORIDS(R.string.import_format_list_of_urls)
}
