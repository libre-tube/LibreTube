package com.github.libretube.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Subscribed(
    var subscribed: Boolean? = null
)
