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
package org.openrewrite.text;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class FindAndReplaceTest implements RewriteTest {

    @DocumentExample
    @Test
    void nonTxtExtension() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", null, null, null, null, null)),
          text(
            """
              This is text.
              """,
            """
              This is textG
              """,
            spec -> spec.path("test.yml")
          )
        );
    }

    @Test
    void defaultNonRegex() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", null, null, null, null, null)),
          text(
            """
              This is text.
              """,
            """
              This is textG
              """
          )
        );
    }

    @Test
    void regexReplace() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", true, null, null, null, null)),
          text(
            """
              This is text.
              """,
            """
              GGGGGGGGGGGGG
              """
          )
        );
    }

    @Test
    void captureGroups() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace("This is ([^.]+).", "I like $1.", true, null, null, null, null)),
          text(
            """
              This is text.
              """,
            """
              I like text.
              """
          )
        );
    }

    @Test
    void noRecursive() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace("test", "tested", false, null, null, null, null)),
          text("test", "tested")
        );
    }

    @Test
    void multilineReplace() {
        rewriteRun(
          spec -> spec.recipe(
            new FindAndReplace("""
              final MongoClientURI uri = new MongoClientURI(clientUri, buildMongoClientOptionsBuilder());
                    return new MongoClient(uri);""",
              """
              final MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder().applyConnectionString(new ConnectionString(clientUri));
                    if (appMongoProperties.isUseClientCert()) {
                        settingsBuilder.applyToSslSettings(sslBuilder -> {
                            sslBuilder.enabled(true);
                            sslBuilder.context(buildSslContext());
                        });
                    }
                    return MongoClients.create(settingsBuilder.build());""",
              null,
              null,
              null,
              null,
              null)),
          text(
            """
              class test {
                  void test() {
                    final MongoClientURI uri = new MongoClientURI(clientUri, buildMongoClientOptionsBuilder());
                    return new MongoClient(uri);
                  }
              }""",
            """
              class test {
                  void test() {
                    final MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder().applyConnectionString(new ConnectionString(clientUri));
                    if (appMongoProperties.isUseClientCert()) {
                        settingsBuilder.applyToSslSettings(sslBuilder -> {
                            sslBuilder.enabled(true);
                            sslBuilder.context(buildSslContext());
                        });
                    }
                    return MongoClients.create(settingsBuilder.build());
                  }
              }
              """
          )
        );
    }
}
