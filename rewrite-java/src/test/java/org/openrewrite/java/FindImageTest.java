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

import java.util.function.Consumer;

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
          assertImages("maven:latest"),
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
              image: ~~(maven:latest)~~>maven:latest
              
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
    void yamlFileWithMultipleImages() {
        rewriteRun(
          assertImages("golang:1.7.0", "golang:1.7.0", "golang:1.7.3"),
          yaml(
            """
              test:
                image: golang:1.7.3
              
              accp:
                image: golang:1.7.0
              
              prod:
                image: golang:1.7.0
              """,
            """
              test:
                image: ~~(golang:1.7.3)~~>golang:1.7.3
              
              accp:
                image: ~~(golang:1.7.0)~~>golang:1.7.0
              
              prod:
                image: ~~(golang:1.7.0)~~>golang:1.7.0
              """
          )
        );
    }

    @Test
    void dockerFile() {
        rewriteRun(
          assertImages("golang:1.7.3"),
          text(
            //language=Dockerfile
            """
              FROM golang:1.7.3 as builder
              WORKDIR /go/src/github.com/alexellis/href-counter/
              RUN go get -d -v golang.org/x/net/html
              COPY app.go .
              RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
              """,
            """
              FROM ~~(golang:1.7.3)~~>golang:1.7.3 as builder
              WORKDIR /go/src/github.com/alexellis/href-counter/
              RUN go get -d -v golang.org/x/net/html
              COPY app.go .
              RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
              """,
            spec -> spec.path("Dockerfile")
          )
        );
    }

    @Test
    void dockerMultipleStageFileWithLowerCaseText() {
        rewriteRun(
          assertImages("alpine:latest", "golang:1.7.3"),
          text(
            //language=Dockerfile
            """
              FROM golang:1.7.3 as builder
              WORKDIR /go/src/github.com/alexellis/href-counter/
              RUN go get -d -v golang.org/x/net/html
              COPY app.go .
              RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
              
              from alpine:latest
              run apk --no-cache add ca-certificates
              workdir /root/
              copy --from=builder /go/src/github.com/alexellis/href-counter/app .
              cmd ["./app"]
              """,
            """
              FROM ~~(golang:1.7.3)~~>golang:1.7.3 as builder
              WORKDIR /go/src/github.com/alexellis/href-counter/
              RUN go get -d -v golang.org/x/net/html
              COPY app.go .
              RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
              
              from ~~(alpine:latest)~~>alpine:latest
              run apk --no-cache add ca-certificates
              workdir /root/
              copy --from=builder /go/src/github.com/alexellis/href-counter/app .
              cmd ["./app"]
              """,
            spec -> spec.path("Dockerfile")
          )
        );
    }

    @Test
    void dockerMultipleStageFileWithImageInFromOption() {
        rewriteRun(
          assertImages("alpine:latest", "nginx:latest"),
          text(
            //language=Dockerfile
            """
              FROM alpine:latest
              COPY --from=nginx:latest /etc/nginx/nginx.conf /etc/nginx/
              COPY --from=nginx:latest /usr/share/nginx/html /usr/share/nginx/html
              RUN apk add --no-cache bash
              WORKDIR /usr/share/nginx/html
              CMD ["ls", "-la", "/usr/share/nginx/html"]
              """,
            """
              FROM ~~(alpine:latest)~~>alpine:latest
              COPY --from=~~(nginx:latest)~~>nginx:latest /etc/nginx/nginx.conf /etc/nginx/
              COPY --from=~~(nginx:latest)~~>nginx:latest /usr/share/nginx/html /usr/share/nginx/html
              RUN apk add --no-cache bash
              WORKDIR /usr/share/nginx/html
              CMD ["ls", "-la", "/usr/share/nginx/html"]
              """,
            spec -> spec.path("Dockerfile")
          )
        );
    }

    @Test
    void dockerMultipleStageFileWithFromOptionAsStageNumber() {
        rewriteRun(
          assertImages("golang:1.23", "scratch"),
          text(
            //language=Dockerfile
            """
              # syntax=docker/dockerfile:1
              FROM golang:1.23
              WORKDIR /src
              COPY <<EOF ./main.go
              package main
              
              import "fmt"

              func main() {
                fmt.Println("hello, world")
              }
              EOF

              RUN go build -o /bin/hello ./main.go
              
              FROM scratch
              COPY --from=0 /bin/hello /bin/hello
              CMD ["/bin/hello"]
              """,
            """
              # syntax=docker/dockerfile:1
              FROM ~~(golang:1.23)~~>golang:1.23
              WORKDIR /src
              COPY <<EOF ./main.go
              package main
              
              import "fmt"

              func main() {
                fmt.Println("hello, world")
              }
              EOF

              RUN go build -o /bin/hello ./main.go
              
              FROM ~~(scratch)~~>scratch
              COPY --from=0 /bin/hello /bin/hello
              CMD ["/bin/hello"]
              """,
            spec -> spec.path("Dockerfile")
          )
        );
    }


    @Test
    void platformDockerfile() {
        rewriteRun(
          assertImages("alpine:latest"),
          text(
            //language=Dockerfile
            """
              FROM --platform=linux/arm64 alpine:latest
              RUN echo "Hello from ARM64!" > /message.txt
              CMD ["cat", "/message.txt"]
              """,
            """
              FROM --platform=linux/arm64 ~~(alpine:latest)~~>alpine:latest
              RUN echo "Hello from ARM64!" > /message.txt
              CMD ["cat", "/message.txt"]
              """,
            spec -> spec.path("Dockerfile")
          )
        );
    }

    @Test
    void dockerFileIgnoreComment() {
        rewriteRun(
          assertImages("alpine:latest"),
          text(
            //language=Dockerfile
            """
              # FROM alpine
              FROM alpine:latest
              """,
            """
              # FROM alpine
              FROM ~~(alpine:latest)~~>alpine:latest
              """,
            spec -> spec.path("Dockerfile")
          )
        );
    }

    private static Consumer<RecipeSpec> assertImages(String... expected) {
        return spec -> spec.recipe(new FindImage())
          .dataTable(ImageSourceFiles.Row.class, rows ->
            assertThat(rows)
              .hasSize(expected.length)
              .extracting(ImageSourceFiles.Row::getValue)
              .containsExactlyInAnyOrder(expected)
          );
    }
}
