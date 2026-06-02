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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * This class provides an empty implementation of {@link XMLParserListener},
 * which can be extended to create a listener which only needs to handle a subset
 * of the available methods.
 */
@SuppressWarnings("CheckReturnValue")
public class XMLParserBaseListener implements XMLParserListener {
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterDocument(XMLParser.DocumentContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitDocument(XMLParser.DocumentContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterProlog(XMLParser.PrologContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitProlog(XMLParser.PrologContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterXmldecl(XMLParser.XmldeclContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitXmldecl(XMLParser.XmldeclContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterMisc(XMLParser.MiscContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitMisc(XMLParser.MiscContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterDoctypedecl(XMLParser.DoctypedeclContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitDoctypedecl(XMLParser.DoctypedeclContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterIntsubset(XMLParser.IntsubsetContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitIntsubset(XMLParser.IntsubsetContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterMarkupdecl(XMLParser.MarkupdeclContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitMarkupdecl(XMLParser.MarkupdeclContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterDeclSep(XMLParser.DeclSepContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitDeclSep(XMLParser.DeclSepContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterExternalid(XMLParser.ExternalidContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitExternalid(XMLParser.ExternalidContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterProcessinginstruction(XMLParser.ProcessinginstructionContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitProcessinginstruction(XMLParser.ProcessinginstructionContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterContent(XMLParser.ContentContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitContent(XMLParser.ContentContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterElement(XMLParser.ElementContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitElement(XMLParser.ElementContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterJspdirective(XMLParser.JspdirectiveContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitJspdirective(XMLParser.JspdirectiveContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterJspscriptlet(XMLParser.JspscriptletContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitJspscriptlet(XMLParser.JspscriptletContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterJspexpression(XMLParser.JspexpressionContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitJspexpression(XMLParser.JspexpressionContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterJspdeclaration(XMLParser.JspdeclarationContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitJspdeclaration(XMLParser.JspdeclarationContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterJspcomment(XMLParser.JspcommentContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitJspcomment(XMLParser.JspcommentContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterReference(XMLParser.ReferenceContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitReference(XMLParser.ReferenceContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterAttribute(XMLParser.AttributeContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitAttribute(XMLParser.AttributeContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterChardata(XMLParser.ChardataContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitChardata(XMLParser.ChardataContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterEveryRule(ParserRuleContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitEveryRule(ParserRuleContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void visitTerminal(TerminalNode node) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void visitErrorNode(ErrorNode node) { }
}
