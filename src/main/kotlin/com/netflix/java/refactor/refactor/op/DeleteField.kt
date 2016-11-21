package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.refactor.RefactorVisitor

data class DeleteField(val decls: Tr.VariableDecls) : RefactorVisitor() {

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<AstTransform<*>> =
        if(multiVariable.id == decls.id) {
            listOf(AstTransform<Tr.Block<*>>(cursor().parent()) {
                copy(statements = statements - decls)
            })
        } else emptyList()
}