package org.openrewrite.java.Utils;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.utils.ExpressionUtils;
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
