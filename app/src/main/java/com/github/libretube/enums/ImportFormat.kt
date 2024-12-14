package com.github.libretube.enums

import androidx.annotation.StringRes
import com.github.libretube.R

enum class ImportFormat(@StringRes val value: Int, val fileExtension: String) {
    NEWPIPE(R.string.import_format_newpipe, "json"),
    FREETUBE(R.string.import_format_freetube, "db"),
    YOUTUBECSV(R.string.import_format_youtube_csv, "csv"),
    YOUTUBEJSON(R.string.youtube, "json"),
    PIPED(R.string.import_format_piped, "json"),
    URLSORIDS(R.string.import_format_list_of_urls, "txt")
}
