package com.netflix.java.refactor.find

import com.netflix.java.refactor.ast.AnnotationMatcher
import com.netflix.java.refactor.ast.AstScannerBuilder
import com.netflix.java.refactor.ast.SingleCompilationUnitAstScanner
import com.sun.source.tree.AnnotationTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context

data class Annotation(val name: String, val ast: AnnotationTree)

class FindAnnotations(signature: String) : AstScannerBuilder<List<Annotation>> {
    val matcher = AnnotationMatcher(signature)
    override fun scanner() = FindAnnotationScanner(this)
}

class FindAnnotationScanner(val op: FindAnnotations) : SingleCompilationUnitAstScanner<List<Annotation>>() {
    override fun visitAnnotation(annotation: AnnotationTree, context: Context) : List<Annotation> {
        val type = (annotation as JCTree.JCAnnotation).annotationType
        val name = (type as JCTree.JCIdent).sym.toString()
        return if (op.matcher.matches(annotation)) {
            listOf(Annotation(name, annotation))
        } else {
            emptyList<Annotation>()
        }
    }

    override fun reduce(r1: List<Annotation>?, r2: List<Annotation>?) = (r1 ?: emptyList()).plus(r2 ?: emptyList())
}
