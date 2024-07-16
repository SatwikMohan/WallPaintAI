package com.example.wallpaintai

import android.graphics.Color

interface ClickInterface {
    fun onClick(color: Int)
    fun onLongClick(reason:String)
}