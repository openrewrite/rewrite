package org.openrewrite.java.refactor;

import lombok.Getter;
import org.openrewrite.ScopedVisitorSupport;

import java.util.UUID;

public abstract class ScopedJavaRefactorVisitor extends JavaRefactorVisitor implements ScopedVisitorSupport {
    @Getter
    private final UUID scope;

    public ScopedJavaRefactorVisitor(UUID scope) {
        this.scope = scope;
    }

    @Override
    public boolean isCursored() {
        return true;
    }
}
