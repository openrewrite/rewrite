/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;

public class RepeatTest implements RewriteTest {

    /**
     * This visitor verifies that the cursor is well-formed.
     */
    static class VerifyCursorWellFormed<P> extends JavaVisitor<P> {
        @Override
        public J preVisit(J tree, P p) {
            assertNotNull(getCursor().firstEnclosing(JavaSourceFile.class), "JavaSourceFile should be accessible");
            assertNotEquals(getCursor().getParentOrThrow().getValue(), tree, "Tree should not be the same as its parent");
            return tree;
        }
    }

    static class VerifyCursorWellFormedInRepeat extends JavaVisitor<ExecutionContext> {
        @Override
        public J preVisit(J tree, ExecutionContext executionContext) {
            return (J) Repeat.repeatUntilStable(new VerifyCursorWellFormed<>()).visitNonNull(tree, executionContext, getCursor().getParentTreeCursor());
        }
    }

    @Test
    void repeatInPreVisit() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(VerifyCursorWellFormedInRepeat::new)),
          java("class A {}")
        );
    }

    public static class VerifyCursorWellFormedRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "Verify cursor well-formed";
        }

        @Override
        public String getDescription() {
            return "This recipe verifies that the cursor is well-formed.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Repeat.repeatUntilStable(new VerifyCursorWellFormed<>());
        }
    }

    @Test
    void repeatInRecipe() {
        rewriteRun(
          spec -> spec.recipe(new VerifyCursorWellFormedRecipe()),
          java("class A {}")
        );
    }

    static class VisitorThatFailsToSetCursor extends JavaVisitor<ExecutionContext> {
        @Override
        public @Nullable J preVisit(J tree, ExecutionContext executionContext) {
            return (J) Repeat.repeatUntilStable(new JavaVisitor<>()).visitNonNull(tree, executionContext);
        }
    }

    @Test
    void repeatValidatesCursorIsPassed() {
        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(VisitorThatFailsToSetCursor::new)),
              java("class A {}")
            );
        });
        assertThat(assertionError).cause().isInstanceOf(RecipeRunException.class);
        RecipeRunException e = (RecipeRunException) assertionError.getCause();
        assertThat(e.getMessage())
          .contains(
            "Repeat visitor called on a non-source file tree without a cursor pointing to the root of the tree. " +
            "Passed tree type: `org.openrewrite.java.tree.J$ClassDeclaration`. " +
            "This is likely a bug in the calling code. " +
            "Use a `visit` method that accepts a cursor instead."
          );
    }
}
