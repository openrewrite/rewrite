package org.openrewrite.java

class Java11SearchCompatibilityTest: JavaSearchCompatibilityKit() {
    override fun javaParser(): Java11Parser = Java11Parser.builder().build()
}
