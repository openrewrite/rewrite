// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-cobol/src/main/antlr/CobolPreprocessor.g4 by ANTLR 4.9.3
package org.openrewrite.cobol.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CobolPreprocessorParser}.
 */
public interface CobolPreprocessorListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#startRule}.
	 * @param ctx the parse tree
	 */
	void enterStartRule(CobolPreprocessorParser.StartRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#startRule}.
	 * @param ctx the parse tree
	 */
	void exitStartRule(CobolPreprocessorParser.StartRuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#compilerOptions}.
	 * @param ctx the parse tree
	 */
	void enterCompilerOptions(CobolPreprocessorParser.CompilerOptionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#compilerOptions}.
	 * @param ctx the parse tree
	 */
	void exitCompilerOptions(CobolPreprocessorParser.CompilerOptionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#compilerXOpts}.
	 * @param ctx the parse tree
	 */
	void enterCompilerXOpts(CobolPreprocessorParser.CompilerXOptsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#compilerXOpts}.
	 * @param ctx the parse tree
	 */
	void exitCompilerXOpts(CobolPreprocessorParser.CompilerXOptsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#compilerOption}.
	 * @param ctx the parse tree
	 */
	void enterCompilerOption(CobolPreprocessorParser.CompilerOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#compilerOption}.
	 * @param ctx the parse tree
	 */
	void exitCompilerOption(CobolPreprocessorParser.CompilerOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#execCicsStatement}.
	 * @param ctx the parse tree
	 */
	void enterExecCicsStatement(CobolPreprocessorParser.ExecCicsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#execCicsStatement}.
	 * @param ctx the parse tree
	 */
	void exitExecCicsStatement(CobolPreprocessorParser.ExecCicsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#execSqlStatement}.
	 * @param ctx the parse tree
	 */
	void enterExecSqlStatement(CobolPreprocessorParser.ExecSqlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#execSqlStatement}.
	 * @param ctx the parse tree
	 */
	void exitExecSqlStatement(CobolPreprocessorParser.ExecSqlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#execSqlImsStatement}.
	 * @param ctx the parse tree
	 */
	void enterExecSqlImsStatement(CobolPreprocessorParser.ExecSqlImsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#execSqlImsStatement}.
	 * @param ctx the parse tree
	 */
	void exitExecSqlImsStatement(CobolPreprocessorParser.ExecSqlImsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#copyStatement}.
	 * @param ctx the parse tree
	 */
	void enterCopyStatement(CobolPreprocessorParser.CopyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#copyStatement}.
	 * @param ctx the parse tree
	 */
	void exitCopyStatement(CobolPreprocessorParser.CopyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#copySource}.
	 * @param ctx the parse tree
	 */
	void enterCopySource(CobolPreprocessorParser.CopySourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#copySource}.
	 * @param ctx the parse tree
	 */
	void exitCopySource(CobolPreprocessorParser.CopySourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#copyLibrary}.
	 * @param ctx the parse tree
	 */
	void enterCopyLibrary(CobolPreprocessorParser.CopyLibraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#copyLibrary}.
	 * @param ctx the parse tree
	 */
	void exitCopyLibrary(CobolPreprocessorParser.CopyLibraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#replacingPhrase}.
	 * @param ctx the parse tree
	 */
	void enterReplacingPhrase(CobolPreprocessorParser.ReplacingPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#replacingPhrase}.
	 * @param ctx the parse tree
	 */
	void exitReplacingPhrase(CobolPreprocessorParser.ReplacingPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#replaceArea}.
	 * @param ctx the parse tree
	 */
	void enterReplaceArea(CobolPreprocessorParser.ReplaceAreaContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#replaceArea}.
	 * @param ctx the parse tree
	 */
	void exitReplaceArea(CobolPreprocessorParser.ReplaceAreaContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#replaceByStatement}.
	 * @param ctx the parse tree
	 */
	void enterReplaceByStatement(CobolPreprocessorParser.ReplaceByStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#replaceByStatement}.
	 * @param ctx the parse tree
	 */
	void exitReplaceByStatement(CobolPreprocessorParser.ReplaceByStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#replaceOffStatement}.
	 * @param ctx the parse tree
	 */
	void enterReplaceOffStatement(CobolPreprocessorParser.ReplaceOffStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#replaceOffStatement}.
	 * @param ctx the parse tree
	 */
	void exitReplaceOffStatement(CobolPreprocessorParser.ReplaceOffStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#replaceClause}.
	 * @param ctx the parse tree
	 */
	void enterReplaceClause(CobolPreprocessorParser.ReplaceClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#replaceClause}.
	 * @param ctx the parse tree
	 */
	void exitReplaceClause(CobolPreprocessorParser.ReplaceClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#directoryPhrase}.
	 * @param ctx the parse tree
	 */
	void enterDirectoryPhrase(CobolPreprocessorParser.DirectoryPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#directoryPhrase}.
	 * @param ctx the parse tree
	 */
	void exitDirectoryPhrase(CobolPreprocessorParser.DirectoryPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#familyPhrase}.
	 * @param ctx the parse tree
	 */
	void enterFamilyPhrase(CobolPreprocessorParser.FamilyPhraseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#familyPhrase}.
	 * @param ctx the parse tree
	 */
	void exitFamilyPhrase(CobolPreprocessorParser.FamilyPhraseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#replaceable}.
	 * @param ctx the parse tree
	 */
	void enterReplaceable(CobolPreprocessorParser.ReplaceableContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#replaceable}.
	 * @param ctx the parse tree
	 */
	void exitReplaceable(CobolPreprocessorParser.ReplaceableContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#replacement}.
	 * @param ctx the parse tree
	 */
	void enterReplacement(CobolPreprocessorParser.ReplacementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#replacement}.
	 * @param ctx the parse tree
	 */
	void exitReplacement(CobolPreprocessorParser.ReplacementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#ejectStatement}.
	 * @param ctx the parse tree
	 */
	void enterEjectStatement(CobolPreprocessorParser.EjectStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#ejectStatement}.
	 * @param ctx the parse tree
	 */
	void exitEjectStatement(CobolPreprocessorParser.EjectStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#skipStatement}.
	 * @param ctx the parse tree
	 */
	void enterSkipStatement(CobolPreprocessorParser.SkipStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#skipStatement}.
	 * @param ctx the parse tree
	 */
	void exitSkipStatement(CobolPreprocessorParser.SkipStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#titleStatement}.
	 * @param ctx the parse tree
	 */
	void enterTitleStatement(CobolPreprocessorParser.TitleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#titleStatement}.
	 * @param ctx the parse tree
	 */
	void exitTitleStatement(CobolPreprocessorParser.TitleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#pseudoText}.
	 * @param ctx the parse tree
	 */
	void enterPseudoText(CobolPreprocessorParser.PseudoTextContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#pseudoText}.
	 * @param ctx the parse tree
	 */
	void exitPseudoText(CobolPreprocessorParser.PseudoTextContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#charData}.
	 * @param ctx the parse tree
	 */
	void enterCharData(CobolPreprocessorParser.CharDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#charData}.
	 * @param ctx the parse tree
	 */
	void exitCharData(CobolPreprocessorParser.CharDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#charDataSql}.
	 * @param ctx the parse tree
	 */
	void enterCharDataSql(CobolPreprocessorParser.CharDataSqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#charDataSql}.
	 * @param ctx the parse tree
	 */
	void exitCharDataSql(CobolPreprocessorParser.CharDataSqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#charDataLine}.
	 * @param ctx the parse tree
	 */
	void enterCharDataLine(CobolPreprocessorParser.CharDataLineContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#charDataLine}.
	 * @param ctx the parse tree
	 */
	void exitCharDataLine(CobolPreprocessorParser.CharDataLineContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#cobolWord}.
	 * @param ctx the parse tree
	 */
	void enterCobolWord(CobolPreprocessorParser.CobolWordContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#cobolWord}.
	 * @param ctx the parse tree
	 */
	void exitCobolWord(CobolPreprocessorParser.CobolWordContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(CobolPreprocessorParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(CobolPreprocessorParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#filename}.
	 * @param ctx the parse tree
	 */
	void enterFilename(CobolPreprocessorParser.FilenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#filename}.
	 * @param ctx the parse tree
	 */
	void exitFilename(CobolPreprocessorParser.FilenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CobolPreprocessorParser#charDataKeyword}.
	 * @param ctx the parse tree
	 */
	void enterCharDataKeyword(CobolPreprocessorParser.CharDataKeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link CobolPreprocessorParser#charDataKeyword}.
	 * @param ctx the parse tree
	 */
	void exitCharDataKeyword(CobolPreprocessorParser.CharDataKeywordContext ctx);
}