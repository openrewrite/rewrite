/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub;

import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor.CobolSourceFormatEnum;
import org.openrewrite.cobol.internal.params.CobolDialect;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;

public class CobolLine {

    public static CobolLine copyCobolLineWithContentArea(final String contentArea, final CobolLine line) {
        return new CobolLine(line.sequenceArea, line.sequenceAreaOriginal, line.indicatorArea,
                line.indicatorAreaOriginal, extractContentAreaA(contentArea), line.contentAreaAOriginal,
                extractContentAreaB(contentArea), line.contentAreaBOriginal, line.commentArea, line.commentAreaOriginal,
                line.format, line.dialect, line.number, line.type, line.predecessor, line.successor);
    }

    public static CobolLine copyCobolLineWithIndicatorAndContentArea(final String indicatorArea,
                                                                     final String contentArea, final CobolLine line) {
        return new CobolLine(line.sequenceArea, line.sequenceAreaOriginal, indicatorArea, line.indicatorAreaOriginal,
                extractContentAreaA(contentArea), line.contentAreaAOriginal, extractContentAreaB(contentArea),
                line.contentAreaBOriginal, line.commentArea, line.commentAreaOriginal, line.format, line.dialect,
                line.number, line.type, line.predecessor, line.successor);
    }

    public static CobolLine copyCobolLineWithIndicatorArea(final String indicatorArea, final CobolLine line) {
        return new CobolLine(line.sequenceArea, line.sequenceAreaOriginal, indicatorArea, line.indicatorAreaOriginal,
                line.contentAreaA, line.contentAreaAOriginal, line.contentAreaB, line.contentAreaBOriginal,
                line.commentArea, line.commentAreaOriginal, line.format, line.dialect, line.number, line.type,
                line.predecessor, line.successor);
    }

    public static String createBlankSequenceArea(final CobolSourceFormatEnum format) {
        // return CobolSourceFormatEnum.TANDEM.equals(format) ? "" : CobolPreprocessor.WS.repeat(6);
        return CobolSourceFormatEnum.TANDEM.equals(format) ? "" : new String(new char[6]).replace("\0", CobolPreprocessor.WS);
        // repeated = ;
    }

    protected static String extractContentAreaA(final String contentArea) {
        return contentArea.length() > 4 ? contentArea.substring(0, 4) : contentArea;
    }

    protected static String extractContentAreaB(final String contentArea) {
        return contentArea.length() > 4 ? contentArea.substring(4) : "";
    }

    public static CobolLine newCobolLine(final String sequenceArea, final String indicatorArea,
                                         final String contentAreaA, final String contentAreaB, final String commentArea,
                                         final CobolSourceFormatEnum format, final CobolDialect dialect, final int number,
                                         final CobolLineTypeEnum type) {
        return new CobolLine(sequenceArea, sequenceArea, indicatorArea, indicatorArea, contentAreaA, contentAreaA,
                contentAreaB, contentAreaB, commentArea, commentArea, format, dialect, number, type, null, null);
    }

    public int length() {
        return
                commentArea.length() +
                        contentAreaA.length() +
                        contentAreaB.length() +
                        indicatorArea.length() +
                        sequenceArea.length();
    }

    public int originalLength() {
        return
                commentAreaOriginal.length() +
                        contentAreaAOriginal.length() +
                        contentAreaBOriginal.length() +
                        indicatorAreaOriginal.length() +
                        sequenceAreaOriginal.length();
    }

    protected String commentArea;

    protected String commentAreaOriginal;

    protected String contentAreaA;

    protected String contentAreaAOriginal;

    protected String contentAreaB;

    protected String contentAreaBOriginal;

    protected CobolDialect dialect;

    protected CobolSourceFormatEnum format;

    protected String indicatorArea;

    protected String indicatorAreaOriginal;

    protected int number;

    protected CobolLine predecessor;

    protected String sequenceArea;

    protected String sequenceAreaOriginal;

    protected CobolLine successor;

    protected CobolLineTypeEnum type;

    protected CobolLine(final String sequenceArea, final String sequenceAreaOriginal, final String indicatorArea,
                        final String indicatorAreaOriginal, final String contentAreaA, final String contentAreaAOriginal,
                        final String contentAreaB, final String contentAreaBOriginal, final String commentArea,
                        final String commentAreaOriginal, final CobolSourceFormatEnum format, final CobolDialect dialect,
                        final int number, final CobolLineTypeEnum type, final CobolLine predecessor, final CobolLine successor) {
        this.sequenceArea = sequenceArea;
        this.indicatorArea = indicatorArea;
        this.contentAreaA = contentAreaA;
        this.contentAreaB = contentAreaB;
        this.commentArea = commentArea;

        this.sequenceAreaOriginal = sequenceAreaOriginal;
        this.indicatorAreaOriginal = indicatorAreaOriginal;
        this.contentAreaAOriginal = contentAreaAOriginal;
        this.contentAreaBOriginal = contentAreaBOriginal;
        this.commentAreaOriginal = commentAreaOriginal;

        this.format = format;
        this.dialect = dialect;
        this.number = number;
        this.type = type;

        setPredecessor(predecessor);
        setSuccessor(successor);
    }

    public String getBlankSequenceArea() {
        return createBlankSequenceArea(format);
    }

    public String getCommentArea() {
        return commentArea;
    }

    public String getCommentAreaOriginal() {
        return commentAreaOriginal;
    }

    public String getContentArea() {
        return contentAreaA + contentAreaB;
    }

    public String getContentAreaA() {
        return contentAreaA;
    }

    public String getContentAreaAOriginal() {
        return contentAreaAOriginal;
    }

    public String getContentAreaB() {
        return contentAreaB;
    }

    public String getContentAreaBOriginal() {
        return contentAreaBOriginal;
    }

    public String getContentAreaOriginal() {
        return contentAreaAOriginal + contentAreaBOriginal;
    }

    public CobolDialect getDialect() {
        return dialect;
    }

    public CobolSourceFormatEnum getFormat() {
        return format;
    }

    public String getIndicatorArea() {
        return indicatorArea;
    }

    public String getIndicatorAreaOriginal() {
        return indicatorAreaOriginal;
    }

    public int getNumber() {
        return number;
    }

    public CobolLine getPredecessor() {
        return predecessor;
    }

    public String getSequenceArea() {
        return sequenceArea;
    }

    public String getSequenceAreaOriginal() {
        return sequenceAreaOriginal;
    }

    public CobolLine getSuccessor() {
        return successor;
    }

    public CobolLineTypeEnum getType() {
        return type;
    }

    public String serialize() {
        return sequenceArea + indicatorArea + contentAreaA + contentAreaB + commentArea;
    }

    public void setPredecessor(final CobolLine predecessor) {
        this.predecessor = predecessor;

        if (predecessor != null) {
            predecessor.successor = this;
        }
    }

    public void setSuccessor(final CobolLine successor) {
        this.successor = successor;

        if (successor != null) {
            successor.predecessor = this;
        }
    }

    @Override
    public String toString() {
        return serialize();
    }
}
