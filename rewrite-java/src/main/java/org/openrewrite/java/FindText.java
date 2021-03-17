package org.openrewrite.java;

import org.openrewrite.Recipe;

import java.util.List;

public class FindText extends Recipe {
    private List<String> patterns;

    @Override
    public String getDisplayName() {
        return "Find text";
    }

    @Override
    public String getDescription() {
        return "";
    }
}
