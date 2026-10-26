package com.jrla.exifoneline.io

/** Turns EXIF make/model codes into the names people actually use. */
object Models {
    // Samsung writes model codes (SM-S948B). Region suffix is dropped before lookup.
    private val galaxy = mapOf(
        "SM-S948" to "Galaxy S26 Ultra",
        "SM-S947" to "Galaxy S26+",
        "SM-S942" to "Galaxy S26",
        "SM-S938" to "Galaxy S25 Ultra",
        "SM-S937" to "Galaxy S25 Edge",
        "SM-S936" to "Galaxy S25+",
        "SM-S931" to "Galaxy S25",
        "SM-S928" to "Galaxy S24 Ultra",
        "SM-S926" to "Galaxy S24+",
        "SM-S921" to "Galaxy S24",
        "SM-S918" to "Galaxy S23 Ultra",
        "SM-S916" to "Galaxy S23+",
        "SM-S911" to "Galaxy S23",
        "SM-F966" to "Galaxy Z Fold7",
        "SM-F766" to "Galaxy Z Flip7",
        "SM-F956" to "Galaxy Z Fold6",
        "SM-F741" to "Galaxy Z Flip6",
    )

    private val brandNames = mapOf(
        "samsung" to "Samsung",
        "fujifilm" to "Fujifilm",
        "dji" to "DJI",
        "hmd" to "HMD",
        "om" to "OM System",
    )

    fun brand(make: String): String {
        val first = make.trim().split(" ").firstOrNull().orEmpty()
        if (first.isEmpty()) return ""
        return brandNames[first.lowercase()] ?: first.lowercase().replaceFirstChar { it.titlecase() }
    }

    fun friendly(model: String): String {
        if (model.startsWith("SM-", ignoreCase = true) && model.length >= 7) {
            galaxy[model.take(7).uppercase()]?.let { return it }
        }
        return model
    }
}
