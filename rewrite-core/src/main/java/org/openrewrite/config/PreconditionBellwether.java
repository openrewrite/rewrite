package org.openrewrite.config;

import org.openrewrite.Recipe;

public class PreconditionBellwether extends Recipe {
    @Override
    public String getDisplayName() {
        return "Precondition bellwether";
    }

    @Override
    public String getDescription() {
        return "Evaluates a precondition as an implementation detail";
    }
}
