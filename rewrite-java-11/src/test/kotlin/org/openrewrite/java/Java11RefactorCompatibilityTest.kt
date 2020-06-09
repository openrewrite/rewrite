package org.openrewrite.java

class Java11RefactorCompatibilityTest: JavaRefactorCompatibilityKit() {
    override fun javaParser(): Java11Parser = Java11Parser.builder().build()
}
