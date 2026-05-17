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
package org.openrewrite.android.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AndroidLifecycleMethodTest implements RewriteTest {

    // Stub Android classes used so JavaParser can resolve type hierarchies during tests.
    private static final String ANDROID_STUBS = """
            package android.app;
            public class Activity {
                public void onCreate(android.os.Bundle savedInstanceState) {}
                public void onStart() {}
                public void onResume() {}
                public void onPause() {}
                public void onStop() {}
                public void onDestroy() {}
            }
            """;

    private static final String BUNDLE_STUB = """
            package android.os;
            public class Bundle {}
            """;

    private static final String FRAGMENT_STUB = """
            package androidx.fragment.app;
            public class Fragment {
                public void onAttach(Object ctx) {}
                public void onCreateView() {}
                public void onResume() {}
            }
            """;

    // KotlinParser.dependsOn requires Kotlin source, not Java. These are the same shapes
    // as the Java stubs above but expressed as `open class` / annotation declarations.
    private static final String ANDROID_STUBS_KOTLIN = """
            package android.app

            open class Activity {
                open fun onCreate(savedInstanceState: android.os.Bundle?) {}
                open fun onStart() {}
                open fun onResume() {}
                open fun onPause() {}
                open fun onStop() {}
                open fun onDestroy() {}
            }
            """;

    private static final String BUNDLE_STUB_KOTLIN = """
            package android.os

            class Bundle
            """;

    private static final String COMPOSABLE_STUB_KOTLIN = """
            package androidx.compose.runtime

            annotation class Composable
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new AndroidLifecycleMethod.Matcher().asVisitor(
                (trait, ctx) -> SearchResult.found(trait.getTree(),
                        trait.getComponentFamily() + ":" + trait.getMethodName())
        )));
    }

    @Test
    void javaActivityOnCreate() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(ANDROID_STUBS, BUNDLE_STUB)),
          java(
            """
              import android.app.Activity;
              import android.os.Bundle;
              class MainActivity extends Activity {
                  @Override
                  public void onCreate(Bundle savedInstanceState) {}
                  public void helper() {}
              }
              """,
            """
              import android.app.Activity;
              import android.os.Bundle;
              class MainActivity extends Activity {
                  /*~~(ACTIVITY:onCreate)~~>*/@Override
                  public void onCreate(Bundle savedInstanceState) {}
                  public void helper() {}
              }
              """
          )
        );
    }

    @Test
    void kotlinActivityOnCreate() {
        // Kotlin classpath integration: rely on the same Android stub source but expressed
        // in Kotlin (KotlinParser.dependsOn accepts Kotlin source only). Exercises the
        // J.MethodDeclaration shape that, in Kotlin, is wrapped by an enclosing
        // K.MethodDeclaration on the cursor.
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().dependsOn(ANDROID_STUBS_KOTLIN, BUNDLE_STUB_KOTLIN)),
          kotlin(
            """
              import android.app.Activity
              import android.os.Bundle

              class MainActivity : Activity() {
                  override fun onCreate(savedInstanceState: Bundle?) {}
                  fun helper() {}
              }
              """,
            """
              import android.app.Activity
              import android.os.Bundle

              class MainActivity : Activity() {
                  /*~~(ACTIVITY:onCreate)~~>*/override fun onCreate(savedInstanceState: Bundle?) {}
                  fun helper() {}
              }
              """
          )
        );
    }

    @Test
    void doesNotMatchUnrelatedMethodName() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(ANDROID_STUBS, BUNDLE_STUB)),
          java(
            """
              import android.app.Activity;
              class MainActivity extends Activity {
                  public void notALifecycle() {}
              }
              """
          )
        );
    }

    @Test
    void doesNotMatchNonAndroidClass() {
        rewriteRun(
          java(
            """
              class Pojo {
                  public void onCreate() {}
                  public void onResume() {}
              }
              """
          )
        );
    }

    @Test
    void composableFunctionMatchedByAnnotation() {
        // Composable is a method-level annotation; functions need not live inside any
        // particular base class. Verifies the annotation-path of detectFamily().
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().dependsOn(COMPOSABLE_STUB_KOTLIN)),
          kotlin(
            """
              import androidx.compose.runtime.Composable

              @Composable
              fun Greeting(name: String) {}

              fun notAComposable() {}
              """,
            """
              import androidx.compose.runtime.Composable

              /*~~(COMPOSE:Greeting)~~>*/@Composable
              fun Greeting(name: String) {}

              fun notAComposable() {}
              """
          )
        );
    }

    @Test
    void fragmentOnResumeJava() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(FRAGMENT_STUB)),
          java(
            """
              import androidx.fragment.app.Fragment;
              class MyFragment extends Fragment {
                  @Override
                  public void onResume() {}
              }
              """,
            """
              import androidx.fragment.app.Fragment;
              class MyFragment extends Fragment {
                  /*~~(FRAGMENT:onResume)~~>*/@Override
                  public void onResume() {}
              }
              """
          )
        );
    }

    /**
     * Load-bearing K-composition unwrap coverage. Walks the parsed Kotlin LST and
     * collects {@link AndroidLifecycleMethod} traits directly from
     * {@link AndroidLifecycleMethod.Matcher#test(org.openrewrite.Cursor)} via a cursor
     * pointing at a {@link K.MethodDeclaration}. If the matcher's K→J unwrap is
     * removed (and the cast naively does {@code (J.MethodDeclaration) cursor.getValue()}),
     * this test fails with a {@link ClassCastException}.
     */
    /**
     * Load-bearing K&rarr;J composition unwrap coverage. The rewrite-kotlin parser
     * only emits {@link K.MethodDeclaration} when the function has type-constraints
     * (a {@code where} clause); plain functions and overrides come through as bare
     * {@link J.MethodDeclaration}. So we need a Kotlin source with a {@code where}
     * clause AND a body that satisfies one of our lifecycle predicates to actually
     * exercise the unwrap path inside {@code Matcher.test(Cursor)}.
     * <p>
     * If the matcher's K&rarr;J unwrap is removed (naive {@code (J.MethodDeclaration)
     * cursor.getValue()}), {@code Matcher.get(cursor)} for this K.MethodDeclaration
     * throws ClassCastException and this test fails.
     */
    @Test
    void matcherTestAcceptsKMethodDeclarationCursor() {
        // Synthesize a lifecycle-named method with a `where` clause so the Kotlin parser
        // wraps it in K.MethodDeclaration. We use BroadcastReceiver.onReceive() as the
        // lifecycle entry point because it has the simplest signature; the extra
        // type-parameter / where clause is the K.MethodDeclaration trigger.
        String receiverStub = """
                package android.content

                open class BroadcastReceiver {
                    open fun <T> onReceive(): T? where T : Any = null
                }
                """;
        List<AndroidLifecycleMethod> matches = new ArrayList<>();
        List<Class<?>> sawTypes = new ArrayList<>();
        rewriteRun(
          spec -> spec
            .parser(KotlinParser.builder().dependsOn(receiverStub))
            .recipe(toRecipe(() -> new TreeVisitor<org.openrewrite.Tree, ExecutionContext>() {
                @Override
                public org.openrewrite.Tree preVisit(org.openrewrite.Tree tree, ExecutionContext ctx) {
                    if (tree instanceof K.MethodDeclaration) {
                        sawTypes.add(K.MethodDeclaration.class);
                        AndroidLifecycleMethod.Matcher matcher = new AndroidLifecycleMethod.Matcher();
                        matcher.get(getCursor()).ifPresent(matches::add);
                    } else if (tree instanceof J.MethodDeclaration) {
                        sawTypes.add(J.MethodDeclaration.class);
                    }
                    return tree;
                }
            })),
          kotlin(
            """
              import android.content.BroadcastReceiver

              class MyReceiver : BroadcastReceiver() {
                  override fun <T> onReceive(): T? where T : Any = null
              }
              """
          )
        );

        assertThat(sawTypes)
                .as("must visit at least one K.MethodDeclaration to actually exercise the unwrap path; " +
                        "if this list contains only J.MethodDeclaration the Kotlin parser changed and " +
                        "the test fixture must be updated to re-trigger K.MethodDeclaration emission")
                .contains(K.MethodDeclaration.class);
        assertThat(matches).hasSize(1);
        AndroidLifecycleMethod match = matches.get(0);
        Object cursorValue = match.getCursor().getValue();
        assertThat(cursorValue).isInstanceOf(K.MethodDeclaration.class);
        J.MethodDeclaration unwrapped = match.getMethodDeclaration();
        assertThat(unwrapped.getSimpleName()).isEqualTo("onReceive");
        assertThat(match.getComponentFamily()).isEqualTo(AndroidLifecycleMethod.ComponentFamily.BROADCAST_RECEIVER);
    }
}
