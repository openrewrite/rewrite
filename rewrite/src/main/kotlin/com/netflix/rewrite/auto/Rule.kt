package com.netflix.rewrite.auto

import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.refactor.Refactor

interface Rule {
    fun refactor(cu: Tr.CompilationUnit): Refactor
}