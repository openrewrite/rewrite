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
package org.openrewrite.maven

import org.junit.jupiter.api.Test
import org.openrewrite.*
import org.openrewrite.maven.tree.Maven

class AddOrUpdateDependencyManagementTest : RefactorVisitorTestForParser<Maven.Pom> {
    override val parser: Parser<Maven.Pom> = MavenParser.builder()
            .resolveDependencies(false)
            .build()

    @Test
    fun shouldCreateDependencyManagementWithDependencyWhenNoneExists() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("org.junit.jupiter")
                setArtifactId("junit-jupiter-api")
                setVersion("5.6.2")
                setScope("test")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-api</artifactId>
                                <version>5.6.2</version>
                                <scope>test</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )

    @Test
    fun shouldAddDependencyWhenDependencyManagementAlreadyExists() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("org.projectlombok")
                setArtifactId("lombok")
                setVersion("1.18.12")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-api</artifactId>
                                <version>5.6.2</version>
                                <scope>test</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-api</artifactId>
                                <version>5.6.2</version>
                                <scope>test</scope>
                            </dependency>
                            <dependency>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>1.18.12</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )

    @Test
    fun shouldUpdateVersionIfDifferent() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("org.junit.jupiter")
                setArtifactId("junit-jupiter-api")
                setVersion("10.100")
                setScope("test")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-api</artifactId> 
                                <version>5.6.2</version> 
                                <scope>test</scope> 
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-api</artifactId>
                                <version>10.100</version>
                                <scope>test</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )

    @Test
    fun shouldUpdateScopeIfDifferent() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("org.junit.jupiter")
                setArtifactId("junit-jupiter-api")
                setVersion("10.100")
                setScope("test")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"> 
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name> 
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-api</artifactId> 
                                <version>5.6.2</version> 
                                <scope>compile</scope> 
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"> 
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name> 
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-api</artifactId>
                                <version>10.100</version>
                                <scope>test</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )


    @Test
    fun shouldRemoveScopeIfRemoved() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("org.junit.jupiter")
                setArtifactId("junit-jupiter-api")
                setVersion("10.100")
                setScope(null)
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-api</artifactId>
                                <version>5.6.2</version>
                                <scope>compile</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-api</artifactId>
                                <version>10.100</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )

    @Test
    fun shouldAddTypeIfGiven() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("org.springramework.boot")
                setArtifactId("spring-boot-dependencies")
                setVersion("2.3.4.RELEASE")
                setType("pom")
                setScope("import")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springramework.boot</groupId>
                                <artifactId>spring-boot-dependencies</artifactId>
                                <version>2.3.4.RELEASE</version>
                                <scope>import</scope>
                                <type>pom</type>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )

    @Test
    fun shouldSortDependencies_usingSetOfSameGroupId() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("b")
                setArtifactId("a")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>b</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>b</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>b</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )

    @Test
    fun shouldSortDependencies_usingSetOfSameGroupId_lastPosition() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("b")
                setArtifactId("a")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>b</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )


    @Test
    fun shouldSortDependencies_usingSetOfSameGroupId_firstPosition() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("c")
                setArtifactId("a")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )

    @Test
    fun shouldSortDependencies_usingSetOfSameGroupId_middlePosition() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("a")
                setArtifactId("c")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>c</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )

    @Test
    fun shouldSortDependencies_usingSetOfSameGroupId_withNewGroupId() = assertRefactored(
            visitors = listOf(AddOrUpdateDependencyManagement().apply {
                setGroupId("a")
                setArtifactId("a")
            }),
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>b</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>dependency-management-example</artifactId>
                    <version>0.1-SNAPSHOT</version>
                    <name>dependency-management-example</name>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>a</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>c</groupId>
                                <artifactId>b</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>b</groupId>
                                <artifactId>a</artifactId>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
    )
}
