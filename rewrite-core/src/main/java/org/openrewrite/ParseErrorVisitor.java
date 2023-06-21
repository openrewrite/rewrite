package org.openrewrite;

public class ParseErrorVisitor <P> extends TreeVisitor<Tree, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof ParseError;
    }

    @Override
    public boolean isAdaptableTo(@SuppressWarnings("rawtypes") Class<? extends TreeVisitor> adaptTo) {
        return adaptTo.isAssignableFrom(ParseErrorVisitor.class);
    }

    public ParseError visitParseError(ParseError e, P p) {
        return e.withMarkers(visitMarkers(e.getMarkers(), p));
    }
}
