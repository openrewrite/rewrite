package org.openrewrite.java.internal

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.java.JavaParser

interface JavaPrinter2Test {

    @Test
    fun print(jp: JavaParser) {
        val cu = jp.parse("""
            package foo;
            
            public class Test {
                public String fun1() {
                    @SuppressWarnings({ "ALL" })
                    String s = "Hello World";
                    return s;
                }
            }
        """)[0]
        val printed = JavaPrinter2<ExecutionContext>().print(cu, ExecutionContext.builder().build())
        println(printed)
    }

}