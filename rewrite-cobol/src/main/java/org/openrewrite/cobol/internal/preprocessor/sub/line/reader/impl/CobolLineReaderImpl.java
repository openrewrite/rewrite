/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.line.reader.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;
import org.openrewrite.cobol.internal.preprocessor.exception.CobolPreprocessorException;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLineTypeEnum;
import org.openrewrite.cobol.internal.preprocessor.sub.line.reader.CobolLineReader;
import org.openrewrite.cobol.internal.preprocessor.sub.line.reader.CobolScanner;
import org.openrewrite.cobol.internal.preprocessor.sub.line.reader.Line;

public class CobolLineReaderImpl implements CobolLineReader {

    protected CobolLineTypeEnum determineType(final String indicatorArea) {
        final CobolLineTypeEnum result;

        switch (indicatorArea) {
            case CobolPreprocessor.CHAR_D:
            case CobolPreprocessor.CHAR_D_:
                result = CobolLineTypeEnum.DEBUG;
                break;
            case CobolPreprocessor.CHAR_MINUS:
                result = CobolLineTypeEnum.CONTINUATION;
                break;
            case CobolPreprocessor.CHAR_ASTERISK:
            case CobolPreprocessor.CHAR_SLASH:
                result = CobolLineTypeEnum.COMMENT;
                break;
            case CobolPreprocessor.CHAR_DOLLAR_SIGN:
                result = CobolLineTypeEnum.COMPILER_DIRECTIVE;
                break;
            case CobolPreprocessor.WS:
            default:
                result = CobolLineTypeEnum.NORMAL;
                break;
        }

        return result;
    }

    @Override
    public CobolLine parseLine(final String line, final String newLine, final int lineNumber, final CobolParserParams params) {
        final CobolPreprocessor.CobolSourceFormatEnum format = params.getFormat();
        final Pattern pattern = format.getPattern();
        final Matcher matcher = pattern.matcher(line);

        final CobolLine result;

        if (!matcher.matches()) {
            final String formatDescription;

            switch (format) {
                case FIXED:
                    formatDescription = "Columns 1-6 sequence number, column 7 indicator area, columns 8-72 for areas A and B";
                    break;
                case TANDEM:
                    formatDescription = "Column 1 indicator area, columns 2 and all following for areas A and B";
                    break;
                case VARIABLE:
                    formatDescription = "Columns 1-6 sequence number, column 7 indicator area, columns 8 and all following for areas A and B";
                    break;
                default:
                    formatDescription = "";
                    break;
            }

            final String message = "Is " + params.getFormat() + " the correct line format (" + formatDescription
                    + ")? Could not parse line " + (lineNumber + 1) + ": " + line;

            throw new CobolPreprocessorException(message);
        } else {
            final String sequenceAreaGroup = matcher.group(1);
            final String indicatorAreaGroup = matcher.group(2);
            final String contentAreaAGroup = matcher.group(3);
            final String contentAreaBGroup = matcher.group(4);
            final String commentAreaGroup = matcher.group(5);

            final String sequenceArea = sequenceAreaGroup != null ? sequenceAreaGroup : "";
            final String indicatorArea = indicatorAreaGroup != null ? indicatorAreaGroup : " ";
            final String contentAreaA = contentAreaAGroup != null ? contentAreaAGroup : "";
            final String contentAreaB = contentAreaBGroup != null ? contentAreaBGroup : "";
            final String commentArea = commentAreaGroup != null ? commentAreaGroup : "";

            final CobolLineTypeEnum type = determineType(indicatorArea);

            result = CobolLine.newCobolLine(sequenceArea, indicatorArea, contentAreaA, contentAreaB, commentArea,
                    params.getFormat(), params.getDialect(), lineNumber, type, newLine);
        }

        return result;
    }

    @Override
    public List<CobolLine> processLines(final String lines, final CobolParserParams params) {
        final CobolScanner scanner = new CobolScanner(lines);
        final List<CobolLine> result = new ArrayList<CobolLine>();

        Line currentLine = null;
        String currentNewLine = null;
        CobolLine lastCobolLine = null;
        int lineNumber = 0;

        while (scanner.hasNextLine()) {
            currentLine = scanner.nextLine();

            final CobolLine currentCobolLine = parseLine(currentLine.getText(), currentLine.getNewLine(), lineNumber, params);
            currentCobolLine.setPredecessor(lastCobolLine);
            result.add(currentCobolLine);

            lineNumber++;
            lastCobolLine = currentCobolLine;
        }

        scanner.close();
        return result;
    }
}
