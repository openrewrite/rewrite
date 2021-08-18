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
package org.openrewrite.yaml.format

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.yaml.YamlRecipeTest
import org.openrewrite.yaml.style.YamlDefaultStyles

class IndentsTest : YamlRecipeTest {
    override val recipe: Recipe
        get() = toRecipe {
            IndentsVisitor(
                YamlDefaultStyles.indents(),
                null
            )
        }

    @Test
    fun indentSequence() = assertChanged(
        before = """
            root:
                - a: 0
                  b: 0
        """,
        after = """
            root:
              - a: 0
                b: 0
        """
    )

    @Test
    fun indents() = assertChanged(
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                    bucketPolicyOnly: true
                    lifecycleRule:
                            - action:
                                type: Delete
                              condition:
                                age: 7
        """,
        after = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
              bucketPolicyOnly: true
              lifecycleRule:
                - action:
                    type: Delete
                  condition:
                    age: 7
        """
    )
}
