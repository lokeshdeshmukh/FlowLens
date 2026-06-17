package com.flowlens.export

import com.flowlens.domain.FlowGraph

class JsonExporter {
    fun export(graph: FlowGraph): String {
        val screens = graph.screens.joinToString(",\n") { screen ->
            """
            {
              "id": "${escape(screen.id)}",
              "name": "${escape(screen.name)}",
              "type": "${screen.type}",
              "filePath": ${stringOrNull(screen.filePath)},
              "route": ${stringOrNull(screen.route)},
              "className": ${stringOrNull(screen.className)},
              "deepLinks": ${stringArray(screen.deepLinks)},
              "relatedViewModels": ${stringArray(screen.relatedViewModels)},
              "relatedRepositories": ${stringArray(screen.relatedRepositories)},
              "isStartDestination": ${screen.isStartDestination}
            }
            """.trimIndent()
        }

        val connections = graph.connections.joinToString(",\n") { connection ->
            """
            {
              "from": "${escape(connection.from)}",
              "to": "${escape(connection.to)}",
              "kind": "${connection.kind}",
              "label": ${stringOrNull(connection.label)},
              "sourceFilePath": ${stringOrNull(connection.sourceFilePath)}
            }
            """.trimIndent()
        }

        return """
        {
          "screens": [
        ${indentBlock(screens, 4)}
          ],
          "connections": [
        ${indentBlock(connections, 4)}
          ]
        }
        """.trimIndent()
    }

    private fun stringOrNull(value: String?): String =
        value?.let { "\"${escape(it)}\"" } ?: "null"

    private fun stringArray(values: Set<String>): String =
        values.sorted().joinToString(prefix = "[", postfix = "]") { "\"${escape(it)}\"" }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun indentBlock(value: String, spaces: Int): String {
        if (value.isBlank()) return ""
        val prefix = " ".repeat(spaces)
        return value.lines().joinToString("\n") { "$prefix$it" }
    }
}

