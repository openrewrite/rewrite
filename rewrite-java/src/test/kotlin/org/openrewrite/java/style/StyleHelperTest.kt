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
package org.openrewrite.java.style

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.style.StyleHelper

class StyleHelperTest {

    @Test
    fun mergeTabsAndIndentsStyles() {
        val merged = StyleHelper.merge(IntelliJ.tabsAndIndents(), TabsAndIndentsStyle(true, 1, 1, 2, true, TabsAndIndentsStyle.MethodDeclarationParameters(true)))
        assertThat(merged.useTabCharacter).isTrue
        assertThat(merged.tabSize).isEqualTo(1)
        assertThat(merged.indentSize).isEqualTo(1)
        assertThat(merged.continuationIndent).isEqualTo(2)
        assertThat(merged.methodDeclarationParameters.alignWhenMultiple).isTrue
        assertThat(merged.indentsRelativeToExpressionStart).isTrue
    }

    @Test
    fun mergeSpacesStyles() {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val merged = StyleHelper.merge(
            IntelliJ.spaces(),
            SpacesStyle(
                SpacesStyle.BeforeParentheses(true, true, false, false, false, false, false, false, false, true),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )
        )
        assertThat(merged.beforeParentheses.methodDeclaration).isTrue
        assertThat(merged.beforeParentheses.methodCall).isTrue
        assertThat(merged.beforeParentheses.ifParentheses).isFalse
        assertThat(merged.beforeParentheses.forParentheses).isFalse
        assertThat(merged.beforeParentheses.whileParentheses).isFalse
        assertThat(merged.beforeParentheses.switchParentheses).isFalse
        assertThat(merged.beforeParentheses.tryParentheses).isFalse
        assertThat(merged.beforeParentheses.catchParentheses).isFalse
        assertThat(merged.beforeParentheses.synchronizedParentheses).isFalse
        assertThat(merged.beforeParentheses.annotationParameters).isTrue
        assertThat(merged.aroundOperators).isEqualTo(IntelliJ.spaces().aroundOperators)
    }
}
