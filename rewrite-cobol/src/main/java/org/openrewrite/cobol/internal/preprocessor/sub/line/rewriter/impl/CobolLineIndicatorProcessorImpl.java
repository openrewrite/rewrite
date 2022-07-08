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

import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLineTypeEnum;
import org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.CobolLineIndicatorProcessor;

public class CobolLineIndicatorProcessorImpl implements CobolLineIndicatorProcessor {

    protected static final String EMPTY_STRING = "";

    protected String conditionalRightTrimContentArea(final CobolLine line) {
        final String result;

        if (!isNextLineContinuation(line)) {
            result = rightTrimContentArea(line.getContentArea());
        } else if (!isEndingWithOpenLiteral(line)) {
            result = rightTrimContentArea(line.getContentArea());
        } else {
            result = line.getContentArea();
        }

        return result;
    }

    protected boolean isEndingWithOpenLiteral(final CobolLine line) {
        final String contentArea = line.getContentAreaOriginal();
        final String contentAreaWithoutStringLiterals = removeStringLiterals(contentArea);
        return contentAreaWithoutStringLiterals.contains("\"") || contentAreaWithoutStringLiterals.contains("'");
    }

    protected boolean isNextLineContinuation(final CobolLine line) {
        return line.getSuccessor() == null ? false
                : CobolLineTypeEnum.CONTINUATION.equals(line.getSuccessor().getType());
    }

    /**
     * Normalizes a line by stripping the sequence number and line indicator, and
     * interpreting the line indicator.
     */
    @Override
    public CobolLine processLine(final CobolLine line) {
        final String conditionalRightTrimmedContentArea = conditionalRightTrimContentArea(line);
        final CobolLine result;

        switch (line.getType()) {
            case DEBUG:
                result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS,
                        conditionalRightTrimmedContentArea, line);
                break;
            case CONTINUATION:
                if (conditionalRightTrimmedContentArea == null || conditionalRightTrimmedContentArea.isEmpty()) {
                    result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS, EMPTY_STRING, line);
                }
                /**
                 * If a line, which is continued on the next line, ends in column 72 with a
                 * quotation mark as the last character ...
                 */
                else if (line.getPredecessor() != null && (line.getPredecessor().getContentAreaOriginal().endsWith("\"")
                        || line.getPredecessor().getContentAreaOriginal().endsWith("'"))) {
                    final String trimmedContentArea = trimLeadingWhitespace(conditionalRightTrimmedContentArea);

                    /**
                     * ... the continuation line by specification has to start with two consecutive
                     * quotation marks.
                     */
                    if (trimmedContentArea.startsWith("\"") || trimmedContentArea.startsWith("'")) {
                        /**
                         * We have to remove the first quotation mark of the continuation line, the 1
                         * quotation mark from the continued line and the 2 quotations marks from the
                         * continuation line become 2 successive quotation marks.
                         */
                        result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS,
                                trimLeadingChar(trimmedContentArea), line);
                    }
                    /**
                     * However there are non-compliant parsers out there without the two consecutive
                     * quotation marks in the continuation line ...
                     */
                    else {
                        /**
                         * ... where we simply remove leading whitespace.
                         */
                        result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS,
                                trimLeadingWhitespace(conditionalRightTrimmedContentArea), line);
                    }
                }
                /**
                 * If we are ending with an open literal ...
                 */
                else if (line.getPredecessor() != null && isEndingWithOpenLiteral(line.getPredecessor())) {
                    final String trimmedContentArea = trimLeadingWhitespace(conditionalRightTrimmedContentArea);

                    /**
                     * ... the continuation line might start with a single quotation mark. This
                     * indicates, that the literal from the continued line stays open ...
                     */
                    if (trimmedContentArea.startsWith("\"") || trimmedContentArea.startsWith("'")) {
                        /**
                         * so we are removing the leading quotation mark to keep the literal open.
                         */
                        result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS,
                                trimLeadingChar(trimmedContentArea), line);
                    } else {
                        result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS,
                                conditionalRightTrimmedContentArea, line);
                    }
                }
                /**
                 * If we are ending with a closed literal and the continued line ends with a
                 * quotation mark ...
                 */
                else if (line.getPredecessor() != null && (line.getPredecessor().getContentArea().endsWith("\"")
                        || line.getPredecessor().getContentArea().endsWith("'"))) {
                    /**
                     * ... prepend a whitespace to the continuation line
                     */
                    result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS,
                            CobolPreprocessor.WS + trimLeadingWhitespace(conditionalRightTrimmedContentArea), line);
                }
                /**
                 * As fallback ...
                 */
                else {
                    /**
                     * ... trim leading whitespace.
                     */
                    result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS,
                            trimLeadingWhitespace(conditionalRightTrimmedContentArea), line);
                }
                break;
            case COMMENT:
                result = CobolLine.copyCobolLineWithIndicatorAndContentArea(
                        CobolPreprocessor.COMMENT_TAG + CobolPreprocessor.WS, conditionalRightTrimmedContentArea, line);
                break;
            case COMPILER_DIRECTIVE:
                result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS, EMPTY_STRING, line);
                break;
            case NORMAL:
            default:
                result = CobolLine.copyCobolLineWithIndicatorAndContentArea(CobolPreprocessor.WS,
                        conditionalRightTrimmedContentArea, line);
                break;
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

    protected String removeStringLiterals(final String contentArea) {
        final String doubleQuoteLiteralPattern = "\"([^\"]|\"\"|'')*\"";
        final String singleQuoteLiteralPattern = "'([^']|''|\"\")*'";
        return contentArea.replaceAll(doubleQuoteLiteralPattern, EMPTY_STRING).replaceAll(singleQuoteLiteralPattern,
                EMPTY_STRING);
    }

    protected String repairTrailingComma(final String contentArea) {
        final String result;

        /*
         * repair trimmed whitespace after comma separator
         */
        if (contentArea.isEmpty()) {
            result = contentArea;
        } else {
            final char lastCharAtTrimmedLineArea = contentArea.charAt(contentArea.length() - 1);

            if (lastCharAtTrimmedLineArea == ',' || lastCharAtTrimmedLineArea == ';') {
                result = contentArea + CobolPreprocessor.WS;
            } else {
                result = contentArea;
            }
        }

        return result;
    }

    protected String rightTrimContentArea(final String contentarea) {
        final String contentAreaWithTrimmedTrailingWhitespace = trimTrailingWhitespace(contentarea);
        return repairTrailingComma(contentAreaWithTrimmedTrailingWhitespace);
    }

    protected String trimLeadingChar(final String contentArea) {
        return contentArea.substring(1);
    }

    protected String trimLeadingWhitespace(final String contentarea) {
        return contentarea.replaceAll("^\\s+", EMPTY_STRING);
    }

    protected String trimTrailingWhitespace(final String contentArea) {
        return contentArea.replaceAll("\\s+$", EMPTY_STRING);
    }
}
