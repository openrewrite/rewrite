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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.table.ImageSourceFiles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

class FindImageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindImage());
    }


    @DocumentExample
    @Test
    void gitlabCIFile() {
        rewriteRun(
          yaml(
            """
              image: maven:latest
              
              variables:
                MAVEN_CLI_OPTS: "-s .m2/settings.xml --batch-mode"
                MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
              
              cache:
                paths:
                  - .m2/repository/
                  - target/
              
              build:
                stage: build
                script:
                  - mvn $MAVEN_CLI_OPTS compile
              
              test:
                stage: test
                script:
                  - mvn $MAVEN_CLI_OPTS test
              
              deploy:
                stage: deploy
                script:
                  - mvn $MAVEN_CLI_OPTS deploy
                only:
                  - master
              """,
            """
              ~~(maven:latest)~~>image: maven:latest
              
              variables:
                MAVEN_CLI_OPTS: "-s .m2/settings.xml --batch-mode"
                MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
              
              cache:
                paths:
                  - .m2/repository/
                  - target/
              
              build:
                stage: build
                script:
                  - mvn $MAVEN_CLI_OPTS compile
              
              test:
                stage: test
                script:
                  - mvn $MAVEN_CLI_OPTS test
              
              deploy:
                stage: deploy
                script:
                  - mvn $MAVEN_CLI_OPTS deploy
                only:
                  - master
              """,
            spec -> spec.path(".gitlab-ci")
          )
        );
    }

    @Test
    void dockerFile() {
        rewriteRun(
          text(
            //language=Dockerfile
            """
              FROM openjdk:8-jdk-alpine
              ARG JAR_FILE=target/*.jar
              COPY ${JAR_FILE} app.jar
              ENTRYPOINT ["java","-jar","/app.jar"]
              """,
            """
              ~~(FROM openjdk:8-jdk-alpine)~~>FROM openjdk:8-jdk-alpine
              ARG JAR_FILE=target/*.jar
              COPY ${JAR_FILE} app.jar
              ENTRYPOINT ["java","-jar","/app.jar"]
              """,
            spec -> spec.path("Dockerfile")
          )
        );
    }
}
