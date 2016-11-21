package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.AstTransform
import com.netflix.java.refactor.ast.Formatting
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.Type
import com.netflix.java.refactor.refactor.RefactorVisitor

class ChangeMethodTargetToStatic(val cu: Tr.CompilationUnit, val meth: Tr.MethodInvocation, val clazz: String): RefactorVisitor() {

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        if(meth.id == this.meth.id) {
            val classType = Type.Class.build(cu.typeCache(), clazz)
            return listOf(AstTransform<Tr.MethodInvocation>(cursor()) {
                meth.copy(select = Tr.Ident(classType.className(), classType, meth.select?.formatting ?:
                        Formatting.Reified.Empty), declaringType = classType)
            })
        }
        return super.visitMethodInvocation(meth)
    }
}