package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.table.ImageSourceFiles;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

class FindImageTest implements RewriteTest {

    @DocumentExample
    @Test
    void gitlabCIFile() {
        rewriteRun(
          spec -> spec.recipe(new FindImage())
            .dataTable(ImageSourceFiles.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getValue()).isEqualTo("maven:latest");
            }),
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
              """
          )
        );
    }

    @Test
    void dockerFile() {
        rewriteRun(
          spec -> spec.recipe(new FindImage())
            .dataTable(ImageSourceFiles.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getValue()).isEqualTo("openjdk:8-jdk-alpine");
            }),
          text(
            //language=Dockerfile
            """
              FROM openjdk:8-jdk-alpine
              ARG JAR_FILE=target/*.jar
              COPY ${JAR_FILE} app.jar
              ENTRYPOINT ["java","-jar","/app.jar"]
              """
          )
        );
    }
}
