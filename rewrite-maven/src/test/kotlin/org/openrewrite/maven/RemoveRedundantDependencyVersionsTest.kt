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

class RemoveRedundantDependencyVersionsTest : MavenRecipeTest {

    override val recipe = RemoveRedundantDependencyVersions(null, null, true)

    @Test
    fun givenScopeIsDefinedWhenVersionMatchesParentDmForDifferentScopeThenKeepIt() {
        val parent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>org.example</groupId>
                <artifactId>parent-pom-test</artifactId>
                <packaging>pom</packaging>
                <version>1.0-SNAPSHOT</version>
                <modules>
                    <module>child-module1</module>
                </modules>
            
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child),
            before = parent,
        )

        assertUnchanged(
            dependsOn = arrayOf(parent),
            before = child,
        )
    }

    @Test
    fun givenScopeIsDefinedWhenVersionMatchesParentDmForSameScopeAndCompileAndEmptyThenRemoveIt() {
        val parent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>org.example</groupId>
                <artifactId>parent-pom-test</artifactId>
                <packaging>pom</packaging>
                <version>1.0-SNAPSHOT</version>
                <modules>
                    <module>child-module1</module>
                </modules>
            
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                            <scope>compile</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child),
            before = parent,
        )

        assertChanged(
            dependsOn = arrayOf(parent),
            before = child,
            after = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()
        )
    }

    @Test
    fun givenScopeIsDefinedWhenVersionMatchesParentDmForSameScopeAndEmptyAndCompileThenRemoveIt() {
        val parent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>org.example</groupId>
                <artifactId>parent-pom-test</artifactId>
                <packaging>pom</packaging>
                <version>1.0-SNAPSHOT</version>
                <modules>
                    <module>child-module1</module>
                </modules>
            
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child),
            before = parent,
        )

        assertChanged(
            dependsOn = arrayOf(parent),
            before = child,
            after = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()
        )
    }

    @Test
    fun givenScopeIsDefinedWhenVersionMatchesParentDmForSameScopeThenRemoveIt() {
        val parent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>org.example</groupId>
                <artifactId>parent-pom-test</artifactId>
                <packaging>pom</packaging>
                <version>1.0-SNAPSHOT</version>
                <modules>
                    <module>child-module1</module>
                </modules>
            
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child),
            before = parent,
        )

        assertChanged(
            dependsOn = arrayOf(parent),
            before = child,
            after = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()
        )
    }

    @Test
    fun givenScopeIsDefinedWhenVersionMatchesParentDmForSameScopeAndDifferentScopeThenRemoveCorrectOne() {
        val parent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>org.example</groupId>
                <artifactId>parent-pom-test</artifactId>
                <packaging>pom</packaging>
                <version>1.0-SNAPSHOT</version>
                <modules>
                    <module>child-module1</module>
                </modules>
            
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                            <scope>runtime</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                        <scope>test</scope>
                    </dependency>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child),
            before = parent,
        )

        assertChanged(
            dependsOn = arrayOf(parent),
            before = child,
            after = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <scope>test</scope>
                    </dependency>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()
        )
    }

    @Test
    fun givenScopeIsDefinedAndNestedPomsWhenVersionMatchesTopLevelParentForSameScopeThenNextLevelParentForDifferentScopeThenRemoveItCorrectOne() {
        val parent = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
        
            <groupId>org.example</groupId>
            <artifactId>parent-pom-test</artifactId>
            <packaging>pom</packaging>
            <version>1.0-SNAPSHOT</version>
            <modules>
                <module>child-module-1</module>
            </modules>
        
            <dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </dependencyManagement>
        </project>
        """.trimIndent()

        val child1 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>

                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
                <modules>
                    <module>child-module-2</module>
                </modules>
                
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                            <scope>runtime</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child2 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>child-module-1</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-2</artifactId>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child1, child2),
            before = parent,
        )

        assertUnchanged(
            dependsOn = arrayOf(parent, child2),
            before = child1,
        )

        assertUnchanged(
            dependsOn = arrayOf(parent, child1),
            before = child2,
        )
    }

    @Test
    fun matchesParentDmThenRemoveIt() {
        val parent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>org.example</groupId>
                <artifactId>parent-pom-test</artifactId>
                <packaging>pom</packaging>
                <version>1.0-SNAPSHOT</version>
                <modules>
                    <module>child-module1</module>
                </modules>
            
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child),
            before = parent,
        )

        assertChanged(
            dependsOn = arrayOf(parent),
            before = child,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <parent>
                        <artifactId>parent-pom-test</artifactId>
                        <groupId>org.example</groupId>
                        <version>1.0-SNAPSHOT</version>
                    </parent>
                    <modelVersion>4.0.0</modelVersion>
                
                    <artifactId>child-module-1</artifactId>
                    <packaging>pom</packaging>
                
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                        </dependency>
                    </dependencies>
                
                </project>
                """.trimIndent()
        )
    }

    @Test
    fun matchesOwnDmThenRemoveIt() {
        assertChanged(
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                
                    <groupId>org.example</groupId>
                    <artifactId>parent-pom-test</artifactId>
                    <packaging>pom</packaging>
                    <version>1.0-SNAPSHOT</version>
                    <modules>
                        <module>child-module1</module>
                    </modules>
                
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>30.0-jre</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                        </dependency>
                    </dependencies>
                
                </project>
            """.trimIndent(),
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                
                    <groupId>org.example</groupId>
                    <artifactId>parent-pom-test</artifactId>
                    <packaging>pom</packaging>
                    <version>1.0-SNAPSHOT</version>
                    <modules>
                        <module>child-module1</module>
                    </modules>
                
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>30.0-jre</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                        </dependency>
                    </dependencies>
                
                </project>
            """.trimIndent()
        )
    }

    @Test
    fun givenVersionMatchesParentDmWhenOwnDmIsDifferentThenKeepIt() {
        val parent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>org.example</groupId>
                <artifactId>parent-pom-test</artifactId>
                <packaging>pom</packaging>
                <version>1.0-SNAPSHOT</version>
                <modules>
                    <module>child-module1</module>
                </modules>
            
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
            
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(parent),
            before = child
        )
    }

    @Test
    fun whenNoParentOrOwnDmThenKeepIt() {
        val parent = """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany</groupId>
                    <artifactId>myApiApp</artifactId>
                    <version>1.0.0</version>
                </project>
            """.trimIndent()

        val child = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany</groupId>
                <artifactId>mySharedLibrary</artifactId>
                <version>1.0.0</version>
                <parent>
                    <groupId>com.mycompany</groupId>
                    <artifactId>myApiApp</artifactId>
                    <version>1.0.0</version>
                </parent>
                
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child),
            before = parent
        )

        assertUnchanged(
            dependsOn = arrayOf(parent),
            before = child
        )
    }

    @Test
    fun whenNoVersionDefinedThenMakeNoChanges() {
        val parent = """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany</groupId>
                    <artifactId>myApiApp</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>30.0-jre</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()

        val child = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany</groupId>
                <artifactId>mySharedLibrary</artifactId>
                <version>1.0.0</version>
                <parent>
                    <groupId>com.mycompany</groupId>
                    <artifactId>myApiApp</artifactId>
                    <version>1.0.0</version>
                </parent>
                
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child),
            before = parent
        )

        assertUnchanged(
            dependsOn = arrayOf(parent),
            before = child
        )
    }

    @Test
    fun doesntMatchParentDmOrOwnDmThenKeepIt() {
        val parent = """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany</groupId>
                    <artifactId>myApiApp</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>30.0-jre</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()

        val child = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany</groupId>
                <artifactId>mySharedLibrary</artifactId>
                <version>1.0.0</version>
                <parent>
                    <groupId>com.mycompany</groupId>
                    <artifactId>myApiApp</artifactId>
                    <version>1.0.0</version>
                </parent>
                
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.0-jre</version>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child),
            before = parent
        )

        assertUnchanged(
            dependsOn = arrayOf(parent),
            before = child
        )
    }

    @Test
    fun givenNestedPomsWhenVersionMatchesImmediateParentThenRemoveIt() {
        val parent = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
        
            <groupId>org.example</groupId>
            <artifactId>parent-pom-test</artifactId>
            <packaging>pom</packaging>
            <version>1.0-SNAPSHOT</version>
            <modules>
                <module>child-module-1</module>
            </modules>
        
            <dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                </dependencies>
            </dependencyManagement>
        </project>
        """.trimIndent()

        val child1 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>

                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
                <modules>
                    <module>child-module-2</module>
                </modules>

                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        val child2 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>child-module-1</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-2</artifactId>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child1, child2),
            before = parent,
        )

        assertUnchanged(
            dependsOn = arrayOf(parent, child2),
            before = child1,
        )

        assertChanged(
            dependsOn = arrayOf(parent, child1),
            before = child2,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <parent>
                        <artifactId>child-module-1</artifactId>
                        <groupId>org.example</groupId>
                        <version>1.0-SNAPSHOT</version>
                    </parent>
                    <modelVersion>4.0.0</modelVersion>
                
                    <artifactId>child-module-2</artifactId>
                
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                        </dependency>
                    </dependencies>
                
                </project>
                """.trimIndent()
        )
    }

    @Test
    fun givenNestedPomsWhenVersionMatchesTopLevelParentThenRemoveIt() {
        val parent = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
        
            <groupId>org.example</groupId>
            <artifactId>parent-pom-test</artifactId>
            <packaging>pom</packaging>
            <version>1.0-SNAPSHOT</version>
            <modules>
                <module>child-module-1</module>
            </modules>
        
            <dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                </dependencies>
            </dependencyManagement>
        </project>
        """.trimIndent()

        val child1 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>

                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
                <modules>
                    <module>child-module-2</module>
                </modules>

                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        val child2 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>child-module-1</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-2</artifactId>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                </dependencies>
            
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child1, child2),
            before = parent,
        )

        assertUnchanged(
            dependsOn = arrayOf(parent, child2),
            before = child1,
        )

        assertChanged(
            dependsOn = arrayOf(parent, child1),
            before = child2,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <parent>
                        <artifactId>child-module-1</artifactId>
                        <groupId>org.example</groupId>
                        <version>1.0-SNAPSHOT</version>
                    </parent>
                    <modelVersion>4.0.0</modelVersion>
                
                    <artifactId>child-module-2</artifactId>
                
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                        </dependency>
                    </dependencies>
                
                </project>
                """.trimIndent()
        )
    }

    @Test
    fun givenNestedPomsWhenVersionMatchesTopLevelParentAndDoesntMatchFirstLevelParentThenKeepIt() {
        val parent = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
        
            <groupId>org.example</groupId>
            <artifactId>parent-pom-test</artifactId>
            <packaging>pom</packaging>
            <version>1.0-SNAPSHOT</version>
            <modules>
                <module>child-module-1</module>
            </modules>
        
            <dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                </dependencies>
            </dependencyManagement>
        </project>
        """.trimIndent()

        val child1 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>

                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
                <modules>
                    <module>child-module-2</module>
                </modules>
                
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child2 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>child-module-1</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-2</artifactId>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child1, child2),
            before = parent,
        )

        assertUnchanged(
            dependsOn = arrayOf(parent, child2),
            before = child1,
        )

        assertUnchanged(
            dependsOn = arrayOf(parent, child1),
            before = child2,
        )
    }

    @Test
    fun givenNestedPomsWhenVersionDoesntMatchAnyDmThenKeepIt() {
        val parent = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
        
            <groupId>org.example</groupId>
            <artifactId>parent-pom-test</artifactId>
            <packaging>pom</packaging>
            <version>1.0-SNAPSHOT</version>
            <modules>
                <module>child-module-1</module>
            </modules>
        
            <dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                    </dependency>
                </dependencies>
            </dependencyManagement>
        </project>
        """.trimIndent()

        val child1 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>parent-pom-test</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>

                <artifactId>child-module-1</artifactId>
                <packaging>pom</packaging>
                <modules>
                    <module>child-module-2</module>
                </modules>
                
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>28.0-jre</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """.trimIndent()

        val child2 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <parent>
                    <artifactId>child-module-1</artifactId>
                    <groupId>org.example</groupId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <modelVersion>4.0.0</modelVersion>
            
                <artifactId>child-module-2</artifactId>
            
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.0-jre</version>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent()

        assertUnchanged(
            dependsOn = arrayOf(child1, child2),
            before = parent,
        )

        assertUnchanged(
            dependsOn = arrayOf(parent, child2),
            before = child1,
        )

        assertUnchanged(
            dependsOn = arrayOf(parent, child1),
            before = child2,
        )
    }
}
