/*
 * Copyright 2026 the original author or authors.
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
// Generated from rewrite-xml/src/main/antlr/XMLParser.g4 by ANTLR 4.13.2
package org.openrewrite.xml.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link XMLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface XMLParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link XMLParser#document}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDocument(XMLParser.DocumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#prolog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProlog(XMLParser.PrologContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#xmldecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmldecl(XMLParser.XmldeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#misc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMisc(XMLParser.MiscContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#doctypedecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoctypedecl(XMLParser.DoctypedeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#intsubset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntsubset(XMLParser.IntsubsetContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#markupdecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMarkupdecl(XMLParser.MarkupdeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#declSep}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclSep(XMLParser.DeclSepContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#externalid}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternalid(XMLParser.ExternalidContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#processinginstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcessinginstruction(XMLParser.ProcessinginstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#content}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContent(XMLParser.ContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElement(XMLParser.ElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#jspdirective}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJspdirective(XMLParser.JspdirectiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#jspscriptlet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJspscriptlet(XMLParser.JspscriptletContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#jspexpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJspexpression(XMLParser.JspexpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#jspdeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJspdeclaration(XMLParser.JspdeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#jspcomment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJspcomment(XMLParser.JspcommentContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#reference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReference(XMLParser.ReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#attribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute(XMLParser.AttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link XMLParser#chardata}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChardata(XMLParser.ChardataContext ctx);
}
