package com.example.wallpaintai

import com.beust.klaxon.Json


data class AISuggestion(
    val suggestionList: ArrayList<AISuggestionObject>
)

data class AISuggestionObject(
    @Json("color")
    val color:String,
    @Json("HexCode")
    val HexCode:String,
    @Json("reason")
    val reason:String
)
