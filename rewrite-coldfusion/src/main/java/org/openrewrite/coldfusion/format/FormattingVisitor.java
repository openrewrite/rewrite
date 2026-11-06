/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.ColdFusionIsoVisitor;
import org.openrewrite.coldfusion.internal.TemplateOutput;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.coldfusion.tree.CfSpace;
import org.openrewrite.java.tree.*;

/**
 * What the formatting visitors share: stopping after {@code stopAfter}, knowing which space is an element's prefix,
 * and knowing which whitespace lies between tags and is written to a template's output.
 */
abstract class FormattingVisitor<P> extends ColdFusionIsoVisitor<P> {
    private static final String PREFIX = "org.openrewrite.coldfusion.format.prefix";

    @Nullable
    protected final Tree stopAfter;

    FormattingVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            //noinspection unchecked
            return (J) tree;
        }
        return super.visit(tree, p);
    }

    @Override
    public @Nullable J preVisit(J tree, P p) {
        // every element's prefix is the first space visited at its cursor
        getCursor().putMessage(PREFIX, Boolean.TRUE);
        return super.preVisit(tree, p);
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        return whitespace(space, loc, isPrefix(), p);
    }

    @Override
    public Space visitSpace(Space space, CfSpace.Location loc, P p) {
        return whitespace(space, loc, isPrefix(), p);
    }

    /**
     * Formats a space, which is a {@link Space.Location} or a {@link CfSpace.Location}. {@code prefix} says whether it
     * is the prefix of the element at the cursor.
     */
    protected Space whitespace(Space space, Object loc, boolean prefix, P p) {
        return space;
    }

    private boolean isPrefix() {
        Cursor cursor = getCursor();
        return cursor.getValue() instanceof J && cursor.pollMessage(PREFIX) != null;
    }

    /**
     * Whether the space being visited lies between tags, where a template's text and whitespace are.
     */
    protected boolean isBetweenTags(Object loc, boolean prefix) {
        Cursor cursor = getCursor();
        Object value = cursor.getValue();
        if (value instanceof Cf.CompilationUnit) {
            return TemplateOutput.isTemplate((Cf.CompilationUnit) value);
        }
        if (prefix) {
            return value instanceof Statement && TemplateOutput.isInTemplateList(cursor);
        }
        if (loc == Space.Location.BLOCK_END || loc == Space.Location.BLOCK_PREFIX ||
            loc == Space.Location.BLOCK_STATEMENT_SUFFIX) {
            return TemplateOutput.isTemplateBody(cursor);
        }
        if (loc == Space.Location.METHOD_DECLARATION_PARAMETER_SUFFIX && value instanceof JContainer) {
            Object method = cursor.getParentTreeCursor().getValue();
            return method instanceof J.MethodDeclaration && TemplateOutput.isTagSyntax((J) method);
        }
        return false;
    }

    /**
     * Whether a template writes the space being visited to its output. Whitespace inside tags and in script never is.
     */
    protected boolean isWritten(Object loc, boolean prefix) {
        if (!isBetweenTags(loc, prefix)) {
            return false;
        }
        Cursor cursor = getCursor();
        if (prefix) {
            return TemplateOutput.writesBefore(cursor);
        }
        if (loc == Space.Location.BLOCK_END || loc == Space.Location.COMPILATION_UNIT_EOF) {
            return TemplateOutput.writesAtEnd(cursor);
        }
        // the space before a body's first statement and after a statement in a body, which are empty
        return true;
    }

    /**
     * Whether the space being visited is inside a string, as an interpolation in {@code "Hello #name#"} is.
     */
    protected boolean isInString() {
        for (Cursor c = getCursor(); c != null; c = c.getParent()) {
            Object value = c.getValue();
            if (value instanceof Cf.StringTemplate) {
                return true;
            }
            if (value instanceof JavaSourceFile) {
                return false;
            }
        }
        return false;
    }

    /**
     * Whether the space being visited is inside an interpolation, {@code #expression#}, whose author's spacing stays.
     */
    protected boolean isInInterpolation() {
        for (Cursor c = getCursor(); c != null; c = c.getParent()) {
            Object value = c.getValue();
            if (value instanceof Cf.Interpolation || value instanceof Cf.StringTemplate) {
                return true;
            }
            if (value instanceof JavaSourceFile) {
                return false;
            }
        }
        return false;
    }
}
