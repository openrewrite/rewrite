package com.netflix.java.refactor.ast

import com.netflix.java.refactor.aspectj.AspectJLexer
import com.netflix.java.refactor.aspectj.AnnotationSignatureParser
import com.sun.tools.javac.tree.JCTree
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class AnnotationParameter(val id: String, val value: String)

class AnnotationMatcher(signature: String) {
    lateinit var match: AnnotationSignatureParser.AnnotationContext
    val logger: Logger = LoggerFactory.getLogger(AnnotationMatcher::class.java)
    init {
        val parser = AnnotationSignatureParser(CommonTokenStream(AspectJLexer(ANTLRInputStream(signature))))
        parser.addErrorListener(object: ANTLRErrorListener {
            override fun reportAmbiguity(recognizer: Parser?, dfa: DFA?, startIndex: Int, stopIndex: Int, exact: Boolean, ambigAlts: BitSet?, configs: ATNConfigSet?) {
                logger.error(
                    "reportAmbiguity",
                    recognizer,
                    dfa,
                    startIndex,
                    stopIndex,
                    exact,
                    ambigAlts,
                    configs
                )
            }

            override fun reportAttemptingFullContext(recognizer: Parser?, dfa: DFA?, startIndex: Int, stopIndex: Int, conflictingAlts: BitSet?, configs: ATNConfigSet?) {
                logger.error(
                    "reportAttemptingFullContext",
                    recognizer,
                    dfa,
                    startIndex,
                    stopIndex,
                    conflictingAlts,
                    configs
                )
            }

            override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?) {
                logger.error(
                    "syntaxError",
                    recognizer,
                    offendingSymbol,
                    line,
                    charPositionInLine,
                    msg
                )
            }

            override fun reportContextSensitivity(recognizer: Parser?, dfa: DFA?, startIndex: Int, stopIndex: Int, prediction: Int, configs: ATNConfigSet?) {
                logger.error(
                    "reportContextSensitivity",
                    recognizer,
                    dfa,
                    startIndex,
                    stopIndex,
                    prediction,
                    configs
                )
            }
        })
        match = parser.annotation()
    }

    fun matchesSingleParameter(annotation: JCTree.JCAnnotation) : Boolean {
        return match.elementValue() == null ||
            (annotation.args[0] as JCTree.JCAssign).rhs.toString() == match.elementValue().text
    }

    fun matchesNamedParameters(annotation: JCTree.JCAnnotation) : Boolean {
        val matchArgs = match
            .elementValuePairs()
            .elementValuePair()
            .map {
                AnnotationParameter(it.Identifier().text, it.elementValue().text)
            }

        return annotation
            .args
            .map {
                AnnotationParameter(
                    (it as JCTree.JCAssign).lhs.toString(),
                    it.rhs.toString()
                )
            }
            .all {
                matchArgs.any { other -> it == other }
            }
    }

    fun matchesAnnotationName(annotation: JCTree.JCAnnotation) : Boolean {
        val annotationType = (annotation.annotationType as JCTree.JCIdent).sym
        return match.annotationName().text == annotationType.toString()

    }

    fun matches(annotation: JCTree.JCAnnotation) : Boolean {
        return matchesAnnotationName(annotation) &&
            matchesSingleParameter(annotation) &&
            matchesNamedParameters(annotation)
    }


}

