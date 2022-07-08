/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.document.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.cobol.internal.grammar.CobolPreprocessorBaseListener;
import org.openrewrite.cobol.internal.grammar.CobolPreprocessorParser;
import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;
import org.openrewrite.cobol.internal.preprocessor.exception.CobolPreprocessorException;
import org.openrewrite.cobol.internal.preprocessor.impl.CobolPreprocessorImpl;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;
import org.openrewrite.cobol.internal.preprocessor.sub.copybook.CobolWordCopyBookFinder;
import org.openrewrite.cobol.internal.preprocessor.sub.copybook.FilenameCopyBookFinder;
import org.openrewrite.cobol.internal.preprocessor.sub.copybook.LiteralCopyBookFinder;
import org.openrewrite.cobol.internal.preprocessor.sub.copybook.impl.CobolWordCopyBookFinderImpl;
import org.openrewrite.cobol.internal.preprocessor.sub.copybook.impl.FilenameCopyBookFinderImpl;
import org.openrewrite.cobol.internal.preprocessor.sub.copybook.impl.LiteralCopyBookFinderImpl;
import org.openrewrite.cobol.internal.preprocessor.sub.document.CobolDocumentParserListener;
import org.openrewrite.cobol.internal.preprocessor.sub.util.TokenUtils;

/**
 * ANTLR visitor, which preprocesses a given COBOL program by executing COPY and
 * REPLACE statements.
 */
public class CobolDocumentParserListenerImpl extends CobolPreprocessorBaseListener
		implements CobolDocumentParserListener {

	// private final static Logger LOG = LoggerFactory.getLogger(CobolDocumentParserListenerImpl.class);

	private final Stack<CobolDocumentContext> contexts = new Stack<CobolDocumentContext>();

	private final CobolParserParams params;

	private final BufferedTokenStream tokens;

	public CobolDocumentParserListenerImpl(final CobolParserParams params, final BufferedTokenStream tokens) {
		this.params = params;
		this.tokens = tokens;

		contexts.push(new CobolDocumentContext());
	}

	protected String buildLines(final String text, final String linePrefix) {
		final StringBuffer sb = new StringBuffer(text.length());
		final Scanner scanner = new Scanner(text);
		boolean firstLine = true;

		while (scanner.hasNextLine()) {
			if (!firstLine) {
				sb.append(CobolPreprocessor.NEWLINE);
			}

			final String line = scanner.nextLine();
			final String trimmedLine = line.trim();
			final String prefixedLine = linePrefix + CobolPreprocessor.WS + trimmedLine;
			final String suffixedLine = prefixedLine.replaceAll("(?i)(end-exec)",
					"$1 " + CobolPreprocessor.EXEC_END_TAG);

			sb.append(suffixedLine);
			firstLine = false;
		}

		scanner.close();
		return sb.toString();
	}

	@Override
	public CobolDocumentContext context() {
		return contexts.peek();
	}

	protected CobolWordCopyBookFinder createCobolWordCopyBookFinder() {
		return new CobolWordCopyBookFinderImpl();
	}

	protected FilenameCopyBookFinder createFilenameCopyBookFinder() {
		return new FilenameCopyBookFinderImpl();
	}

	protected LiteralCopyBookFinder createLiteralCopyBookFinder() {
		return new LiteralCopyBookFinderImpl();
	}

	@Override
	public void enterCompilerOptions(final CobolPreprocessorParser.CompilerOptionsContext ctx) {
		// push a new context for COMPILER OPTIONS terminals
		push();
	}

	@Override
	public void enterCopyStatement(final CobolPreprocessorParser.CopyStatementContext ctx) {
		// push a new context for COPY terminals
		push();
	}

	@Override
	public void enterEjectStatement(final CobolPreprocessorParser.EjectStatementContext ctx) {
		push();
	}

	@Override
	public void enterExecCicsStatement(final CobolPreprocessorParser.ExecCicsStatementContext ctx) {
		// push a new context for SQL terminals
		push();
	}

	@Override
	public void enterExecSqlImsStatement(final CobolPreprocessorParser.ExecSqlImsStatementContext ctx) {
		// push a new context for SQL IMS terminals
		push();
	}

	@Override
	public void enterExecSqlStatement(final CobolPreprocessorParser.ExecSqlStatementContext ctx) {
		// push a new context for SQL terminals
		push();
	}

	@Override
	public void enterReplaceArea(final CobolPreprocessorParser.ReplaceAreaContext ctx) {
		push();
	}

	@Override
	public void enterReplaceByStatement(final CobolPreprocessorParser.ReplaceByStatementContext ctx) {
		push();
	}

	@Override
	public void enterReplaceOffStatement(final CobolPreprocessorParser.ReplaceOffStatementContext ctx) {
		push();
	}

	@Override
	public void enterSkipStatement(final CobolPreprocessorParser.SkipStatementContext ctx) {
		push();
	}

	@Override
	public void enterTitleStatement(final CobolPreprocessorParser.TitleStatementContext ctx) {
		push();
	}

	@Override
	public void exitCompilerOptions(final CobolPreprocessorParser.CompilerOptionsContext ctx) {
		// throw away COMPILER OPTIONS terminals
		pop();
	}

	@Override
	public void exitCopyStatement(final CobolPreprocessorParser.CopyStatementContext ctx) {
		// throw away COPY terminals
		//XXX
		pop();

		// a new context for the copy book content
		push();

		/*
		 * replacement phrase
		 */
		for (final CobolPreprocessorParser.ReplacingPhraseContext replacingPhrase : ctx.replacingPhrase()) {
			context().storeReplaceablesAndReplacements(replacingPhrase.replaceClause());
		}

		/*
		 * copy the copy book
		 */
		final CobolPreprocessorParser.CopySourceContext copySource = ctx.copySource();
		final String copyBookContent = getCopyBookContent(copySource, params);

		if (copyBookContent != null) {
			// Copy the raw copybook to the current context
			context().write(copyBookContent + CobolPreprocessor.NEWLINE);
			// If COPY .. REPLACING ..., replace in the current context
			context().replaceReplaceablesByReplacements(tokens);
		}

		final String content = context().read();
		pop();

		context().write(content);
	}

	@Override
	public void exitEjectStatement(final CobolPreprocessorParser.EjectStatementContext ctx) {
		// throw away eject statement
		pop();
	}

	@Override
	public void exitExecCicsStatement(final CobolPreprocessorParser.ExecCicsStatementContext ctx) {
		// throw away EXEC CICS terminals
		pop();

		// a new context for the CICS statement
		push();

		/*
		 * text
		 */
		final String text = TokenUtils.getTextIncludingHiddenTokens(ctx, tokens);
		final String linePrefix = CobolLine.createBlankSequenceArea(params.getFormat())
				+ CobolPreprocessor.EXEC_CICS_TAG;
		final String lines = buildLines(text, linePrefix);

		context().write(lines);

		final String content = context().read();
		pop();

		context().write(content);
	}

	@Override
	public void exitExecSqlImsStatement(final CobolPreprocessorParser.ExecSqlImsStatementContext ctx) {
		// throw away EXEC SQLIMS terminals
		pop();

		// a new context for the SQLIMS statement
		push();

		/*
		 * text
		 */
		final String text = TokenUtils.getTextIncludingHiddenTokens(ctx, tokens);
		final String linePrefix = CobolLine.createBlankSequenceArea(params.getFormat())
				+ CobolPreprocessor.EXEC_SQLIMS_TAG;
		final String lines = buildLines(text, linePrefix);

		context().write(lines);

		final String content = context().read();
		pop();

		context().write(content);
	}

	@Override
	public void exitExecSqlStatement(final CobolPreprocessorParser.ExecSqlStatementContext ctx) {
		// throw away EXEC SQL terminals
		pop();

		// a new context for the SQL statement
		push();

		/*
		 * text
		 */
		final String text = TokenUtils.getTextIncludingHiddenTokens(ctx, tokens);
		final String linePrefix = CobolLine.createBlankSequenceArea(params.getFormat())
				+ CobolPreprocessor.EXEC_SQL_TAG;
		final String lines = buildLines(text, linePrefix);

		context().write(lines);

		final String content = context().read();
		pop();

		context().write(content);
	}

	@Override
	public void exitReplaceArea(final CobolPreprocessorParser.ReplaceAreaContext ctx) {
		/*
		 * replacement phrase
		 */
		final List<CobolPreprocessorParser.ReplaceClauseContext> replaceClauses = ctx.replaceByStatement().replaceClause();
		context().storeReplaceablesAndReplacements(replaceClauses);

		context().replaceReplaceablesByReplacements(tokens);
		final String content = context().read();

		pop();
		context().write(content);
	}

	@Override
	public void exitReplaceByStatement(final CobolPreprocessorParser.ReplaceByStatementContext ctx) {
		// throw away terminals
		pop();
	}

	@Override
	public void exitReplaceOffStatement(final CobolPreprocessorParser.ReplaceOffStatementContext ctx) {
		// throw away REPLACE OFF terminals
		pop();
	}

	@Override
	public void exitSkipStatement(final CobolPreprocessorParser.SkipStatementContext ctx) {
		// throw away skip statement
		pop();
	}

	@Override
	public void exitTitleStatement(final CobolPreprocessorParser.TitleStatementContext ctx) {
		// throw away title statement
		pop();
	}

	protected File findCopyBook(final CobolPreprocessorParser.CopySourceContext copySource, final CobolParserParams params) {
		final File result;

		if (copySource.cobolWord() != null) {
			result = createCobolWordCopyBookFinder().findCopyBook(params, copySource.cobolWord());
		} else if (copySource.literal() != null) {
			result = createLiteralCopyBookFinder().findCopyBook(params, copySource.literal());
		} else if (copySource.filename() != null) {
			result = createFilenameCopyBookFinder().findCopyBook(params, copySource.filename());
		} else {
			// LOG.warn("unknown copy book reference type {}", copySource);
			result = null;
		}

		return result;
	}

	protected String getCopyBookContent(final CobolPreprocessorParser.CopySourceContext copySource, final CobolParserParams params) {
		final File copyBook = findCopyBook(copySource, params);
		String result;

		if (copyBook == null) {
			throw new CobolPreprocessorException("Could not find copy book " + copySource.getText()
					+ " in directory of COBOL input file or copy books param object.");
		} else {
			try {
				result = new CobolPreprocessorImpl().process(copyBook, params);
			} catch (final IOException e) {
				result = null;
				// LOG.warn(e.getMessage());
			}
		}

		return result;
	}

	/**
	 * Pops the current preprocessing context from the stack.
	 */
	protected CobolDocumentContext pop() {
		return contexts.pop();
	}

	/**
	 * Pushes a new preprocessing context onto the stack.
	 */
	protected CobolDocumentContext push() {
		return contexts.push(new CobolDocumentContext());
	}

	@Override
	public void visitTerminal(final TerminalNode node) {
		final int tokPos = node.getSourceInterval().a;
		context().write(TokenUtils.getHiddenTokensToLeft(tokPos, tokens));

		if (!TokenUtils.isEOF(node)) {
			final String text = node.getText();
			context().write(text);
		}
	}
}
