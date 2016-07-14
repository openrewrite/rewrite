package com.netflix.java.refactor

interface RefactorOperation<T> {
    fun scanner(): RefactoringScanner<T>
}

interface FixingOperation: RefactorOperation<List<RefactorFix>>