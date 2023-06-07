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
package org.openrewrite.maven.utilities;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static org.assertj.core.api.Assertions.assertThat;

class PrintMavenAsCycloneDxBomTest implements RewriteTest {

    @Test
    void cycloneDxBom() {
        Xml.Document pom = MavenParser.builder()
          .build()
          .parse(
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  \s
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                 \s
                  <dependencies>
                    <dependency>
                        <groupId>org.yaml</groupId>
                        <artifactId>snakeyaml</artifactId>
                        <version>1.27</version>
                    </dependency>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter</artifactId>
                      <version>5.7.0</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
                """
          ).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not parse as XML"));

        String bom = PrintMavenAsCycloneDxBom.print(pom)
          .replaceAll("<timestamp>.*</timestamp>", "<timestamp>TODAY</timestamp>");

        assertThat(bom).isEqualTo(String.format(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <bom xmlns="http://cyclonedx.org/schema/bom/1.2" serialNumber="urn:uuid:%s" version="1">
                <metadata>
                    <timestamp>TODAY</timestamp>
                    <tools>
                        <tool>
                            <vendor>OpenRewrite</vendor>
                            <name>OpenRewrite CycloneDX</name>
                            <version>7.18.0</version>
                        </tool>
                    </tools>
                    <component bom-ref="pkg:maven/com.mycompany.app/my-app@1?type=jar" type="library">
                        <group>com.mycompany.app</group>
                        <name>my-app</name>
                        <version>1</version>
                        <scope>required</scope>
                        <purl>pkg:maven/com.mycompany.app/my-app@1?type=jar</purl>
                    </component>
                </metadata>
                <components>
                    <component bom-ref="pkg:maven/org.yaml/snakeyaml@1.27?type=jar" type="library">
                        <group>org.yaml</group>
                        <name>snakeyaml</name>
                        <version>1.27</version>
                        <scope>required</scope>
                        <licenses>
                            <license>
                                <id>Apache-2.0</id>
                                <name>Apache License, Version 2.0</name>
                            </license>
                        </licenses>
                        <purl>pkg:maven/org.yaml/snakeyaml@1.27?type=jar</purl>
                    </component>
                </components>
                <dependencies>
                    <dependency ref="pkg:maven/org.yaml/snakeyaml@1.27?type=jar">
                    </dependency>
                </dependencies>
            </bom>
            """, pom.getId().toString())
        );

    }
}
