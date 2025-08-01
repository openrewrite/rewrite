/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class RemoveImportTest implements RewriteTest {

    static Recipe removeTypeImportRecipe(String type) {
        return toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                maybeRemoveImport(type);
                return cu;
            }
        });
    }

    static Recipe removeMemberImportRecipe(String type, String member) {
        return toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                maybeRemoveImport(type + '.' + member);
                return cu;
            }
        });
    }

    @DocumentExample
    @Test
    void jvmStaticMember() {
        rewriteRun(
          spec -> spec.recipe(removeMemberImportRecipe("java.lang.Integer", "MAX_VALUE")),
          kotlin(
            """
              import java.lang.Integer
              import java.lang.Long

              import java.lang.Integer.MAX_VALUE

              class A
              """,
            """
              import java.lang.Integer
              import java.lang.Long

              class A
              """
          )
        );
    }

    @Test
    void removeStarFoldPackage() {
        rewriteRun(
          spec -> spec.recipe(removeTypeImportRecipe("java.io.OutputStream")).expectedCyclesThatMakeChanges(2),
          kotlin(
            """
              import java.io.*

              class A {
                  val f = File("foo")
              }
              """,
            """
              import java.io.File

              class A {
                  val f = File("foo")
              }
              """
          )
        );
    }

    @Test
    void keepStarFoldPackage() {
        rewriteRun(
          spec -> spec.recipe(removeTypeImportRecipe("java.io.OutputStream")),
          kotlin(
            """
              import java.io.*

              class A {
                  val c : Closeable? = null
                  val f : File? = null
                  val ff : FileFilter? = null
                  val fin : FileInputStream? = null
                  val fos : FileOutputStream? = null
              }
              """
          )
        );
    }

    @Test
    void removeStarFoldTypeMembers() {
        rewriteRun(
          spec -> spec.recipe(removeMemberImportRecipe("java.util.regex.Pattern", "MULTILINE")),
          kotlin(
            """
              import java.util.regex.Pattern.*

              class A {
                  val i = CASE_INSENSITIVE
                  val x = COMMENTS
              }
              """,
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE
              import java.util.regex.Pattern.COMMENTS

              class A {
                  val i = CASE_INSENSITIVE
                  val x = COMMENTS
              }
              """
          )
        );
    }

    @Test
    void keepStarFoldTypeMembers() {
        rewriteRun(
          spec -> spec.recipe(removeMemberImportRecipe("java.util.regex.Pattern", "DOTALL")),
          kotlin(
            """
              import java.util.regex.Pattern.*

              class A {
                  val i = CASE_INSENSITIVE
                  val x = COMMENTS
                  val m = MULTILINE
              }
              """
          )
        );
    }

    @Test
    void keepImportAlias() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(removeMemberImportRecipe("java.util.regex.Pattern", "COMMENTS")),
          kotlin(
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE as i
              import java.util.regex.Pattern.COMMENTS as x

              class A {
                  val f = arrayOf(i, x)
              }
              """
          )
        );
    }

    @Test
    void removeImportAlias() {
        // TODO check if this is really what we want to happen
        rewriteRun(
          // Type validation is disabled until https://github.com/openrewrite/rewrite-kotlin/issues/545 is implemented.
          spec -> spec.typeValidationOptions(TypeValidation.none())
            .recipe(removeMemberImportRecipe("java.util.regex.Pattern", "COMMENTS")),
          kotlin(
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE as i
              import java.util.regex.Pattern.COMMENTS as x

              class A {
                  val f = arrayOf(i)
              }
              """,
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE as i

              class A {
                  val f = arrayOf(i)
              }
              """
          )
        );
    }

    @Test
    void noImportOfImplicitTypes() {
        rewriteRun(
          spec -> spec.recipe(removeTypeImportRecipe("kotlin.Pair")),
          kotlin(
            """
              class A
              """
          )
        );
        rewriteRun(
          spec -> spec.recipe(removeTypeImportRecipe("java.lang.Integer")),
          kotlin(
            """
              class A
              """
          )
        );
    }
}
