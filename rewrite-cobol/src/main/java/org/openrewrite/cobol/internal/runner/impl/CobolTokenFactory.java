/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.runner.impl;

import org.antlr.v4.runtime.CharStream;
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

//		String originalText = preprocessedInput.getText(start,stop);
//		String preprocessedText = source.b.getText(Interval.of(start,stop));

        //String originalText =  preprocessedInput.getOriginalText(start, stop);
        String preprocessedText = preprocessedInput.getPreprocessedText(start, stop);
        t.setText(preprocessedText);

        return t;
    }

    @Override
    public CobolToken create(int type, String text) {
        return new CobolToken(type, text);
    }

}
