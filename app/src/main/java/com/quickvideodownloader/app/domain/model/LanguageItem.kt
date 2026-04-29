package com.quickvideodownloader.app.domain.model

data class LanguageItem(
    val name: String,
    val nativeName: String,
    val code: String,
    val flag: String = ""
)
