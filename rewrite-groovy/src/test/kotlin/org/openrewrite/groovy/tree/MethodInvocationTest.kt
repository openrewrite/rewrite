package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MethodInvocationTest : GroovyTreeTest {
    @Disabled
    @Test
    fun gradle() = assertParsePrintAndProcess("""
        plugins {
            id 'java-library'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            implementation 'org.hibernate:hibernate-core:3.6.7.Final'
            api 'com.google.guava:guava:23.0'
            testImplementation 'junit:junit:4.+'
        }
    """.trimIndent())
}
