package com.wandr.domain.model

/** Reads an invite code from what a user typed or what a QR code contains (`wandr://invite/{code}` or the bare code). */
object InviteCode {
    private const val URL_PREFIX = "wandr://invite/"
    private val valid = Regex("[A-Za-z0-9]{4,32}")

    /** The bare code (case is kept, the server compares case-insensitively), or null if [input] is not a code. */
    fun parse(input: String): String? {
        var text = input.trim()
        if (text.startsWith(URL_PREFIX, ignoreCase = true)) text = text.substring(URL_PREFIX.length)
        text = text.trim().trimEnd('/')
        return text.takeIf { valid.matches(it) }
    }
}
