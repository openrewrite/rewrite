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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

@SuppressWarnings({"KubernetesUnknownResourcesInspection", "KubernetesNonEditableResources"})
class YamlExampleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.yaml.example");
    }

    @DocumentExample
    @Test
    void desiredSituation() {
        rewriteRun(
          yaml(
            """
              apiVersion: apps/v1
              kind: ReplicaSet
              metadata:
                name: frontend
                labels:
                  app: guestbook
                  tier: frontend
              ---
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                name: nginx-deployment
                labels:
                  app: nginx
              """,
            """
              apiVersion: apps/v1
              kind: ReplicaSet
              metadata:
                name: frontend
                labels:
                  app: guestbook
                  tier: frontend
              ---
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                name: nginx-deployment
                labels:
                  app: nginx
                annotations:
                  openrewrite/injected-by: "OpenRewrite Recipe"
                  openrewrite/recipe-version: "1.0.0"
                  openrewrite/agent-type: "Java"
              """
          )
        );
    }

    @Test
    void currentSituation() {
        rewriteRun(
          yaml(
            """
              apiVersion: apps/v1
              kind: ReplicaSet
              metadata:
                name: frontend
                labels:
                  app: guestbook
                  tier: frontend
              ---
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                name: nginx-deployment
                labels:
                  app: nginx
              """,
            """
              apiVersion: apps/v1
              kind: ReplicaSet
              metadata:
                name: frontend
                labels:
                  app: guestbook
                  tier: frontend
                annotations:
                  openrewrite/injected-by: "OpenRewrite Recipe"
                  openrewrite/recipe-version: "1.0.0"
                  openrewrite/agent-type: "Java"
              ---
              apiVersion: apps/v1
              kind: Deployment
              metadata:
                name: nginx-deployment
                labels:
                  app: nginx
                annotations:
                  openrewrite/injected-by: "OpenRewrite Recipe"
                  openrewrite/recipe-version: "1.0.0"
                  openrewrite/agent-type: "Java"
              """
          )
        );
    }
}
