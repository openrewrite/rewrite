package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.ast.visitor.AstVisitor

class FindInheritedFields(val fullyQualifiedClassName: String): AstVisitor<List<Type.Var>>(emptyList()) {
    override fun visitClassDecl(classDecl: Tr.ClassDecl): List<Type.Var> {
        return superFields(classDecl.type.asClass()?.supertype)
    }

    private fun superFields(type: Type.Class?): List<Type.Var> {
        if (type == null)
            return emptyList()

        if (type.supertype == null)
            return emptyList()

        return type.members.filter { !it.hasFlags(Type.Var.Flags.Private) && it.type.hasElementType(fullyQualifiedClassName) } +
                superFields(type.supertype)
    }
}