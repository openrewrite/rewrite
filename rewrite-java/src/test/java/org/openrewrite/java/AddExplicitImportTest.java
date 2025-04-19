/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


class AddExplicitImportTest implements RewriteTest {

    @Test
    void addExplicitImportWhenNoExistingImports() {
        rewriteRun(spec -> spec.recipe(new AddExplicitImport("foo.bar"))
          , java("""
            class Dummy {}
            """, """
            import foo.bar;

            class Dummy {}
            """));
    }

    @Test
    void addExplicitImportWhenExistingImports() {
        rewriteRun(spec -> spec.recipe(new AddExplicitImport("foo.bar"))
          , java("""
            import xyz.bbb.ccc.D;

            class Dummy {}
            """, """
            import xyz.bbb.ccc.D;
            import foo.bar;

            class Dummy {}
            """));
    }

    @Test
    void addStaticImports() {
        rewriteRun(spec -> spec.recipe(new AddExplicitImport("static foo.bar.staticMethod"))
          , java("""
            import xyz.bbb.ccc.D;

            class Dummy {}
            """, """
            import xyz.bbb.ccc.D;
            import static foo.bar.staticMethod;

            class Dummy {}
            """));
    }
}
