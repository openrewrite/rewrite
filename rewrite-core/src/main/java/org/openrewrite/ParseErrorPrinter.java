package org.openrewrite;

public class ParseErrorPrinter<P> extends ParseErrorVisitor<PrintOutputCapture<P>> {
    @Override
    public ParseError visitParseError(ParseError e, PrintOutputCapture<P> p) {
        p.append(e.getText());
        return e;
    }
}
