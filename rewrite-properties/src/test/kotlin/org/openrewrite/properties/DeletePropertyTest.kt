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
package org.openrewrite.properties

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.Issue
import org.openrewrite.Recipe

@Suppress("UnusedProperty")
class DeletePropertyTest : PropertiesRecipeTest {
    override val recipe: Recipe
        get() = DeleteProperty("delete.me", null, null)

    @Test
    fun basic() = assertChanged(
        before = """
            preserve = foo
            delete.me = baz
            delete.me.not = bar
        """,
        after = """
            preserve = foo
            delete.me.not = bar
        """
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
        recipe = DeleteProperty(propertyKey, true, null),
        before = """
            spring.datasource.schema=classpath*:db/database/schema.sql
            acme.my-project.person.first-name=example
            acme.myProject.person.firstName=example
            acme.my_project.person.first_name=example
        """,
        after = "spring.datasource.schema=classpath*:db/database/schema.sql"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    @Test
    fun exactMatch() = assertChanged(
        recipe = DeleteProperty("acme.my-project.person.first-name", false, null),
        before = """
            spring.datasource.schema=classpath*:db/database/schema.sql
            acme.my-project.person.first-name=example
            acme.myProject.person.firstName=example
            acme.my_project.person.first_name=example
        """,
        after = """
            spring.datasource.schema=classpath*:db/database/schema.sql
            acme.myProject.person.firstName=example
            acme.my_project.person.first_name=example
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    @Test
    fun updatePrefix() = assertChanged(
        recipe = DeleteProperty("acme.my-project.person.first-name", false, null),
        before = """
            acme.my-project.person.first-name=example
            acme.myProject.person.firstName=example
            acme.my_project.person.first_name=example
        """,
        after = """
            acme.myProject.person.firstName=example
            acme.my_project.person.first_name=example
        """
    )
}
