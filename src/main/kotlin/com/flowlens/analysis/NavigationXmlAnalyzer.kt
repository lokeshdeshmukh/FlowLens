package com.flowlens.analysis

import com.flowlens.domain.AnalyzerOutput
import com.flowlens.domain.DiscoveredConnection
import com.flowlens.domain.DiscoveredNode
import com.flowlens.domain.NavigationKind
import com.flowlens.domain.NodeType
import com.flowlens.util.DisplayNames
import com.flowlens.util.NodeKeys
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

class NavigationXmlAnalyzer {
    fun analyze(xmlFile: XmlFile): AnalyzerOutput {
        val root = xmlFile.rootTag ?: return AnalyzerOutput()
        if (root.name != "navigation") {
            return AnalyzerOutput()
        }

        val nodes = linkedSetOf<DiscoveredNode>()
        val connections = linkedSetOf<DiscoveredConnection>()
        val idToKey = linkedMapOf<String, String>()
        val deferredConnections = mutableListOf<Pair<String, String>>()
        val startNodes = linkedSetOf<String>()

        fun attribute(tag: XmlTag, localName: String): String? =
            tag.attributes.firstOrNull { it.localName == localName }?.value

        fun visitDestination(tag: XmlTag) {
            when (tag.name) {
                "fragment", "activity", "dialog", "navigation", "bottomSheet" -> {
                    val id = attribute(tag, "id")
                    val className = attribute(tag, "name")
                    val route = attribute(tag, "route")
                    val deepLinks = tag.findSubTags("deepLink").mapNotNull { child ->
                        attribute(child, "uri")
                    }.toSet()

                    val type = when {
                        tag.name == "activity" -> NodeType.ACTIVITY
                        tag.name == "dialog" -> NodeType.DIALOG
                        tag.name == "bottomSheet" -> NodeType.BOTTOM_SHEET
                        className?.contains("Dialog", ignoreCase = true) == true -> NodeType.DIALOG
                        className?.contains("BottomSheet", ignoreCase = true) == true -> NodeType.BOTTOM_SHEET
                        else -> NodeType.FRAGMENT
                    }

                    val key = when {
                        !className.isNullOrBlank() -> NodeKeys.className(className)
                        !route.isNullOrBlank() -> NodeKeys.route(route)
                        !id.isNullOrBlank() -> NodeKeys.xmlId(id)
                        else -> NodeKeys.xmlId("${xmlFile.virtualFile.name}:${tag.textOffset}")
                    }

                    if (!id.isNullOrBlank()) {
                        idToKey[NodeKeys.trimResourcePrefix(id)] = key
                    }

                    nodes += DiscoveredNode(
                        key = key,
                        name = when {
                            !className.isNullOrBlank() -> DisplayNames.simpleClassName(className)
                            !route.isNullOrBlank() -> DisplayNames.humanizeRoute(route)
                            !id.isNullOrBlank() -> DisplayNames.humanizeRoute(NodeKeys.trimResourcePrefix(id))
                            else -> tag.name.replaceFirstChar { it.uppercase() }
                        },
                        type = type,
                        filePath = xmlFile.virtualFile.path,
                        route = route,
                        className = className,
                        deepLinks = deepLinks,
                        aliases = buildSet {
                            if (!id.isNullOrBlank()) add(NodeKeys.xmlId(id))
                        },
                    )

                    tag.findSubTags("action").forEach { action ->
                        val destination = attribute(action, "destination") ?: return@forEach
                        deferredConnections += key to NodeKeys.trimResourcePrefix(destination)
                    }

                    tag.subTags.forEach(::visitDestination)
                }
            }
        }

        visitDestination(root)

        val startDestination = attribute(root, "startDestination")
        if (!startDestination.isNullOrBlank()) {
            startNodes += idToKey[NodeKeys.trimResourcePrefix(startDestination)] ?: NodeKeys.xmlId(startDestination)
        }

        deferredConnections.forEach { (fromKey, destinationId) ->
            connections += DiscoveredConnection(
                fromKey = fromKey,
                toKey = idToKey[destinationId] ?: NodeKeys.xmlId(destinationId),
                kind = NavigationKind.NAVIGATION_XML,
                label = "action",
                sourceFilePath = xmlFile.virtualFile.path,
            )
        }

        return AnalyzerOutput(
            nodes = nodes.toList(),
            connections = connections.toList(),
            startNodeKeys = startNodes,
        )
    }
}
