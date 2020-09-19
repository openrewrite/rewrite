package org.openrewrite.java

abstract class JavaEcjTest {
    fun javaParser(): JavaEcjParser = JavaEcjParser.builder().build()
}
