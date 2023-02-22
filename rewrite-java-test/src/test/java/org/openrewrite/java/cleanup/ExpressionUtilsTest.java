/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class ExpressionUtilsTest {

    @Test
    void concatBinary() {
        Expression result = ExpressionUtils.concatAdditionBinary(buildStringLiteral("A"), buildStringLiteral("B"));
        assertThat(result.toString()).isEqualTo("\"A\" + \"B\"");
    }

    @Test
    void additiveExpression() {
        Expression result = ExpressionUtils.additiveExpression(buildIntLiteral(1),
          buildIntLiteral(2),
          buildIntLiteral(3));
        assertThat(result.toString()).isEqualTo("1 + 2 + 3");
    }

    private static J.Literal buildStringLiteral(String s) {
        return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, s,
          "\"" + s + "\"", null, JavaType.Primitive.String);
    }

    private static J.Literal buildIntLiteral(int i) {
        return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, i,
          String.valueOf(i), null, JavaType.Primitive.Int);
    }
}