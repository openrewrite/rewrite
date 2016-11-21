package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.AnnotationMatcher
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.visitor.AstVisitor

class FindAnnotations(signature: String) : AstVisitor<List<Tr.Annotation>>(emptyList()) {
    private val matcher = AnnotationMatcher(signature)

    override fun visitAnnotation(annotation: Tr.Annotation): List<Tr.Annotation> {
        return if (matcher.matches(annotation)) listOf(annotation) else emptyList()
    }
}
