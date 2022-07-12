/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.runner.impl;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;
import org.openrewrite.cobol.internal.StringWithOriginalPositions;

public class CobolTokenFactory implements TokenFactory<CobolToken> {

    StringWithOriginalPositions preprocessedInput; // This is what is lexed

    public CobolTokenFactory(StringWithOriginalPositions preprocessedInput) {
        this.preprocessedInput = preprocessedInput;
    }

    @Override
    public CobolToken create(Pair<TokenSource, CharStream> source, int type, String text,
                             int channel, int start, int stop,
                             int line, int charPositionInLine) {
        CobolToken t = new CobolToken(source, type, channel, start, stop);
        t.setLine(line);
        t.setCharPositionInLine(charPositionInLine);

        assert text == null;

        String preprocessedText = preprocessedInput.getPreprocessedText(start, stop);
        t.setText(preprocessedText);

        if(t.getChannel() == 0) {
            removeTrailingWhitespace(t);
        }
        return t;
    }

    @Override
    public CobolToken create(int type, String text) {
        return new CobolToken(type, text);
    }


    /**
     * Line breaks are significant in Cobol and must be part of some tokens for lexing. However they are a problem
     * when building ASTs, where we need line breaks to belong to whitespace. This method removes trailing whitespace
     * from tokens but adjusting their stop index. Subsequent processing will pick the correct amount of whitespace
     * by referring to this adjusted stop index.
     */
    private void removeTrailingWhitespace(CobolToken t) {
        while(isWhiteSpace(preprocessedInput.preprocessedText.charAt(t.getStopIndex())) && t.getStopIndex() > t.getStartIndex()) {
            t.setStopIndex(t.getStopIndex()-1);
            t.setText(t.getText().substring(0, t.getText().length()-1));
        }
    }

    private boolean isWhiteSpace(char c) {
        return c == '\r' | c == '\n' | c == '\f' | c == '\t' | c == ' ';
    }
}
