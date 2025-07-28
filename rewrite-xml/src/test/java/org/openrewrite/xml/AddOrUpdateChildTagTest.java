/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class AddOrUpdateChildTagTest implements RewriteTest {
    @CsvSource({"true", "false"})
    @NullSource
    @ParameterizedTest
    void addsTagEverywhereWhenAbsent(Boolean replaceExisting) {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateChildTag(
                        "/project//plugin[groupId='org.apache.maven.plugins' and artifactId='maven-resources-plugin']" +
                        "//configuration",
                        "<skip>true</skip>",
                        replaceExisting)),
                xml(
                        """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>com.example</groupId>
                                    <artifactId>my-project</artifactId>
                                    <version>1.0</version>
                                
                                    <build>
                                        <pluginManagement>
                                            <plugins>
                                                <plugin>
                                                    <groupId>org.apache.maven.plugins</groupId>
                                                    <artifactId>maven-resources-plugin</artifactId>
                                                    <version>3.3.1</version>
                                                    <configuration>
                                                        <encoding>UTF-8</encoding>
                                                    </configuration>
                                                </plugin>
                                            </plugins>
                                        </pluginManagement>
                                        <plugins>
                                            <plugin>
                                                <groupId>org.apache.maven.plugins</groupId>
                                                <artifactId>maven-resources-plugin</artifactId>
                                                <version>3.3.1</version>
                                                <executions>
                                                    <execution>
                                                        <configuration>
                                                            <encoding>UTF-8</encoding>
                                                        </configuration>
                                                    </execution>
                                                </executions>
                                            </plugin>
                                        </plugins>
                                    </build>
                                    <profiles>
                                        <profile>
                                            <id>profile1</id>
                                            <build>
                                                <pluginManagement>
                                                    <plugins>
                                                        <plugin>
                                                            <groupId>org.apache.maven.plugins</groupId>
                                                            <artifactId>maven-resources-plugin</artifactId>
                                                            <version>3.3.1</version>
                                                            <executions>
                                                                <execution>
                                                                    <configuration>
                                                                        <encoding>UTF-8</encoding>
                                                                    </configuration>
                                                                </execution>
                                                            </executions>
                                                        </plugin>
                                                    </plugins>
                                                </pluginManagement>
                                                <plugins>
                                                    <plugin>
                                                        <groupId>org.apache.maven.plugins</groupId>
                                                        <artifactId>maven-resources-plugin</artifactId>
                                                        <version>3.3.1</version>
                                                        <configuration>
                                                            <encoding>UTF-8</encoding>
                                                        </configuration>
                                                    </plugin>
                                                </plugins>
                                            </build>
                                        </profile>
                                    </profiles>
                                </project>
                                """,
                        """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>com.example</groupId>
                                    <artifactId>my-project</artifactId>
                                    <version>1.0</version>
                                
                                    <build>
                                        <pluginManagement>
                                            <plugins>
                                                <plugin>
                                                    <groupId>org.apache.maven.plugins</groupId>
                                                    <artifactId>maven-resources-plugin</artifactId>
                                                    <version>3.3.1</version>
                                                    <configuration>
                                                        <encoding>UTF-8</encoding>
                                                        <skip>true</skip>
                                                    </configuration>
                                                </plugin>
                                            </plugins>
                                        </pluginManagement>
                                        <plugins>
                                            <plugin>
                                                <groupId>org.apache.maven.plugins</groupId>
                                                <artifactId>maven-resources-plugin</artifactId>
                                                <version>3.3.1</version>
                                                <executions>
                                                    <execution>
                                                        <configuration>
                                                            <encoding>UTF-8</encoding>
                                                            <skip>true</skip>
                                                        </configuration>
                                                    </execution>
                                                </executions>
                                            </plugin>
                                        </plugins>
                                    </build>
                                    <profiles>
                                        <profile>
                                            <id>profile1</id>
                                            <build>
                                                <pluginManagement>
                                                    <plugins>
                                                        <plugin>
                                                            <groupId>org.apache.maven.plugins</groupId>
                                                            <artifactId>maven-resources-plugin</artifactId>
                                                            <version>3.3.1</version>
                                                            <executions>
                                                                <execution>
                                                                    <configuration>
                                                                        <encoding>UTF-8</encoding>
                                                                        <skip>true</skip>
                                                                    </configuration>
                                                                </execution>
                                                            </executions>
                                                        </plugin>
                                                    </plugins>
                                                </pluginManagement>
                                                <plugins>
                                                    <plugin>
                                                        <groupId>org.apache.maven.plugins</groupId>
                                                        <artifactId>maven-resources-plugin</artifactId>
                                                        <version>3.3.1</version>
                                                        <configuration>
                                                            <encoding>UTF-8</encoding>
                                                            <skip>true</skip>
                                                        </configuration>
                                                    </plugin>
                                                </plugins>
                                            </build>
                                        </profile>
                                    </profiles>
                                </project>
                                """
                )
        );
    }

    @CsvSource("true")
    @NullSource
    @ParameterizedTest
    void updateTagEverywhere(Boolean replaceExisting) {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateChildTag(
                        "/project//plugin[groupId='org.apache.maven.plugins' and artifactId='maven-resources-plugin']" +
                        "//configuration",
                        "<skip>true</skip>",
                        replaceExisting)),
                xml(
                        """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>com.example</groupId>
                                    <artifactId>my-project</artifactId>
                                    <version>1.0</version>
                                
                                    <build>
                                        <pluginManagement>
                                            <plugins>
                                                <plugin>
                                                    <groupId>org.apache.maven.plugins</groupId>
                                                    <artifactId>maven-resources-plugin</artifactId>
                                                    <version>3.3.1</version>
                                                    <configuration>
                                                        <encoding>UTF-8</encoding>
                                                        <skip>false</skip>
                                                    </configuration>
                                                </plugin>
                                            </plugins>
                                        </pluginManagement>
                                        <plugins>
                                            <plugin>
                                                <groupId>org.apache.maven.plugins</groupId>
                                                <artifactId>maven-resources-plugin</artifactId>
                                                <version>3.3.1</version>
                                                <configuration>
                                                    <encoding>UTF-8</encoding>
                                                    <skip>false</skip>
                                                </configuration>
                                            </plugin>
                                        </plugins>
                                    </build>
                                    <profiles>
                                        <profile>
                                            <id>profile1</id>
                                            <build>
                                                <pluginManagement>
                                                    <plugins>
                                                        <plugin>
                                                            <groupId>org.apache.maven.plugins</groupId>
                                                            <artifactId>maven-resources-plugin</artifactId>
                                                            <version>3.3.1</version>
                                                            <configuration>
                                                                <encoding>UTF-8</encoding>
                                                                <skip>false</skip>
                                                            </configuration>
                                                        </plugin>
                                                    </plugins>
                                                </pluginManagement>
                                                <plugins>
                                                    <plugin>
                                                        <groupId>org.apache.maven.plugins</groupId>
                                                        <artifactId>maven-resources-plugin</artifactId>
                                                        <version>3.3.1</version>
                                                        <configuration>
                                                            <encoding>UTF-8</encoding>
                                                            <skip>false</skip>
                                                        </configuration>
                                                    </plugin>
                                                </plugins>
                                            </build>
                                        </profile>
                                    </profiles>
                                </project>
                                """,
                        """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>com.example</groupId>
                                    <artifactId>my-project</artifactId>
                                    <version>1.0</version>
                                
                                    <build>
                                        <pluginManagement>
                                            <plugins>
                                                <plugin>
                                                    <groupId>org.apache.maven.plugins</groupId>
                                                    <artifactId>maven-resources-plugin</artifactId>
                                                    <version>3.3.1</version>
                                                    <configuration>
                                                        <encoding>UTF-8</encoding>
                                                        <skip>true</skip>
                                                    </configuration>
                                                </plugin>
                                            </plugins>
                                        </pluginManagement>
                                        <plugins>
                                            <plugin>
                                                <groupId>org.apache.maven.plugins</groupId>
                                                <artifactId>maven-resources-plugin</artifactId>
                                                <version>3.3.1</version>
                                                <configuration>
                                                    <encoding>UTF-8</encoding>
                                                    <skip>true</skip>
                                                </configuration>
                                            </plugin>
                                        </plugins>
                                    </build>
                                    <profiles>
                                        <profile>
                                            <id>profile1</id>
                                            <build>
                                                <pluginManagement>
                                                    <plugins>
                                                        <plugin>
                                                            <groupId>org.apache.maven.plugins</groupId>
                                                            <artifactId>maven-resources-plugin</artifactId>
                                                            <version>3.3.1</version>
                                                            <configuration>
                                                                <encoding>UTF-8</encoding>
                                                                <skip>true</skip>
                                                            </configuration>
                                                        </plugin>
                                                    </plugins>
                                                </pluginManagement>
                                                <plugins>
                                                    <plugin>
                                                        <groupId>org.apache.maven.plugins</groupId>
                                                        <artifactId>maven-resources-plugin</artifactId>
                                                        <version>3.3.1</version>
                                                        <configuration>
                                                            <encoding>UTF-8</encoding>
                                                            <skip>true</skip>
                                                        </configuration>
                                                    </plugin>
                                                </plugins>
                                            </build>
                                        </profile>
                                    </profiles>
                                </project>
                                """
                )
        );
    }

    @Test
    void dontUpdateIfReplaceIsDisabled() {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateChildTag(
                        "/project//plugin[groupId='org.apache.maven.plugins' and artifactId='maven-resources-plugin']" +
                        "//configuration",
                        "<skip>true</skip>",
                        false)),
                xml(
                        """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>com.example</groupId>
                                    <artifactId>my-project</artifactId>
                                    <version>1.0</version>
                                
                                    <build>
                                        <pluginManagement>
                                            <plugins>
                                                <plugin>
                                                    <groupId>org.apache.maven.plugins</groupId>
                                                    <artifactId>maven-resources-plugin</artifactId>
                                                    <version>3.3.1</version>
                                                    <configuration>
                                                        <encoding>UTF-8</encoding>
                                                        <skip>false</skip>
                                                    </configuration>
                                                </plugin>
                                            </plugins>
                                        </pluginManagement>
                                        <plugins>
                                            <plugin>
                                                <groupId>org.apache.maven.plugins</groupId>
                                                <artifactId>maven-resources-plugin</artifactId>
                                                <version>3.3.1</version>
                                                <configuration>
                                                    <encoding>UTF-8</encoding>
                                                    <skip>false</skip>
                                                </configuration>
                                            </plugin>
                                        </plugins>
                                    </build>
                                    <profiles>
                                        <profile>
                                            <id>profile1</id>
                                            <build>
                                                <pluginManagement>
                                                    <plugins>
                                                        <plugin>
                                                            <groupId>org.apache.maven.plugins</groupId>
                                                            <artifactId>maven-resources-plugin</artifactId>
                                                            <version>3.3.1</version>
                                                            <configuration>
                                                                <encoding>UTF-8</encoding>
                                                                <skip>false</skip>
                                                            </configuration>
                                                        </plugin>
                                                    </plugins>
                                                </pluginManagement>
                                                <plugins>
                                                    <plugin>
                                                        <groupId>org.apache.maven.plugins</groupId>
                                                        <artifactId>maven-resources-plugin</artifactId>
                                                        <version>3.3.1</version>
                                                        <configuration>
                                                            <encoding>UTF-8</encoding>
                                                            <skip>false</skip>
                                                        </configuration>
                                                    </plugin>
                                                </plugins>
                                            </build>
                                        </profile>
                                    </profiles>
                                </project>
                                """
                )
        );
    }

    @CsvSource({"true", "false"})
    @NullSource
    @ParameterizedTest
    void dontTouchAnythingElse(Boolean replaceExisting) {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateChildTag(
                        "/project//plugin[groupId='org.apache.maven.plugins' and artifactId='maven-resources-plugin']" +
                        "//configuration",
                        "<skip>true</skip>",
                        replaceExisting)),
                xml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                            <modelVersion>4.0.0</modelVersion>
                            <groupId>com.example</groupId>
                            <artifactId>my-project</artifactId>
                            <version>1.0</version>
                        
                            <build>
                                <pluginManagement>
                                    <plugins>
                                        <plugin>
                                            <groupId>org.apache.maven.plugins</groupId>
                                            <artifactId>maven-other-plugin</artifactId>
                                            <version>3.3.1</version>
                                            <configuration>
                                                <encoding>UTF-8</encoding>
                                                <skip>false</skip>
                                            </configuration>
                                        </plugin>
                                    </plugins>
                                </pluginManagement>
                                <plugins>
                                    <plugin>
                                        <groupId>org.apache.other.plugins</groupId>
                                        <artifactId>maven-resources-plugin</artifactId>
                                        <version>3.3.1</version>
                                        <configuration>
                                            <encoding>UTF-8</encoding>
                                            <skip>false</skip>
                                        </configuration>
                                    </plugin>
                                </plugins>
                            </build>
                            <profiles>
                                <profile>
                                    <id>profile1</id>
                                    <build>
                                        <pluginManagement>
                                            <plugins>
                                                <plugin>
                                                    <groupId>org.apache.maven.plugins</groupId>
                                                    <artifactId>maven-resources-plugin</artifactId>
                                                    <version>3.3.1</version>
                                                </plugin>
                                            </plugins>
                                        </pluginManagement>
                                        <plugins>
                                            <plugin>
                                                <groupId>org.apache.maven.plugins</groupId>
                                                <artifactId>maven-other-plugin</artifactId>
                                                <version>3.3.1</version>
                                                <configuration>
                                                    <encoding>UTF-8</encoding>
                                                    <skip>false</skip>
                                                </configuration>
                                            </plugin>
                                        </plugins>
                                    </build>
                                </profile>
                            </profiles>
                        </project>
                        """
                )
        );
    }
}
