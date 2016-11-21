package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.visitor.AstVisitor
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.Type

class HasType(val clazz: String): AstVisitor<Boolean>(false) {

    override fun visitIdentifier(ident: Tr.Ident): Boolean =
        ident.type is Type.Class && ident.type.fullyQualifiedName == clazz

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): Boolean {
        if(meth.firstMethodInChain().select == null) {
            // either a same-class instance method or a statically imported method
            return meth.declaringType?.fullyQualifiedName == clazz
        }
        return super.visitMethodInvocation(meth)
    }
}