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
package org.openrewrite.text;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.text;

class FindAndReplaceJavaTest implements RewriteTest {

    @DocumentExample
    @Test
    void findAndReplaceJava() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace("Test", "Replaced", null, null, null, null, null)),
          java(
            "class Test {}",
            "class Replaced {}"
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3532")
    void filePatternShouldLimitApplication() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
              type: specs.openrewrite.org/v1beta/recipe
              name: com.yourorg.FindAndReplaceExample
              displayName: Find and replace example
              recipeList:
                - org.openrewrite.text.FindAndReplace:
                    find: blacklist
                    replace: denylist
                    filePattern: '**/*.java'
              """,
            "com.yourorg.FindAndReplaceExample"),
          java(
            "class blacklist {}",
            "class denylist {}"
          ),
          text("See `class blacklist {}`")
        );
    }
}
