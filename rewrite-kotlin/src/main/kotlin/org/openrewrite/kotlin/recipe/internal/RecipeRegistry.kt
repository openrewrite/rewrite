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
package org.openrewrite.kotlin.recipe.internal

import org.openrewrite.Recipe
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridge between compiler-plugin-generated `Recipe` subclasses and the runtime
 * [org.openrewrite.recipe] lookup function. Each generated class registers itself
 * here in a static initializer (the compiler plugin emits the registration alongside
 * the class). Lookups happen by recipe name — the string passed to `recipe("...")`
 * in the author's source.
 *
 * Plain `ConcurrentHashMap`: registrations happen during class loading (single-threaded
 * within a classloader), lookups happen during recipe instantiation (any thread). No
 * removal semantics — recipes live for the lifetime of the JVM.
 */
public object RecipeRegistry {
    private val byName: MutableMap<String, Recipe> = ConcurrentHashMap()

    /**
     * Registered by compiler-plugin-generated code at class init time. If a name
     * collides, the later registration wins — we don't error because shadowing
     * across classloaders (test harnesses, IDE incremental compile) is normal.
     */
    public fun register(name: String, recipe: Recipe) {
        byName[name] = recipe
    }

    /** Returns the registered recipe, or `null` if no class registered under [name]. */
    public fun lookup(name: String): Recipe? = byName[name]
}
