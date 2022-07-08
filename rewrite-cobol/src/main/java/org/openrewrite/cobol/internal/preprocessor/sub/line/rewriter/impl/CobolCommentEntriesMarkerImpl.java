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

import org.openrewrite.cobol.internal.params.CobolDialect;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLineTypeEnum;
import org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter.CobolCommentEntriesMarker;

public class CobolCommentEntriesMarkerImpl implements CobolCommentEntriesMarker {

    protected final Pattern commentEntryTriggerLinePattern;

    protected boolean foundCommentEntryTriggerInPreviousLine = false;

    protected boolean isInCommentEntry = false;

    protected final String[] triggersEnd = new String[]{"PROGRAM-ID.", "AUTHOR.", "INSTALLATION.", "DATE-WRITTEN.",
            "DATE-COMPILED.", "SECURITY.", "ENVIRONMENT", "DATA.", "PROCEDURE."};

    protected final String[] triggersStart = new String[]{"AUTHOR.", "INSTALLATION.", "DATE-WRITTEN.",
            "DATE-COMPILED.", "SECURITY.", "REMARKS."};

    public CobolCommentEntriesMarkerImpl() {
        final String commentEntryTriggerLineFormat = new String(
                "([ \\t]*)(" + String.join("|", triggersStart) + ")(.+)");
        commentEntryTriggerLinePattern = Pattern.compile(commentEntryTriggerLineFormat, Pattern.CASE_INSENSITIVE);
    }

    protected CobolLine buildMultiLineCommentEntryLine(final CobolLine line) {
        return CobolLine.copyCobolLineWithIndicatorArea(CobolPreprocessor.COMMENT_ENTRY_TAG + CobolPreprocessor.WS,
                line);
    }

    /**
     * Escapes in a given line a potential comment entry.
     */
    protected CobolLine escapeCommentEntry(final CobolLine line) {
        final CobolLine result;

        final Matcher matcher = commentEntryTriggerLinePattern.matcher(line.getContentArea());

        if (matcher.matches()) {
            final String whitespace = matcher.group(1);
            final String trigger = matcher.group(2);
            final String commentEntry = matcher.group(3);
            final String newContentArea = whitespace + trigger + CobolPreprocessor.WS
                    + CobolPreprocessor.COMMENT_ENTRY_TAG + commentEntry;

            result = CobolLine.copyCobolLineWithContentArea(newContentArea, line);
        } else {
            result = line;
        }

        return result;
    }

    protected boolean isInCommentEntry(final CobolLine line, final boolean isContentAreaAEmpty,
                                       final boolean isInOsvsCommentEntry) {
        final boolean result = CobolLineTypeEnum.COMMENT.equals(line.getType()) || isContentAreaAEmpty
                || isInOsvsCommentEntry;
        return result;
    }

    /**
     * OSVS: The comment-entry can be contained in either area A or area B of the
     * comment-entry lines. However, the next occurrence in area A of any one of the
     * following COBOL words or phrases terminates the comment-entry and begin the
     * next paragraph or division.
     */
    protected boolean isInOsvsCommentEntry(final CobolLine line) {
        final boolean result = CobolDialect.OSVS.equals(line.getDialect()) && !startsWithTrigger(line, triggersEnd);
        return result;
    }

    @Override
    public CobolLine processLine(final CobolLine line) {
        final CobolLine result;

        if (line.getFormat().isCommentEntryMultiLine()) {
            result = processMultiLineCommentEntry(line);
        } else {
            result = processSingleLineCommentEntry(line);
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

    /**
     * If the Compiler directive SOURCEFORMAT is specified as or defaulted to FIXED,
     * the comment-entry can be contained on one or more lines but is restricted to
     * area B of those lines; the next line commencing in area A begins the next
     * non-comment entry.
     */
    protected CobolLine processMultiLineCommentEntry(final CobolLine line) {
        final boolean foundCommentEntryTriggerInCurrentLine = startsWithTrigger(line, triggersStart);
        final CobolLine result;

        if (foundCommentEntryTriggerInCurrentLine) {
            result = escapeCommentEntry(line);
        } else if (foundCommentEntryTriggerInPreviousLine || isInCommentEntry) {
            final boolean isContentAreaAEmpty = line.getContentAreaA().trim().isEmpty();
            final boolean isInOsvsCommentEntry = isInOsvsCommentEntry(line);

            isInCommentEntry = isInCommentEntry(line, isContentAreaAEmpty, isInOsvsCommentEntry);

            if (isInCommentEntry) {
                result = buildMultiLineCommentEntryLine(line);
            } else {
                result = line;
            }
        } else {
            result = line;
        }

        foundCommentEntryTriggerInPreviousLine = foundCommentEntryTriggerInCurrentLine;

        return result;
    }

    protected CobolLine processSingleLineCommentEntry(final CobolLine line) {
        final boolean foundCommentEntryTriggerInCurrentLine = startsWithTrigger(line, triggersStart);
        final CobolLine result;

        if (foundCommentEntryTriggerInCurrentLine) {
            result = escapeCommentEntry(line);
        } else {
            result = line;
        }

        return result;
    }

    /**
     * Checks, whether given line starts with a trigger keyword indicating a comment
     * entry.
     */
    protected boolean startsWithTrigger(final CobolLine line, final String[] triggers) {
        final String contentAreaUpperCase = new String(line.getContentArea()).toUpperCase();

        boolean result = false;

        for (final String trigger : triggers) {
            final boolean containsTrigger = contentAreaUpperCase.trim().startsWith(trigger);

            if (containsTrigger) {
                result = true;
                break;
            }
        }

        return result;
    }
}
