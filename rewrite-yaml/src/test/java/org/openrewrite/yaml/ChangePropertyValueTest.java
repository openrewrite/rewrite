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
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", null, null, null, null)),
          yaml("""
            my.prop: foo
            """, """
            my.prop: bar
            """)
        );
    }

    @Test
    void simpleIndented() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", null, null, null, null)),
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
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", "foo", null, null, null)),
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
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", "fooz", null, null, null)),
          yaml("""
            my:
              prop: foo
            """)
        );
    }

    @Test
    void regex() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar$1", "f(o+)", true, null, null)),
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
          spec -> spec.recipe(new ChangePropertyValue("my.prop", "bar", ".+", null, null, null)),
          yaml("""
            my:
              prop: foo
            """)
        );
    }

    @Test
    void partialMatchRegex() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("*", "[replaced:$1]", "\\[replaceme:(.*?)]", true, true,null)),
          yaml("""
            multiple: "[replaceme:1][replaceme:2]"
            multiple-prefixed: "test[replaceme:1]test[replaceme:2]"
            multiple-suffixed: "[replaceme:1]test[replaceme:2]test"
            multiple-both: "test[replaceme:1]test[replaceme:2]test"
            """, """
            multiple: "[replaced:1][replaced:2]"
            multiple-prefixed: "test[replaced:1]test[replaced:2]"
            multiple-suffixed: "[replaced:1]test[replaced:2]test"
            multiple-both: "test[replaced:1]test[replaced:2]test"
            """)
        );
    }

    @Test
    void partialMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyValue("*", "replaced", "replaceme", null, true,null)),
          yaml("""
            multiple: "[replaceme:1][replaceme:2]"
            multiple-prefixed: "test[replaceme:1]test[replaceme:2]"
            multiple-suffixed: "[replaceme:1]test[replaceme:2]test"
            multiple-both: "test[replaceme:1]test[replaceme:2]test"
            """, """
            multiple: "[replaced:1][replaced:2]"
            multiple-prefixed: "test[replaced:1]test[replaced:2]"
            multiple-suffixed: "[replaced:1]test[replaced:2]test"
            multiple-both: "test[replaced:1]test[replaced:2]test"
            """)
        );
    }

    @Test
    void validatesThatOldValueIsRequiredIfPartialMatchEnabled() {
        assertTrue(new ChangePropertyValue("my.prop", "bar", null, null, true,null).validate().isInvalid());
    }

    @Test
    void validatesThatOldValueIsRequiredIfRegexEnabled() {
        assertTrue(new ChangePropertyValue("my.prop", "bar", null, true, null,null).validate().isInvalid());
    }
}
