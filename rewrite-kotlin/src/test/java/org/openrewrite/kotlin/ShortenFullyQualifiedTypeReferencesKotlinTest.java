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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class ShortenFullyQualifiedTypeReferencesKotlinTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ShortenFullyQualifiedTypeReferences());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8237")
    @Test
    void doesNotShortenNestedTypeReachedViaImportedOuter() {
        rewriteRun(
          kotlin(
            """
              import java.util.Locale

              val category = Locale.Category.DISPLAY
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8237")
    @Test
    void doesNotShortenNestedTypeReachedViaAliasedOuter() {
        // The outer type is imported under an alias, so `Loc.Category` matches neither the
        // fully-qualified nor the package-relative name of the nested `Category`. It must still
        // keep its own nested type and be left unchanged.
        rewriteRun(
          kotlin(
            """
              import java.util.Locale as Loc

              val category = Loc.Category.DISPLAY
              """
          )
        );
    }
}
