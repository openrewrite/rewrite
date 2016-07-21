package com.netflix.java.refactor.ast

import com.netflix.java.refactor.RefactorFix

interface RefactorOperation<T> {
    fun scanner(): RefactoringScanner<T>
}

interface FixingOperation: RefactorOperation<List<RefactorFix>>