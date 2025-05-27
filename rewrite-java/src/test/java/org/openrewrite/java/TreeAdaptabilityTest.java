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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class TreeAdaptabilityTest implements RewriteTest {

    @Test
    void adaptParameterizedPlainTextTreeVisitor() {
        //noinspection rawtypes
        assertThat(new PlainTextVisitor() {}.isAdaptableTo(JavaVisitor.class)).isFalse();
        assertThat(new TreeVisitor<PlainText,Integer > () {}.isAdaptableTo(JavaVisitor.class)).isFalse();
        assertThat(new TreeVisitor<Tree, Integer>() {}.isAdaptableTo(PlainTextVisitor.class)).isTrue();
    }
}
