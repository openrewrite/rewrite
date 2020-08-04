package org.openrewrite.java

abstract class Java11Test {
    fun javaParser(): Java11Parser = Java11Parser.builder().build()
}
