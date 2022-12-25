/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.ChangeText;

import static org.openrewrite.test.SourceSpecs.text;

class HasSourcePathTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeTextWhenSourcePath("hello jon", "glob", "**/hello.txt"));
    }

    @Example
    @Test
    void hasFileMatch() {
        rewriteRun(
          text("hello world", "hello jon", spec -> spec.path("a/b/hello.txt")),
          text("hello world", "hello jon", spec -> spec.path("hello.txt"))
        );
    }

    @Test
    void hasNoFileMatch() {
        rewriteRun(
          text("hello world", spec -> spec.path("a/b/goodbye.txt"))
        );
    }

    @Test
    void regexMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTextWhenSourcePath("hello jon", "regex", ".+\\.gradle(\\.kts)?$")),
          text("", "hello jon", spec -> spec.path("build.gradle")),
          text("", "hello jon", spec -> spec.path("build.gradle.kts")),
          text("", spec -> spec.path("pom.xml"))
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1878")
    @Test
    void githubYaml() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTextWhenSourcePath("hello jon", "glob", ".github/workflows/*.yml")),
          text("", "hello jon", spec -> spec.path(".github/workflows/ci.yml"))
        );
    }

    private static class ChangeTextWhenSourcePath extends ChangeText {
        private final String syntax;
        private final String filePattern;

        @JsonCreator
        public ChangeTextWhenSourcePath(String toText, String syntax, String filePattern) {
            super(toText);
            this.syntax = syntax;
            this.filePattern = filePattern;
        }

        @Override
        protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
            return new HasSourcePath<>(syntax, filePattern);
        }
    }
}
