package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.Type
import com.netflix.java.refactor.refactor.RefactorVisitor

data class ChangeFieldType(val cu: Tr.CompilationUnit, val decls: Tr.VariableDecls, val targetType: String) : RefactorVisitor() {

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<AstTransform<*>> {
        if(multiVariable.id == decls.id) {
            val classType = Type.Class.build(cu.typeCache(), targetType)
            if(decls.typeExpr.type != classType) {
                return listOf(AstTransform<Tr.VariableDecls>(cursor()) {
                    decls.copy(typeExpr = Tr.Ident(classType.className(), classType, decls.typeExpr.formatting),
                            vars = decls.vars.map { it.copy(type = classType, name = it.name.copy(type = classType)) })
                })
            }
        }
        return emptyList()
    }
}