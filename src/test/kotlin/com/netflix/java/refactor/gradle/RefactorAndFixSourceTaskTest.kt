package com.netflix.java.refactor.gradle

import com.netflix.java.refactor.TestKitTest
import com.netflix.java.refactor.compiler.JavaCompilerHelper
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class RefactorAndFixSourceTaskTest: TestKitTest() {
    @Ignore("This test pollutes the classloader, causing refactoring op test to fail")
    @Test
    fun refactorSource() {
        val repo = temp.newFolder("repo")

        createJavaSourceFile("""
            |class B {
            |   @Deprecated public void foo(int i) {}
            |   public void bar(int i) {}
            |}
        """.trimMargin())

        val a = createJavaSourceFile("""
            |class A {
            |   public void test() {
            |       new B().foo(0);
            |   }
            |}
        """.trimMargin())
        
        // publish a jar with a rule
        publishDependency(repo, DefaultModuleVersionIdentifier("netflix", "rule-source", "1.0"), """
            |package netflix.rule;
            |import com.netflix.java.refactor.*;
            |public class MyRules {
            |   @Refactor(value = "foo-to-bar", description = "replace foo() with bar()")
            |   public static RefactorRule fooToBar() {
            |       return new RefactorRule()
            |           .changeMethod("B foo(int)")
            |               .refactorName("bar")
            |                   .done();
            |   }
            |}
        """.trimMargin())
        
        buildFile.writeText("""
            plugins {
                id 'java'
                id 'nebula.source-refactor'
            }
            
            repositories {
                maven { url { "${repo.toURI().toURL()}" } }
            }
            
            dependencies {
                compile "netflix:rule-source:1.0"
            }
        """)
        
        val result = runTasksSuccessfully("fixSourceLint")
        
        println(result.output)
        
        assertEquals("""
            |class A {
            |   public void test() {
            |       new B().bar(0);
            |   }
            |}
        """.trimMargin(), a.readText())
    }
    
    fun publishDependency(repo: File, mvid: ModuleVersionIdentifier, vararg sources: String) {
        val artifactFolder = File(repo, "${mvid.group.replace("\\.", "/")}/${mvid.name.replace("\\.", "/")}/${mvid.version}")
        val java = JavaCompilerHelper()
        java.jar(File(artifactFolder, "${mvid.name}-${mvid.version}.jar"), sources.toList())
        
        File(artifactFolder, "${mvid.name}-${mvid.version}.pom").writeText("""
            |<?xml version="1.0" encoding="UTF-8"?>
            |<project xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            |  <modelVersion>4.0.0</modelVersion>
            |  <groupId>${mvid.group}</groupId>
            |  <artifactId>${mvid.name}</artifactId>
            |  <version>${mvid.version}</version>
            |</project>
        """.trimMargin())
    }
}
