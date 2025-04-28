/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class ChangeKeyTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeNestedKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.metadata.name", "name2")),
          yaml(
            """
                  apiVersion: v1
                  metadata:
                    name: monitoring-tools
                    namespace: monitoring-tools
              """,
            """
                  apiVersion: v1
                  metadata:
                    name2: monitoring-tools
                    namespace: monitoring-tools
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/434")
    @Test
    void simpleChangeRootKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.description", "newDescription")),
          yaml(
            """
              id: something
              description: desc
              other: whatever
              """,
            """
              id: something
              newDescription: desc
              other: whatever
              """
          )
        );
    }

    @Test
    void changeRelativeKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(".name", "name2")),
          yaml(
            """
                  apiVersion: v1
                  metadata:
                    name: monitoring-tools
                    namespace: monitoring-tools
              """,
            """
                  apiVersion: v1
                  metadata:
                    name2: monitoring-tools
                    namespace: monitoring-tools
              """
          )
        );
    }

    @Test
    void changeSequenceKeyByWildcard() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.subjects[*].kind", "kind2")),
          yaml(
            """
                  subjects:
                    - kind: ServiceAccount
                      name: monitoring-tools
              """,
            """
                  subjects:
                    - kind2: ServiceAccount
                      name: monitoring-tools
              """
          )
        );
    }

    @Test
    void changeSequenceKeyByExactMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.subjects[?(@.kind == 'ServiceAccount')].kind", "kind2")),
          yaml(
            """
                  subjects:
                    - kind: User
                      name: some-user
                    - kind: ServiceAccount
                      name: monitoring-tools
              """,
            """
                  subjects:
                    - kind: User
                      name: some-user
                    - kind2: ServiceAccount
                      name: monitoring-tools
              """
          )
        );
    }

    @Test
    void relocatesPropertyWithVariableInfix() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "\\$.spring.security.saml2.relyingparty.registration.*[?(@.identityprovider)]",
            "assertingparty"
          )),
          yaml(
            """
                  spring:
                    security:
                      saml2:
                        relyingparty:
                          registration:
                            idpone:
                              identityprovider:
                                entity-id: https://idpone.com
                                sso-url: https://idpone.com
                                verification:
                                  credentials:
                                    - certificate-location: "classpath:saml/idpone.crt"
              """,
            """
                  spring:
                    security:
                      saml2:
                        relyingparty:
                          registration:
                            idpone:
                              assertingparty:
                                entity-id: https://idpone.com
                                sso-url: https://idpone.com
                                verification:
                                  credentials:
                                    - certificate-location: "classpath:saml/idpone.crt"
              """
          )
        );
    }
}
