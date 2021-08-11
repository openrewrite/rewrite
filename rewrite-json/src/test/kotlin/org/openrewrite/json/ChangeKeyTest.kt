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
package org.openrewrite.json

import org.junit.jupiter.api.Test

class ChangeKeyTest : JsonRecipeTest {

    @Test
    fun simpleChangeRootKey() = assertChanged(
        recipe = ChangeKey(
            "$.description",
            "\"newDescription\"",
            null
        ),
        before = """
            {
              "id": "something",
              "description": "desc",
              "other": "whatever"
            }
        """,
        after = """
            {
              "id": "something",
              "newDescription": "desc",
              "other": "whatever"
            }
        """
    )

    @Test
    fun changeNestedKey() = assertChanged(
        recipe = ChangeKey(
            "$.metadata.name",
            "\"name2\"",
            null
        ),
        before = """
            {
              "apiVersion": "v1",
              "metadata": {
                "name": "monitoring-tools",
                "namespace": "monitoring-tools"
              }
            }
        """,
        after = """
            {
              "apiVersion": "v1",
              "metadata": {
                "name2": "monitoring-tools",
                "namespace": "monitoring-tools"
              }
            }
        """
    )

    @Test
    fun changeArrayKey() = assertChanged(
        recipe = ChangeKey(
            "$.subjects.kind",
            "\"kind2\"",
            null
        ),
        before = """
            {
              "subjects": [
                {
                  "kind": "ServiceAccount",
                  "name": "monitoring-tools"
                }
              ]
            }
        """,
        after = """
            {
              "subjects": [
                {
                  "kind2": "ServiceAccount",
                  "name": "monitoring-tools"
                }
              ]
            }
        """
    )
}
