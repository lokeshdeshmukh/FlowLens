package com.flowlens.util

object DisplayNames {
    fun simpleClassName(qualifiedName: String?): String =
        qualifiedName
            ?.substringAfterLast('.')
            ?.substringAfterLast('$')
            .orEmpty()

    fun humanizeRoute(route: String): String {
        val normalized = route
            .substringBefore('?')
            .substringBefore('/')
            .replace("{", "")
            .replace("}", "")
            .replace('_', ' ')
            .replace('-', ' ')
        return normalized
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { char -> char.uppercase() }
            }
            .ifBlank { route }
    }

    fun fromKey(key: String): String = when {
        key.startsWith("route:") -> humanizeRoute(key.removePrefix("route:"))
        key.startsWith("class:") -> simpleClassName(key.removePrefix("class:"))
        key.startsWith("xml:") -> humanizeRoute(key.removePrefix("xml:"))
        key.startsWith("deeplink:") -> "Deep Link"
        else -> humanizeRoute(key.substringAfter(':', key))
    }
}

