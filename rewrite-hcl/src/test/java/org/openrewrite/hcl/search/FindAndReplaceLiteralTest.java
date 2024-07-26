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
package org.openrewrite.hcl.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.List;

import static org.openrewrite.hcl.Assertions.hcl;

class FindAndReplaceLiteralTest implements RewriteTest {

    @Test
    void defaultNonRegexReplace() {
        rewriteRun(
          spec -> spec.recipe( new FindAndReplaceLiteral("app-cluster", "new-app-cluster", null, null, null, null, null)),
          hcl(

            """
            config = {
              app_deployment = {
                cluster_name = "app-cluster"
              }
            }
            """,
            """
            config = {
              app_deployment = {
                cluster_name = "new-app-cluster"
              }
            }
            """
          )
        );
    }

    @Test
    void removeWhenReplaceIsNullOrEmpty() {
        rewriteRun(
          spec -> spec.recipe( new FindAndReplaceLiteral("prefix-", null, null, null, null, null, null)),
          hcl(

            """
            config = {
              app_deployment = {
                cluster_name = "prefix-app-cluster"
              }
            }
            """,
            """
            config = {
              app_deployment = {
                cluster_name = "app-cluster"
              }
            }
            """
          )
        );
    }

    @Test
    void regexReplace() {
        rewriteRun(
          spec -> spec.recipe( new FindAndReplaceLiteral(".", "a", true, null, null, null, null)),
          hcl(
            """
            config = {
              app_deployment = {
                cluster_name = "app-cluster"
              }
            }
            """,
            """
            config = {
              app_deployment = {
                cluster_name = "aaaaaaaaaaa"
              }
            }
            """
          )
        );
    }

    @Test
    void captureGroupsReplace() {
        rewriteRun(
          spec -> spec.recipe( new FindAndReplaceLiteral("old-([^.]+)", "new-$1", true, null, null, null, null)),
          hcl(
            """
            config = {
              app_deployment = {
                cluster_name = "old-app-cluster"
              }
            }
            """,
            """
            config = {
              app_deployment = {
                cluster_name = "new-app-cluster"
              }
            }
            """
          )
        );
    }

    @Test
    void noRecursiveReplace() {
        rewriteRun(
          spec -> spec.recipe( new FindAndReplaceLiteral("app", "application", null, null, null, null, null)),
          hcl(
            """
            config = {
              app_deployment = {
                cluster_name = "app-cluster"
              }
            }
            """,
            """
            config = {
              app_deployment = {
                cluster_name = "application-cluster"
              }
            }
            """
          )
        );
    }

    @Test
    void compatibleWithDollarSigns() {
        rewriteRun(
          spec -> spec.recipe( new FindAndReplaceLiteral("$${app-cluster}", "$${new-app-cluster}", null, null, null, null, null)),
          hcl(
            """
            config = {
              app_deployment = {
                cluster_name = "$${app-cluster}"
              }
            }
            """,
            """
            config = {
              app_deployment = {
                cluster_name = "$${new-app-cluster}"
              }
            }
            """
          )
        );
    }

    @Test
    void doesNotReplaceStringTemplate() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplaceLiteral("app-name", "new-app-name", null, null, null, null, null)),
          hcl(
            """
            config = {
              app_deployment = {
                cluster_name = "old-${app-name}-cluster"
              }
            }
            """
          )
        );
    }

    @Test
    void doesNothingIfLiteralNotFound() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplaceLiteral("hello", "goodbye", null, null, null, null, null)),
          hcl(
            """
            config = {
              app_deployment = {
                cluster_name = "app-cluster"
              }
            }
            """
          )
        );
    }

    @Test
    void doesNotReplaceVariableNames() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplaceLiteral("app_deployment", "replacement_deployment_name", null, null, null, null, null)),
          hcl(
            """
            config = {
              app_deployment = {
                cluster_name = "app-cluster"
              }
            }
            """
          )
        );
    }

    @Test
    void replacesNumericLiterals() {
        rewriteRun(
          spec -> spec.recipe( new FindAndReplaceLiteral("2", "1",null, null, null, null, null)),
          hcl(
            """
            config = {
              app_deployment = {
                cluster_name = "app-cluster"
                app_replica = 2
              }
            }
            """,
            """
            config = {
              app_deployment = {
                cluster_name = "app-cluster"
                app_replica = 1
              }
            }
            """
          )
        );
    }

        @Value
        @EqualsAndHashCode(callSuper = false)
    static class MultiFindAndReplaceLiteral extends Recipe {

        @Override
        public String getDisplayName() {
            return "Replaces \"cluster-1\" with \"cluster-2\" then \"cluster-3\" then \"cluster-4\"";
        }

        @Override
        public String getDescription() {
            return "Replaces \"cluster-1\" with \"cluster-2\" then \"cluster-3\" then \"cluster-4\".";
        }

        @Override
        public List<Recipe> getRecipeList() {
            return Arrays.asList(
              new FindAndReplaceLiteral("cluster-1", "cluster-2", null, null, null, null, null),
              new FindAndReplaceLiteral("cluster-2", "cluster-3", null, null, null, null, null),
              new FindAndReplaceLiteral("cluster-3", "cluster-4", null, null, null, null, null)
            );
        }
    }

    @Test
    void successiveReplacement() {
        rewriteRun(
          spec -> spec.recipe(new MultiFindAndReplaceLiteral()),
          hcl(
            """
            config = {
              app_deployment = {
                cluster_name = "cluster-1"
              }
            }
            """,
            """
               config = {
                 app_deployment = {
                   cluster_name = "cluster-4"
                 }
               }
            """
          )
        );
    }
}
