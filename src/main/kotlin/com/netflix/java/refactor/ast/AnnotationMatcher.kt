package com.netflix.java.refactor.ast

import com.netflix.java.refactor.aspectj.AnnotationSignatureParser
import com.netflix.java.refactor.aspectj.AspectJLexer
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

class AnnotationMatcher(signature: String) {
    val parser = AnnotationSignatureParser(CommonTokenStream(AspectJLexer(ANTLRInputStream(signature))))
    var match = parser.annotation()

    private data class AnnotationParameter(val id: String, val value: String)

    fun matchesSingleParameter(annotation: Tr.Annotation) : Boolean {
        if(match.elementValue() == null)
            return true

        return annotation.args?.args?.firstOrNull()?.let {
            when(it) {
                is Tr.Assign -> it.assignment.printTrimmed() == match.elementValue().text
                is Tr.Literal -> it.valueSource == match.elementValue().text
                else -> false
            }
        } ?: true
    }

    fun matchesNamedParameters(annotation: Tr.Annotation) : Boolean {
        val matchArgs = match
            .elementValuePairs()
            ?.elementValuePair()
            ?.map { AnnotationParameter(it.Identifier().text, it.elementValue().text) } ?: return true

        return annotation
            .args
            ?.args
            ?.map {
                AnnotationParameter(
                    (it as Tr.Assign).variable.printTrimmed(),
                    it.assignment.printTrimmed()
                )
            }
            ?.all {
                matchArgs.any { other -> it == other }
            } ?: false
    }

    fun matchesAnnotationName(annotation: Tr.Annotation) : Boolean {
        return match.annotationName().text == annotation.type.asClass()?.fullyQualifiedName
    }

    fun matches(annotation: Tr.Annotation) : Boolean {
        return matchesAnnotationName(annotation) &&
            matchesSingleParameter(annotation) &&
            matchesNamedParameters(annotation)
    }
}
