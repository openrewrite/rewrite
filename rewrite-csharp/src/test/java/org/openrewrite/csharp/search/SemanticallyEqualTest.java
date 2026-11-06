/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.openrewrite.Tree.randomId;

class SemanticallyEqualTest {

    @Test
    void typeOfDoesNotThrow() {
        // given
        J.Binary left = typeOfEquals(JavaType.Primitive.String);
        J.Binary right = typeOfEquals(JavaType.Primitive.Int);

        // when / then
        assertThatCode(() -> SemanticallyEqual.areEqual(left, right)).doesNotThrowAnyException();
    }

    @Test
    void sizeOfDoesNotThrow() {
        // given
        Cs.SizeOf left = sizeOf(JavaType.Primitive.Int);
        Cs.SizeOf right = sizeOf(JavaType.Primitive.Long);

        // when / then
        assertThatCode(() -> SemanticallyEqual.areEqual(left, right)).doesNotThrowAnyException();
    }

    private static J.Binary typeOfEquals(JavaType.Primitive primitive) {
        Cs.TypeOf typeOf = new Cs.TypeOf(randomId(), Space.EMPTY, Markers.EMPTY, parens(primitive), null);
        J.Identifier sourceType = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, null, "sourceType", null, null);
        return new J.Binary(randomId(), Space.EMPTY, Markers.EMPTY, typeOf,
                JLeftPadded.build(J.Binary.Type.Equal), sourceType, JavaType.Primitive.Boolean);
    }

    private static Cs.SizeOf sizeOf(JavaType.Primitive primitive) {
        return new Cs.SizeOf(randomId(), Space.EMPTY, Markers.EMPTY, parens(primitive), JavaType.Primitive.Int);
    }

    private static J.ControlParentheses<TypeTree> parens(JavaType.Primitive primitive) {
        return new J.ControlParentheses<>(randomId(), Space.EMPTY, Markers.EMPTY,
                JRightPadded.build(new J.Primitive(randomId(), Space.EMPTY, Markers.EMPTY, primitive)));
    }
}
