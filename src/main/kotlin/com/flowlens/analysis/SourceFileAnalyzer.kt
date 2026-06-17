package com.flowlens.analysis

import com.flowlens.domain.AnalyzerOutput
import com.intellij.psi.PsiFile
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType

class SourceFileAnalyzer(
    private val screenDiscoveryAnalyzer: ScreenDiscoveryAnalyzer = ScreenDiscoveryAnalyzer(),
    private val composeNavigationAnalyzer: ComposeNavigationAnalyzer = ComposeNavigationAnalyzer(),
    private val activityNavigationAnalyzer: ActivityNavigationAnalyzer = ActivityNavigationAnalyzer(),
) {
    fun analyze(virtualFile: VirtualFile, psiFile: PsiFile): AnalyzerOutput {
        val uFile = psiFile.toUElementOfType<UFile>()
        val context = SourceAnalysisContext(
            virtualFile = virtualFile,
            psiFile = psiFile,
            uFile = uFile,
            sourceText = psiFile.text,
        )

        return screenDiscoveryAnalyzer.analyze(context) +
            composeNavigationAnalyzer.analyze(context) +
            activityNavigationAnalyzer.analyze(context)
    }
}

