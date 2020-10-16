package org.openrewrite.maven.tree;

import org.openrewrite.internal.lang.Nullable;

public enum Scope {
    Compile,
    Provided,
    Runtime,
    Test,
    System;

    /**
     * See the table at <a href="Dependency Scope">https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-scope</a>.
     * <code>this</code> represents the scope on the top row of the table.
     *
     * @param scope The scope on the left column of the table.
     * @return The scope inside the table.
     */
    @Nullable
    public Scope transitiveOf(@Nullable Scope scope) {
        if(scope == null) {
            return this;
        }

        switch(scope) {
            case Compile:
                switch(this) {
                    case Compile:
                        return Compile;
                    case Runtime:
                        return Runtime;
                    case Provided:
                    case Test:
                    default:
                        return null;
                }
            case Provided:
                switch(this) {
                    case Compile:
                    case Runtime:
                        return Provided;
                    case Provided:
                    case Test:
                    default:
                        return null;
                }
            case Runtime:
                switch(this) {
                    case Compile:
                    case Runtime:
                        return Runtime;
                    case Provided:
                    case Test:
                    default:
                        return null;
                }
            case Test:
                switch(this) {
                    case Compile:
                    case Runtime:
                        return Test;
                    case Provided:
                    case Test:
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    public static Scope fromName(@Nullable String scope) {
        if(scope == null) {
            return Compile;
        }
        switch(scope) {
            case "compile":
                return Compile;
            case "provided":
                return Provided;
            case "runtime":
                return Runtime;
            case "test":
                return Test;
            case "system":
                return System;
            default:
                throw new IllegalArgumentException("unsupported scope '" + scope + "'");
        }
    }
}
