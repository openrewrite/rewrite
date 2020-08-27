/*
 * Copyright 2020 the original author or authors.
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

import org.openrewrite.Assertions.whenParsedBy
import org.openrewrite.config.ClasspathResourceLoader
import java.nio.file.Path

fun <S : SourceFile> String.whenParsedBy(parser: Parser<S>): Assertions.StringSourceFileAssert<S> =
        whenParsedBy(parser, this)

fun <S : SourceFile> Path.whenParsedBy(parser: Parser<S>): Assertions.PathSourceFileAssert<S> =
        whenParsedBy(parser, this)

/**
 * Retrieve an environment for the named recipe from the classpath.
 */
fun loadRefactorPlan(recipeName: String): Environment {
    val crl = ClasspathResourceLoader(emptyList())
    val recipeConfig = crl.loadRecipes().asSequence()
            .find { it.name == recipeName } ?: throw RuntimeException("Couldn't load recipe named '$recipeName'. " +
                    "Verify that there's a yml file defining a recipe with this name under src/test/resources/META-INF/rewrite")

    val recipe = recipeConfig.build()
    val visitors = crl.loadVisitors()
            .filter { recipe.accept(it) == Recipe.FilterReply.ACCEPT }
    if (visitors.isEmpty()) {
        throw RuntimeException("Couldn't find any visitors for recipe named `$recipeName`. " +
                "Verify that your recipe has an include pattern that accepts at least one visitor according to SourceVisitor<J>.accept()")
    }
    return Environment.builder()
            .loadRecipe(recipeConfig)
            .loadVisitors(visitors)
            .build()
}

/**
 * Retrieve the visitors for the named recipe from the classpath
 */
fun loadVisitors(recipeName: String): Collection<RefactorVisitor<*>> = loadRefactorPlan(recipeName).visitors(recipeName)
