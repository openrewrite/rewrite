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
package org.openrewrite.yaml.search

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.Issue
import org.openrewrite.yaml.YamlRecipeTest

class FindPropertyTest : YamlRecipeTest {

    @Test
    fun findProperty() = assertChanged(
        recipe = FindProperty("management.metrics.binders.files.enabled", null),
        before = "management.metrics.binders.files.enabled: true",
        after = "management.metrics.binders.files.enabled: ~~>true"
    )

    @Test
    fun findGlobProperty() = assertChanged(
        recipe = FindProperty("management.metrics.binders.*.enabled", null),
        before = "management.metrics.binders.files.enabled: true",
        after = "management.metrics.binders.files.enabled: ~~>true"
    )

    @ParameterizedTest
    @ValueSource(
        strings = [
            "acme.my-project.person.first-name",
            "acme.myProject.person.firstName",
            "acme.my_project.person.first_name",
        ]
    )
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    fun relaxedBinding(propertyKey: String) = assertChanged(
        recipe = FindProperty(propertyKey, true),
        before = """
            acme.my-project.person.first-name: example
            acme.myProject.person.firstName: example
            acme.my_project.person.first_name: example
        """,
        after = """
            acme.my-project.person.first-name: ~~>example
            acme.myProject.person.firstName: ~~>example
            acme.my_project.person.first_name: ~~>example
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    fun exactMatch() = assertChanged(
        recipe = FindProperty("acme.my-project.person.first-name", false),
        before = """
            acme.my-project.person.first-name: example
            acme.myProject.person.firstName: example
            acme.my_project.person.first_name: example
        """,
        after = """
            acme.my-project.person.first-name: ~~>example
            acme.myProject.person.firstName: example
            acme.my_project.person.first_name: example
        """
    )

}
