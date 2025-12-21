package io.github.jmatsu.license.ext

import java.util.Locale

fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.uppercase(Locale.getDefault()) else it.toString() }

fun String.decapitalized(): String = replaceFirstChar { if (it.isUpperCase()) it.lowercase(Locale.getDefault()) else it.toString() }
