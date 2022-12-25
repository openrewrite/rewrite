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
package org.openrewrite.java.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.style.StyleHelper;

import static org.assertj.core.api.Assertions.assertThat;

class StyleHelperTest {

    @Test
    void mergeTabsAndIndentsStyles() {
        var merged = StyleHelper.merge(IntelliJ.tabsAndIndents(), 
                new TabsAndIndentsStyle(true, 1, 1, 2, true, new TabsAndIndentsStyle.MethodDeclarationParameters(true)));
        assertThat(merged.getUseTabCharacter()).isTrue();
        assertThat(merged.getTabSize()).isEqualTo(1);
        assertThat(merged.getIndentSize()).isEqualTo(1);
        assertThat(merged.getContinuationIndent()).isEqualTo(2);
        assertThat(merged.getMethodDeclarationParameters().getAlignWhenMultiple()).isTrue();
        assertThat(merged.getIndentsRelativeToExpressionStart()).isTrue();
    }

    @Test
    void mergeSpacesStyles() {
        var merged = StyleHelper.merge(
            IntelliJ.spaces(),
            new SpacesStyle(
                new SpacesStyle.BeforeParentheses(true, true, false, false, false, false, false, false, false, true),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )
        );
        assertThat(merged.getBeforeParentheses().getMethodDeclaration()).isTrue();
        assertThat(merged.getBeforeParentheses().getMethodCall()).isTrue();
        assertThat(merged.getBeforeParentheses().getIfParentheses()).isFalse();
        assertThat(merged.getBeforeParentheses().getForParentheses()).isFalse();
        assertThat(merged.getBeforeParentheses().getWhileParentheses()).isFalse();
        assertThat(merged.getBeforeParentheses().getSwitchParentheses()).isFalse();
        assertThat(merged.getBeforeParentheses().getTryParentheses()).isFalse();
        assertThat(merged.getBeforeParentheses().getCatchParentheses()).isFalse();
        assertThat(merged.getBeforeParentheses().getSynchronizedParentheses()).isFalse();
        assertThat(merged.getBeforeParentheses().getAnnotationParameters()).isTrue();
        assertThat(merged.getAroundOperators()).isEqualTo(IntelliJ.spaces().getAroundOperators());
    }
}
