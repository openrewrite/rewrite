package com.netflix.java.refactor.refactor

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Tree
import com.netflix.java.refactor.ast.visitor.AstVisitor

abstract class RefactorVisitor: AstVisitor<List<AstTransform<*>>>(emptyList())