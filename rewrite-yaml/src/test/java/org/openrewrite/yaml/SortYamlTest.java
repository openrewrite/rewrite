package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.yaml.Assertions.yaml;

class SortYamlTest implements RewriteTest {

    @Test
    @DocumentExample
    void sortYaml() {
        rewriteRun(
          spec -> spec.recipe(new SortYaml(List.of("stages", "include", "*.stage", "*.image"), null)),
          yaml(
            """
              sample-job1:
                stage: a
                image: openjdk:8
                script:
                  - echo "Hello, world 1!"
              sample-job2:
                script:
                  - echo "Hello, world 2!"
                image: openjdk:8
                stage: b
              sample-job3:
                script:
                  - echo "Hello, world 3!"
                image:
                  name: openjdk:8
              stages:
                - a
                - b
                - c
              include:
                - template: Build-Push-Publish-Docker-Image.gitlab-ci.yml
                - component: 'deploy-component@1.20.0'
                  input:
                    stage: 'deploy'
              """,
            """
              stages:
                - a
                - b
                - c
              include:
                - template: Build-Push-Publish-Docker-Image.gitlab-ci.yml
                - component: 'deploy-component@1.20.0'
                  input:
                    stage: 'deploy'
              sample-job1:
                stage: a
                image: openjdk:8
                script:
                  - echo "Hello, world 1!"
              sample-job2:
                stage: b
                image: openjdk:8
                script:
                  - echo "Hello, world 2!"
              sample-job3:
                image:
                  name: openjdk:8
                script:
                  - echo "Hello, world 3!"
              """
          )
        );
    }

    @Test
    void sortValueBased() {
        rewriteRun(
          spec -> spec.recipe(new SortYaml(List.of("*[subkey]"), null)),
          yaml(
            """
              sample-job2:
                subkey: b
              sample-job3:
                subkey: c
              sample-job1:
                subkey: a
              """,
            """
              sample-job1:
                subkey: a
              sample-job2:
                subkey: b
              sample-job3:
                subkey: c
              """
          )
        );
    }

    @Test
    void nestedSortValueBased() {
        rewriteRun(
          spec -> spec.recipe(new SortYaml(List.of("*.*[subkey]"), null)),
          yaml(
            """
              nested:
                sample-job3:
                  subkey: c
                sample-job2:
                  subkey: b
              """,
            """
              nested:
                sample-job2:
                  subkey: b
                sample-job3:
                  subkey: c
              """
          )
        );
    }

    @Test
    void deeplyNestedSort() {
        rewriteRun(
          spec -> spec.recipe(new SortYaml(List.of("*", "*.*", "*.*.*", "*.*.*.*"), null)),
          yaml(
            """
              1:
                0:
                  2: c
                  0: a
                  1: b
              0:
                1:
                  1: b
                  0: a
                0:
                  1:
                    1: b
                    2: c
                    0: a
                  0:
                    2: c
                    0: a
                    1: b
              """,
            """
              0:
                0:
                  0:
                    0: a
                    1: b
                    2: c
                  1:
                    0: a
                    1: b
                    2: c
                1:
                  0: a
                  1: b
              1:
                0:
                  0: a
                  1: b
                  2: c
              """
          )
        );
    }

    @Test
    void deeplyNestedSortNotFullySorted() {
        rewriteRun(
          spec -> spec.recipe(new SortYaml(List.of("*", "*.*"), null)),
          yaml(
            """
              1:
                0:
                  2: c
                  0: a
                  1: b
              0:
                1:
                  1: b
                  0: a
                0:
                  1:
                    1: b
                    2: c
                    0: a
                  0:
                    2: c
                    0: a
                    1: b
              """,
            """
              0:
                0:
                  1:
                    1: b
                    2: c
                    0: a
                  0:
                    2: c
                    0: a
                    1: b
                1:
                  1: b
                  0: a
              1:
                0:
                  2: c
                  0: a
                  1: b
              """
          )
        );
    }

    @Test
    void multiWildCard() {
        rewriteRun(
          spec -> spec.recipe(new SortYaml(List.of("**"), null)),
          yaml(
            """
              1:
                0:
                  2: c
                  0: a
                  1: b
              0:
                1:
                  1: b
                  0: a
                0:
                  1:
                    1: b
                    2: c
                    0: a
                  0:
                    2: c
                    0: a
                    1: b
              """,
            """
              0:
                0:
                  0:
                    0: a
                    1: b
                    2: c
                  1:
                    0: a
                    1: b
                    2: c
                1:
                  0: a
                  1: b
              1:
                0:
                  0: a
                  1: b
                  2: c
              """
          )
        );
    }

    @Test
    void multiWildCardAtEnd() {
        rewriteRun(
          spec -> spec.recipe(new SortYaml(List.of("1.**", "0.0.**"), null)),
          yaml(
            """
              1:
                0:
                  2: c
                  0: a
                  1: b
              0:
                1:
                  1: b
                  0: a
                0:
                  1:
                    1: b
                    2: c
                    0: a
                  0:
                    2: c
                    0: a
                    1: b
              """,
            """
              1:
                0:
                  0: a
                  1: b
                  2: c
              0:
                0:
                  0:
                    0: a
                    1: b
                    2: c
                  1:
                    0: a
                    1: b
                    2: c
                1:
                  1: b
                  0: a
              """
          )
        );
    }

    @Test
    void sortValueBasedWithMultipleWildcards() {
        rewriteRun(
          spec -> spec.recipe(new SortYaml(List.of("**[subkey]"), null)),
          yaml(
            """
              sample-job2:
                nested:
                  subkey: b
              sample-job3:
                subkey: c
              sample-job1:
                can:
                  be:
                    very:
                      deeply:
                        nested:
                          subkey: a
              """,
            """
              sample-job1:
                can:
                  be:
                    very:
                      deeply:
                        nested:
                          subkey: a
              sample-job2:
                nested:
                  subkey: b
              sample-job3:
                subkey: c
              """
          )
        );
    }
}
