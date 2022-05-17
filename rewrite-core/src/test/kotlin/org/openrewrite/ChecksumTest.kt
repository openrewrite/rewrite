/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.net.URL

class ChecksumTest {

    @Disabled
    @Test
    fun checksum() {
        val checksum = Checksum("SHA-256", URL("https://services.gradle.org/distributions/gradle-7.4.2-wrapper.jar.sha256")
            .readBytes())
        assertThat(checksum.hexValue).isEqualTo("575098db54a998ff1c6770b352c3b16766c09848bee7555dab09afc34e8cf590")
    }
}
