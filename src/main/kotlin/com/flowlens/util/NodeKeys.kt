package com.flowlens.util

object NodeKeys {
    fun route(route: String): String = "route:${route.trim()}"

    fun className(qualifiedName: String): String = "class:${qualifiedName.trim()}"

    fun xmlId(id: String): String = "xml:${trimResourcePrefix(id)}"

    fun deepLink(uri: String, targetId: String): String =
        "deeplink:${uri.trim()}->${targetId.trim()}"

    fun bottomSheet(ownerKey: String, label: String): String =
        "sheet:${ownerKey.trim()}#${label.trim()}"

    fun trimResourcePrefix(raw: String): String =
        raw.substringAfter('/').substringAfter(':').trim()
}

