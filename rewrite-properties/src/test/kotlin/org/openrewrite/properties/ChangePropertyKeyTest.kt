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

class ChangePropertyKeyTest : PropertiesRecipeTest {

    override val recipe = ChangePropertyKey(
        "management.metrics.binders.files.enabled",
        "management.metrics.enable.process.files"
    )

    @Test
    fun changeKey() = assertChanged(
        parser = PropertiesParser(),
        before = "management.metrics.binders.files.enabled=true",
        after = "management.metrics.enable.process.files=true"
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangePropertyKey(null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("newPropertyKey")
        assertThat(valid.failures()[1].property).isEqualTo("oldPropertyKey")

        recipe = ChangePropertyKey(null, "management.metrics.enable.process.files")
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("oldPropertyKey")

        recipe = ChangePropertyKey("management.metrics.binders.files.enabled", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newPropertyKey")

        recipe =
            ChangePropertyKey("management.metrics.binders.files.enabled", "management.metrics.enable.process.files")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
