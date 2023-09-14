/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import java.util.Collections;
import java.util.List;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class ChangeParentPomTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-parent</artifactId>
                  <version>2.12</version>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void changeParentWithRelativePath() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            "",
            "../../pom.xml",
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-parent</artifactId>
                  <version>2.12</version>
                  <relativePath>../../pom.xml</relativePath>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void changeParentAddRelativePathEmptyValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            null,
            "",
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-parent</artifactId>
                  <version>2.12</version>
                  <relativePath />
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void changeParentAddRelativePathNonEmptyValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            null,
            "../pom.xml",
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-parent</artifactId>
                  <version>2.12</version>
                  <relativePath>../pom.xml</relativePath>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    @RepeatedTest(10)
    void multiModuleRelativePath() {
      ChangeParentPom recipe = new ChangeParentPom("org.springframework.boot", null, "spring-boot-starter-parent", null, "2.6.7", null, "", null, false, null);
      rewriteRun(
        spec -> spec.recipe(recipe),
        mavenProject("parent",
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.5.0</version>
                </parent>
                
                <modules>
                  <module>module1</module>
                  <module>module2</module>
                </modules>
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.6.7</version>
                  <relativePath />
                </parent>

                <modules>
                  <module>module1</module>
                  <module>module2</module>
                </modules>
              </project>
              """
          ),
          mavenProject("module1",
            pomXml(
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                  </parent>
                  <artifactId>module1</artifactId>
                </project>
                """
            )),
          mavenProject("module2",
            pomXml(
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                  </parent>
                  <artifactId>module2</artifactId>
                </project>
                """
            )
          )
        )
      );
  }

  @Test
  @RepeatedTest(10)
  void multiModuleRelativePathChangeChildrens() {
    ChangeParentPom recipe = new ChangeParentPom("org.sample", "org.springframework.boot", "sample", "spring-boot-starter-parent", "2.5.0", null, "", null, true, null);
    rewriteRun(
      spec -> spec.recipe(recipe),
      mavenProject("parent",
        pomXml(
          """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.5.0</version>
              </parent>
              
              <modules>
                <module>module1</module>
                <module>module2</module>
              </modules>
            </project>
            """
        ),
        mavenProject("module1",
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                </parent>
                <artifactId>module1</artifactId>
              </project>
              """,
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                    <relativePath />
                  </parent>
                  <artifactId>module1</artifactId>
                </project>
                """
          )),
        mavenProject("module2",
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                </parent>
                <artifactId>module2</artifactId>
              </project>
              """,
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                    <relativePath />
                  </parent>
                  <artifactId>module2</artifactId>
                </project>
                """
          )
        )
      )
    );
}

    @Test
    void upgradeVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "~1.5",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeToExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.22.RELEASE",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void doNotDowngradeToLowerVersionWhenArtifactsAreTheSame() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.12.RELEASE",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void downgradeToLowerVersionWhenFlagIsSet() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.12.RELEASE",
            null,
            null,
            null,
            true,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void wildcardVersionUpdate() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "*",
            null,
            "*",
            null,
            "~1.5",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void removesRedundantExplicitVersionsMatchingOldParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.junit",
            null,
            "junit-bom",
            null,
            "5.9.1",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.0</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.9.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.1</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removesRedundantExplicitVersionsMatchingNewParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.junit",
            null,
            "junit-bom",
            null,
            "5.9.1",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.0</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.9.1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.1</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepsRedundantExplicitVersionsNotMatchingOldOrNewParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.junit",
            null,
            "junit-bom",
            null,
            "5.9.1",
            null,
            null,
            null,
            false,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.0</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.8.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.1</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.8.0</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeNonSemverVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom("org.springframework.cloud", null, "spring-cloud-starter-parent", null, "2021.0.5", null, null, null, false, null)),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                
                <parent>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-starter-parent</artifactId>
                  <version>Hoxton.SR12</version>
                </parent>
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                
                <parent>
                  <groupId>org.springframework.cloud</groupId>
                  <artifactId>spring-cloud-starter-parent</artifactId>
                  <version>2021.0.5</version>
                </parent>
              </project>
              """
          )
        );
    }

    @Nested
    @Issue("https://github.com/openrewrite/rewrite/issues/2418")
    class RetainVersions {
        @DocumentExample
        @Test
        void dependencyWithExplicitVersionRemovedFromDepMgmt() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom("org.springframework.cloud", null, "spring-cloud-config-dependencies", null, "3.1.4", null, null, null, null,
                List.of("com.jcraft:jsch"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-config-dependencies</artifactId>
                      <version>3.1.2</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-config-dependencies</artifactId>
                      <version>3.1.4</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void dependencyWithoutExplicitVersionRemovedFromDepMgmt() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom("org.springframework.cloud", null, "spring-cloud-config-dependencies", null, "3.1.4", null, null, null, null,
                Collections.singletonList("com.jcraft:jsch"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-config-dependencies</artifactId>
                      <version>3.1.2</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-config-dependencies</artifactId>
                      <version>3.1.4</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void dependencyWithoutExplicitVersionRemovedFromDepMgmtRetainSpecificVersion() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom("org.springframework.cloud", null, "spring-cloud-config-dependencies", null, "3.1.4", null, null, null, null,
                Collections.singletonList("com.jcraft:jsch:0.1.50"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-config-dependencies</artifactId>
                      <version>3.1.2</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-config-dependencies</artifactId>
                      <version>3.1.4</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.50</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void multipleRetainVersions() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom("org.springframework.cloud", null, "spring-cloud-dependencies", null, "2021.0.5", null, null, null, true,
                Lists.newArrayList("com.jcraft:jsch", "org.springframework.cloud:spring-cloud-schema-registry-*:1.1.1"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-dependencies</artifactId>
                      <version>2020.0.1</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-dependencies</artifactId>
                      <version>2021.0.5</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                        <version>1.1.1</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void globGavWithNoVersion() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom("org.springframework.cloud", null, "spring-cloud-dependencies", null, "2021.0.5", null, null, null, true,
                Lists.newArrayList("org.springframework.cloud:spring-cloud-schema-registry-*"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-dependencies</artifactId>
                      <version>2020.0.1</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-dependencies</artifactId>
                      <version>2021.0.5</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                        <version>1.1.1</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void preservesExplicitVersionIfNotRequested() {
            rewriteRun(
              spec -> spec.recipe(new ChangeParentPom("org.springframework.cloud", null, "spring-cloud-dependencies", null, "2021.0.5", null, null, null, true,
                Lists.newArrayList("org.springframework.cloud:spring-cloud-schema-registry-*"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-dependencies</artifactId>
                      <version>2020.0.1</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                        <version>1.1.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <parent>
                      <groupId>org.springframework.cloud</groupId>
                      <artifactId>spring-cloud-dependencies</artifactId>
                      <version>2021.0.5</version>
                    </parent>
                    
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                        <version>1.1.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }
    }

    @RepeatedTest(10)
    @Issue("https://github.com/openrewrite/rewrite/issues/1753")
    void multiModule() {
        ChangeParentPom recipe = new ChangeParentPom("org.springframework.boot", null, "spring-boot-starter-parent", null, "2.6.7", null, null, null, true, null);
        rewriteRun(
          spec -> spec.recipe(recipe),
          mavenProject("parent",
            pomXml(
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.5.0</version>
                  </parent>
                  
                  <modules>
                    <module>module1</module>
                    <module>module2</module>
                  </modules>
                </project>
                """,
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.6.7</version>
                  </parent>

                  <modules>
                    <module>module1</module>
                    <module>module2</module>
                  </modules>
                </project>
                """
            ),
            mavenProject("module1",
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """
              )),
            mavenProject("module2",
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """
              )
            )
          )
        );
    }
}
