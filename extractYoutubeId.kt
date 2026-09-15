fun extractYouTubeId(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
    val compiledPattern = java.util.regex.Pattern.compile(pattern)
    val matcher = compiledPattern.matcher(url)
    return if (matcher.find()) {
        matcher.group()
    } else null
}
