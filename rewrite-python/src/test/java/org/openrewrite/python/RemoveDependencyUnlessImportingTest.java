/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.python.Assertions.*;

class RemoveDependencyUnlessImportingTest implements RewriteTest {

    @Test
    void keepsTheDependencyOfAProjectThatStillImportsIt() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("async-timeout", null, null, List.of("async_timeout"))),
          python(
            """
              import async_timeout


              async def fetch(session):
                  async with async_timeout.timeout(10):
                      return await session.get("/")
              """
          ),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "aiohttp>=3.9",
                  "async-timeout>=4.0",
              ]
              """
          )
        );
    }

    @Test
    void removesTheDependencyWhenNoSourceImportsIt() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("async-timeout", null, null, List.of("async_timeout"))),
          python(
            """
              import asyncio


              async def fetch(session):
                  async with asyncio.timeout(10):
                      return await session.get("/")
              """
          ),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "aiohttp>=3.9",
                  "async-timeout>=4.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "aiohttp>=3.9",
              ]
              """
          )
        );
    }

    @Test
    void anImportOfASubmoduleKeepsTheDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("retry", null, null, List.of("retry"))),
          python(
            """
              from retry.api import retry_call
              """
          ),
          requirementsTxt("requests>=2.28.0\nretry>=0.9")
        );
    }

    @Test
    void anyOfSeveralModulesKeepsTheDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("beautifulsoup4", null, null, List.of("bs4", "BeautifulSoup"))),
          python(
            """
              from bs4 import BeautifulSoup
              """
          ),
          requirementsTxt("beautifulsoup4>=4.12")
        );
    }

    @Test
    void keepsOnlyTheProjectThatImportsIt() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("retry", null, null, List.of("retry"))),
          python(
            """
              from retry import retry
              """,
            spec -> spec.path("svc-a/src/svc_a/client.py")
          ),
          python(
            """
              import tenacity
              """,
            spec -> spec.path("svc-b/src/svc_b/client.py")
          ),
          pyproject(
            """
              [project]
              name = "svc-a"
              version = "1.0.0"
              dependencies = ["retry>=0.9"]
              """,
            spec -> spec.path("svc-a/pyproject.toml")
          ),
          pyproject(
            """
              [project]
              name = "svc-b"
              version = "1.0.0"
              dependencies = ["retry>=0.9", "tenacity>=8.0.0"]
              """,
            """
              [project]
              name = "svc-b"
              version = "1.0.0"
              dependencies = ["tenacity>=8.0.0"]
              """,
            spec -> spec.path("svc-b/pyproject.toml")
          )
        );
    }

    @Test
    void anImportInANestedProjectThatDoesNotDeclareItKeepsTheEnclosingDeclaration() {
        // The nested project gets `retry` from the root's declaration, so removing that would break the import.
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("retry", null, null, List.of("retry"))),
          python(
            """
              from retry import retry
              """,
            spec -> spec.path("libs/inner/inner/core.py")
          ),
          pyproject(
            """
              [project]
              name = "root"
              version = "1.0.0"
              dependencies = ["retry>=0.9"]
              """
          ),
          pyproject(
            """
              [project]
              name = "inner"
              version = "1.0.0"
              dependencies = []
              """,
            spec -> spec.path("libs/inner/pyproject.toml")
          )
        );
    }

    @Test
    void removesTheDependencyOnceAnEarlierRecipeRemovesTheLastImport() {
        rewriteRun(
          spec -> spec
            .recipes(new RenameModule("async_timeout", "asyncio"),
              new RemoveDependency("async-timeout", null, null, List.of("async_timeout")))
            .cycles(3)
            .expectedCyclesThatMakeChanges(2),
          python(
            """
              import async_timeout


              async def fetch(session):
                  async with async_timeout.timeout(10):
                      return await session.get("/")
              """,
            """
              import asyncio


              async def fetch(session):
                  async with asyncio.timeout(10):
                      return await session.get("/")
              """
          ),
          pyproject(
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "aiohttp>=3.9",
                  "async-timeout>=4.0",
              ]
              """,
            """
              [project]
              name = "myapp"
              version = "1.0.0"
              dependencies = [
                  "aiohttp>=3.9",
              ]
              """
          )
        );
    }

    /**
     * Stands in for a migration that rewrites a module's imports and uses, and like one asks for another cycle so
     * that a dependency recipe scanning the sources sees the result.
     */
    private static class RenameModule extends Recipe {
        private final String from;
        private final String to;

        RenameModule(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String getDisplayName() {
            return "Rename a module";
        }

        @Override
        public String getDescription() {
            return "Renames every identifier spelling the module.";
        }

        @Override
        public boolean causesAnotherCycle() {
            return true;
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PythonIsoVisitor<ExecutionContext>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                    J.Identifier i = super.visitIdentifier(identifier, ctx);
                    return from.equals(i.getSimpleName()) ? i.withSimpleName(to) : i;
                }
            };
        }
    }
}
