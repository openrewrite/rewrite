package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.CompilationUnit

interface CommentTest : JavaTreeTest {

    @Test
    fun backToBackMultilineComments(jp: JavaParser)  = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            class Test {
                /*
                    Comment 1
                *//*
                    Comment 2
                */
            }
        """
    )
}
