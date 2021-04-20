package org.openrewrite.java.cleanup;

import lombok.Value;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.StyleHelper;
import org.openrewrite.style.Style;

@Value
public class ExplicitInitializationStyle implements Style {
    Boolean onlyObjectReferences;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.explicitInitialization(), this);
    }
}
