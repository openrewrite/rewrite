package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.RefactorVisitorTest

interface UseGettersAndSettersTest : RefactorVisitorTest {

    @Test
    fun useGettersAndSetters(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu -> UseGettersAndSetters().apply { setClassType(cu.classes[0].type)} },
            before =
                """
                    public class A {
                        public String foo;
                    }
                """
            ,
            after =
                """
                    public class A {
                        private String foo;

                        public String getFoo() {
                            return foo;
                        }

                        public void setFoo(String value) {
                            foo = value;
                        }
                    }
                """
    )
}