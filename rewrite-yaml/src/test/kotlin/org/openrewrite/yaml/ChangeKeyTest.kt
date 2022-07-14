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
package org.openrewrite.yaml

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.Issue
import java.nio.file.Path

class ChangeKeyTest : YamlRecipeTest {
    @Issue("https://github.com/openrewrite/rewrite/issues/434")
    @Test
    fun simpleChangeRootKey() = assertChanged(
        recipe = ChangeKey(
            "$.description",
            "newDescription",
            null
        ),
        before = """
            id: something
            description: desc
            other: whatever
        """,
        after = """
            id: something
            newDescription: desc
            other: whatever
        """
    )

    @Test
    fun changeNestedKey() = assertChanged(
        recipe = ChangeKey(
            "$.metadata.name",
            "name2",
            null
        ),
        before = """
            apiVersion: v1
            metadata:
              name: monitoring-tools
              namespace: monitoring-tools
        """,
        after = """
            apiVersion: v1
            metadata:
              name2: monitoring-tools
              namespace: monitoring-tools
        """
    )

    @Test
    fun changeRelativeKey() = assertChanged(
        recipe = ChangeKey(
            ".name",
            "name2",
            null
        ),
        before = """
            apiVersion: v1
            metadata:
              name: monitoring-tools
              namespace: monitoring-tools
        """,
        after = """
            apiVersion: v1
            metadata:
              name2: monitoring-tools
              namespace: monitoring-tools
        """
    )

    @Test
    fun changeSequenceKeyByWildcard() = assertChanged(
        recipe = ChangeKey(
            "$.subjects[*].kind",
            "kind2",
            null
        ),
        before = """
            subjects:
              - kind: ServiceAccount
                name: monitoring-tools
        """,
        after = """
            subjects:
              - kind2: ServiceAccount
                name: monitoring-tools
        """
    )

    @Test
    fun changeSequenceKeyByExactMatch() = assertChanged(
        recipe = ChangeKey(
            "$.subjects[?(@.kind == 'ServiceAccount')].kind",
            "kind2",
            null
        ),
        before = """
            subjects:
              - kind: User
                name: some-user
              - kind: ServiceAccount
                name: monitoring-tools
        """,
        after = """
            subjects:
              - kind: User
                name: some-user
              - kind2: ServiceAccount
                name: monitoring-tools
        """
    )

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("description: desc")
        }.toFile()
        val nonMatchingFile = tempDir.resolve("b.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("description: desc")
        }.toFile()
        val recipe = ChangeKey("$.description", "newDescription", "**/a.yml")
        assertChanged(recipe = recipe, before = matchingFile, after = "newDescription: desc")
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }


    @Test
    fun relocatesPropertyWithVariableInfix() = assertChanged(
        recipe = ChangeKey(
            "\$.spring.security.saml2.relyingparty.registration.*[?(@.identityprovider)]",
            "assertingparty",
            null
        ),
        before = """
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
        after = """
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
}
