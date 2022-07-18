// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-cobol/src/main/antlr/CobolPreprocessor.g4 by ANTLR 4.9.3
package org.openrewrite.cobol.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CobolPreprocessorParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CobolPreprocessorVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#startRule}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartRule(CobolPreprocessorParser.StartRuleContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#compilerOptions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilerOptions(CobolPreprocessorParser.CompilerOptionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#compilerXOpts}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilerXOpts(CobolPreprocessorParser.CompilerXOptsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#compilerOption}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilerOption(CobolPreprocessorParser.CompilerOptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#execCicsStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecCicsStatement(CobolPreprocessorParser.ExecCicsStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#execSqlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecSqlStatement(CobolPreprocessorParser.ExecSqlStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#execSqlImsStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecSqlImsStatement(CobolPreprocessorParser.ExecSqlImsStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#copyStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopyStatement(CobolPreprocessorParser.CopyStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#copySource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopySource(CobolPreprocessorParser.CopySourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#copyLibrary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopyLibrary(CobolPreprocessorParser.CopyLibraryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#replacingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplacingPhrase(CobolPreprocessorParser.ReplacingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#replaceArea}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceArea(CobolPreprocessorParser.ReplaceAreaContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#replaceByStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceByStatement(CobolPreprocessorParser.ReplaceByStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#replaceOffStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceOffStatement(CobolPreprocessorParser.ReplaceOffStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#replaceClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceClause(CobolPreprocessorParser.ReplaceClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#directoryPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectoryPhrase(CobolPreprocessorParser.DirectoryPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#familyPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFamilyPhrase(CobolPreprocessorParser.FamilyPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#replaceable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceable(CobolPreprocessorParser.ReplaceableContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#replacement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplacement(CobolPreprocessorParser.ReplacementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#ejectStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEjectStatement(CobolPreprocessorParser.EjectStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#skipStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkipStatement(CobolPreprocessorParser.SkipStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#titleStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTitleStatement(CobolPreprocessorParser.TitleStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#pseudoText}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoText(CobolPreprocessorParser.PseudoTextContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#charData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharData(CobolPreprocessorParser.CharDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#charDataSql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharDataSql(CobolPreprocessorParser.CharDataSqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#charDataLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharDataLine(CobolPreprocessorParser.CharDataLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#cobolWord}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCobolWord(CobolPreprocessorParser.CobolWordContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(CobolPreprocessorParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#filename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilename(CobolPreprocessorParser.FilenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CobolPreprocessorParser#charDataKeyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharDataKeyword(CobolPreprocessorParser.CharDataKeywordContext ctx);
}