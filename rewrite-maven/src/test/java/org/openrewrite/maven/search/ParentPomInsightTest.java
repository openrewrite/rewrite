/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.table.ParentPomsInUse;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class ParentPomInsightTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ParentPomInsight("org.springframework.boot", "spring-boot-starter-parent", null));
    }

    @DocumentExample
    @Test
    void findParent() {
        rewriteRun(
          spec -> spec.dataTable(ParentPomsInUse.Row.class, rows -> assertThat(rows)
            .singleElement()
            .satisfies(row -> {
                  assertThat(row.getProjectArtifactId()).isEqualTo("demo");
                  assertThat(row.getGroupId()).isEqualTo("org.springframework.boot");
                  assertThat(row.getArtifactId()).isEqualTo("spring-boot-starter-parent");
                  assertThat(row.getVersion()).isEqualTo("3.1.4");
                  assertThat(row.getRelativePath()).isNull();
              }
            )
          ),
          mavenProject("demo",
            pomXml(
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                	<modelVersion>4.0.0</modelVersion>
                	<parent>
                		<groupId>org.springframework.boot</groupId>
                		<artifactId>spring-boot-starter-parent</artifactId>
                		<version>3.1.4</version>
                		<relativePath/> <!-- lookup parent from repository -->
                	</parent>
                	<groupId>com.example</groupId>
                	<artifactId>demo</artifactId>
                	<version>0.0.1-SNAPSHOT</version>
                </project>
                """,
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                	<modelVersion>4.0.0</modelVersion>
                	<!--~~>--><parent>
                		<groupId>org.springframework.boot</groupId>
                		<artifactId>spring-boot-starter-parent</artifactId>
                		<version>3.1.4</version>
                		<relativePath/> <!-- lookup parent from repository -->
                	</parent>
                	<groupId>com.example</groupId>
                	<artifactId>demo</artifactId>
                	<version>0.0.1-SNAPSHOT</version>
                </project>
                """
            )
          )
        );
    }

    @Test
    void multiModuleOnlyRoot() {
        rewriteRun(
          spec -> spec
            .recipe(new ParentPomInsight("*", "*", null))
            .dataTableAsCsv(ParentPomsInUse.class.getName(), """
              projectArtifactId,groupId,artifactId,version,relativePath
              sample,org.springframework.boot,"spring-boot-starter-parent",2.5.0,
              module1,org.sample,sample,1.0.0,../
              module2,org.sample,sample,1.0.0,../
              """),
          mavenProject("sample",
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

                  <!--~~>--><parent>
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
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <!--~~>--><parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                spec -> spec.path("module1/pom.xml")
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
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <!--~~>--><parent>
                      <groupId>org.sample</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                spec -> spec.path("module2/pom.xml")
              )
            )
          )
        );
    }

    @Test
    void matchNonSnapshot() {
        rewriteRun(
          spec -> spec
            .recipe(new ParentPomInsight("*", "*", "~2"))
            .dataTableAsCsv(ParentPomsInUse.class.getName(), """
              projectArtifactId,groupId,artifactId,version,relativePath
              sample,org.springframework.boot,"spring-boot-starter-parent",2.5.0,
              """),
          mavenProject("sample",
            pomXml(
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  
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
                  <version>1.0.0-SNAPSHOT</version>

                  <!--~~>--><parent>
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
                      <version>1.0.0-SNAPSHOT</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module1</artifactId>
                  </project>
                  """,
                spec -> spec.path("module1/pom.xml")
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
                      <version>1.0.0-SNAPSHOT</version>
                      <relativePath>../</relativePath>
                    </parent>
                    <artifactId>module2</artifactId>
                  </project>
                  """,
                spec -> spec.path("module2/pom.xml")
              )
            )
          )
        );
    }
}
