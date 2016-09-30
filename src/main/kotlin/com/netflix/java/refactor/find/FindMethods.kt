package com.netflix.java.refactor.find

import com.netflix.java.refactor.ast.AstScannerBuilder
import com.netflix.java.refactor.ast.MethodMatcher
import com.netflix.java.refactor.ast.SingleCompilationUnitAstScanner
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

data class Method(val name: String, val source: String, val args: List<Argument>)
data class Argument(val source: String)

class FindMethods(signature: String): AstScannerBuilder<List<Method>> {
    val matcher = MethodMatcher(signature)
    
    override fun scanner() = FindMethodScanner(this)
}

class FindMethodScanner(val op: FindMethods): SingleCompilationUnitAstScanner<List<Method>>() {

    override fun visitMethodInvocation(node: MethodInvocationTree, context: Context): List<Method>? {
        val invocation = node as JCTree.JCMethodInvocation
        if(op.matcher.matches(invocation)) {
            return listOf(Method(invocation.meth.toString(), invocation.source(), invocation.args.map { Argument(it.source()) }))
        }
        return super.visitMethodInvocation(node, context)
    }

    override fun reduce(r1: List<Method>?, r2: List<Method>?) = (r1 ?: emptyList()).plus(r2 ?: emptyList())
}