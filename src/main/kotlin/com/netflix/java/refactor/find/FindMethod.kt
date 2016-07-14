package com.netflix.java.refactor.find

import com.netflix.java.refactor.BaseRefactoringScanner
import com.netflix.java.refactor.MethodMatcher
import com.netflix.java.refactor.RefactorOperation
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

data class Method(val name: String?) {
    companion object {
        val NO_METHOD = Method(null)
    }
    val exists = name is String
}

class FindMethod(signature: String): RefactorOperation<Method> {
    val matcher = MethodMatcher(signature)
    
    override fun scanner() = FindMethodScanner(this)
}

class FindMethodScanner(val op: FindMethod): BaseRefactoringScanner<Method>() {

    override fun visitMethodInvocation(node: MethodInvocationTree, context: Context): Method? {
        val invocation = node as JCTree.JCMethodInvocation
        if(op.matcher.matches(invocation)) {
            return Method(invocation.meth.toString())
        }
        return super.visitMethodInvocation(node, context)
    }
    
    override fun reduce(r1: Method?, r2: Method?): Method =
            r1 ?: r2 ?: Method.NO_METHOD
}