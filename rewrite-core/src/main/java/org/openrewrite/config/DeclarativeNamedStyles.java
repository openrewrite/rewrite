package org.openrewrite.config;

import org.openrewrite.Validated;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Collection;

public class DeclarativeNamedStyles extends NamedStyles {
    private Validated validation = Validated.none();

    public DeclarativeNamedStyles(String name, Collection<Style> styles) {
        super(name, styles);
    }

    void addValidation(Validated validated) {
        validation = validation.and(validated);
    }

    @Override
    public Validated validate() {
        return validation;
    }
}
