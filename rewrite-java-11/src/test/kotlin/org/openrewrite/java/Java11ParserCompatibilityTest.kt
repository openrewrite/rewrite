package org.openrewrite.java

class Java11ParserCompatibilityTest: JavaParserCompatibilityKit() {
    override fun javaParser(): Java11Parser = Java11Parser.builder().build()
}
