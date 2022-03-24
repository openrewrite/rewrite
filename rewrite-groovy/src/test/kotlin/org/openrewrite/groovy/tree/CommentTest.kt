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
package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CommentTest : GroovyTreeTest {

    @Disabled
    @Test
    fun blockComment() = assertParsePrintAndProcess(
        """
            if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
                logger.lifecycle '''
            ************************************* WARNING ***********************************************
            ****  You're running the build with an older JDK. NEVER try to release with an old JDK!  ****
            ****  You must use a JDK 16+ in order to compile all features of the language.           ****
            *********************************************************************************************
            '''
            }
        """.trimIndent()
    )
}
