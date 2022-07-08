/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.openrewrite.cobol.internal.StringWithOriginalPositions;
import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;
import org.openrewrite.cobol.internal.preprocessor.sub.document.CobolDocumentParser;
import org.openrewrite.cobol.internal.preprocessor.sub.document.impl.CobolDocumentParserImpl;
import org.openrewrite.cobol.internal.preprocessor.sub.line.reader.CobolLineReader;
import org.openrewrite.cobol.internal.preprocessor.sub.line.reader.impl.CobolLineReaderImpl;
import org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.CobolCommentEntriesMarker;
import org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.CobolInlineCommentEntriesNormalizer;
import org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.CobolLineIndicatorProcessor;
import org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.impl.CobolCommentEntriesMarkerImpl;
import org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.impl.CobolInlineCommentEntriesNormalizerImpl;
import org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.impl.CobolLineIndicatorProcessorImpl;
import org.openrewrite.cobol.internal.preprocessor.sub.line.writer.CobolLineWriter;
import org.openrewrite.cobol.internal.preprocessor.sub.line.writer.impl.CobolLineWriterImpl;

public class CobolPreprocessorImpl implements CobolPreprocessor {

	// private final static Logger LOG = LoggerFactory.getLogger(CobolPreprocessorImpl.class);

	protected CobolCommentEntriesMarker createCommentEntriesMarker() {
		return new CobolCommentEntriesMarkerImpl();
	}

	protected CobolDocumentParser createDocumentParser() {
		return new CobolDocumentParserImpl();
	}

	protected CobolInlineCommentEntriesNormalizer createInlineCommentEntriesNormalizer() {
		return new CobolInlineCommentEntriesNormalizerImpl();
	}

	protected CobolLineIndicatorProcessor createLineIndicatorProcessor() {
		return new CobolLineIndicatorProcessorImpl();
	}

	protected CobolLineReader createLineReader() {
		return new CobolLineReaderImpl();
	}

	protected CobolLineWriter createLineWriter() {
		return new CobolLineWriterImpl();
	}

	protected StringWithOriginalPositions parseDocument(final String originalCode, final List<CobolLine> lines, final CobolParserParams params) {
		final StringWithOriginalPositions code = createLineWriter().serialize(originalCode, lines);
		final StringWithOriginalPositions result = createDocumentParser().processLines(code, params);
		return result;
	}

	@Override
	public StringWithOriginalPositions processWithOriginalPositions(final File cobolFile, final CobolParserParams params) throws IOException {
		final Charset charset = params.getCharset();

		//LOG.info("Preprocessing file {} with line format {} and charset {}.", cobolFile.getName(), params.getFormat(),
		//		charset);

		//final String cobolFileContent = Files.readString(cobolFile.toPath(), charset);
		final String cobolFileContent = new String(Files.readAllBytes(cobolFile.toPath()), charset);
		final StringWithOriginalPositions result = processWithOriginalPositions(cobolFileContent, params);
		return result;
	}

	@Override
	public StringWithOriginalPositions processWithOriginalPositions(final String cobolCode, final CobolParserParams params) {
		final List<CobolLine> lines = readLines(cobolCode, params);
		
		int cobolCodeLength = cobolCode.length();
		int linesLength = lines.stream().map(l -> l.length()).reduce(0, Integer::sum)
				+ (lines.size()-1) * 2; // newlines
		//assert linesLength == cobolCodeLength;
		
		final List<CobolLine> rewrittenLines = rewriteLines(lines);
		assert lines.size() == rewrittenLines.size();
		for(int i=0; i<lines.size(); i++) {
			assert lines.get(i).length() == rewrittenLines.get(i).originalLength();
		}
		int rewrittenLinesLength = rewrittenLines.stream().map(l -> l.originalLength()).reduce(0, Integer::sum)
				+ (rewrittenLines.size()-1) * 2; // newlines
		//assert linesLength == rewrittenLinesLength;
		
		final StringWithOriginalPositions result = parseDocument(cobolCode, rewrittenLines, params);
		return result;
	}

	protected List<CobolLine> readLines(final String cobolCode, final CobolParserParams params) {
		final List<CobolLine> lines = createLineReader().processLines(cobolCode, params);
		return lines;
	}

	/**
	 * Normalizes lines of given COBOL source code, so that comment entries can be
	 * parsed and lines have a unified line format.
	 */
	protected List<CobolLine> rewriteLines(final List<CobolLine> lines) {
		final List<CobolLine> lineIndicatorProcessedLines = createLineIndicatorProcessor().processLines(lines);
		final List<CobolLine> normalizedInlineCommentEntriesLines = createInlineCommentEntriesNormalizer()
				.processLines(lineIndicatorProcessedLines);
		final List<CobolLine> result = createCommentEntriesMarker().processLines(normalizedInlineCommentEntriesLines);
		return result;
	}
}
