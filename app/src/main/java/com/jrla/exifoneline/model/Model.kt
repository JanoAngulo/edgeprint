package com.jrla.exifoneline.model

import java.time.LocalDateTime

/** Per-photo values shown on the watermark. Every field is user-editable text. */
data class PhotoInfo(
    val brand: String = "",
    val model: String = "",
    val focal: String = "",
    val aperture: String = "",
    val shutter: String = "",
    val iso: String = "",
    val date: String = "",
    val location: String = "",
    val dateTime: LocalDateTime? = null,
)

/** Text shared by every photo, remembered between sessions. */
data class Texts(
    val author: String = "User",
    val title: String = "",
    val quote: String = "",
)

enum class LogoTint(val label: String) {
    AUTO("Auto"), ORIGINAL("Original"), WHITE("White"), BLACK("Black")
}

enum class Ratio(val label: String, val w: Int, val h: Int) {
    ORIGINAL("Original", 0, 0),
    SQUARE("1:1", 1, 1),
    PORTRAIT("4:5", 4, 5),
    STORY("9:16", 9, 16),
    WIDE("16:9", 16, 9),
}

data class Style(
    val templateId: String = "bar_classic",
    /** Null means the template's own default background. */
    val background: Int? = null,
    val logoTint: LogoTint = LogoTint.AUTO,
    val ratio: Ratio = Ratio.ORIGINAL,
    val showLogo: Boolean = true,
    val showDate: Boolean = true,
    val showLocation: Boolean = true,
    val showAuthor: Boolean = true,
    val borderScale: Float = 1f,
    /** "auto" (brand from EXIF), "text", "custom", or a brand logo id. */
    val logoId: String = "auto",
    val font: String = "inter",
)
