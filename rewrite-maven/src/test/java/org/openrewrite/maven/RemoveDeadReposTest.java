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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveDeadReposTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveDeadRepos());
    }

    @Test
    void replaceHttpsMavenGlassfishDeadRepo() {
        rewriteRun(pomXml("""
              <project>
                  <repositories>
                      <repository>
                          <id>central</id>
                          <url>https://maven.glassfish.org/content/groups/public</url>
                      </repository>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """, """
              <project>
                  <repositories>
                      <repository>
                          <id>central</id>
                          <url>https://maven.java.net</url>
                      </repository>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """));
    }


    @Test
    void removeFakeAbcmavenDeadRepo() {
        rewriteRun(pomXml("""
              <project>
                  <repositories>
                      <repository>
                          <id>abcmaven</id>
                          <url>https://abcmaven.com</url>
                      </repository>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """, """
              <project>
                  <repositories>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """));
    }

    @Test
    void removeFakeAbcmavenDeadRepoHalfInProperty() {
        rewriteRun(pomXml("""
              <project>
                  <properties>
                      <maven.repo.url.suffix>maven</maven.repo.url.suffix>
                  </properties>
                  <repositories>
                      <repository>
                          <id>abcmaven</id>
                          <url>https://abc${maven.repo.url.suffix}.com</url>
                      </repository>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """, """
              <project>
                  <properties>
                      <maven.repo.url.suffix>maven</maven.repo.url.suffix>
                  </properties>
                  <repositories>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """));
    }

    @Test
    void removeFakeAbcmavenDeadRepoHalfInPropertyMultipleProps() {
        rewriteRun(pomXml("""
              <project>
                  <properties>
                      <maven.repo.url.suffix>maven</maven.repo.url.suffix>
                      <https.prefix>https://</https.prefix>
                  </properties>
                  <repositories>
                      <repository>
                          <id>abcmaven</id>
                          <url>${https.prefix}abc${maven.repo.url.suffix}.com</url>
                      </repository>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """, """
              <project>
                  <properties>
                      <maven.repo.url.suffix>maven</maven.repo.url.suffix>
                      <https.prefix>https://</https.prefix>
                  </properties>
                  <repositories>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """));
    }

    @Test
    void removeFakeAbcmavenDeadRepoHalfInPropertyMultipleMultipleProps() {
        rewriteRun(pomXml("""
              <project>
                  <properties>
                      <maven.repo.url.suffix>maven</maven.repo.url.suffix>
                      <https.prefix>https://</https.prefix>
                      <abc.prefix>abc</abc.prefix>
                  </properties>
                  <repositories>
                      <repository>
                          <id>abcmaven</id>
                          <url>${https.prefix}abc${maven.repo.url.suffix}.com</url>
                      </repository>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>abcmaven</id>
                          <url>${https.prefix}${abc.prefix}maven.com</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """, """
              <project>
                  <properties>
                      <maven.repo.url.suffix>maven</maven.repo.url.suffix>
                      <https.prefix>https://</https.prefix>
                      <abc.prefix>abc</abc.prefix>
                  </properties>
                  <repositories>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """));
    }

    @Test
    void replaceHttpsMavenGlassfishDeadRepoLinkInProperties() {
        rewriteRun(pomXml("""
              <project>
                  <properties>
                      <maven.repo.url>https://maven.glassfish.org/content/groups/public</maven.repo.url>
                  </properties>
                  <repositories>
                      <repository>
                          <id>central</id>
                          <url>${maven.repo.url}</url>
                      </repository>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>${maven.repo.url}</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """, """
              <project>
                  <properties>
                      <maven.repo.url>https://maven.java.net</maven.repo.url>
                  </properties>
                  <repositories>
                      <repository>
                          <id>central</id>
                          <url>${maven.repo.url}</url>
                      </repository>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>${maven.repo.url}</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """

        ));
    }

    @Test
    void removeHttpsFakeAbcMavenDeadRepoInPropertiesAndAllAssociatedRepos() {
        rewriteRun(pomXml("""
              <project>
                  <properties>
                      <abc.repo.url>https://abcmaven.com</abc.repo.url>
                  </properties>
                  <repositories>
                      <repository>
                          <id>abcmaven</id>
                          <url>${abc.repo.url}</url>
                      </repository>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>${abc.repo.url}</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """, """
              <project>
                  <properties>
                  </properties>
                  <repositories>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """

        ));
    }

    @Test
    void noDeadRepos() {
        rewriteRun(pomXml("""
              <project>
                  <repositories>
                      <repository>
                          <id>spring-snapshots</id>
                          <url>https://repo.spring.io/snapshot</url>
                      </repository>
                      <repository>
                          <id>spring-milestones</id>
                          <url>https://repo.spring.io/milestone</url>
                      </repository>
                  </repositories>
              </project>
          """));
    }
}