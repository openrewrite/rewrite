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
package org.openrewrite.kotlin;

import com.tschuchort.compiletesting.KotlinCompilation;
import com.tschuchort.compiletesting.SourceFile;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.trait.VariableAccess;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScopeTraitTest implements RewriteTest {

    @Test
    void namesAreVisibleWhereKotlinScopesThem() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> {
              String visible = Arrays.stream(new String[]{"top", "prop", "a", "b", "w", "it", "x"})
                .filter(name -> scope.lookup(name).isPresent())
                .map(name -> "it".equals(name) ? name + "=" + scope.lookup(name).get().getKind() : name)
                .collect(joining(" "));
              return visible.isEmpty() ? "none" : visible;
          })),
          kotlin(
            """
              val top = 1
              class C(val prop: Int) {
                  fun m(p: Pair<Int, Int>, s: Any) {
                      val (a, b) = p
                      when (val w = s) {
                          is String -> here()
                      }
                      here()
                      listOf(1).forEach { here() }
                      listOf(1).forEach { x -> here() }
                  }
              }
              fun here() {}
              """,
            """
              val top = 1
              class C(val prop: Int) {
                  fun m(p: Pair<Int, Int>, s: Any) {
                      val (a, b) = p
                      when (val w = s) {
                          is String -> /*~~(top prop a b w)~~>*/here()
                      }
                      /*~~(top prop a b)~~>*/here()
                      listOf(1).forEach { /*~~(top prop a b it=PARAMETER)~~>*/here() }
                      listOf(1).forEach { x -> /*~~(top prop a b x)~~>*/here() }
                  }
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void functionWithoutAReturnTypeIsBoundAsAMethod() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("here", Scope.Namespace.METHOD).isPresent() ? "bound" : "unbound")),
          kotlin(
            """
              fun m() {
                  here()
              }
              fun here() {}
              """,
            """
              fun m() {
                  /*~~(bound)~~>*/here()
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void localFunctionIsVisibleInItsOwnBody() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("local", Scope.Namespace.METHOD).isPresent() ? "bound" : "unbound")),
          kotlin(
            """
              fun m() {
                  fun local() {
                      here()
                  }
              }
              fun here() {}
              """,
            """
              fun m() {
                  fun local() {
                      /*~~(bound)~~>*/here()
                  }
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void parameterIsVisibleInTheDefaultOfALaterOne() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> Stream.of("first", "second", "third")
            .filter(name -> scope.lookup(name).isPresent())
            .collect(joining(" ")))),
          kotlin(
            """
              fun m(first: Int, second: Any = here(), third: Int = 0) {}
              fun here() {}
              """,
            """
              fun m(first: Int, second: Any = /*~~(first)~~>*/here(), third: Int = 0) {}
              fun here() {}
              """
          )
        );
    }

    @Test
    void namedArgumentIsNotAVariableAccess() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new VariableAccess.Matcher()
            .asVisitor(access -> SearchResult.found(access.getTree())))),
          kotlin(
            """
              fun g(named: Int) {}
              fun m(local: Int) {
                  g(named = local)
              }
              """,
            """
              fun g(named: Int) {}
              fun m(local: Int) {
                  g(named = /*~~>*/local)
              }
              """
          )
        );
    }

    @Test
    void freeNameAvoidsALaterDestructuredName() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> "a=" + scope.freeName("a", n -> false) + ", c=" + scope.freeName("c", n -> false))),
          kotlin(
            """
              fun m(pair: Pair<Int, Int>) {
                  here()
                  val (a, b) = pair
              }
              fun here() {}
              """,
            """
              fun m(pair: Pair<Int, Int>) {
                  /*~~(a=a1, c=c)~~>*/here()
                  val (a, b) = pair
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void referencesAndRenameSafetyFollowKotlinScoping() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  return Scope.bindingOf(getCursor(), identifier)
                    .filter(b -> b.getName().endsWith("_"))
                    .map(b -> SearchResult.found(identifier, b.references().count() + " references, " +
                      (b.isEffectivelyFinal() ? "final" : "assigned") + ", " +
                      Stream.of("free", "taken", "it").map(n -> n + (b.canRenameTo(n) ? ":safe" : ":unsafe")).collect(joining(" "))))
                    .orElse(identifier);
              }
          })),
          kotlin(
            """
              fun m(p: Int): Int {
                  val read_ = p
                  var assigned_ = p
                  val taken = 2
                  listOf(1).forEach { use(it + read_) }
                  assigned_ = taken
                  return read_ + assigned_
              }
              fun use(i: Int) {}
              class C(val property_: Int) {
                  val taken = 1
                  fun get() = property_
              }
              """,
            """
              fun m(p: Int): Int {
                  val /*~~(2 references, final, free:safe taken:unsafe it:unsafe)~~>*/read_ = p
                  var /*~~(2 references, assigned, free:safe taken:unsafe it:safe)~~>*/assigned_ = p
                  val taken = 2
                  listOf(1).forEach { use(it + read_) }
                  assigned_ = taken
                  return read_ + assigned_
              }
              fun use(i: Int) {}
              class C(val /*~~(1 references, final, free:safe taken:unsafe it:safe)~~>*/property_: Int) {
                  val taken = 1
                  fun get() = property_
              }
              """
          )
        );
    }

    @Test
    void blockCanMoveAfterItsStatementUnlessANameWouldClash() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.If visitIf(J.If iff, ExecutionContext ctx) {
                  Cursor parent = getCursor().getParentTreeCursor();
                  if (!(parent.getValue() instanceof J.Block)) {
                      return iff;
                  }
                  Cursor block = new Cursor(getCursor(), iff.getThenPart());
                  return SearchResult.found(iff, Scope.at(block).canMoveAfter(getCursor()) ? "moves" : "stays");
              }
          })),
          kotlin(
            """
              val field = 0
              fun declaredLater(c: Boolean) {
                  if (c) { val x = 1 }
                  val x = 2
              }
              fun readLater(c: Boolean) {
                  if (c) { val field = 1 }
                  use(field)
              }
              fun untouched(c: Boolean) {
                  if (c) { val y = 1 }
                  use(field)
              }
              fun declaredEarlier(c: Boolean) {
                  val x = 0
                  if (c) { val x = 1 }
              }
              fun use(i: Int) {}
              """,
            """
              val field = 0
              fun declaredLater(c: Boolean) {
                  /*~~(stays)~~>*/if (c) { val x = 1 }
                  val x = 2
              }
              fun readLater(c: Boolean) {
                  /*~~(stays)~~>*/if (c) { val field = 1 }
                  use(field)
              }
              fun untouched(c: Boolean) {
                  /*~~(moves)~~>*/if (c) { val y = 1 }
                  use(field)
              }
              fun declaredEarlier(c: Boolean) {
                  val x = 0
                  /*~~(stays)~~>*/if (c) { val x = 1 }
              }
              fun use(i: Int) {}
              """
          )
        );
    }

    @Test
    void nestedClassReferencesAPropertyOfTheOuterOneOnlyIfInner() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("prop").map(scope::canReference).map(String::valueOf).orElse("unbound"))),
          kotlin(
            """
              class Outer {
                  val prop = 1
                  fun own() { here() }
                  class Nested {
                      fun m() { here() }
                  }
                  inner class Inner {
                      fun m() { here() }
                  }
                  companion object {
                      fun c() { here() }
                  }
              }
              fun here() {}
              """,
            """
              class Outer {
                  val prop = 1
                  fun own() { /*~~(true)~~>*/here() }
                  class Nested {
                      fun m() { /*~~(false)~~>*/here() }
                  }
                  inner class Inner {
                      fun m() { /*~~(true)~~>*/here() }
                  }
                  companion object {
                      fun c() { /*~~(false)~~>*/here() }
                  }
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void classSeesTheMembersOfItsCompanionObject() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> Stream.of(scope.lookup("logger"), scope.lookup("helper", Scope.Namespace.METHOD))
            .map(found -> found.map(binding -> binding.getKind() + (scope.canReference(binding) ? "" : " out of reach")).orElse("unbound"))
            .collect(joining(", ")))),
          kotlin(
            """
              class A {
                  companion object {
                      private val logger = "log"
                      fun helper() {}
                  }
                  fun f() { here() }
                  class Nested {
                      fun n() { here() }
                  }
              }
              fun here() {}
              """,
            """
              class A {
                  companion object {
                      private val logger = "log"
                      fun helper() {}
                  }
                  fun f() { /*~~(MEMBER, MEMBER)~~>*/here() }
                  class Nested {
                      fun n() { /*~~(MEMBER, MEMBER)~~>*/here() }
                  }
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void enumReadsItsCompanionFromAMethodButNotFromAPropertyInitializer() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("logger").map(scope::canReference).map(String::valueOf).orElse("unbound"))),
          kotlin(
            """
              enum class E {
                  A;
                  companion object {
                      private val logger = "log"
                  }
                  val property = here()
                  fun method() = here()
              }
              fun here() = 1
              """,
            """
              enum class E {
                  A;
                  companion object {
                      private val logger = "log"
                  }
                  val property = /*~~(false)~~>*/here()
                  fun method() = /*~~(true)~~>*/here()
              }
              fun here() = 1
              """
          )
        );
    }

    @Test
    void impliedLambdaParameterIsReferencedFromANestedLambda() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("it").map(scope::canReference).map(String::valueOf).orElse("unbound"))),
          kotlin(
            """
              fun f(xs: List<Int>) {
                  xs.forEach { xs.map { y -> here() } }
              }
              fun here() {}
              """,
            """
              fun f(xs: List<Int>) {
                  xs.forEach { xs.map { y -> /*~~(true)~~>*/here() } }
              }
              fun here() {}
              """
          )
        );
    }

    @Test
    void classNestedInAnObjectReferencesTheObjectsMembers() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("prop").map(scope::canReference).map(String::valueOf).orElse("unbound"))),
          kotlin(
            """
              object O {
                  val prop = 1
                  class Nested {
                      fun m() { here() }
                  }
              }
              fun here() {}
              """,
            """
              object O {
                  val prop = 1
                  class Nested {
                      fun m() { /*~~(true)~~>*/here() }
                  }
              }
              fun here() {}
              """
          )
        );
    }

    private static Recipe atHere(Function<Scope, String> question) {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return "here".equals(m.getSimpleName()) ? SearchResult.found(m, question.apply(Scope.at(getCursor()))) : m;
            }
        });
    }

    @Test
    void initBlockIsAnInstanceInitializer() {
        @Language("kotlin") String source =
          """
            fun use(row: String, read: Any?): Int = 0
            class C {
                val earlier = 1
                init {
                    use("readsProperty", earlier)
                    use("readsLaterProperty", later)
                    use("readsCompanionProperty", logger)
                }
                val later = 1
                companion object {
                    val logger = Any()
                }
            }
            """;
        assertThat(readsRefused(source)).containsExactly("readsLaterProperty");
        assertThat(rowsKotlincRejects(source)).isEqualTo(readsRefused(source));
    }

    @Test
    void initializerCannotUseAPropertyInitializedAfterIt() {
        @Language("kotlin") String source =
          """
            fun use(row: String, read: Any?): Int = 0
            abstract class C {
                init {
                    assignedLater = use("assignsLaterProperty", null)
                    use("readsLaterPropertyWithGetter", laterWithGetter)
                    use("readsLaterLateinitProperty", lateinitLater)
                    use("readsLaterAbstractProperty", abstractLater)
                }
                val withGetter: Int = use("initializerWithGetterReadsLater", later)
                    get() = field
                val getter: Int
                    get() = use("getterReadsLater", later)
                var assignedLater = 0
                val laterWithGetter: Int = 1
                    get() = field
                lateinit var lateinitLater: String
                abstract val abstractLater: Int
                val later = 1
            }
            """;
        assertThat(readsRefused(source)).containsExactly(
          "assignsLaterProperty",
          "initializerWithGetterReadsLater",
          "readsLaterPropertyWithGetter");
        assertThat(rowsKotlincRejects(source)).isEqualTo(readsRefused(source));
    }

    @Test
    void enumUsesNoCompanionMemberOrEntryWhileAnEntryIsConstructed() {
        @Language("kotlin") String source =
          """
            fun use(row: String, read: Any?): Int = 0
            enum class E(val n: Int) {
                A(use("entryArgument", logger)),
                B(use("entryArgumentReadsEarlierEntry", A));
                init {
                    use("initBlock", logger)
                    use("initBlockReadsConst", X)
                    use("initBlockCallsCompanionFunction", helper())
                    use("initBlockReadsEntry", A)
                    use("initBlockReadsProperty", n)
                }
                constructor(s: String) : this(1) {
                    use("secondaryConstructorReadsEntry", A)
                    use("secondaryConstructorReadsCompanion", logger)
                }
                var withAccessors: Any = use("propertyWithAccessors", logger)
                    get() = field
                companion object {
                    val logger = Any()
                    const val X = 1
                    fun helper() = 0
                }
            }
            """;
        assertThat(readsRefused(source)).containsExactly(
          "entryArgument",
          "initBlock",
          "initBlockCallsCompanionFunction",
          "initBlockReadsConst",
          "initBlockReadsEntry",
          "propertyWithAccessors",
          "secondaryConstructorReadsEntry");
        assertThat(rowsKotlincRejects(source)).isEqualTo(readsRefused(source));
    }

    private static final Pattern USE_ROW = Pattern.compile("use\\(\"(\\w+)\"");

    /**
     * The rows of a source whose second argument to {@code use(row, read)}, or the variable the call is assigned
     * to, names something {@link Scope#canReference(Scope.Binding)} refuses there, in alphabetical order.
     */
    private static Set<String> readsRefused(@Language("kotlin") String source) {
        Set<String> refused = new TreeSet<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                if ("use".equals(method.getSimpleName())) {
                    Cursor parent = getCursor().getParentTreeCursor();
                    Expression read = parent.getValue() instanceof J.Assignment assignment &&
                                      assignment.getAssignment() == method ? assignment.getVariable() :
                      method.getArguments().get(1);
                    J.Identifier name = read instanceof J.MethodInvocation call ? call.getName() : (J.Identifier) read;
                    Cursor at = Scope.cursorTo(parent, name);
                    Scope scope = Scope.at(at);
                    Scope.Binding binding = scope.lookup(name.getSimpleName(), Scope.service(at).namespaceOf(at)).orElseThrow();
                    if (!scope.canReference(binding)) {
                        refused.add((String) ((J.Literal) method.getArguments().getFirst()).getValue());
                    }
                }
                return super.visitMethodInvocation(method, p);
            }
        }.visit(KotlinParser.builder().classpath(JavaParser.runtimeClasspath()).build()
          .parse(new InMemoryExecutionContext(), source).findFirst().orElseThrow(), 0);
        return refused;
    }

    /**
     * The rows in which kotlinc reports an error, in alphabetical order. A row starts at the line that names it. An
     * error above every row counts by its line number, so that it fails the comparison.
     */
    private static Set<String> rowsKotlincRejects(@Language("kotlin") String source) {
        KotlinCompilation compilation = new KotlinCompilation();
        compilation.setSources(List.of(SourceFile.Companion.kotlin("Rows.kt", source, false)));
        compilation.setInheritClassPath(true);
        compilation.setMessageOutputStream(OutputStream.nullOutputStream());
        String[] lines = source.split("\n");
        Set<String> rejected = new TreeSet<>();
        Matcher error = Pattern.compile("^e: .*Rows\\.kt:(\\d+):\\d+ ", Pattern.MULTILINE)
          .matcher(compilation.compile().getMessages());
        while (error.find()) {
            String row = "line " + error.group(1);
            for (int line = Integer.parseInt(error.group(1)) - 1; line >= 0; line--) {
                Matcher named = USE_ROW.matcher(lines[line]);
                if (named.find()) {
                    row = named.group(1);
                    break;
                }
            }
            rejected.add(row);
        }
        return rejected;
    }
}
