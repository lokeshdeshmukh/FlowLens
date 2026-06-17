package com.flowlens.util

object SourceTextParsers {
    private val quotedValue = "\"([^\"]+)\"".toRegex()
    private val routeNamed = """\broute\s*=\s*"([^"]+)"""".toRegex()
    private val startDestinationNamed = """\bstartDestination\s*=\s*"([^"]+)"""".toRegex()
    private val navIdReference = """R\.id\.([A-Za-z0-9_]+)""".toRegex()
    private val kotlinClassReference = """([A-Za-z0-9_$.]+)::class\.java""".toRegex()
    private val javaClassReference = """([A-Za-z0-9_$.]+)\.class""".toRegex()
    private val deepLinkPattern = """uriPattern\s*=\s*"([^"]+)"""".toRegex()
    private val viewModelPattern = """\b([A-Z][A-Za-z0-9_]*ViewModel)\b""".toRegex()
    private val repositoryPattern = """\b([A-Z][A-Za-z0-9_]*(?:Repository|Repo))\b""".toRegex()

    fun extractRouteDeclaration(callText: String): String? =
        routeNamed.find(callText)?.groupValues?.getOrNull(1)
            ?: quotedValue.find(callText.substringBefore('{'))?.groupValues?.getOrNull(1)

    fun extractStartDestination(callText: String): String? =
        startDestinationNamed.find(callText)?.groupValues?.getOrNull(1)
            ?: navIdReference.find(callText.substringBefore('{'))?.groupValues?.getOrNull(1)

    fun extractNavigationTarget(callText: String): String? =
        routeNamed.find(callText)?.groupValues?.getOrNull(1)
            ?: navIdReference.find(callText)?.groupValues?.getOrNull(1)
            ?: quotedValue.find(callText.substringBefore('{'))?.groupValues?.getOrNull(1)

    fun extractIntentTarget(callText: String): String? =
        kotlinClassReference.find(callText)?.groupValues?.getOrNull(1)
            ?: javaClassReference.find(callText)?.groupValues?.getOrNull(1)

    fun extractDeepLinks(callText: String): Set<String> =
        deepLinkPattern.findAll(callText).mapNotNull { it.groupValues.getOrNull(1) }.toSet()

    fun extractViewModels(sourceText: String): Set<String> =
        viewModelPattern.findAll(sourceText).mapNotNull { it.groupValues.getOrNull(1) }.toSortedSet()

    fun extractRepositories(sourceText: String): Set<String> =
        repositoryPattern.findAll(sourceText).mapNotNull { it.groupValues.getOrNull(1) }.toSortedSet()
}

