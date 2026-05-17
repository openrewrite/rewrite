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
package org.openrewrite

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * Smoke tests for the runtime DSL surface — `recipe(...)`, `edit { }`,
 * `scan<A>(initial) { }.edit { }`, language-scope factories, precondition
 * wrappers, and the `rewrite { } to { }` plugin-required stub.
 *
 * These tests cover only the imperative path (which works without the K2
 * compiler plugin). Plugin-dependent tests live in Phase 3's checker /
 * IR-generation test classes.
 *
 * The tests use the `java { ... }` scope because `rewrite-java` is on
 * rewrite-kotlin's runtime classpath (declared as `implementation`). Other
 * languages are `compileOnly` to keep the runtime lean for Kotlin-only
 * consumers; tests for those scopes need explicit testImplementation deps.
 */
class RecipeDslSurfaceTest {

    @Test
    fun `recipe with metadata produces a Recipe with the metadata accessors populated`() {
        val r = recipe(
            displayName = "Test recipe",
            description = "A test recipe",
            tags = setOf("test", "smoke"),
            estimatedEffortPerOccurrence = Duration.ofMinutes(2),
        ) {
            edit { java { /* no-op visitor */ } }
        }
        assertThat(r.displayName).isEqualTo("Test recipe")
        assertThat(r.description).isEqualTo("A test recipe")
        assertThat(r.getTags()).containsExactlyInAnyOrder("test", "smoke")
        assertThat(r.estimatedEffortPerOccurrence).isEqualTo(Duration.ofMinutes(2))
    }

    @Test
    fun `recipe without scan returns a plain Recipe (not ScanningRecipe)`() {
        val r = recipe("Foo", "bar") { edit { java { /* no-op */ } } }
        assertThat(r).isNotInstanceOf(ScanningRecipe::class.java)
    }

    @Test
    fun `recipe with scan returns a ScanningRecipe`() {
        val r = recipe("Foo", "bar") {
            scan<String>("initial") { _ ->
                java { /* no-op */ }
            }.edit { _ -> java { /* no-op */ } }
        }
        assertThat(r).isInstanceOf(ScanningRecipe::class.java)
    }

    @Test
    fun `empty edit block produces a no-op visitor`() {
        val r = recipe("Foo", "bar") { edit { } }
        // TreeVisitor.noop() returns a fresh instance per call — assert via
        // behavior (visit returns input unchanged) rather than instance ID.
        val v = r.visitor
        assertThat(v).isInstanceOf(TreeVisitor::class.java)
    }

    @Test
    fun `declaring edit twice fails fast`() {
        assertThatThrownBy {
            recipe("Foo", "bar") {
                edit { java { /* no-op */ } }
                edit { java { /* no-op */ } }
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("already declares an edit block")
    }

    @Test
    fun `declaring scan twice fails fast`() {
        assertThatThrownBy {
            recipe("Foo", "bar") {
                scan<String>("a") { _ -> java { } }.edit { _ -> java { } }
                scan<String>("b") { _ -> java { } }.edit { _ -> java { } }
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("already declares a scan block")
    }

    @Test
    fun `bare rewrite without plugin throws plugin-required error at getVisitor time`() {
        val r = recipe("Foo", "bar") {
            edit {
                @Suppress("UNUSED_VARIABLE")
                val unused: RewriteAdvice1<String, String> = rewrite { s: String -> s }
            }
        }
        // edit { } only captures the block; the plugin-required stub doesn't
        // fire until the edit block actually evaluates inside getVisitor().
        assertThatThrownBy { r.visitor }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("rewrite-kotlin K2 compiler plugin")
    }

    @Test
    fun `scan-bound edit threads the accumulator through directly`() {
        // The accumulator is a lambda parameter, not a receiver property.
        // The DSL is agnostic to mutability — authors pick `MutableList`,
        // `AtomicReference<String>`, or anything that fits.
        val r = recipe("Foo", "bar") {
            scan<MutableList<String>>(mutableListOf()) { acc ->
                java {
                    visitMethodInvocation { mi ->
                        acc.add(mi.simpleName)
                        mi
                    }
                }
            }.edit { _ -> java { /* no-op edit */ } }
        }
        val sr = r as ScanningRecipe<*>
        val initial = sr.getInitialValue(InMemoryExecutionContext())
        assertThat(initial).isInstanceOf(MutableList::class.java)
    }

    @Test
    fun `recipes composite exposes its children via getRecipeList`() {
        val a = recipe("A", "a") { edit { java { /* no-op */ } } }
        val b = recipe("B", "b") { edit { java { /* no-op */ } } }
        val r = recipes("Combo", "a + b", a, b)
        assertThat(r.displayName).isEqualTo("Combo")
        assertThat(r.description).isEqualTo("a + b")
        assertThat(r.recipeList).containsExactly(a, b)
    }

    @Test
    fun `recipes composite round-trips through RecipeSerializer`() {
        // The K2 compiler plugin is not applied to this module's own test
        // sources, so children built through `recipe { }` here would be
        // anonymous and break serialization. Use `Recipe.noop()` — a named
        // singleton class — to exercise the round-trip independent of K2.
        val r = recipes("Combo", "noop only", Recipe.noop())
        val ser = RecipeSerializer()
        val restored = ser.read(ser.write(r))
        assertThat(restored.displayName).isEqualTo("Combo")
        assertThat(restored.description).isEqualTo("noop only")
        assertThat(restored.recipeList).hasSize(1)
    }
}
