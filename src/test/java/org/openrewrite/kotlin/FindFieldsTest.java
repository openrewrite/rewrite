package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.search.FindFields;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class FindFieldsTest implements RewriteTest {

    @Test
    void jvmStaticField() {
        rewriteRun(
          spec -> spec.recipe(new FindFields("java.lang.Integer", "MAX_VALUE")),
          kotlin(
            """
              import java.lang.Integer
              import java.lang.Integer.MAX_VALUE
              
              val i1 = java.lang.Integer.MAX_VALUE
              val i2 = Integer.MAX_VALUE
              val i3 = MAX_VALUE
              val i4 = `MAX_VALUE`
              """,
            """
              import java.lang.Integer
              import java.lang.Integer.MAX_VALUE
              
              val i1 = /*~~>*/java.lang.Integer.MAX_VALUE
              val i2 = /*~~>*/Integer.MAX_VALUE
              val i3 = /*~~>*/MAX_VALUE
              val i4 = /*~~>*/`MAX_VALUE`
              """
          )
        );
    }
}
