package org.openrewrite.java

class Java8ParserCompatibilityTest: JavaParserCompatibilityKit() {
    override fun javaParser(): Java8Parser = Java8Parser.builder().build()
}
