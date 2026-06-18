/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.internal;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public final class PackageNameUtils {

    /** A package segment diverged from the target: the package is definitively not a prefix. */
    private static final int NO_MATCH = -1;
    /** The name tree couldn't be interpreted; callers should fall back to a string comparison. */
    private static final int UNINTERPRETABLE = -2;

    private PackageNameUtils() {
    }

    /**
     * Returns the canonical dotted name of a package declaration (e.g. {@code "org.openrewrite.java"}).
     * <p>
     * The name is reconstructed by walking the declaration's name tree ({@link J.Identifier} /
     * {@link J.FieldAccess}) rather than printing it, which is comparatively expensive and requires a
     * {@link org.openrewrite.Cursor}. Any whitespace a source file may place around the {@code .}
     * separators is dropped, so the result is always a canonical fully qualified package name.
     *
     * @param pkg the package declaration
     * @return the dotted package name
     */
    public static String getPackageName(J.Package pkg) {
        StringBuilder name = new StringBuilder();
        if (appendName(pkg.getExpression(), name)) {
            return name.toString();
        }
        // Fallback for an unexpected expression shape (e.g. J.Unknown in unparseable source).
        return packageNameByPrinting(pkg.getExpression());
    }

    /**
     * Whether the package declaration's name is a prefix of {@code fullyQualifiedName} at a package
     * boundary — e.g. package {@code a.b} matches {@code a.b.C} but not {@code a.bc.C}. Useful as a
     * cheap pre-filter for "could a type named {@code fullyQualifiedName} be declared in this file?".
     * <p>
     * The package name tree is matched against {@code fullyQualifiedName} segment by segment without
     * building an intermediate string, short-circuiting as soon as a segment diverges.
     *
     * @param pkg                 the package declaration
     * @param fullyQualifiedName  the dotted name to test the package against
     * @return whether the package name is a package-boundary prefix of {@code fullyQualifiedName}
     */
    public static boolean isPrefixOf(J.Package pkg, String fullyQualifiedName) {
        int matched = matchPrefix(pkg.getExpression(), fullyQualifiedName);
        if (matched == UNINTERPRETABLE) {
            // Fall back to the string form to preserve behavior for exotic expression shapes.
            return fullyQualifiedName.startsWith(getPackageName(pkg) + ".");
        }
        return matched != NO_MATCH;
    }

    private static boolean appendName(Expression expression, StringBuilder name) {
        if (expression instanceof J.Identifier) {
            name.append(((J.Identifier) expression).getSimpleName());
            return true;
        } else if (expression instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
            if (!appendName(fieldAccess.getTarget(), name)) {
                return false;
            }
            name.append('.').append(fieldAccess.getSimpleName());
            return true;
        }
        return false;
    }

    /**
     * Matches the package described by {@code expression} against the leading segments of
     * {@code fullyQualifiedName}, returning the offset just past the matched prefix (including its
     * trailing {@code '.'}), or {@link #NO_MATCH} / {@link #UNINTERPRETABLE}. The name tree is
     * structured outermost-first, so recursion descends to the leftmost segment and matches on the way
     * back up, comparing in left-to-right order without building an intermediate string.
     */
    private static int matchPrefix(Expression expression, String fullyQualifiedName) {
        String segment;
        int start;
        if (expression instanceof J.Identifier) {
            segment = ((J.Identifier) expression).getSimpleName();
            start = 0;
        } else if (expression instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
            start = matchPrefix(fieldAccess.getTarget(), fullyQualifiedName);
            if (start < 0) {
                return start; // propagate no-match / uninterpretable
            }
            segment = fieldAccess.getSimpleName();
        } else {
            return UNINTERPRETABLE;
        }
        int end = start + segment.length();
        if (end < fullyQualifiedName.length() &&
                fullyQualifiedName.charAt(end) == '.' &&
                fullyQualifiedName.regionMatches(start, segment, 0, segment.length())) {
            return end + 1;
        }
        return NO_MATCH;
    }

    @SuppressWarnings("deprecation")
    private static String packageNameByPrinting(Expression expression) {
        return expression.printTrimmed().replaceAll("\\s", "");
    }
}
