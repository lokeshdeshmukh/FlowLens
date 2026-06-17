package com.flowlens.analysis

import com.flowlens.build.FlowGraphBuilder
import com.flowlens.domain.AnalyzerOutput
import com.flowlens.domain.FlowGraph
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap

data class CachedFileAnalysis(
    val modificationStamp: Long,
    val output: AnalyzerOutput,
)

class ProjectFlowAnalyzer(
    private val navigationXmlAnalyzer: NavigationXmlAnalyzer = NavigationXmlAnalyzer(),
    private val sourceFileAnalyzer: SourceFileAnalyzer = SourceFileAnalyzer(),
    private val graphBuilder: FlowGraphBuilder = FlowGraphBuilder(),
) {
    fun analyze(
        project: Project,
        indicator: ProgressIndicator,
        cache: ConcurrentHashMap<String, CachedFileAnalysis>,
        dirtyPaths: MutableSet<String>,
        forceRefresh: Boolean,
    ): FlowGraph {
        val relevantFiles = collectRelevantFiles(project)
        val currentPaths = relevantFiles.mapTo(linkedSetOf()) { it.path }
        cache.keys.retainAll(currentPaths)
        dirtyPaths.retainAll(currentPaths)

        val outputs = ArrayList<AnalyzerOutput>(relevantFiles.size)
        relevantFiles.forEachIndexed { index, file ->
            indicator.checkCanceled()
            indicator.fraction = if (relevantFiles.isEmpty()) 1.0 else index.toDouble() / relevantFiles.size.toDouble()
            indicator.text2 = file.presentableUrl

            val cached = cache[file.path]
            val canReuse = !forceRefresh &&
                file.path !in dirtyPaths &&
                cached?.modificationStamp == file.modificationStamp

            if (canReuse) {
                outputs += cached.output
                return@forEachIndexed
            }

            val output = ReadAction.compute<AnalyzerOutput, RuntimeException> {
                val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@compute AnalyzerOutput()
                when (psiFile) {
                    is XmlFile -> navigationXmlAnalyzer.analyze(psiFile)
                    else -> sourceFileAnalyzer.analyze(file, psiFile)
                }
            }
            cache[file.path] = CachedFileAnalysis(file.modificationStamp, output)
            outputs += output
        }

        dirtyPaths.clear()
        indicator.fraction = 1.0
        indicator.text2 = "Building flow graph"
        return graphBuilder.build(outputs)
    }

    private fun collectRelevantFiles(project: Project): List<VirtualFile> {
        val scope = GlobalSearchScope.projectScope(project)
        val fileIndex = ProjectFileIndex.getInstance(project)

        val xmlFiles = FilenameIndex.getAllFilesByExt(project, "xml", scope)
        val kotlinFiles = FilenameIndex.getAllFilesByExt(project, "kt", scope)
        val javaFiles = FilenameIndex.getAllFilesByExt(project, "java", scope)

        return (xmlFiles + kotlinFiles + javaFiles)
            .asSequence()
            .filter { fileIndex.isInContent(it) && !fileIndex.isExcluded(it) }
            .filterNot { it.path.contains("/build/") || it.path.contains("/test/") || it.path.contains("/androidTest/") }
            .filter { file ->
                when (file.extension) {
                    "xml" -> file.path.contains("/res/navigation/")
                    "kt", "java" -> true
                    else -> false
                }
            }
            .distinctBy { it.path }
            .sortedBy { it.path }
            .toList()
    }
}
