package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class FindMethodsTest implements RewriteTest {

    @Test
    void jvmStaticMethod() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("java.lang.Integer decode(..)", false)),
          kotlin(
            """
              import java.lang.Integer
              import java.lang.Integer.decode
              
              val i1 = java.lang.Integer.decode("1")
              val i2 = Integer.decode("1")
              val i3 = decode("1")
              val i4 = listOf("1").map {Integer::decode}
              val i5 = listOf("1").map {::decode}
              """,
            """
              import java.lang.Integer
              import java.lang.Integer.decode
              
              val i1 = /*~~>*/java.lang.Integer.decode("1")
              val i2 = /*~~>*/Integer.decode("1")
              val i3 = /*~~>*/decode("1")
              val i4 = listOf("1").map {Integer::/*~~>*/decode}
              val i5 = listOf("1").map {::/*~~>*/decode}
              """
          )
        );
    }
}
