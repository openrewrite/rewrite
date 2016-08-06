package com.netflix.java.refactor.find

import com.netflix.java.refactor.ast.AstScannerBuilder
import com.netflix.java.refactor.ast.SingleCompilationUnitAstScanner
import com.sun.source.tree.CompilationUnitTree
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

data class Field(val name: String)

class FindFields(val clazz: String, val includeInherited: Boolean) : AstScannerBuilder<List<Field>> {
    override fun scanner() = FindFieldScanner(this)
}

class FindFieldScanner(val op: FindFields) : SingleCompilationUnitAstScanner<List<Field>>() {
    override fun visitCompilationUnit(node: CompilationUnitTree?, p: Context?): List<Field> {
        super.visitCompilationUnit(node, p)
        return cu.defs.filterIsInstance<JCTree.JCClassDecl>().flatMap { superFields(it.type as Type.ClassType) }
    }

    private fun superFields(type: Type.ClassType, inHierarchy: Boolean = false): List<Field> {
        if (type.supertype_field == Type.noType)
            return emptyList()

        val fields = (type.tsym as Symbol.ClassSymbol).members_field.elements
                .filter { it is Symbol.VarSymbol }
                .filter { it.type.toString() == op.clazz }
                .filter { !inHierarchy || it.flags() and Flags.PRIVATE.toLong() == 0L }
                .map { Field(it.name.toString()) }

        return fields + (
                if (op.includeInherited && type.supertype_field is Type.ClassType)
                    superFields(type.supertype_field as Type.ClassType, true)
                else emptyList())
    }

    override fun reduce(r1: List<Field>?, r2: List<Field>?) = (r1 ?: emptyList()) + (r2 ?: emptyList())
}