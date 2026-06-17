package com.flowlens.service

import com.flowlens.analysis.CachedFileAnalysis
import com.flowlens.analysis.ProjectFlowAnalyzer
import com.flowlens.domain.FlowGraph
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class FlowLensProjectService(private val project: Project) : Disposable {
    private val analyzer = ProjectFlowAnalyzer()
    private val cache = ConcurrentHashMap<String, CachedFileAnalysis>()
    private val dirtyPaths = ConcurrentHashMap.newKeySet<String>()
    private val messageBusConnection = project.messageBus.connect(this)

    @Volatile
    private var latestGraph: FlowGraph? = null

    init {
        messageBusConnection.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    events.mapNotNullTo(dirtyPaths) { event ->
                        val path = event.path
                        if (path.endsWith(".kt") || path.endsWith(".java") || path.endsWith(".xml")) path else null
                    }
                    if (events.any { it.path.endsWith(".kt") || it.path.endsWith(".java") || it.path.endsWith(".xml") }) {
                        latestGraph = null
                    }
                }
            },
        )
    }

    fun latestGraph(): FlowGraph? = latestGraph

    fun analyzeProject(
        forceRefresh: Boolean = false,
        onSuccess: (FlowGraph) -> Unit,
        onError: (Throwable) -> Unit = {},
    ) {
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Analyzing Android App Flow", true) {
                private var result: FlowGraph? = null

                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false
                    result = analyzer.analyze(
                        project = project,
                        indicator = indicator,
                        cache = cache,
                        dirtyPaths = dirtyPaths,
                        forceRefresh = forceRefresh,
                    )
                }

                override fun onSuccess() {
                    val graph = result ?: return
                    latestGraph = graph
                    onSuccess(graph)
                }

                override fun onThrowable(error: Throwable) {
                    onError(error)
                }
            },
        )
    }

    override fun dispose() = Unit
}

