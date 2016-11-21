package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.visitor.AstVisitor
import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Tr

class FindMethods(signature: String): AstVisitor<List<Tr.MethodInvocation>>(emptyList()) {
    val matcher = MethodMatcher(signature)
    
    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<Tr.MethodInvocation> =
        if(matcher.matches(meth))
            listOf(meth)
        else super.visitMethodInvocation(meth)
}