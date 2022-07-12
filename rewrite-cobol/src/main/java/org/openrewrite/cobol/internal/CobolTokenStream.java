package org.openrewrite.cobol.internal;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.openrewrite.cobol.internal.runner.impl.CobolToken;

public class CobolTokenStream extends CommonTokenStream {

    private String input;

    public CobolTokenStream(String input, TokenSource tokenSource) {
        super(tokenSource);
        this.input = input;
    }
}
