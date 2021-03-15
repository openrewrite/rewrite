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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.RecipeTest
import org.openrewrite.java.format.TabsAndIndents
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import kotlin.streams.toList

class AutoFormatIntegTest : RecipeTest {
    companion object {
        private val predicate = BiPredicate<Path, BasicFileAttributes> { p, bfa ->
            bfa.isRegularFile && p.fileName.toString().endsWith(".java") &&
                    !p.toString().contains("/grammar/") &&
                    !p.toString().contains("/gen/")
        }

        private val paths = Files.find(Paths.get("../"), 999, predicate).toList()
    }

    @Test
    fun tabsAndIndents() {
        paths.forEach {
            assertUnchanged(
                parser = JavaParser.fromJavaVersion().build(),
                recipe = TabsAndIndents(),
                before = it.toFile()
            )
        }
    }
}
