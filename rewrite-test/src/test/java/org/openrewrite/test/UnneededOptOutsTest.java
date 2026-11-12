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
package org.openrewrite.test;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;
import org.openrewrite.text.ChangeText;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class UnneededOptOutsTest implements RewriteTest {

    @Test
    void narrowTypeValidationToTheChecksThatFail() {
        AssertionError e = withOptOutsChecked("all", () -> assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          validatedText("a", (source, validation) -> {
              if (validation.unknown()) {
                  throw new IllegalStateException("LST contains unknown elements");
              }
              return source;
          })
        )));
        assertThat(e)
          .hasMessageContaining("typeValidationOptions(..) in the test's RecipeSpec turns off checks that pass: " +
                                "classDeclarations, identifiers,")
          .hasMessageContaining("Replace typeValidationOptions(..) in the test's RecipeSpec with:\n" +
                                "    spec.typeValidationOptions(TypeValidation.all().unknown(false))")
          .hasMessageNotContaining("cursorAcyclic");

        AssertionError predicate = withOptOutsChecked("all", () -> assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.all().allowMissingType(o -> true).identifiers(false)),
          validatedText("a", (source, validation) -> {
              if (!validation.allowMissingType().apply(source)) {
                  throw new IllegalStateException("LST is missing a type");
              }
              return source;
          })
        )));
        assertThat(predicate).hasMessageContaining(
          "    spec.typeValidationOptions(TypeValidation.all().allowMissingType(..))\n" +
          "Where these show .., keep the test's own argument.");
    }

    @Test
    void keepAnAfterValidationThatWouldFallBackOnTheBeforeValidation() {
        SourceSpec<PlainText> changed = new SourceSpec<>(PlainText.class, null, PlainTextParser.builder(), "a",
          (source, validation) -> {
              if (validation.unknown() && "a".equals(((PlainText) source).getText())) {
                  throw new IllegalStateException("LST contains unknown elements");
              }
              return source;
          }, ctx -> {
        });
        changed.after(actual -> "b");
        AssertionError e = withOptOutsChecked("all", () -> assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(new ChangeText("b"))
            .typeValidationOptions(TypeValidation.all().unknown(false))
            .afterTypeValidationOptions(TypeValidation.all().identifiers(false)),
          changed
        )));
        assertThat(e).hasMessageContaining("Replace afterTypeValidationOptions(..) in the test's RecipeSpec with:\n" +
                                           "    spec.afterTypeValidationOptions(TypeValidation.all())\n");
    }

    @Test
    void carryOverTheQualityOptOutsOfTheTestClassDefaults() {
        RewriteTest optsOutInDefaults = new RewriteTest() {
            @Override
            public void defaults(RecipeSpec spec) {
                spec.recipeQualityValidation(RecipeQualityValidation.builder().minimalFormatting(false).build());
            }
        };
        AssertionError e = withOptOutsChecked("identifiers", () -> assertThrows(AssertionError.class, () ->
          optsOutInDefaults.rewriteRun(
            spec -> spec.recipe(new EditsWhileScanning())
              .typeValidationOptions(TypeValidation.all().identifiers(false).immutableScanning(false)),
            text("a")
          )));
        assertThat(e).hasMessageContaining("    spec.recipeQualityValidation(RecipeQualityValidation.builder()" +
                                           ".noEditsInScanningPhase(false).minimalFormatting(false).build())");
    }

    @Test
    void leaveChecksThatAreOffByDefaultOnWhenTheTestTurnsThemOn() {
        withSystemProperty("org.openrewrite.test.parserTypedTrees", null, () -> {
            withOptOutsChecked("all", () -> rewriteRun(
              spec -> spec.recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(true).build()),
              text("a")
            ));
            return null;
        });
    }

    @Test
    void offUnlessItsSystemPropertyNamesChecks() {
        withOptOutsChecked(null, () -> rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          text("a")
        ));
    }

    @Test
    void checkOnlyTheChecksTheSystemPropertyNames() {
        AssertionError e = withOptOutsChecked("identifiers", () -> assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.all().identifiers(false).methodInvocations(false)),
          text("a")
        )));
        assertThat(e)
          .hasMessageContaining("turns off checks that pass: identifiers.")
          .hasMessageContaining("    spec.typeValidationOptions(TypeValidation.all().methodInvocations(false))");
    }

    @Test
    void rejectUnknownChecks() {
        IllegalArgumentException e = withOptOutsChecked("identifier", () ->
          assertThrows(IllegalArgumentException.class, () -> rewriteRun(text("a"))));
        assertThat(e).hasMessageContaining("names checks that don't exist: identifier.")
          .hasMessageContaining("classDeclarations, identifiers, methodDeclarations");
    }

    @Test
    void checkSerializationOptOuts() {
        AssertionError e = withOptOutsChecked("all", () -> assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(new ChangeText("b")).validateRecipeSerialization(false),
          text("a", "b")
        )));
        assertThat(e).hasMessageContaining("Remove validateRecipeSerialization(false) from the test's RecipeSpec.");

        withOptOutsChecked("all", () -> rewriteRun(
          spec -> spec.recipe(new DescribedWithoutPeriod()).validateRecipeSerialization(false),
          text("a")
        ));
    }

    @Test
    void reportOptOutsOfTheTestClassDefaultsForThisTest() {
        RewriteTest optsOutInDefaults = new RewriteTest() {
            @Override
            public void defaults(RecipeSpec spec) {
                spec.typeValidationOptions(TypeValidation.all().identifiers(false));
            }
        };
        AssertionError e = withOptOutsChecked("all", () -> assertThrows(AssertionError.class, () ->
          optsOutInDefaults.rewriteRun(text("a"))));
        assertThat(e)
          .hasMessageContaining("typeValidationOptions(..) in defaults(RecipeSpec) turns off checks that pass: " +
                                "identifiers.")
          .hasMessageContaining("This test needs none of the opt-outs in defaults(RecipeSpec).");
    }

    @Test
    void checkEveryRecipeQualityCheckThatIsTurnedOff() throws IllegalAccessException {
        RecipeQualityValidation all = withParserTypedTrees(RecipeQualityValidation::all);
        RecipeQualityValidation allOff = withParserTypedTrees(RecipeQualityValidation::all);
        StringBuilder turnedOff = new StringBuilder();
        for (Field field : RecipeQualityValidation.class.getDeclaredFields()) {
            if (field.getType() == boolean.class && !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                if (field.getBoolean(all)) {
                    field.setBoolean(allOff, false);
                    turnedOff.append(turnedOff.length() == 0 ? "" : ", ").append(field.getName());
                }
            }
        }
        AssertionError e = withOptOutsChecked("all", () -> withParserTypedTrees(() ->
          assertThrows(AssertionError.class, () -> rewriteRun(
            spec -> spec.recipe(toRecipe()).recipeQualityValidation(allOff),
            text("a")
          ))));
        assertThat(e)
          .hasMessageContaining("recipeQualityValidation(..) in the test's RecipeSpec turns off checks that pass: " +
                                turnedOff + ".")
          .hasMessageContaining("Remove recipeQualityValidation(..) from the test's RecipeSpec.");
    }

    @Test
    void keepOptOutsOfChecksThatFail() {
        withOptOutsChecked("all", () -> rewriteRun(
          spec -> spec
            .recipes(
              toRecipe(() -> new PlainTextVisitor<>() {
                  @Override
                  public PlainText visitText(PlainText text, ExecutionContext ctx) {
                      ctx.putMessage("seen", true);
                      return text;
                  }
              }),
              new EditsWhileScanning(),
              toRecipe(() -> new PlainTextVisitor<>() {
                  @Override
                  public PlainText visitText(PlainText text, ExecutionContext ctx) {
                      return text.getMarkers().findFirst(Seen.class).isPresent() ? text :
                        text.withMarkers(text.getMarkers().add(new Seen(Tree.randomId())));
                  }
              }))
            .expectedCyclesThatMakeChanges(1)
            .recipeQualityValidation(RecipeQualityValidation.builder()
              .executionContextUnchanged(false)
              .noEditsInScanningPhase(false)
              .reportingMarkersOnly(false)
              .build()),
          text("a")
        ));
    }

    @Test
    void turnOffTheCheckThatTypeValidationKeptFromRunning() {
        AssertionError e = withOptOutsChecked("all", () -> withParserTypedTrees(() ->
          assertThrows(AssertionError.class, () -> rewriteRun(
            spec -> spec
              .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.Primitive visitPrimitive(J.Primitive primitive, ExecutionContext ctx) {
                      return primitive.getType() == JavaType.Primitive.Int ?
                        primitive.withType(JavaType.Primitive.Long) : primitive;
                  }
              }))
              .typeValidationOptions(TypeValidation.all().identifiers(false)),
            java(
              """
                class A {
                    int x = 1;
                }
                """,
              """
                class A {
                    long x = 1;
                }
                """
            )
          ))));
        assertThat(e)
          .hasMessageContaining("turns off checks that pass: identifiers.")
          .hasMessageContaining("With the type checks on, parserTypedTrees runs and fails")
          .hasMessageContaining("Replace typeValidationOptions(..) in the test's RecipeSpec with:\n" +
                                "    spec.recipeQualityValidation(RecipeQualityValidation.builder()" +
                                ".parserTypedTrees(false).build())");
    }

    private static SourceSpecs validatedText(String before, SourceSpec.ValidateSource validateSource) {
        return new SourceSpec<>(PlainText.class, null, PlainTextParser.builder(), before, validateSource, ctx -> {
        });
    }

    private static <T> T withOptOutsChecked(@Nullable String checks, Supplier<T> test) {
        return withSystemProperty(UnneededOptOuts.PROPERTY, checks, test);
    }

    private static void withOptOutsChecked(@Nullable String checks, Runnable test) {
        withSystemProperty(UnneededOptOuts.PROPERTY, checks, () -> {
            test.run();
            return null;
        });
    }

    private static <T> T withParserTypedTrees(Supplier<T> test) {
        return withSystemProperty("org.openrewrite.test.parserTypedTrees", "true", test);
    }

    private static <T> T withSystemProperty(String property, @Nullable String value, Supplier<T> test) {
        String set = System.getProperty(property);
        try {
            if (value == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, value);
            }
            return test.get();
        } finally {
            if (set == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, set);
            }
        }
    }

    @Value
    @With
    static class Seen implements Marker {
        UUID id;
    }

    static class DescribedWithoutPeriod extends Recipe {
        @Override
        public String getDisplayName() {
            return "Described without a period";
        }

        @Override
        public String getDescription() {
            return "Its description fails the checks of recipe descriptions";
        }
    }

    static class EditsWhileScanning extends ScanningRecipe<AtomicInteger> {
        @Override
        public String getDisplayName() {
            return "Edit while scanning";
        }

        @Override
        public String getDescription() {
            return "Changes the text in its scanner, whose edits the scanning phase discards.";
        }

        @Override
        public AtomicInteger getInitialValue(ExecutionContext ctx) {
            return new AtomicInteger();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(AtomicInteger acc) {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    acc.incrementAndGet();
                    return text.withText("b");
                }
            };
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(AtomicInteger acc) {
            return TreeVisitor.noop();
        }
    }
}
