package com.flowlens.ui

import com.flowlens.domain.FlowGraph
import com.flowlens.domain.ScreenNode
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

class ScreenDetailsPanel(private val project: Project) : JBPanel<ScreenDetailsPanel>(BorderLayout()) {
    private val titleLabel = JBLabel("Select a screen")
    private val typeLabel = JBLabel("")
    private val filePathArea = createTextArea()
    private val incomingArea = createTextArea()
    private val outgoingArea = createTextArea()
    private val deepLinksArea = createTextArea()
    private val viewModelsArea = createTextArea()
    private val repositoriesArea = createTextArea()
    private val openFileButton = JButton("Open File")

    private var selectedNode: ScreenNode? = null

    init {
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        titleLabel.border = BorderFactory.createEmptyBorder(0, 0, 8, 0)
        openFileButton.addActionListener { openSelectedFile() }
        openFileButton.isEnabled = false

        val form = FormBuilder.createFormBuilder()
            .addComponent(titleLabel)
            .addLabeledComponent("Type", typeLabel)
            .addLabeledComponent("File Path", wrap(filePathArea))
            .addComponent(openFileButton)
            .addLabeledComponent("Incoming Paths", wrap(incomingArea))
            .addLabeledComponent("Outgoing Paths", wrap(outgoingArea))
            .addLabeledComponent("Deep Links", wrap(deepLinksArea))
            .addLabeledComponent("Related ViewModel", wrap(viewModelsArea))
            .addLabeledComponent("Repository References", wrap(repositoriesArea))
            .panel

        add(JBScrollPane(form).apply { border = BorderFactory.createEmptyBorder() }, BorderLayout.CENTER)
        clear()
    }

    fun showNode(node: ScreenNode?, graph: FlowGraph?) {
        selectedNode = node
        if (node == null || graph == null) {
            clear()
            return
        }

        titleLabel.text = node.name
        titleLabel.icon = FlowIcons.forType(node.type)
        typeLabel.text = buildString {
            append(node.type.name.replace('_', ' '))
            if (node.isStartDestination) {
                append(" • Start Destination")
            }
        }
        filePathArea.text = node.filePath ?: "Not resolved"
        openFileButton.isEnabled = !node.filePath.isNullOrBlank()

        incomingArea.text = graph.incoming(node.id)
            .joinToString("\n") { edge ->
                val sourceNode = graph.node(edge.from)?.name ?: edge.from
                sourceNode + edge.labelSuffix()
            }
            .ifBlank { "None" }

        outgoingArea.text = graph.outgoing(node.id)
            .joinToString("\n") { edge ->
                val targetNode = graph.node(edge.to)?.name ?: edge.to
                targetNode + edge.labelSuffix()
            }
            .ifBlank { "None" }

        deepLinksArea.text = node.deepLinks.sorted().joinToString("\n").ifBlank { "None" }
        viewModelsArea.text = node.relatedViewModels.sorted().joinToString("\n").ifBlank { "None" }
        repositoriesArea.text = node.relatedRepositories.sorted().joinToString("\n").ifBlank { "None" }
    }

    fun clear() {
        titleLabel.text = "Select a screen"
        titleLabel.icon = null
        typeLabel.text = "No selection"
        filePathArea.text = "No file selected"
        incomingArea.text = "None"
        outgoingArea.text = "None"
        deepLinksArea.text = "None"
        viewModelsArea.text = "None"
        repositoriesArea.text = "None"
        openFileButton.isEnabled = false
    }

    private fun openSelectedFile() {
        val path = selectedNode?.filePath ?: return
        val file = LocalFileSystem.getInstance().findFileByPath(path) ?: return
        OpenFileDescriptor(project, file).navigate(true)
    }

    private fun createTextArea(): JBTextArea =
        JBTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            rows = 4
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        }

    private fun wrap(area: JBTextArea): JPanel =
        JPanel(BorderLayout()).apply {
            border = BorderFactory.createLineBorder(com.intellij.ui.JBColor.border(), 1, true)
            add(area, BorderLayout.CENTER)
        }

    private fun com.flowlens.domain.FlowConnection.labelSuffix(): String =
        label?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
}

