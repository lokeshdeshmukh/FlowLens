package com.flowlens.export

import com.flowlens.domain.FlowGraph

class MermaidExporter {
    fun export(graph: FlowGraph): String {
        val builder = StringBuilder("graph TD\n")
        val aliases = graph.screens.associate { it.id to sanitizeId(it.id) }

        graph.screens.forEach { screen ->
            builder.append("    ")
                .append(aliases.getValue(screen.id))
                .append("[\"")
                .append(screen.name.replace("\"", "\\\""))
                .append("\"]\n")
        }

        graph.connections.forEach { connection ->
            builder.append("    ")
                .append(aliases.getValue(connection.from))
                .append(" --> ")
                .append(aliases.getValue(connection.to))
            connection.label?.takeIf { it.isNotBlank() }?.let { label ->
                builder.append(":::").append(sanitizeId(label))
            }
            builder.append('\n')
        }

        return builder.toString().trimEnd()
    }

    private fun sanitizeId(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9_]"), "_")
}

