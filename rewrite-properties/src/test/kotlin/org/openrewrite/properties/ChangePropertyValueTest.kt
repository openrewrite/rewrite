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
package org.openrewrite.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue

class ChangePropertyValueTest : PropertiesRecipeTest {

    override val recipe = ChangePropertyValue(
        "management.metrics.binders.files.enabled",
        "false",
        null
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/575")
    @Test
    fun preserveComment() = assertChanged(
        parser = PropertiesParser(),
        before = """
            management.metrics.binders.files.enabled=true
            # comment
        """,
        after = """
            management.metrics.binders.files.enabled=false
            # comment
        """
    )

    @Test
    fun changeValue() = assertChanged(
        before = "management.metrics.binders.files.enabled=true",
        after = "management.metrics.binders.files.enabled=false"
    )

    @Test
    fun conditionallyChangeValue() = assertChanged(
        parser = PropertiesParser(),
        recipe = ChangePropertyValue("quarkus.quartz.store-type","jdbc-cmt","db"),
        before = "quarkus.quartz.store-type=db",
        after = "quarkus.quartz.store-type=jdbc-cmt"
    )

    @Test
    fun conditionallyChangeValueNoChange() = assertUnchanged(
        parser = PropertiesParser(),
        recipe = ChangePropertyValue("quarkus.quartz.store-type","jdbc-cmt","cache"),
        before = "quarkus.quartz.store-type=db"
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangePropertyValue(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("newValue")
        assertThat(valid.failures()[1].property).isEqualTo("propertyKey")

        recipe = ChangePropertyValue(null, "false", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("propertyKey")

        recipe = ChangePropertyValue("management.metrics.binders.files.enabled", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newValue")

        recipe = ChangePropertyValue("management.metrics.binders.files.enabled", "false", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()

        recipe = ChangePropertyValue("management.metrics.binders.files.enabled", "false", "true")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
