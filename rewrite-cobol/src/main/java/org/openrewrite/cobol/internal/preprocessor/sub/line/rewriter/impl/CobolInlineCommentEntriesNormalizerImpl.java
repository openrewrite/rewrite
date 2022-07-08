/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;
import org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.CobolInlineCommentEntriesNormalizer;

public class CobolInlineCommentEntriesNormalizerImpl implements CobolInlineCommentEntriesNormalizer {

	protected static final String denormalizedCommentEntryRegex = "\\*>[^ ]";

	protected final Pattern denormalizedCommentEntryPattern = Pattern.compile(denormalizedCommentEntryRegex);

	@Override
	public CobolLine processLine(final CobolLine line) {
		final Matcher matcher = denormalizedCommentEntryPattern.matcher(line.getContentArea());
		final CobolLine result;

		if (!matcher.find()) {
			result = line;
		} else {
			final String newContentArea = line.getContentArea().replace(CobolPreprocessor.COMMENT_TAG,
					CobolPreprocessor.COMMENT_TAG + CobolPreprocessor.WS);
			result = CobolLine.copyCobolLineWithContentArea(newContentArea, line);
		}

		return result;
	}

	@Override
	public List<CobolLine> processLines(final List<CobolLine> lines) {
		final List<CobolLine> result = new ArrayList<CobolLine>();

		for (final CobolLine line : lines) {
			final CobolLine processedLine = processLine(line);
			result.add(processedLine);
		}

		return result;
	}
}
