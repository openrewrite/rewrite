package org.openrewrite.java;

import org.openrewrite.java.tree.J;

import java.util.List;
public final class ModifierResults {

    private final List<J.Annotation> leadingAnnotations;
    private final List<J.Modifier> modifiers;

    public ModifierResults(List<J.Annotation> leadingAnnotations, List<J.Modifier> modifiers) {
        this.leadingAnnotations = leadingAnnotations;
        this.modifiers = modifiers;
    }

    public List<J.Annotation> getLeadingAnnotations() {
        return leadingAnnotations;
    }

    public List<J.Modifier> getModifiers() {
        return modifiers;
    }
}
