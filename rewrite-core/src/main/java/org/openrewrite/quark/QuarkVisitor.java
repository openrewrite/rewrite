package org.openrewrite.quark;

import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;

public class QuarkVisitor<P> extends TreeVisitor<Quark, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Quark;
    }

    @Override
    public String getLanguage() {
        return "other";
    }

    public Quark visitQuark(Quark quark, P p) {
        Quark q = quark;
        q = q.withMarkers(visitMarkers(q.getMarkers(), p));
        return q;
    }
}
