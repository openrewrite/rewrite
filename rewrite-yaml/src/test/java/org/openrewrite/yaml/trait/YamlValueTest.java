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
package org.openrewrite.yaml.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class YamlValueTest implements RewriteTest {

    @DocumentExample
    @Test
    void yamlValue() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new YamlValue.Matcher("$.key").asVisitor(entry ->
            entry.withKey("key2").withValue("value2").getTree()))),
          yaml(
            "key: value",
            "key2: value2"
          )
        );
    }
}
