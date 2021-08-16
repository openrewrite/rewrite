/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.json

import org.junit.jupiter.api.Test

class UpsertMemberTest : JsonRecipeTest {

    @Test
    fun existingKey() = assertChanged(
        recipe = UpsertMember(
            "$.metadata",
            "name",
            "\"foo\"",
            false,
            null
        ),
        before = """
            {
              "name": "invariant",
              "metadata": {
                "name": "monitoring-tools",
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
              }
            }
        """,
        after = """
            {
              "name": "invariant",
              "metadata": {
                "name": "foo",
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
              }
            }
        """
    )

    @Test
    fun existingKeyUseTheirs() = assertUnchanged(
        recipe = UpsertMember(
            "$.metadata",
            "name",
            "\"foo\"",
            true,
            null
        ),
        before = """
            {
              "name": "invariant",
              "metadata": {
                "name": "monitoring-tools",
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
              }
            }
        """
    )

    @Test
    fun nonExistingKey() = assertChanged(
        recipe = UpsertMember(
            "$.metadata",
            "name",
            "\"foo\"",
            false,
            null
        ),
        before = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
              }
            }
        """,
        after = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
                "name": "foo",
              }
            }
        """
    )

    @Test
    fun upsertBoolean() = assertChanged(
        recipe = UpsertMember(
            "$.metadata",
            "hasAuthor",
            "false",
            false,
            null
        ),
        before = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
              }
            }
        """,
        after = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
                "hasAuthor": false,
              }
            }
        """
    )

    @Test
    fun upsertNumber() = assertChanged(
        recipe = UpsertMember(
            "$.metadata",
            "years",
            "3",
            false,
            null
        ),
        before = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
              }
            }
        """,
        after = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
                "years": 3,
              }
            }
        """
    )

    @Test
    fun upsertArray() = assertChanged(
        recipe = UpsertMember(
            "$.metadata",
            "tags",
            "[\"fiction\", \"adventure\"]",
            false,
            null
        ),
        before = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
              }
            }
        """,
        after = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
                "tags": ["fiction", "adventure"],
              }
            }
        """
    )

    @Test
    fun upsertObject() = assertChanged(
        recipe = UpsertMember(
            "$.metadata",
            "locationA",
            "{\n" +
                    "        \"count\": 5\n" +
                    "    }",
            false,
            null
        ),
        before = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
              }
            }
        """,
        after = """
            {
              "name": "invariant",
              "metadata": {
                "namespace": "monitoring-tools",
                "details": "something",
                "a": [],
                "b": {},
                "locationA": {
                    "count": 5
                },
              }
            }
        """
    )
}
