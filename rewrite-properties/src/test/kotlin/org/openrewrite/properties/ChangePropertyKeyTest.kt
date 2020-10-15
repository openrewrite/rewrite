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

import org.junit.jupiter.api.Test
import org.openrewrite.whenParsedBy

class ChangePropertyKeyTest {
    @Test
    fun changeKey() {
        "management.metrics.binders.files.enabled=true"
                .whenParsedBy(PropertiesParser())
                .whenVisitedBy(ChangePropertyKey().apply {
                    setProperty("management.metrics.binders.files.enabled")
                    setToProperty("management.metrics.enable.process.files")
                })
                .isRefactoredTo("management.metrics.enable.process.files=true")
    }
}
