/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Generated from java-escape by ANTLR 4.11.1
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link AnnotationSignatureParser}.
 */
public interface AnnotationSignatureParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(AnnotationSignatureParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(AnnotationSignatureParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationName(AnnotationSignatureParser.AnnotationNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationName(AnnotationSignatureParser.AnnotationNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(AnnotationSignatureParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(AnnotationSignatureParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairs(AnnotationSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairs(AnnotationSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePair(AnnotationSignatureParser.ElementValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePair(AnnotationSignatureParser.ElementValuePairContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void enterElementValue(AnnotationSignatureParser.ElementValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void exitElementValue(AnnotationSignatureParser.ElementValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(AnnotationSignatureParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(AnnotationSignatureParser.PrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(AnnotationSignatureParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(AnnotationSignatureParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceType(AnnotationSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceType(AnnotationSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(AnnotationSignatureParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(AnnotationSignatureParser.LiteralContext ctx);
}