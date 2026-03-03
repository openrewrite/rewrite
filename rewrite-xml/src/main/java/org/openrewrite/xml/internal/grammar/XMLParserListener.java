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
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link XMLParser}.
 */
public interface XMLParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link XMLParser#document}.
	 * @param ctx the parse tree
	 */
	void enterDocument(XMLParser.DocumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#document}.
	 * @param ctx the parse tree
	 */
	void exitDocument(XMLParser.DocumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#prolog}.
	 * @param ctx the parse tree
	 */
	void enterProlog(XMLParser.PrologContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#prolog}.
	 * @param ctx the parse tree
	 */
	void exitProlog(XMLParser.PrologContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#xmldecl}.
	 * @param ctx the parse tree
	 */
	void enterXmldecl(XMLParser.XmldeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#xmldecl}.
	 * @param ctx the parse tree
	 */
	void exitXmldecl(XMLParser.XmldeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#misc}.
	 * @param ctx the parse tree
	 */
	void enterMisc(XMLParser.MiscContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#misc}.
	 * @param ctx the parse tree
	 */
	void exitMisc(XMLParser.MiscContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#doctypedecl}.
	 * @param ctx the parse tree
	 */
	void enterDoctypedecl(XMLParser.DoctypedeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#doctypedecl}.
	 * @param ctx the parse tree
	 */
	void exitDoctypedecl(XMLParser.DoctypedeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#intsubset}.
	 * @param ctx the parse tree
	 */
	void enterIntsubset(XMLParser.IntsubsetContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#intsubset}.
	 * @param ctx the parse tree
	 */
	void exitIntsubset(XMLParser.IntsubsetContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#markupdecl}.
	 * @param ctx the parse tree
	 */
	void enterMarkupdecl(XMLParser.MarkupdeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#markupdecl}.
	 * @param ctx the parse tree
	 */
	void exitMarkupdecl(XMLParser.MarkupdeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#declSep}.
	 * @param ctx the parse tree
	 */
	void enterDeclSep(XMLParser.DeclSepContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#declSep}.
	 * @param ctx the parse tree
	 */
	void exitDeclSep(XMLParser.DeclSepContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#externalid}.
	 * @param ctx the parse tree
	 */
	void enterExternalid(XMLParser.ExternalidContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#externalid}.
	 * @param ctx the parse tree
	 */
	void exitExternalid(XMLParser.ExternalidContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#processinginstruction}.
	 * @param ctx the parse tree
	 */
	void enterProcessinginstruction(XMLParser.ProcessinginstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#processinginstruction}.
	 * @param ctx the parse tree
	 */
	void exitProcessinginstruction(XMLParser.ProcessinginstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#content}.
	 * @param ctx the parse tree
	 */
	void enterContent(XMLParser.ContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#content}.
	 * @param ctx the parse tree
	 */
	void exitContent(XMLParser.ContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#element}.
	 * @param ctx the parse tree
	 */
	void enterElement(XMLParser.ElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#element}.
	 * @param ctx the parse tree
	 */
	void exitElement(XMLParser.ElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#jspdirective}.
	 * @param ctx the parse tree
	 */
	void enterJspdirective(XMLParser.JspdirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#jspdirective}.
	 * @param ctx the parse tree
	 */
	void exitJspdirective(XMLParser.JspdirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#jspscriptlet}.
	 * @param ctx the parse tree
	 */
	void enterJspscriptlet(XMLParser.JspscriptletContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#jspscriptlet}.
	 * @param ctx the parse tree
	 */
	void exitJspscriptlet(XMLParser.JspscriptletContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#jspexpression}.
	 * @param ctx the parse tree
	 */
	void enterJspexpression(XMLParser.JspexpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#jspexpression}.
	 * @param ctx the parse tree
	 */
	void exitJspexpression(XMLParser.JspexpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#jspdeclaration}.
	 * @param ctx the parse tree
	 */
	void enterJspdeclaration(XMLParser.JspdeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#jspdeclaration}.
	 * @param ctx the parse tree
	 */
	void exitJspdeclaration(XMLParser.JspdeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#jspcomment}.
	 * @param ctx the parse tree
	 */
	void enterJspcomment(XMLParser.JspcommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#jspcomment}.
	 * @param ctx the parse tree
	 */
	void exitJspcomment(XMLParser.JspcommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#reference}.
	 * @param ctx the parse tree
	 */
	void enterReference(XMLParser.ReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#reference}.
	 * @param ctx the parse tree
	 */
	void exitReference(XMLParser.ReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#attribute}.
	 * @param ctx the parse tree
	 */
	void enterAttribute(XMLParser.AttributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#attribute}.
	 * @param ctx the parse tree
	 */
	void exitAttribute(XMLParser.AttributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link XMLParser#chardata}.
	 * @param ctx the parse tree
	 */
	void enterChardata(XMLParser.ChardataContext ctx);
	/**
	 * Exit a parse tree produced by {@link XMLParser#chardata}.
	 * @param ctx the parse tree
	 */
	void exitChardata(XMLParser.ChardataContext ctx);
}
