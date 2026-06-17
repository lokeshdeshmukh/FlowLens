package com.flowlens.analysis

import com.flowlens.util.SourceTextParsers
import com.intellij.psi.PsiFile
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.uast.UFile

data class SourceAnalysisContext(
    val virtualFile: VirtualFile,
    val psiFile: PsiFile,
    val uFile: UFile?,
    val sourceText: String,
) {
    val filePath: String = virtualFile.path
    val relatedViewModels: Set<String> by lazy { SourceTextParsers.extractViewModels(sourceText) }
    val relatedRepositories: Set<String> by lazy { SourceTextParsers.extractRepositories(sourceText) }
}

