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
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.style.ImportLayoutStyle;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class AddImportTest implements RewriteTest {

    static Recipe importTypeRecipe(String type) {
        return toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {

                maybeAddImport(type, null, false);
                return cu;
            }
        });
    }

    static Recipe importTypeRecipe(String packageName, String typeName, String alias) {
        return toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                maybeAddImport(packageName, typeName, null, alias, false);
                return cu;
            }
        });
    }

    static Recipe importMemberRecipe(String type, String member) {
        return toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                maybeAddImport(type, member, false);
                return cu;
            }
        });
    }

    public static ImportLayoutStyle importAliasesSeparatelyStyle() {
        // same style as `IntelliJ.importLayout()` but just with `importAliasesSeparately` as false
        return ImportLayoutStyle.builder()
          .importAliasesSeparately(true)
          .packageToFold("kotlinx.android.synthetic.*", true)
          .packageToFold("io.ktor.*", true)
          .importAllOthers()
          .importPackage("java.*")
          .importPackage("javax.*")
          .importPackage("kotlin.*")
          .importAllAliases()
          .build();
    }

    @DocumentExample
    @Test
    void jvmStaticMember() {
        rewriteRun(
          spec -> spec.recipe(importMemberRecipe("java.lang.Integer", "MAX_VALUE")),
          kotlin(
            """
              import java.lang.Integer
              import java.lang.Long

              class A
              """,
            """
              import java.lang.Integer
              import java.lang.Integer.MAX_VALUE
              import java.lang.Long
              
              class A
              """
          )
        );
    }

    @Test
    void normalClass() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("a.b.Target")),
          kotlin(
            """
              package a.b
              class Original
              """
          ),
          kotlin(
            """
              package a.b
              class Target
              """
          ),
          kotlin(
            """
              import a.b.Original
              
              class A {
                  val type : Original = Original()
              }
              """,
            """
              import a.b.Original
              import a.b.Target
              
              class A {
                  val type : Original = Original()
              }
              """
          )
        );
    }

    @Test
    void starFoldPackageTypes() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("java.io.OutputStream")),
          kotlin(
            """
              import java.io.Closeable
              import java.io.File
              import java.io.FileInputStream
              import java.io.FileOutputStream

              class A
              """,
            """
              import java.io.*
              
              class A
              """
          )
        );
    }

    @Test
    void noStarFoldTypeMembers() {
        rewriteRun(
          spec -> spec.recipe(importMemberRecipe("java.util.regex.Pattern", "MULTILINE")),
          kotlin(
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE

              class A
              """,
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE
              import java.util.regex.Pattern.MULTILINE

              class A
              """
          )
        );
    }

    @Test
    void starFoldTypeMembers() {
        rewriteRun(
          spec -> spec.recipe(importMemberRecipe("java.util.regex.Pattern", "MULTILINE")),
          kotlin(
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE
              import java.util.regex.Pattern.COMMENTS

              class A
              """,
            """
              import java.util.regex.Pattern.*
              
              class A
              """
          )
        );
    }

    @Test
    void importAlias() {
        rewriteRun(
          spec -> spec.recipe(importMemberRecipe("java.util.regex.Pattern", "MULTILINE"))
            .parser(KotlinParser.builder().styles(singletonList(
            new NamedStyles(
              randomId(), "test", "test", "test", emptySet(),
              singletonList(importAliasesSeparatelyStyle())
            )
          ))),
          kotlin(
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE as i
              import java.util.regex.Pattern.COMMENTS as x

              class A
              """,
            """
              import java.util.regex.Pattern.MULTILINE

              import java.util.regex.Pattern.CASE_INSENSITIVE as i
              import java.util.regex.Pattern.COMMENTS as x
              
              class A
              """
          )
        );
    }

    @Test
    void packageLevelFunction() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("a.b.method")),
          kotlin(
            """
              package a.b
              class Original
              """
          ),
          kotlin(
            """
              package a.b
              fun method() {}
              """
          ),
          kotlin(
            """
              import a.b.Original
              
              class A {
                  val type : Original = Original()
              }
              """,
            """
              import a.b.Original
              import a.b.method
              
              class A {
                  val type : Original = Original()
              }
              """
          )
        );
    }

    @Test
    void importOrdering() {
        rewriteRun(
          spec -> spec.recipe(
            importTypeRecipe("java.util.LinkedList")
          ),
          kotlin(
            """
              import java.util.HashMap
              import java.util.StringJoiner

              class A {
              }
              """,
            """
              import java.util.HashMap
              import java.util.LinkedList
              import java.util.StringJoiner

              class A {
              }
              """
          )
        );
    }

    @Test
    void importOrderingWithAlias() {
        rewriteRun(
          spec -> spec.recipe(
            importTypeRecipe("java.util", "LinkedList", "MyList")
          ),
          kotlin(
            """
              import java.util.Calendar as CA
              import java.util.HashMap
              import java.util.StringJoiner as MyStringJoiner

              class A {
              }
              """,
            """
              import java.util.Calendar as CA
              import java.util.HashMap
              import java.util.LinkedList as MyList
              import java.util.StringJoiner as MyStringJoiner

              class A {
              }
              """
          )
        );
    }

    @Test
    void importOrderingWithAliasImportAliasesSeparately() {
        rewriteRun(
          spec -> spec.recipe(
            importTypeRecipe("java.util", "LinkedList", "MyList")
          ).parser(KotlinParser.builder().styles(singletonList(
            new NamedStyles(
              randomId(), "test", "test", "test", emptySet(),
              singletonList(importAliasesSeparatelyStyle())
            )
          ))),
          kotlin(
            """
              import java.util.HashMap
              
              import java.util.Calendar as CA
              import java.util.StringJoiner as MyStringJoiner

              class A {
              }
              """,
            """
              import java.util.HashMap
              
              import java.util.Calendar as CA
              import java.util.LinkedList as MyList
              import java.util.StringJoiner as MyStringJoiner

              class A {
              }
              """
          )
        );
    }

    @Test
    void noImportOfImplicitTypes() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("kotlin.Pair")),
          kotlin(
            """
              class A
              """
          )
        );
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("java.lang.Integer")),
          kotlin(
            """
              class A
              """
          )
        );
    }

    @Test
    void addJavaStaticImport() {
        rewriteRun(
          spec -> spec.recipe(importMemberRecipe("org.junit.jupiter.api.Assertions", "assertFalse")),
          kotlin(
            """
              class Foo
              """,
            """
              import org.junit.jupiter.api.Assertions.assertFalse
              
              class Foo
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/493")
    @SuppressWarnings("RemoveRedundantBackticks")
    @Test
    void addEscapedImport() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("`java`.`util`.`List`")),
          kotlin(
            """
              class A
              """,
            """
              import `java`.`util`.`List`
              
              class A
              """,
            spec -> spec.afterRecipe(cu -> {
                AtomicBoolean found = new AtomicBoolean(false);
                new KotlinIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                        assertThat(identifier.getSimpleName().startsWith("`")).isFalse();
                        return super.visitIdentifier(identifier, integer);
                    }

                    @Override
                    public J.Import visitImport(J.Import _import, Integer integer) {
                        assertThat(_import.getQualid().getType()).isNotNull();
                        assertThat(_import.getQualid().getType().toString()).isEqualTo("java.util.List");
                        found.set(true);
                        return super.visitImport(_import, integer);
                    }
                }.visit(cu, 0);
                assertThat(found.get()).isTrue();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/500")
    @Test
    void addImportWithUseSiteAnnotation() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("java.util.List")),
          kotlin(
            """
              @file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

              class A
              """,
            """
              @file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

              import java.util.List

              class A
              """
          )
        );
    }
}
