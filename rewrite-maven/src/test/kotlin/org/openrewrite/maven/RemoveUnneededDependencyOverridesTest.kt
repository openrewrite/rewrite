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
import org.junit.jupiter.api.io.TempDir
import kotlin.io.writeText
import kotlin.text.trimIndent

class RemoveUnneededDependencyOverridesTest : MavenRecipeTest {

    @Test
    fun matchesParentDM_remove() {
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
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(child),
            before = parent,
        )

        assertChanged(
            recipe = RemoveUnneededDependencyOverrides(),
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
    fun matchesOwnDM_remove() {
        assertChanged(
            recipe = RemoveUnneededDependencyOverrides(),
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
    fun matchesParentDM_ownDMDifferent_keep() {
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
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent),
            before = child
        )
    }

    @Test
    fun noParentOrOwnDM_keep() {
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
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(child),
            before = parent
        )

        assertUnchanged(
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent),
            before = child
        )
    }

    @Test
    fun noVersionDefined_noChange() {
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
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(child),
            before = parent
        )

        assertUnchanged(
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent),
            before = child
        )
    }

    @Test
    fun doesntMatchParentDMOrOwnDM_keep() {
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
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(child),
            before = parent
        )

        assertUnchanged(
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent),
            before = child
        )
    }

    @Test
    fun nested_matchesImmediateParent_remove() {
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
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(child1, child2),
            before = parent,
        )

        assertUnchanged(
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent, child2),
            before = child1,
        )

        assertChanged(
            recipe = RemoveUnneededDependencyOverrides(),
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
    fun nested_matchesTopLevelParent_remove() {
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
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(child1, child2),
            before = parent,
        )

        assertUnchanged(
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent, child2),
            before = child1,
        )

        assertChanged(
            recipe = RemoveUnneededDependencyOverrides(),
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
    fun nested_matchesTopLevelParent_doesntMatchFirstLevelParent_keep() {
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
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(child1, child2),
            before = parent,
        )

        assertUnchanged(
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent, child2),
            before = child1,
        )

        assertUnchanged(
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent, child1),
            before = child2,
        )
    }

    @Test
    fun nested_doesntMatchAnyDM_keep() {
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
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(child1, child2),
            before = parent,
        )

        assertUnchanged(
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent, child2),
            before = child1,
        )

        assertUnchanged(
            recipe = RemoveUnneededDependencyOverrides(),
            dependsOn = arrayOf(parent, child1),
            before = child2,
        )
    }
}