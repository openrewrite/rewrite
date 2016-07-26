package com.netflix.java.refactor.ast

import com.netflix.java.refactor.RefactorFix

interface AstScannerBuilder<T> {
    fun scanner(): AstScanner<T>
}

interface RefactoringAstScannerBuilder : AstScannerBuilder<List<RefactorFix>>