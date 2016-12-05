/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.ast

import com.netflix.rewrite.aspectj.AnnotationSignatureParser
import com.netflix.rewrite.aspectj.AspectJLexer
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

class AnnotationMatcher(signature: String) {
    val parser = com.netflix.rewrite.aspectj.AnnotationSignatureParser(CommonTokenStream(com.netflix.rewrite.aspectj.AspectJLexer(ANTLRInputStream(signature))))
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
