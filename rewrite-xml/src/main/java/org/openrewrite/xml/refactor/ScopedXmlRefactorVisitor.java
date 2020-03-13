package org.openrewrite.xml.refactor;

import lombok.Getter;
import org.openrewrite.ScopedVisitorSupport;
import org.openrewrite.xml.tree.Xml;

import java.util.UUID;

public abstract class ScopedXmlRefactorVisitor extends XmlRefactorVisitor implements ScopedVisitorSupport {
    @Getter
    private final UUID scope;

    public ScopedXmlRefactorVisitor(UUID scope) {
        this.scope = scope;
    }

    @Override
    public boolean isCursored() {
        return true;
    }

    public Xml.Tag enclosingTag() {
        return getCursor().firstEnclosing(Xml.Tag.class);
    }

    public Xml.Tag enclosingRootTag() {
        return getCursor().getPathAsStream()
                .filter(t -> t instanceof Xml.Tag)
                .map(Xml.Tag.class::cast)
                .reduce((t1, t2) -> t2)
                .orElseThrow(() -> new IllegalStateException("No root tag. This operation should be called from a cursor scope that is inside of the root tag."));
    }
}
