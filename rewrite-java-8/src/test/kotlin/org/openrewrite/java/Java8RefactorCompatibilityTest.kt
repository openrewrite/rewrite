package org.openrewrite.java

class Java8RefactorCompatibilityTest: JavaRefactorCompatibilityKit() {
    override fun javaParser(): Java8Parser = Java8Parser.builder().build()
}
