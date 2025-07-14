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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

@SuppressWarnings({"KubernetesUnknownResourcesInspection", "KubernetesNonEditableResources"})
class MergeYamlDocumentTest implements RewriteTest {

    @DocumentExample
    @Test
    void addOnlyToDeployment() {
        rewriteRun(
          spec -> spec.recipe(new MergeYamlDocument("$.kind[?(@ == 'Deployment')]", "$.metadata",
            //language=yaml
            """
              annotations:
                org.openrewrite.yaml/merge: "true"
              """, null, null, null, null, null)),
          //language=yaml
          yaml(
            """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: nginx-deployment
            spec:
              replicas: 1
              selector:
                matchLabels:
                  app: nginx
              template:
                metadata:
                  labels:
                    app: nginx
                spec:
                  containers:
                    - name: nginx
                      image: nginx:alpine
                      ports:
                        - containerPort: 80
            ---
            apiVersion: networking.k8s.io/v1
            kind: Ingress
            metadata:
              name: nginx-ingress
            spec:
              rules:
                - host: example.local
                  http:
                    paths:
                      - path: /
                        pathType: Prefix
                        backend:
                          service:
                            name: nginx-service
                            port:
                              number: 80
            """,
            """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: nginx-deployment
              annotations:
                org.openrewrite.yaml/merge: "true"
            spec:
              replicas: 1
              selector:
                matchLabels:
                  app: nginx
              template:
                metadata:
                  labels:
                    app: nginx
                spec:
                  containers:
                    - name: nginx
                      image: nginx:alpine
                      ports:
                        - containerPort: 80
            ---
            apiVersion: networking.k8s.io/v1
            kind: Ingress
            metadata:
              name: nginx-ingress
            spec:
              rules:
                - host: example.local
                  http:
                    paths:
                      - path: /
                        pathType: Prefix
                        backend:
                          service:
                            name: nginx-service
                            port:
                              number: 80
            """
          )
        );
    }
}
