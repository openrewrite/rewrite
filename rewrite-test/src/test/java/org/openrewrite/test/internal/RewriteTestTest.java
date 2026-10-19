/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.test.internal;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.NoStaticImport;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.RemoveUnusedImports;
import org.openrewrite.java.UseStaticImport;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.BuildMetadata;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

class RewriteTestTest implements RewriteTest {

    @Test
    void rejectRecipeWithNameOption() {
        assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(new RecipeWithNameOption("test")),
          text(
            "hello world!"
          )
        ));
    }

    @Test
    void acceptsRecipeWithDescriptionListOfLinks() {
        validateRecipeNameAndDescription(new RecipeWithDescriptionListOfLinks());
    }

    @Test
    void acceptsRecipeWithDescriptionListOfDescribedLinks() {
        validateRecipeNameAndDescription(new RecipeWithDescriptionListOfDescribedLinks());
    }

    @Test
    void rejectsRecipeWithDescriptionNotEndingWithPeriod() {
        assertThrows(
          AssertionError.class,
          () -> validateRecipeNameAndDescription(new RecipeWithDescriptionNotEndingWithPeriod())
        );
    }

    @Test
    void verifyAll() {
        assertThrows(AssertionError.class, this::assertRecipesConfigure);
    }

    @Test
    void multipleFilesWithSamePath() {
        AssertionError e = assertThrows(AssertionError.class,
          () -> rewriteRun(
            spec -> spec.recipe(new CreatesTwoFilesSamePath()),
            text(null, "duplicate", spec -> spec.path("duplicate.txt"))));
        assertThat(e).hasStackTraceContaining("Recipe generated multiple source files at the same path");
    }

    @Test
    void generatesFileCollidingWithExistingFile() {
        AssertionError e = assertThrows(AssertionError.class,
          () -> rewriteRun(
            spec -> spec.recipe(new GeneratesExistingFile()),
            text("existing content", spec -> spec.path("existing.txt"))));
        assertThat(e).hasStackTraceContaining("Recipe generated a source file that already exists in the source set");
    }

    @Test
    void generatesFileWithInvalidPath() {
        AssertionError e = assertThrows(AssertionError.class,
          () -> rewriteRun(
            spec -> spec.recipe(new GeneratesFileWithInvalidPath()),
            text("hello")));
        assertThat(e).hasStackTraceContaining("Recipe generated a source file with an invalid path");
    }

    @Test
    void cursorValidation() {
        AssertionError e = assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipe(new ImproperCursorUsage()),
            text("")
          )
        );
        assertThat(e).hasStackTraceContaining("is a cursor on the same PlainText")
          .hasStackTraceContaining("getCursor().getParentOrThrow()");
    }


    @Test
    void rejectRecipeValidationFailure() {
        assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipeFromYaml("""
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.RefersToNonExistentRecipe
              displayName: Refers to non-existent recipe
              description: Deliberately has a non-existent recipe in its recipe list to trigger a validation failure.
              recipeList:
                - org.openrewrite.DoesNotExist
              
              """, "org.openrewrite.RefersToNonExistentRecipe")
          ));
    }

    @Test
    void rejectExecutionContextMutation() {
        AssertionError e = assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipe(new MutateExecutionContext()),
            text("irrelevant")
          ));
        assertThat(e).hasStackTraceContaining("in a ScanningRecipe's accumulator")
          .hasStackTraceContaining("in Cursor messages")
          .hasStackTraceContaining("executionContextUnchanged(false)");
    }

    @Test
    void rejectScannerEdit() {
        AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(new ScannerEdit()),
          text("foo")
        ));
        assertThat(e).hasStackTraceContaining("make the edits in getVisitor(acc)")
          .hasStackTraceContaining("noEditsInScanningPhase(false)");
    }

    @Test
    void typeValidationAppliedToNewSourceFiles() {
        assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipe(new GeneratesCompilationUnitMissingTypes()),
            java(null, """
              public class A {
                  void foo() {
                  }
              }
              """,
              s -> s.path("A.java"))
          )
        );
    }

    @Test
    void allowScannerEdit() {
        rewriteRun(
          spec -> spec
            .recipeQualityValidation(RecipeQualityValidation.builder().noEditsInScanningPhase(false).build())
            .recipe(new ScannerEdit()),
          text("foo")
        );
    }

    @Nested
    class AddedMarkers {

        @Test
        void rejectCustomMarkerAddedAlongsideChange() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                      if (!"foo".equals(m.getSimpleName())) {
                          return m;
                      }
                      return m.withName(m.getName().withSimpleName("bar"))
                        .withMarkers(m.getMarkers().add(new Visited(Tree.randomId())));
                  }
              })),
              java(
                """
                  class A {
                      void foo() {}
                      void bar() {}
                      void test() { foo(); }
                  }
                  """,
                """
                  class A {
                      void foo() {}
                      void bar() {}
                      void test() { bar(); }
                  }
                  """
              )
            ));
            assertThat(e).hasMessageContaining("The recipe added markers to the LST")
              .hasMessageContaining("org.openrewrite.test.internal.Visited on J.MethodInvocation `bar()`");
        }

        @Test
        void rejectCustomMarkerAddedWithoutChange() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                      J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                      return c.getMarkers().findFirst(Visited.class).isPresent() ? c :
                        c.withMarkers(c.getMarkers().add(new Visited(Tree.randomId())));
                  }
              })),
              java(
                """
                  class A {
                  }
                  """
              )
            ));
            assertThat(e).hasMessageContaining("The recipe added markers to the LST")
              .hasMessageContaining("A.java (the printed source is unchanged)")
              .hasMessageContaining("org.openrewrite.test.internal.Visited on J.ClassDeclaration `class A {...`");
        }

        @Test
        void rejectSourceFileMarkerAttachedToElement() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                      return m.getMarkers().findFirst(BuildMetadata.class).isPresent() ? m :
                        m.withMarkers(m.getMarkers().add(new BuildMetadata(Tree.randomId(), Map.of("origin", "foo"))));
                  }
              })),
              java(
                """
                  class A {
                      void foo() {}
                      void test() { foo(); }
                  }
                  """,
                spec -> spec.markers(new BuildMetadata(Tree.randomId(), Map.of("team", "a")))
              )
            ));
            assertThat(e).hasMessageContaining("org.openrewrite.marker.BuildMetadata on J.MethodInvocation `foo()`");
        }

        @Test
        void notTurnedOffWithoutTypeValidation() {
            assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none())
                .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                        return c.getMarkers().findFirst(Visited.class).isPresent() ? c :
                          c.withMarkers(c.getMarkers().add(new Visited(Tree.randomId())));
                    }
                })),
              java(
                """
                  class A {
                  }
                  """
              )
            ));
        }

        @Test
        void turnedOff() {
            rewriteRun(
              spec -> spec.recipeQualityValidation(RecipeQualityValidation.builder().reportingMarkersOnly(false).build())
                .expectedCyclesThatMakeChanges(1)
                .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                        return c.getMarkers().findFirst(Visited.class).isPresent() ? c :
                          c.withMarkers(c.getMarkers().add(new Visited(Tree.randomId())));
                    }
                })),
              java(
                """
                  class A {
                  }
                  """
              )
            );
        }

        @Test
        void acceptSearchResult() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      return SearchResult.found(super.visitMethodInvocation(method, ctx));
                  }
              })),
              java(
                """
                  class A {
                      void foo() {}
                      void test() { foo(); }
                  }
                  """,
                """
                  class A {
                      void foo() {}
                      void test() { /*~~>*/foo(); }
                  }
                  """
              )
            );
        }

        @Test
        void acceptSyntaxMarkerTheParserProduces() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                      return m.withArguments(ListUtils.map(m.getArguments(), arg ->
                        arg.getMarkers().findFirst(OmitParentheses.class).isPresent() ? arg :
                          arg.withPrefix(Space.SINGLE_SPACE)
                            .withMarkers(arg.getMarkers().add(new OmitParentheses(Tree.randomId())))));
                  }
              })),
              groovy(
                """
                  println("hello")
                  """,
                """
                  println "hello"
                  """
              )
            );
        }

        @Test
        void acceptSyntaxMarkersOfTheLanguageASourceIsConvertedTo() {
            rewriteRun(
              spec -> spec.parser(GroovyParser.builder())
                .expectedCyclesThatMakeChanges(1)
                .recipe(toRecipe(() -> new TreeVisitor<Tree, ExecutionContext>() {
                    @Override
                    public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                        if (!(tree instanceof PlainText text)) {
                            return tree;
                        }
                        return GroovyParser.builder().build().parse(text.getText())
                          .findFirst()
                          .<Tree>map(g -> ((SourceFile) g.withId(text.getId())).withSourcePath(text.getSourcePath()))
                          .orElse(text);
                    }
                })),
              text("println \"hello\"")
            );
        }
    }

    @Nested
    class UnchangedTreeIdentity {

        @Test
        void rejectEqualCopyOfUnchangedFile() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(copiesModifiers()),
              java(
                """
                  public class A {}
                  """
              )
            ));
            assertThat(e).hasMessageContaining("it must return the LST it was given, not an equal copy")
              .hasMessageContaining("org.openrewrite.test.AdHocRecipe returned an equal copy of A.java in cycle 1")
              .hasMessageContaining("J.ClassDeclaration `public class A {}` > modifiers: " +
                                    "a new ArrayList with the same 1 elements");
        }

        @Test
        void rejectEqualCopyWhateverTheCycleSettings() {
            // these settings accept a change in the only cycle, but an equal copy is not one
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1).recipe(copiesModifiers()),
              java(
                """
                  public class A {}
                  """
              )
            ));
            assertThat(e).hasMessageContaining("returned an equal copy of A.java in cycle 1");
        }

        @Test
        void rejectEqualCopyAfterRealChange() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.expectedCyclesThatMakeChanges(2).recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                      // changes 1 to 2, then keeps returning 2 with a recomputed String
                      Object value = literal.getValue();
                      if (Integer.valueOf(1).equals(value) || Integer.valueOf(2).equals(value)) {
                          return literal.withValue(2).withValueSource(String.valueOf(2));
                      }
                      return literal;
                  }
              })),
              java(
                """
                  class A {
                      int i = 1;
                  }
                  """,
                """
                  class A {
                      int i = 2;
                  }
                  """
              )
            ));
            assertThat(e).hasMessageContaining("returned an equal copy of A.java in cycle 2")
              .hasMessageContaining("J.Literal `2` > valueSource: an equal String that is not the same instance \"2\"");
        }

        @Test
        void acceptOriginalTree() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                      J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                      // ListUtils.map returns the original list when it changes no element
                      return c.withModifiers(ListUtils.map(c.getModifiers(), m -> m));
                  }
              })),
              java(
                """
                  public class A {}
                  """
              )
            );
        }

        @Test
        void acceptEqualCopyInsideChangedFile() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                      J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                      // copying an unrelated list is harmless when the class changes anyway
                      return c.getBody() == classDecl.getBody() ? c :
                        c.withModifiers(new ArrayList<>(c.getModifiers()));
                  }

                  @Override
                  public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                      return Integer.valueOf(1).equals(literal.getValue()) ?
                        literal.withValue(2).withValueSource("2") : literal;
                  }
              })),
              java(
                """
                  public class A {
                      int i = 1;
                  }
                  """,
                """
                  public class A {
                      int i = 2;
                  }
                  """
              )
            );
        }

        @Test
        void acceptEqualCopyOfFileGeneratedInSameCycle() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new PlainTextVisitor<>() {
                  @Override
                  public PlainText visitText(PlainText text, ExecutionContext ctx) {
                      return text.withSourcePath(Path.of(text.getSourcePath().toString()));
                  }
              }).withMaxCycles(1).withGenerator(() -> List.of(PlainText.builder()
                .sourcePath(Path.of("generated.txt"))
                .text("generated")
                .build()))),
              text(null, "generated", spec -> spec.path("generated.txt"))
            );
        }

        @Test
        void notTurnedOffWithoutTypeValidation() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none()).recipe(copiesModifiers()),
              java(
                """
                  public class A {}
                  """
              )
            ));
            assertThat(e).hasMessageContaining("returned an equal copy of A.java in cycle 1");
        }

        private Recipe copiesModifiers() {
            return toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                    return c.withModifiers(new ArrayList<>(c.getModifiers()));
                }
            });
        }
    }

    @Nested
    class FormattingScope {

        // Formatted, except for one line away from the call the recipes rename
        private static final String BEFORE = """
          class A {
              void foo() {
              }

              void bar() {
              }

              void test() {
                  foo();
              }

              void other() {
                    int unrelated = 1;
              }
          }
          """;

        @Test
        void rejectFormattingTheWholeFileAfterOneLineEdit() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                      return maybeAutoFormat(cu, super.visitCompilationUnit(cu, ctx), ctx);
                  }

                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      return renameFooToBar(super.visitMethodInvocation(method, ctx));
                  }
              })),
              java(
                BEFORE,
                """
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          bar();
                      }

                      void other() {
                          int unrelated = 1;
                      }
                  }
                  """
              )
            ));
            assertThat(e).hasMessageStartingWith("Format only what the recipe changed")
              .hasMessageContaining("$1.visitCompilationUnit(RewriteTestTest.java:")
              .hasMessageContaining("called maybeAutoFormat on\n    all of A.java");
        }

        @Test
        void acceptFormattingOnlyTheChangedElement() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                      return maybeAutoFormat(m, renameFooToBar(m), ctx);
                  }
              })),
              java(
                BEFORE,
                """
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          bar();
                      }

                      void other() {
                            int unrelated = 1;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void rejectFormattingTheWholeClassAfterAddingAnAnnotation() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                      J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                      return maybeAutoFormat(c, deprecate(c, getCursor()), ctx);
                  }
              })),
              java(
                BEFORE,
                """
                  @Deprecated
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          foo();
                      }

                      void other() {
                          int unrelated = 1;
                      }
                  }
                  """
              )
            ));
            assertThat(e).hasMessageContaining("called maybeAutoFormat on\n    class `A` of A.java")
              .hasMessageContaining("JavaTemplate and its coordinates (addAnnotation, addStatement) already format");
        }

        @Test
        void rejectFormattingTheWholeClassEvenWhenTheTestsClassIsEmpty() {
            assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                      J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                      return maybeAutoFormat(c, deprecate(c, getCursor()), ctx);
                  }
              })),
              java(
                """
                  class A {
                  }
                  """,
                """
                  @Deprecated
                  class A {
                  }
                  """
              )
            ));
        }

        @Test
        void acceptFormattingOnlyTheClassHeader() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                      J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                      J.ClassDeclaration deprecated = deprecate(c, getCursor());
                      return maybeAutoFormat(c, deprecated, deprecated.getName(), ctx,
                        getCursor().getParentTreeCursor());
                  }
              })),
              java(
                BEFORE,
                """
                  @Deprecated
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          foo();
                      }

                      void other() {
                            int unrelated = 1;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void acceptFormattingTheClassWithItsBodySetAside() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                      J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                      J.ClassDeclaration deprecated = deprecate(c, getCursor());
                      if (deprecated == c) {
                          return c;
                      }
                      J.Block body = deprecated.getBody();
                      return maybeAutoFormat(c, deprecated.withBody(body.withStatements(emptyList())), ctx)
                        .withBody(body);
                  }
              })),
              java(
                BEFORE,
                """
                  @Deprecated
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          foo();
                      }

                      void other() {
                            int unrelated = 1;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void rejectAutoFormatVisitorRunAfterTheVisit() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                      J.MethodInvocation renamed = renameFooToBar(m);
                      if (renamed != m) {
                          doAfterVisit(new AutoFormatVisitor<>());
                      }
                      return renamed;
                  }
              })),
              java(
                BEFORE,
                """
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          bar();
                      }

                      void other() {
                          int unrelated = 1;
                      }
                  }
                  """
              )
            ));
            assertThat(e)
              .hasMessageContaining("A visitor's doAfterVisit(new AutoFormatVisitor<>()) ran on\n    all of A.java");
        }

        @Test
        void acceptFormattingRecipe() {
            rewriteRun(
              spec -> spec.recipe(new AutoFormat(null)),
              java(
                BEFORE,
                """
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          foo();
                      }

                      void other() {
                          int unrelated = 1;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void acceptFormattingThatOnlyChangesWhitespace() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                      return autoFormat(cu, ctx);
                  }
              })),
              java(
                BEFORE,
                """
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          foo();
                      }

                      void other() {
                          int unrelated = 1;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void notTurnedOffWithoutTypeValidation() {
            assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none())
                .recipe(formatWholeClassAfterRename()),
              java(
                BEFORE,
                """
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          bar();
                      }

                      void other() {
                          int unrelated = 1;
                      }
                  }
                  """
              )
            ));
        }

        @Test
        void turnedOff() {
            rewriteRun(
              spec -> spec.recipeQualityValidation(RecipeQualityValidation.builder().minimalFormatting(false).build())
                .recipe(formatWholeClassAfterRename()),
              java(
                BEFORE,
                """
                  class A {
                      void foo() {
                      }

                      void bar() {
                      }

                      void test() {
                          bar();
                      }

                      void other() {
                          int unrelated = 1;
                      }
                  }
                  """
              )
            );
        }

        private Recipe formatWholeClassAfterRename() {
            return toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                    J.MethodInvocation renamed = renameFooToBar(m);
                    if (renamed != m) {
                        getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, "renamed", true);
                    }
                    return renamed;
                }

                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                    J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                    return getCursor().getMessage("renamed", false) ? autoFormat(c, ctx) : c;
                }
            });
        }

        private J.MethodInvocation renameFooToBar(J.MethodInvocation m) {
            return "foo".equals(m.getSimpleName()) ? m.withName(m.getName().withSimpleName("bar")) : m;
        }

        private J.ClassDeclaration deprecate(J.ClassDeclaration c, Cursor cursor) {
            if (!c.getLeadingAnnotations().isEmpty()) {
                return c;
            }
            return JavaTemplate.apply("@Deprecated", cursor,
              c.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
        }
    }

    @Nested
    class MatchesByType {

        @Test
        void rejectRecipeThatThrowsOnFullyQualifiedType() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new FindCallsOnImportedType("java.util.Collections emptyList()")),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = /*~~(Collections)~~>*/Collections.emptyList();
                  }
                  """
              )
            ));
            assertThat(e).hasMessageStartingWith("Check the shape of the code the recipe edits before casting it")
              .hasMessageContaining("A.java (changed by org.openrewrite.test.internal.FindCallsOnImportedType), " +
                                    "`Collections` written `java.util.Collections`:\n" +
                                    "    java.lang.ClassCastException")
              .hasMessageContaining("at org.openrewrite.test.internal.FindCallsOnImportedType$1.visitMethodInvocation(" +
                                    "RewriteTestTest.java:")
              .hasMessageNotContaining("Match the code the recipe changes by its type attribution");
        }

        @Test
        void rejectRecipeThatMissesStaticallyImportedCall() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new FindQualifiedCallsByName("Collections", "emptyList")),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = /*~~>*/Collections.emptyList();
                  }
                  """
              )
            ));
            assertThat(e).hasMessageStartingWith("Match the code the recipe changes by its type attribution")
              .hasMessageContaining("A.java (changed by org.openrewrite.test.internal.FindQualifiedCallsByName): " +
                                    "`Collections.emptyList()` written `emptyList()`")
              .hasMessageContaining("matchByTypeNotName(false)");
        }

        @Test
        void rejectRecipeThatMissesQualifiedCall() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new FindUnqualifiedCallsByName("emptyList")),
              java(
                """
                  import java.util.List;

                  import static java.util.Collections.emptyList;

                  class A {
                      List<String> list = emptyList();
                  }
                  """,
                """
                  import java.util.List;

                  import static java.util.Collections.emptyList;

                  class A {
                      List<String> list = /*~~>*/emptyList();
                  }
                  """
              )
            ));
            assertThat(e).hasMessageContaining("`emptyList()` written `Collections.emptyList()`");
        }

        @Test
        void importCleanupDoesNotCountAsTheChange() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipes(new FindQualifiedCallsByName("Collections", "emptyList"), new RemoveUnusedImports()),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = /*~~>*/Collections.emptyList();
                  }
                  """
              )
            ));
            assertThat(e).hasMessageContaining("`Collections.emptyList()` written `emptyList()`");
        }

        @Test
        void rejectRecipeThatMissesFullyQualifiedType() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new FindAnnotationsByPrintedType("Deprecated")),
              java(
                """
                  class A {
                      @Deprecated
                      void old() {
                      }
                  }
                  """,
                """
                  class A {
                      /*~~>*/@Deprecated
                      void old() {
                      }
                  }
                  """
              )
            ));
            assertThat(e).hasMessageContaining("`Deprecated` written `java.lang.Deprecated`");
        }

        @Test
        void nameTheRespellingTheRecipeMisses() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new FindCallsWithPrintedArgument("Boolean.TRUE")),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<Boolean> list = Collections.singletonList(Boolean.TRUE);
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<Boolean> list = /*~~>*/Collections.singletonList(Boolean.TRUE);
                  }
                  """
              )
            ));
            assertThat(e).hasMessageContaining("A.java (changed by org.openrewrite.test.internal." +
                                               "FindCallsWithPrintedArgument): `Boolean` written `java.lang.Boolean`\n");
        }

        @Test
        void acceptRecipeThatMatchesAnnotationsByType() {
            rewriteRun(
              spec -> spec.recipe(new FindAnnotations("@java.lang.Deprecated", null)),
              java(
                """
                  class A {
                      @Deprecated
                      void old() {
                      }
                  }
                  """,
                """
                  class A {
                      /*~~>*/@Deprecated
                      void old() {
                      }
                  }
                  """
              )
            );
        }

        @Test
        void acceptTypesDeclaredInSkippedSources() {
            rewriteRun(
              spec -> spec.recipe(new FindAnnotations("@org.example.Foo", null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;

                  @Foo
                  class A {
                  }
                  """,
                """
                  import org.example.Foo;

                  /*~~>*/@Foo
                  class A {
                  }
                  """
              )
            );
        }

        @Test
        void acceptRecipeThatMatchesCallsByType() {
            rewriteRun(
              spec -> spec.recipe(new FindCallsByType("java.util.Collections emptyList()")),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = /*~~>*/Collections.emptyList();
                  }
                  """
              ),
              java(
                """
                  import java.util.List;

                  import static java.util.Collections.emptyList;

                  class B {
                      List<String> list = emptyList();
                  }
                  """,
                """
                  import java.util.List;

                  import static java.util.Collections.emptyList;

                  class B {
                      List<String> list = /*~~>*/emptyList();
                  }
                  """
              )
            );
        }

        @Test
        void acceptRecipeWhoseChangeIsTheRespelling() {
            rewriteRun(
              spec -> spec.recipe(new UseStaticImport("java.util.Collections emptyList()")),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """,
                """
                  import java.util.List;

                  import static java.util.Collections.emptyList;

                  class A {
                      List<String> list = emptyList();
                  }
                  """
              )
            );
            rewriteRun(
              spec -> spec.recipe(new NoStaticImport("java.util.Collections emptyList()")),
              java(
                """
                  import java.util.List;

                  import static java.util.Collections.emptyList;

                  class A {
                      List<String> list = emptyList();
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """
              )
            );
        }

        @Test
        void acceptImportOnlyChanges() {
            rewriteRun(
              spec -> spec.recipe(new OrderImports(false, null)),
              java(
                """
                  import java.util.List;
                  import java.util.ArrayList;

                  import static java.util.Collections.emptyList;

                  class A {
                      List<String> list = emptyList();
                      List<String> other = new ArrayList<>();
                  }
                  """,
                """
                  import java.util.ArrayList;
                  import java.util.List;

                  import static java.util.Collections.emptyList;

                  class A {
                      List<String> list = emptyList();
                      List<String> other = new ArrayList<>();
                  }
                  """
              )
            );
        }

        @Test
        void acceptChangeToTheTypeOfAStaticallyImportedCall() {
            rewriteRun(
              spec -> spec.recipe(new ChangeType("java.util.Collections", "com.example.Lists", true)),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """,
                """
                  import com.example.Lists;

                  import java.util.List;

                  class A {
                      List<String> list = Lists.emptyList();
                  }
                  """
              )
            );
        }

        @Test
        void acceptImportChangesThatMoveComments() {
            rewriteRun(
              spec -> spec.recipe(new OrderImports(true, null)),
              java(
                """
                  import static java.util.Collections.*; // helpers
                  import java.util.List;

                  class A {
                      List<String> empty = emptyList();
                  }
                  """,
                """
                  import java.util.List;

                  import static java.util.Collections.emptyList; // helpers

                  class A {
                      List<String> empty = emptyList();
                  }
                  """
              )
            );
        }

        @Test
        void matchByTypeNotNameTurnedOff() {
            rewriteRun(
              spec -> spec.recipe(new FindQualifiedCallsByName("Collections", "emptyList"))
                .recipeQualityValidation(RecipeQualityValidation.builder().matchByTypeNotName(false).build()),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = /*~~>*/Collections.emptyList();
                  }
                  """
              )
            );
        }

        @Test
        void notCheckedWithoutTypeValidation() {
            rewriteRun(
              spec -> spec.recipe(new FindQualifiedCallsByName("Collections", "emptyList"))
                .typeValidationOptions(TypeValidation.none()),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = /*~~>*/Collections.emptyList();
                  }
                  """
              )
            );
        }

        @Test
        void adHocRecipesAreNotChecked() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                      return m.getSelect() != null && "emptyList".equals(m.getSimpleName()) ? SearchResult.found(m) : m;
                  }
              })),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = Collections.emptyList();
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list = /*~~>*/Collections.emptyList();
                  }
                  """
              )
            );
        }
    }

    @Nested
    class EmptySources {

        @Test
        void rejectRecipeThatThrowsOnAnEmptyFile() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new ChangeFirstValue("rejected", null)),
              yaml(
                "key: value",
                "key: rejected",
                spec -> spec.path("application.yml").markers(new BuildMetadata(Tree.randomId(), Map.of("team", "a")))
              )
            ));
            assertThat(e).hasMessageStartingWith("Check for the structure the recipe edits before using it")
              .hasMessageContaining("org.openrewrite.test.internal.ChangeFirstValue threw when run on an empty " +
                                    "Yaml.Documents at application.yml with the markers of the test's source (BuildMetadata):\n" +
                                    "    java.lang.IndexOutOfBoundsException")
              .hasMessageContaining("at org.openrewrite.test.internal.ChangeFirstValue$1.visitDocument(RewriteTestTest.java:")
              .hasRootCauseInstanceOf(IndexOutOfBoundsException.class);
        }

        @Test
        void acceptRecipeThatChecksTheStructure() {
            rewriteRun(
              spec -> spec.recipe(new ChangeFirstValue("checked", true)),
              yaml(
                "key: value",
                "key: checked"
              )
            );
        }

        @Test
        void skipAdHocRecipe() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new YamlIsoVisitor<>() {
                  @Override
                  public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                      Yaml.Mapping mapping = (Yaml.Mapping) document.getBlock();
                      Yaml.Scalar scalar = (Yaml.Scalar) mapping.getEntries().get(0).getValue();
                      return document.withBlock(mapping.withEntries(ListUtils.mapFirst(mapping.getEntries(),
                        entry -> entry.withValue(scalar.withValue("adhoc")))));
                  }
              })),
              yaml(
                "key: value",
                "key: adhoc"
              )
            );
        }

        @Test
        void notTurnedOffWithoutTypeValidation() {
            assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none())
                .recipe(new ChangeFirstValue("none", null)),
              yaml(
                "key: value",
                "key: none"
              )
            ));
        }

        @Test
        void toleratedByTheTestsErrorHandler() {
            rewriteRun(
              spec -> spec.executionContext(new InMemoryExecutionContext())
                .recipe(new ChangeFirstValue("tolerated", null)),
              yaml(
                "key: value",
                "key: tolerated"
              )
            );
        }

        @Test
        void emptyFileHasTheMarkersHooksGaveTheSource() {
            rewriteRun(
              spec -> spec.recipe(new RequiresBuildMetadata())
                .beforeRecipe(sources -> sources.replaceAll(source ->
                  source.withMarkers(source.getMarkers().add(new BuildMetadata(Tree.randomId(), Map.of("team", "a")))))),
              text(
                "original",
                "changed"
              )
            );
        }
    }

    @Nested
    class ImportsReferencedTypes {

        @Test
        void rejectRecipeThatNamesATypeWithoutImportingIt() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new UseObjectsIsNull(null)),
              java(
                """
                  class A {
                      boolean test(Object o) {
                          return o == null;
                      }
                  }
                  """,
                """
                  class A {
                      boolean test(Object o) {
                          return Objects.isNull(o);
                      }
                  }
                  """
              )
            ));
            assertThat(e).hasMessageStartingWith("Import the types and static members the code the recipe writes names")
              .hasMessageContaining("A.java (changed by org.openrewrite.test.internal.UseObjectsIsNull): " +
                                    "`Objects` (java.util.Objects)")
              .hasMessageContaining("importReferencedTypes(false)");
        }

        @Test
        void acceptRecipeThatImportsWhatItNames() {
            rewriteRun(
              spec -> spec.recipe(new UseObjectsIsNull(true)),
              java(
                """
                  class A {
                      boolean test(Object o) {
                          return o == null;
                      }
                  }
                  """,
                """
                  import java.util.Objects;

                  class A {
                      boolean test(Object o) {
                          return Objects.isNull(o);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void rejectTemplateThatReliesOnTheFilesImports() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new ReturnEmptyList(null)),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list() {
                          return null;
                      }
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list() {
                          return Collections.emptyList();
                      }
                  }
                  """
              )
            ));
            assertThat(e).hasMessageStartingWith("Declare the types and static members a template names with imports(..)")
              .hasMessageContaining("`Collections` (java.util.Collections) in a template applied at " +
                                    "org.openrewrite.test.internal.ReturnEmptyList$1.visitLiteral(RewriteTestTest.java:")
              .hasMessageNotContaining("Import the types and static members the code the recipe writes names");
        }

        @Test
        void acceptTemplateThatDeclaresWhatItNames() {
            rewriteRun(
              spec -> spec.recipe(new ReturnEmptyList(true)),
              java(
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list() {
                          return null;
                      }
                  }
                  """,
                """
                  import java.util.Collections;
                  import java.util.List;

                  class A {
                      List<String> list() {
                          return Collections.emptyList();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void optOut() {
            rewriteRun(
              spec -> spec.recipe(new UseObjectsIsNull(null))
                .recipeQualityValidation(RecipeQualityValidation.builder().importReferencedTypes(false).build()),
              java(
                """
                  class A {
                      boolean test(Object o) {
                          return o == null;
                      }
                  }
                  """,
                """
                  class A {
                      boolean test(Object o) {
                          return Objects.isNull(o);
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class ParserShapedTrees {

        @Test
        void rejectNodeTheParserBuildsDifferently() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new ReturnNullByHand(null)),
              java(
                """
                  class A {
                      Object f() {
                          return 0;
                      }
                  }
                  """,
                """
                  class A {
                      Object f() {
                          return null;
                      }
                  }
                  """
              )
            ));
            assertThat(e).hasMessageStartingWith("Write code with a template or the format's own parser")
              .hasMessageContaining("A.java (changed by org.openrewrite.test.internal.ReturnNullByHand):\n" +
                                    "    `null` has the String value \"null\" of type null where the parser makes the " +
                                    "value null of type null")
              .hasMessageContaining("parserShapedTrees(false)");
        }

        @Test
        void acceptNodeTheParserBuildsTheSame() {
            rewriteRun(
              spec -> spec.recipe(new ReturnNullByHand(true)),
              java(
                """
                  class A {
                      Object f() {
                          return 0;
                      }
                  }
                  """,
                """
                  class A {
                      Object f() {
                          return null;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void skipCodeNewerThanTheParser() {
            // the Java 21 parser this module tests with reads `_` as an error, which Java 25 code can declare
            rewriteRun(
              spec -> spec.recipe(new NameUnusedLoopVariable()),
              java(
                """
                  import java.util.List;
                  class A {
                      void f(List<String> names) {
                          for (String unused : names) {
                          }
                      }
                  }
                  """,
                """
                  import java.util.List;
                  class A {
                      void f(List<String> names) {
                          for (String _ : names) {
                          }
                      }
                  }
                  """,
                spec -> spec.markers(javaVersion(25))
              )
            );
        }

        @Test
        void rejectDuplicateKey() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new RepeatFirstEntry()),
              yaml(
                "key: value",
                """
                  key: value
                  key: value
                  """
              )
            ));
            assertThat(e).hasMessageContaining("the key `key` appears twice in one mapping, which YAML doesn't allow");
        }

        @Test
        void reparseConvertedSourceWithItsNewLanguage() {
            rewriteRun(
              spec -> spec.recipe(new YamlToJava()),
              yaml(
                "deprecated: true",
                """
                  @Deprecated
                  class A {
                  }
                  """,
                spec -> spec.path("a.yaml")
              )
            );
        }

        @Test
        void optOut() {
            rewriteRun(
              spec -> spec.recipe(new ReturnNullByHand(null))
                .recipeQualityValidation(RecipeQualityValidation.builder().parserShapedTrees(false).build()),
              java(
                """
                  class A {
                      Object f() {
                          return 0;
                      }
                  }
                  """,
                """
                  class A {
                      Object f() {
                          return null;
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class OrderStepsForEarlierChanges {
        private static final String RECURSIVE_FIRST = """
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.test.MovePackagesBroadFirst
          displayName: Move packages
          description: Moves a package and one of its subpackages.
          recipeList:
            - org.openrewrite.java.ChangePackage:
                oldPackageName: a
                newPackageName: x
                recursive: true
            - org.openrewrite.java.ChangePackage:
                oldPackageName: a.b
                newPackageName: y
          """;

        @Test
        void rejectStepThatAnEarlierRenameShadows() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipeFromYaml(RECURSIVE_FIRST, "org.openrewrite.test.MovePackagesBroadFirst"),
              java(
                """
                  package a.b;
                  class C {
                  }
                  """,
                """
                  package x.b;
                  class C {
                  }
                  """
              )
            ));
            assertThat(e).hasMessageStartingWith("Order the steps of a composite so that a step runs before the steps " +
                                                 "that rename what it looks for")
              .hasMessageContaining("ChangePackage `a.b` to `y` (step 2 of org.openrewrite.test.MovePackagesBroadFirst): " +
                                    "ChangePackage `a` to `x` (step 1 of org.openrewrite.test.MovePackagesBroadFirst) " +
                                    "renamed `a.b` to `x.b` first, so code that named it ends up as `x.b`, not `y`")
              .hasMessageContaining("orderStepsForEarlierChanges(false)");

            // every test that runs the composite fails, not just the first
            assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipeFromYaml(RECURSIVE_FIRST, "org.openrewrite.test.MovePackagesBroadFirst")
            ));
        }

        @Test
        void acceptStepWhoseRenamedNameALaterStepRenames() {
            rewriteRun(
              spec -> spec.recipeFromYaml(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: org.openrewrite.test.MovePackageThenRenameEitherWay
                  displayName: Move a package and rename a type in it
                  description: Renames the type under its old package and under its new one.
                  recipeList:
                    - org.openrewrite.java.ChangePackage:
                        oldPackageName: a
                        newPackageName: x
                    - org.openrewrite.java.ChangeType:
                        oldFullyQualifiedTypeName: a.C
                        newFullyQualifiedTypeName: a.D
                    - org.openrewrite.java.ChangeType:
                        oldFullyQualifiedTypeName: x.C
                        newFullyQualifiedTypeName: x.D
                  """, "org.openrewrite.test.MovePackageThenRenameEitherWay")
            );
        }

        @Test
        void acceptNarrowRenameFirst() {
            rewriteRun(
              spec -> spec.recipeFromYaml(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: org.openrewrite.test.MovePackagesNarrowFirst
                  displayName: Move packages
                  description: Moves a subpackage and then the package it was in.
                  recipeList:
                    - org.openrewrite.java.ChangePackage:
                        oldPackageName: a.b
                        newPackageName: y
                    - org.openrewrite.java.ChangePackage:
                        oldPackageName: a
                        newPackageName: x
                        recursive: true
                  """, "org.openrewrite.test.MovePackagesNarrowFirst"),
              java(
                """
                  package a.b;
                  class C {
                  }
                  """,
                """
                  package y;
                  class C {
                  }
                  """
              )
            );
        }

        @Test
        void optOut() {
            rewriteRun(
              spec -> spec.recipeFromYaml(RECURSIVE_FIRST.replace("MovePackagesBroadFirst", "MovePackagesOptOut"),
                  "org.openrewrite.test.MovePackagesOptOut")
                .recipeQualityValidation(RecipeQualityValidation.builder().orderStepsForEarlierChanges(false).build()),
              java(
                """
                  package a.b;
                  class C {
                  }
                  """,
                """
                  package x.b;
                  class C {
                  }
                  """
              )
            );
        }
    }

    @Nested
    class UnparsedSources {

        @Test
        void rejectRecipeThatThrowsOnAFileItsParserFailedOn() {
            AssertionError e = assertThrows(AssertionError.class, () -> rewriteRun(
              spec -> spec.recipe(new ChangeApplicationYml(null)),
              yaml(
                "key: value",
                "key: changed",
                spec -> spec.path("application.yml").markers(new BuildMetadata(Tree.randomId(), Map.of("team", "a")))
              )
            ));
            assertThat(e).hasMessageStartingWith("Check the kind of source file before casting it")
              .hasMessageContaining("org.openrewrite.test.internal.ChangeApplicationYml threw when application.yml " +
                                    "was a ParseError (a file its parser failed on) with the markers of the test's " +
                                    "source (BuildMetadata):\n    java.lang.ClassCastException")
              .hasMessageContaining("at org.openrewrite.test.internal.ChangeApplicationYml$1.visit(RewriteTestTest.java:")
              .hasRootCauseInstanceOf(ClassCastException.class);
        }

        @Test
        void acceptRecipeThatChecksTheKindOfSourceFile() {
            rewriteRun(
              spec -> spec.recipe(new ChangeApplicationYml(true)),
              yaml(
                "key: value",
                "key: changed",
                spec -> spec.path("application.yml")
              )
            );
        }

        @Test
        void skipAdHocRecipe() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new TreeVisitor<Tree, ExecutionContext>() {
                  @Override
                  public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                      return tree instanceof SourceFile sourceFile && sourceFile.getSourcePath().endsWith("adhoc.yml") ?
                        new ChangeFirstValue("adhoc", true).getVisitor().visit((Yaml.Documents) tree, ctx) : tree;
                  }
              })),
              yaml(
                "key: value",
                "key: adhoc",
                spec -> spec.path("adhoc.yml")
              )
            );
        }

        @Test
        void toleratedByTheTestsErrorHandler() {
            rewriteRun(
              spec -> spec.executionContext(new InMemoryExecutionContext())
                .recipe(new ChangeApplicationYml(null)),
              yaml(
                "key: value",
                "key: changed",
                spec -> spec.path("application.yml")
              )
            );
        }
    }

    @Nested
    class RecipeValidation {

        @Test
        void validateRecipeWithNoOptions() {
            rewriteRun(
              spec -> spec.recipe(new RecipeWithNoOptions()),
              new SourceSpecs[0]
            );
        }

        @Test
        void validateRecipeWithOnlyRequiredOptionsPositive() {
            rewriteRun(
              spec -> spec.recipe(new RecipeWithRequiredOptionValidateNoBlank("Test")),
              new SourceSpecs[0]
            );
        }

        @Test
        void validateRecipeWithOnlyRequiredOptionsNegative() {
            assertThatThrownBy(() ->
              rewriteRun(
                spec -> spec.recipe(new RecipeWithRequiredOptionValidateNoBlank("")),
                new SourceSpecs[0]
              )
            );
        }

        @Test
        void skipValidationForRecipeWithOptionalOptions() {
            rewriteRun(
              spec -> spec.recipe(new RecipeWithOptionalOrValidation(null, null)),
              new SourceSpecs[0]
            );
        }

        @Test
        void validateDeclarativeRecipe() {
            assertThrows(AssertionError.class, () ->
              rewriteRun(
                spec -> spec.recipeFromYaml("""
                  type: specs.openrewrite.org/v1beta/recipe
                  name: org.openrewrite.test.internal.StillValidated
                  displayName: Still validated
                  description: Declarative recipe with a non-existent sub-recipe should still fail validation.
                  recipeList:
                    - org.openrewrite.DoesNotExist
                  """, "org.openrewrite.test.internal.StillValidated")
              ));
        }
    }
}

@Value
@With
class Visited implements Marker {
    UUID id;
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class ChangeFirstValue extends Recipe {

    @Option(displayName = "Value",
      description = "The value to set.",
      example = "changed")
    String value;

    @Option(displayName = "Check the document",
      description = "Leave a document unchanged unless its first entry has a scalar value.",
      required = false)
    @Nullable
    Boolean checked;

    String displayName = "Change the first value";

    String description = "Sets the value of the first entry of each YAML document.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<>() {
            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                if (Boolean.TRUE.equals(checked) && !(document.getBlock() instanceof Yaml.Mapping m &&
                                                      !m.getEntries().isEmpty() &&
                                                      m.getEntries().getFirst().getValue() instanceof Yaml.Scalar)) {
                    return document;
                }
                Yaml.Mapping mapping = (Yaml.Mapping) document.getBlock();
                Yaml.Scalar scalar = (Yaml.Scalar) mapping.getEntries().get(0).getValue();
                return document.withBlock(mapping.withEntries(ListUtils.mapFirst(mapping.getEntries(),
                  entry -> entry.withValue(scalar.withValue(value)))));
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class ChangeApplicationYml extends Recipe {

    @Option(displayName = "Check the kind of source file",
      description = "Leave a source unchanged unless it is YAML.",
      required = false)
    @Nullable
    Boolean checked;

    String displayName = "Change the first value in application.yml";

    String description = "Sets the first value of each document in application.yml, which it finds by its path.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile sourceFile) || !sourceFile.getSourcePath().endsWith("application.yml") ||
                    Boolean.TRUE.equals(checked) && !(tree instanceof Yaml.Documents)) {
                    return tree;
                }
                return new ChangeFirstValue("changed", true).getVisitor().visit((Yaml.Documents) tree, ctx);
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class UseObjectsIsNull extends Recipe {

    @Option(displayName = "Add the import",
      description = "Add an import of `java.util.Objects` to the file.",
      required = false)
    @Nullable
    Boolean addImport;

    String displayName = "Use `Objects.isNull`";

    String description = "Replaces comparisons with `null` by `Objects.isNull`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                J j = super.visitBinary(binary, ctx);
                if (!(j instanceof J.Binary b) || b.getOperator() != J.Binary.Type.Equal ||
                    !J.Literal.isLiteralValue(b.getRight(), null)) {
                    return j;
                }
                if (Boolean.TRUE.equals(addImport)) {
                    maybeAddImport("java.util.Objects");
                }
                return JavaTemplate.builder("Objects.isNull(#{any()})")
                  .imports("java.util.Objects")
                  .build()
                  .apply(getCursor(), b.getCoordinates().replace(), b.getLeft());
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class ReturnEmptyList extends Recipe {

    @Option(displayName = "Declare the import",
      description = "Declare the template's import of `java.util.Collections`.",
      required = false)
    @Nullable
    Boolean declared;

    String displayName = "Return an empty list";

    String description = "Replaces `return null` by `return Collections.emptyList()`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                if (literal.getValue() != null || !(getCursor().getParentTreeCursor().getValue() instanceof J.Return)) {
                    return literal;
                }
                JavaTemplate.Builder template = JavaTemplate.builder("Collections.emptyList()").contextSensitive();
                if (Boolean.TRUE.equals(declared)) {
                    template.imports("java.util.Collections");
                }
                return template.build().apply(getCursor(), literal.getCoordinates().replace());
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class ReturnNullByHand extends Recipe {

    @Option(displayName = "Build the literal as the parser does",
      description = "Give the literal the value null, as parsing `null` does.",
      required = false)
    @Nullable
    Boolean parserShaped;

    String displayName = "Return null";

    String description = "Replaces `return 0` by `return null`, building the literal by hand.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                if (!Integer.valueOf(0).equals(literal.getValue()) ||
                    !(getCursor().getParentTreeCursor().getValue() instanceof J.Return)) {
                    return literal;
                }
                return new J.Literal(Tree.randomId(), literal.getPrefix(), Markers.EMPTY,
                  Boolean.TRUE.equals(parserShaped) ? null : "null", "null", null, JavaType.Primitive.Null);
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class NameUnusedLoopVariable extends Recipe {

    String displayName = "Name an unused variable `_`";

    String description = "Renames a variable named `unused` to `_`, as Java 22 allows.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable,
                                                                     ExecutionContext ctx) {
                J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
                if (!"unused".equals(v.getSimpleName())) {
                    return v;
                }
                JavaType.Variable type = v.getVariableType() == null ? null : v.getVariableType().withName("_");
                return v.withName(v.getName().withSimpleName("_").withFieldType(type)).withVariableType(type);
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class RepeatFirstEntry extends Recipe {

    String displayName = "Repeat the first entry";

    String description = "Adds a copy of the first entry of a mapping that has one entry.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<>() {
            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = super.visitMapping(mapping, ctx);
                if (m.getEntries().size() != 1) {
                    return m;
                }
                return m.withEntries(ListUtils.concat(m.getEntries(), m.getEntries().getFirst().copyPaste().withPrefix("\n")));
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class YamlToJava extends Recipe {

    String displayName = "Replace YAML with Java";

    String description = "Replaces a YAML document with a Java source that keeps its id, as converters do.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof Yaml.Documents yaml)) {
                    return tree;
                }
                return JavaParser.fromJavaVersion().build().parse(ctx, "@Deprecated\nclass A {\n}")
                  .findFirst()
                  .<Tree>map(java -> java
                    .withSourcePath(yaml.getSourcePath().resolveSibling("A.java"))
                    .withId(yaml.getId()))
                  .orElse(yaml);
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class FindCallsOnImportedType extends Recipe {

    @Option(displayName = "Method pattern",
      description = "A method pattern to match calls against.",
      example = "java.util.Collections emptyList()")
    String methodPattern;

    String displayName = "Find calls on an imported type";

    String description = "Finds calls by method type, but takes the select of a qualified call to be a name.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher matcher = new MethodMatcher(methodPattern);
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!matcher.matches(m)) {
                    return m;
                }
                String on = m.getSelect() == null ? null : ((J.Identifier) m.getSelect()).getSimpleName();
                return SearchResult.found(m, on);
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class RequiresBuildMetadata extends Recipe {

    String displayName = "Require build metadata";

    String description = "Changes text, and throws on a source without build metadata as recipes that need a project marker do.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (!text.getMarkers().findFirst(BuildMetadata.class).isPresent()) {
                    throw new IllegalStateException("Unable to find BuildMetadata marker.");
                }
                return text.withText("changed");
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class FindCallsWithPrintedArgument extends Recipe {

    @Option(displayName = "Argument",
      description = "How an argument of the call prints.",
      example = "Boolean.TRUE")
    String argument;

    String displayName = "Find calls with a printed argument";

    String description = "Finds calls by how one of their arguments prints, so it misses other spellings of it.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return m.getArguments().stream().anyMatch(arg -> argument.equals(arg.toString())) ?
                  SearchResult.found(m) : m;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class FindQualifiedCallsByName extends Recipe {

    @Option(displayName = "Select",
      description = "How the select of the call prints.",
      example = "Collections")
    String select;

    @Option(displayName = "Method name",
      description = "The name of the method to find.",
      example = "emptyList")
    String methodName;

    String displayName = "Find qualified calls by name";

    String description = "Finds calls by name and by how their select prints, so it misses statically imported calls.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return methodName.equals(m.getSimpleName()) && m.getSelect() != null &&
                       select.equals(m.getSelect().toString()) ? SearchResult.found(m) : m;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class FindUnqualifiedCallsByName extends Recipe {

    @Option(displayName = "Method name",
      description = "The name of the method to find.",
      example = "emptyList")
    String methodName;

    String displayName = "Find unqualified calls by name";

    String description = "Finds calls by name that have no select, so it misses qualified calls.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return methodName.equals(m.getSimpleName()) && m.getSelect() == null ? SearchResult.found(m) : m;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class FindAnnotationsByPrintedType extends Recipe {

    @Option(displayName = "Annotation type",
      description = "How the annotation's type prints.",
      example = "Deprecated")
    String annotationType;

    String displayName = "Find annotations by printed type";

    String description = "Finds annotations by how their type prints, so it misses fully qualified ones.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);
                return annotationType.equals(a.getAnnotationType().toString()) ? SearchResult.found(a) : a;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class FindCallsByType extends Recipe {

    @Option(displayName = "Method pattern",
      description = "A method pattern to match calls against.",
      example = "java.util.Collections emptyList()")
    String methodPattern;

    String displayName = "Find calls by method type";

    String description = "Finds calls whose method type matches the pattern, however they are spelled.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher matcher = new MethodMatcher(methodPattern);
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return matcher.matches(m) ? SearchResult.found(m) : m;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class ScannerEdit extends ScanningRecipe<AtomicBoolean> {

    String displayName = "Attempts mutation during getScanner()";

    String description = "Any changes attempted by a visitor returned from getScanner() should be an error during test execution.";

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                return text.withText("mutated");
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class MutateExecutionContext extends Recipe {

    String displayName = "Mutate execution context";

    String description = "Mutates the execution context to trigger a validation failure.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                ctx.putMessage("mutated", true);
                return tree;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class ImproperCursorUsage extends Recipe {

    String displayName = "Uses cursor improperly";

    String description = "LST elements are acyclic. So a cursor which indicates an element is its own parent is invalid.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        //noinspection NullableProblems
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(Tree tree, ExecutionContext ctx) {
                return new TreeVisitor<>() {
                }.visit(tree, ctx, new Cursor(getCursor(), tree));
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class CreatesTwoFilesSamePath extends ScanningRecipe<AtomicBoolean> {

    String displayName = "Creates two source files with the same path";

    String description = "A source file's path must be unique. " +
      "This recipe creates two source files with the same path to show that the test framework helps protect against this mistake.";

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean alreadyExists) {
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile s) {
                    if ("duplicate.txt".equals(s.getSourcePath().toString())) {
                        alreadyExists.set(true);
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicBoolean alreadyExists, ExecutionContext ctx) {
        if (alreadyExists.get()) {
            return emptyList();
        }
        Path duplicatePath = Path.of("duplicate.txt");
        return List.of(PlainText.builder()
            .text("duplicate")
            .sourcePath(duplicatePath)
            .build(),
          PlainText.builder()
            .text("duplicate")
            .sourcePath(duplicatePath)
            .build()
        );
    }
}

@NullMarked
@SuppressWarnings({"FieldCanBeLocal", "unused"})
class RecipeWithNameOption extends Recipe {
    @Option
    private final String name;

    @JsonCreator
    public RecipeWithNameOption(String name) {
        this.name = name;
    }

    @Getter
    final String displayName = "Recipe with name option";

    @Getter
    final String description = "A fancy description.";
}

@NullMarked
class RecipeWithDescriptionListOfLinks extends Recipe {

    @Getter
    final String displayName = "Recipe with name option";

    @Getter
    final String description = """
      A fancy description.
      For more information, see:
        - [link 1](https://example.com/link1)
        - [link 2](https://example.com/link2)""";
}

@NullMarked
class RecipeWithDescriptionListOfDescribedLinks extends Recipe {

    @Getter
    final String displayName = "Recipe with name option";

    @Getter
    final String description = """
      A fancy description.
      For more information, see:
        - First Resource [link 1](https://example.com/link1).
        - Second Resource [link 2](https://example.com/link2).""";
}

@NullMarked
class RecipeWithDescriptionNotEndingWithPeriod extends Recipe {

    @Getter
    final String displayName = "Recipe with name option";

    @Getter
    final String description = "A fancy description";
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class GeneratesExistingFile extends ScanningRecipe<AtomicBoolean> {

    String displayName = "Generates a file that already exists";

    String description = "A recipe that generates a source file at a path that already exists in the source set.";

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicBoolean acc, ExecutionContext ctx) {
        return List.of(PlainText.builder()
          .text("generated content")
          .sourcePath(Path.of("existing.txt"))
          .build());
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class GeneratesFileWithInvalidPath extends ScanningRecipe<AtomicBoolean> {

    String displayName = "Generates a file with an invalid path";

    String description = "A recipe that generates a source file whose path points outside the source root, to show " +
      "that the test framework helps protect against this mistake.";

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicBoolean acc, ExecutionContext ctx) {
        return List.of(PlainText.builder()
          .text("outside")
          .sourcePath(Path.of("../outside.txt"))
          .build());
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class RecipeWithNoOptions extends Recipe {
    String displayName = "Recipe with no options";
    String description = "Has no configurable options at all.";
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class GeneratesCompilationUnitMissingTypes extends ScanningRecipe<AtomicBoolean> {

    String displayName = "Generates a compilation unit missing type information";

    String description = "A recipe that generates a J.CompilationUnit with class and method declarations that have null types.";

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicBoolean acc, ExecutionContext ctx) {
        return JavaParser.fromJavaVersion().build()
          .parse("public class A {\n    void foo() {\n    }\n}\n")
          .map(cu -> (J.CompilationUnit) cu)
          .map(cu -> cu.withClasses(cu.getClasses().stream()
            .map(c -> c.withType(null))
            .collect(toList())))
          .map(cu -> (SourceFile) cu.withSourcePath(Path.of("A.java")))
          .collect(toList());
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class RecipeWithRequiredOptionValidateNoBlank extends Recipe {
    String displayName = "Recipe with required option";
    String description = "Has a single required parameter.";

    @Option(displayName = "Required param",
      description = "A required parameter.")
    String requiredParam;

    @Override
    public Validated<Object> validate() {
        return super.validate()
          .and(Validated.notBlank("Required param", requiredParam));
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class RecipeWithOptionalOrValidation extends Recipe {
    String displayName = "Recipe with optional OR validation";
    String description = "Has two optional parameters where at least one must be set.";

    @Option(displayName = "Option A",
      description = "First optional parameter.",
      example = "valueA",
      required = false)
    @Nullable
    String optionA;

    @Option(displayName = "Option B",
      description = "Second optional parameter.",
      example = "valueB",
      required = false)
    @Nullable
    String optionB;

    @Override
    public Validated<Object> validate() {
        return super.validate().and(
          Validated.required("optionA", optionA)
            .or(Validated.required("optionB", optionB)));
    }
}
