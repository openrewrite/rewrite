/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml

import org.junit.jupiter.api.Test
import org.openrewrite.RefactorVisitorTestForParser
import org.openrewrite.xml.tree.Xml

class XmlParserTest: XmlParser(), RefactorVisitorTestForParser<Xml.Document> {
    override val parser: XmlParser = XmlParser()

    @Test
    fun parseXmlDocument() = assertUnchanged(
            // TODO add this back after <?xml at some point... <?xml-stylesheet href="mystyle.css" type="text/css"?>
            before = """
                <?xml
                    version="1.0" encoding="UTF-8"?>
                <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN"
                    "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
                <beans >
                    <bean id="myBean"/>
                </beans>
            """
    )

    @Test
    fun parsePomDocument() = assertUnchanged(
            before = """
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
        """
    )
}
