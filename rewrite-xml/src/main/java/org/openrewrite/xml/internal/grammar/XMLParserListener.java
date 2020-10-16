// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-xml/src/main/antlr/XMLParser.g4 by ANTLR 4.8
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