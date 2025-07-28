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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.other;
import static org.openrewrite.test.SourceSpecs.text;

class FindSourceFilesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindSourceFiles("**/hello.txt"));
    }

    @DocumentExample
    @Test
    void findMatchingFile() {
        rewriteRun(
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("a/b/hello.txt")
          )
        );
    }

    @Test
    void starStarMatchesAtRoot() {
        rewriteRun(
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("hello.txt")
          ),
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("./hello.txt")
          )
        );
    }

    @Test
    void windows() {
        rewriteRun(
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("C:\\Windows\\hello.txt")
          ),
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("\\Windows\\hello.txt")
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello.txt", "/hello.txt", "\\hello.txt", "./hello.txt", ".\\hello.txt"})
    void findRoot(String filePattern) {
        rewriteRun(
          spec -> spec.recipe(new FindSourceFiles(filePattern)),
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("hello.txt")
          ),
          text(
            "hello world!",
            spec -> spec.path("a/hello.txt")
          )
        );
    }

    @NullSource
    @ParameterizedTest
    @ValueSource(strings = {""})
    void blankMatchesEverything(String filePattern) {
        rewriteRun(
          spec -> spec.recipe(new FindSourceFiles(filePattern)),
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("hello.txt")
          ),
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("a/hello.txt")
          ),
          text(
            "name: hello-world",
            "~~>name: hello-world",
            spec -> spec.path(".github/workflows/hello.yml")
          ),
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("C:\\Windows\\hello.txt")
          )
        );
    }

    @Test
    void findDotfiles() {
        rewriteRun(
          spec -> spec.recipe(new FindSourceFiles(".github/workflows/*.yml")),
          text(
            """
              name: hello-world
              on: push
              jobs:
                my-job:
                  runs-on: ubuntu-latest
                  steps:
                    - name: my-step
                      run: echo "Hello World!"
              """,
            """
              ~~>name: hello-world
              on: push
              jobs:
                my-job:
                  runs-on: ubuntu-latest
                  steps:
                    - name: my-step
                      run: echo "Hello World!"
              """,
            spec -> spec.path(".github/workflows/hello.yml")
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/3758")
    @Test
    void eitherOr() {
        rewriteRun(
          spec -> spec.recipe(new FindSourceFiles("**/*.{md,txt}")),
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("a/b/hello.md")
          ),
          text(
            "hello world!",
            "~~>hello world!",
            spec -> spec.path("a/c/hello.txt")
          )
        );
    }

    @Test
    void multiplePathsSemicolonDelimitedPaths() {
        rewriteRun(
          spec -> spec.recipe(new FindSourceFiles("a.txt ; b.txt")),
          text(
            "this one",
            "~~>this one",
            spec -> spec.path("a.txt")
          ),
          text(
            "also this one",
            "~~>also this one",
            spec -> spec.path("b.txt")
          ),
          text("not this one", spec -> spec.path("c.txt"))
        );
    }

    @Test
    void negation() {
        rewriteRun(
          spec -> spec.recipe(new FindSourceFiles("!(**/not-this.txt)")),
          text("not-this", spec -> spec.path("not-this.txt")),
          text("this", "~~>this", spec -> spec.path("this.txt"))
        );
    }

    @Test
    void folderNegation() {
        rewriteRun(
          spec -> spec.recipe(new FindSourceFiles("!(not-this/**)")),
          text("not-this", spec -> spec.path("not-this/not-this.txt")),
          text("this", "~~>this", spec -> spec.path("this/this.txt"))
        );
    }


    @Test
    void findMatchingQuark() {
        rewriteRun(
          spec -> spec.recipe(new FindSourceFiles("**/hello.cfg")),
          other(
            "hello world!",
            "~~>⚛⚛⚛ The contents of this file are not visible. ⚛⚛⚛",
            spec -> spec.path("a/b/hello.cfg")
          )
        );
    }
}
