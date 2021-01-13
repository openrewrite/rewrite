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
package org.openrewrite.maven.utilities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.maven.MavenParser

class PrintMavenAsCycloneDxBomTest {
    @Test
    fun cycloneDxBom() {
        val pom = MavenParser.builder()
                .build()
                .parse("""
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                       
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-actuator-autoconfigure</artifactId>
                          <version>2.3.4.RELEASE</version>
                        </dependency>
                        <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                          <version>5.7.0</version>
                          <scope>test</scope>
                        </dependency>
                      </dependencies>
                    </project>
                """)

        assertThat(PrintMavenAsCycloneDxBom.print(pom[0])).isEqualTo("""
            <?xml version="1.0" encoding="UTF-8"?>
            <bom xmlns="http://cyclonedx.org/schema/bom/1.2" serialNumber="urn:uuid:${pom[0].id}" version="1">
              <components>
                <component type="library" bom-ref="my-app">
                  <group>com.mycompany.app</group>
                  <name>my-app</name>
                  <version>1</version>
                  <purl>pkg:maven/com.mycompany.app/my-app@1</purl>
                  <dependencies>
                    <dependency ref="my-app">
                      <dependency ref="pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.11.2"/>
                      <dependency ref="pkg:maven/com.fasterxml.jackson.core/jackson-core@2.11.2"/>
                      <dependency ref="pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.11.2"/>
                      <dependency ref="pkg:maven/com.fasterxml.jackson.datatype/jackson-datatype-jsr310@2.11.2"/>
                      <dependency ref="pkg:maven/org.springframework/spring-aop@5.2.9.RELEASE"/>
                      <dependency ref="pkg:maven/org.springframework/spring-beans@5.2.9.RELEASE"/>
                      <dependency ref="pkg:maven/org.springframework/spring-context@5.2.9.RELEASE"/>
                      <dependency ref="pkg:maven/org.springframework/spring-core@5.2.9.RELEASE"/>
                      <dependency ref="pkg:maven/org.springframework/spring-expression@5.2.9.RELEASE"/>
                      <dependency ref="pkg:maven/org.springframework/spring-jcl@5.2.9.RELEASE"/>
                      <dependency ref="pkg:maven/org.springframework.boot/spring-boot@2.3.4.RELEASE"/>
                      <dependency ref="pkg:maven/org.springframework.boot/spring-boot-actuator@2.3.4.RELEASE"/>
                      <dependency ref="pkg:maven/org.springframework.boot/spring-boot-actuator-autoconfigure@2.3.4.RELEASE"/>
                      <dependency ref="pkg:maven/org.springframework.boot/spring-boot-autoconfigure@2.3.4.RELEASE"/>
                    </dependency>
                  </dependencies>
                </component>
              </components>
            </bom>
        """.trimIndent())
    }
}
