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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.yaml.Assertions.yaml;

class ChangePropertyValueTest implements RewriteTest {
    @DocumentExample
    @Test
    void simpleDotSeparated() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", null, null, null)),
          yaml(
                """
            my.prop: foo
            """, """
            my.prop: bar
            """)
        );
    }

    @Test
    void simpleIndented() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", null, null, null)),
          yaml("""
            my:
              prop: foo
            """, """
            my:
              prop: bar
            """)
        );
    }

    @Test
    void oldValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", "foo", null, null)),
          yaml("""
            my:
              prop: foo
            """, """
            my:
              prop: bar
            """)
        );
    }

    @Test
    void badOldValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", "fooz", null, null)),
          yaml("""
            my:
              prop: foo
            """)
        );
    }

    @Test
    void regex() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar$1", "f(o+)", true, null)),
          yaml("""
            my:
              prop: foooo
            """, """
            my:
              prop: baroooo
            """)
        );
    }

    @Test
    void regexDefaultOff() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", ".+", null, null)),
          yaml("""
            my:
              prop: foo
            """)
        );
    }

    @Test
    void validatesThatOldValueIsRequiredIfRegexEnabled() {
        assertTrue(new ChangePropertyValue("my.prop", "bar", null, true, null).validate().isInvalid());
    }
}
