package org.openrewrite.maven.utilities

import org.junit.jupiter.api.Test
import org.openrewrite.maven.MavenParser

class DependencyInspectorTest {

    @Test
    fun printDependencyTree() {
        val maven = MavenParser.builder()
            .build()
            .parse("""
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                
                    <groupId>org.openrewrite</groupId>
                    <artifactId>eclipse-talk</artifactId>
                    <version>1.0-SNAPSHOT</version>
                
                    <properties>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                        <kotlin.code.style>official</kotlin.code.style>
                        <kotlin.version>1.5.10</kotlin.version>
                        <kotlin.compiler.jvmTarget>11</kotlin.compiler.jvmTarget>
                        <maven-compiler-plugin.version>3.8.1</maven-compiler-plugin.version>
                        <maven.compiler.showWarnings>true</maven.compiler.showWarnings>
                        <java.version>11</java.version>
                        <rewrite.version>7.16.0-SNAPSHOT</rewrite.version>
                    </properties>
                
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite</groupId>
                            <artifactId>rewrite-java-11</artifactId>
                            <version>7.16.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.20</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-api</artifactId>
                            <version>5.6.0</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-engine</artifactId>
                            <version>5.6.0</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.openrewrite</groupId>
                            <artifactId>rewrite-test</artifactId>
                            <version>7.16.0</version>
                            <scope>test</scope>
                            <exclusions>
                                <exclusion>
                                    <groupId>org.openrewrite</groupId>
                                    <artifactId>rewrite-gradle</artifactId>
                                </exclusion>
                            </exclusions>
                        </dependency>
                        <dependency>
                            <groupId>org.assertj</groupId>
                            <artifactId>assertj-core</artifactId>
                            <version>3.19.0</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-test-junit5</artifactId>
                            <version>1.5.10</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib-jdk8</artifactId>
                            <version>1.5.10</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-reflect</artifactId>
                            <version>1.5.10</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """)
        val tree = DependencyInspector.printDependencyTree(maven[0].model);
        println(tree);
    }
}