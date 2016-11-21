package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.ast.visitor.AstVisitor

class FindFields(val fullyQualifiedName: String) : AstVisitor<List<Tr.VariableDecls>>(emptyList()) {

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<Tr.VariableDecls> {
        return if(multiVariable.typeExpr.type.hasElementType(fullyQualifiedName))
            listOf(multiVariable)
        else emptyList()
    }
}