package com.flowlens.ui

import com.flowlens.domain.FlowGraph
import com.flowlens.export.JsonExporter
import com.flowlens.export.MermaidExporter
import com.flowlens.service.FlowLensProjectService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JPanel

class FlowGraphToolWindowPanel(private val project: Project) : JBPanel<FlowGraphToolWindowPanel>(BorderLayout()) {
    private val service = project.service<FlowLensProjectService>()
    private val graphCanvas = FlowGraphCanvas { node -> detailsPanel.showNode(node, currentGraph) }
    private val detailsPanel = ScreenDetailsPanel(project)
    private val mermaidExporter = MermaidExporter()
    private val jsonExporter = JsonExporter()
    private val searchField = JBTextField(18)
    private val statusLabel = JBLabel("Run Analyze App Flow to scan the project")

    private var currentGraph: FlowGraph? = service.latestGraph()

    init {
        border = JBUI.Borders.empty()
        add(buildToolbar(), BorderLayout.NORTH)

        val splitter = OnePixelSplitter(false, 0.72f).apply {
            firstComponent = graphCanvas
            secondComponent = detailsPanel
        }
        add(splitter, BorderLayout.CENTER)
        add(JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.customLineTop(com.intellij.ui.JBColor.border())
            add(statusLabel, BorderLayout.WEST)
        }, BorderLayout.SOUTH)

        currentGraph?.let { updateGraph(it) } ?: detailsPanel.clear()
        searchField.addActionListener { focusSearchResult() }
    }

    fun runAnalysis(forceRefresh: Boolean = false) {
        statusLabel.text = "Analyzing project navigation..."
        service.analyzeProject(
            forceRefresh = forceRefresh,
            onSuccess = { graph ->
                ApplicationManager.getApplication().invokeLater {
                    updateGraph(graph)
                    statusLabel.text = "Analyzed ${graph.screens.size} screens and ${graph.connections.size} connections"
                }
            },
            onError = { error ->
                ApplicationManager.getApplication().invokeLater {
                    statusLabel.text = "Analysis failed"
                    Messages.showErrorDialog(
                        project,
                        error.message ?: "Unknown error while analyzing the project.",
                        "FlowLens",
                    )
                }
            },
        )
    }

    private fun updateGraph(graph: FlowGraph) {
        currentGraph = graph
        graphCanvas.setFlowGraph(graph)
        val defaultNode = graph.screens.firstOrNull { it.isStartDestination } ?: graph.screens.firstOrNull()
        graphCanvas.selectNode(defaultNode?.id)
        detailsPanel.showNode(defaultNode, graph)
    }

    private fun buildToolbar(): JPanel {
        val analyzeButton = JButton("Analyze App Flow").apply {
            addActionListener { runAnalysis(forceRefresh = false) }
        }
        val refreshButton = JButton("Full Refresh").apply {
            addActionListener { runAnalysis(forceRefresh = true) }
        }
        val zoomInButton = JButton("+").apply { addActionListener { graphCanvas.zoomIn() } }
        val zoomOutButton = JButton("-").apply { addActionListener { graphCanvas.zoomOut() } }
        val fitButton = JButton("Fit").apply { addActionListener { graphCanvas.fitToGraph() } }
        val layoutButton = JButton("Auto Layout").apply { addActionListener { graphCanvas.autoLayout() } }
        val exportPngButton = JButton("Export PNG").apply { addActionListener { exportPng() } }
        val exportMermaidButton = JButton("Export Mermaid").apply { addActionListener { exportMermaid() } }
        val exportJsonButton = JButton("Export JSON").apply { addActionListener { exportJson() } }
        val searchButton = JButton("Search").apply { addActionListener { focusSearchResult() } }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            add(
                JPanel().apply {
                    layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0)
                    add(analyzeButton)
                    add(refreshButton)
                    add(zoomInButton)
                    add(zoomOutButton)
                    add(fitButton)
                    add(layoutButton)
                    add(exportPngButton)
                    add(exportMermaidButton)
                    add(exportJsonButton)
                },
                BorderLayout.WEST,
            )
            add(
                JPanel().apply {
                    layout = java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0)
                    searchField.toolTipText = "Search screen by route, class name, or label"
                    add(searchField)
                    add(searchButton)
                },
                BorderLayout.EAST,
            )
        }
    }

    private fun focusSearchResult() {
        val query = searchField.text.trim()
        if (query.isBlank()) {
            statusLabel.text = "Enter a screen name or route to search"
            return
        }
        val screen = graphCanvas.focusScreen(query)
        if (screen == null) {
            statusLabel.text = "No matching screen found for \"$query\""
        } else {
            detailsPanel.showNode(screen, currentGraph)
            statusLabel.text = "Focused ${screen.name}"
        }
    }

    private fun exportPng() {
        val graph = currentGraph ?: return
        val output = chooseFile("flow-graph", "png") ?: return
        graphCanvas.exportPng(output.toFile())
        statusLabel.text = "PNG exported to ${output.fileName}"
    }

    private fun exportMermaid() {
        val graph = currentGraph ?: return
        val output = chooseFile("flow-graph", "mmd") ?: return
        Files.writeString(output, mermaidExporter.export(graph), StandardCharsets.UTF_8)
        statusLabel.text = "Mermaid exported to ${output.fileName}"
    }

    private fun exportJson() {
        val graph = currentGraph ?: return
        val output = chooseFile("flow-graph", "json") ?: return
        Files.writeString(output, jsonExporter.export(graph), StandardCharsets.UTF_8)
        statusLabel.text = "JSON exported to ${output.fileName}"
    }

    private fun chooseFile(baseName: String, extension: String): Path? {
        val chooser = JFileChooser().apply {
            selectedFile = java.io.File("$baseName.$extension")
            dialogTitle = "Export Flow Graph"
        }
        val result = chooser.showSaveDialog(this)
        return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile.toPath() else null
    }
}
