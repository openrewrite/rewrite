package org.openrewrite.xml

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class XmlParserTest: XmlParser() {
    @Test
    fun parseXmlDocument() {
        val xSource = """
            <?xml
                version="1.0" encoding="UTF-8"?>
            <?xml-stylesheet href="mystyle.css" type="text/css"?>
            <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN"
                "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
            <beans >
                <bean id="myBean"/>
            </beans>
        """.trimIndent()

        val x = parse(xSource)

        assertEquals(xSource, x.printTrimmed())
    }

    @Test
    fun parsePomDocument() {
        val xSource = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- comment -->
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <parent>
                <groupId>com.google.guava</groupId>
                <artifactId>guava-parent</artifactId>
                <version>28.2-jre</version>
              </parent>
              <artifactId>guava</artifactId>
              <packaging>bundle</packaging>
              <name>Guava: Google Core Libraries for Java</name>
            </project>
        """.trimIndent()

        val x = parse(xSource)

        assertEquals(xSource, x.printTrimmed())
    }
}
