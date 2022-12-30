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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.yaml.Assertions.yaml;

class ChangePropertyKeyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangePropertyKey(
          "management.metrics.binders.*.enabled",
          "management.metrics.enable.process.files",
          null,
          null,
          null
        ));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1873")
    @Test
    void shorterNewKeyWithIndentedConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey("a.b.c.d.e", "x.y", null, null, null)),
          yaml(
            """
              a:
                b:
                  c:
                    d:
                      e:
                        child: true
              """,
            """
              x.y:
                child: true
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1873")
    @Test
    void longerNewKeyWithIndentedConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey("x.y", "a.b.c.d.e", null, null, null)),
          yaml(
            """
              x:
                y:
                  child: true
              """,
            """
              a.b.c.d.e:
                child: true
              """
          )
        );
    }

    @Test
    void singleEntry() {
        rewriteRun(
          yaml("management.metrics.binders.files.enabled: true",
            "management.metrics.enable.process.files: true"
          )
        );
    }

    @Test
    void singleGlobEntry() {
        rewriteRun(
          yaml("management.metrics.binders.files.enabled: true",
            "management.metrics.enable.process.files: true"
          )
        );
    }

    @Test
    void nestedEntry() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey(
            "management.metrics.binders.files.enabled",
            "management.metrics.enable.process.files",
            null,
            null,
            null
          )),
          yaml(
            """
                  unrelated.property: true
                  management.metrics:
                      binders:
                          jvm.enabled: true
                          files.enabled: true
              """,
            """
                  unrelated.property: true
                  management.metrics:
                      binders:
                          jvm.enabled: true
                      enable.process.files: true
              """
          )
        );
    }

    @Test
    void nestedEntryEmptyPartialPathRemoved() {
        rewriteRun(
          yaml(
            """
                  unrelated.property: true
                  management.metrics:
                      binders:
                          files.enabled: true
              """,
            """
                  unrelated.property: true
                  management.metrics:
                      enable.process.files: true
              """
          )
        );
    }

    @Nested
    class AvoidsRegenerativeChangesTest implements RewriteTest {
        @Test
        void indentedProperty() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("a.b.c", "a.b.c.d", null, null, null)),
              yaml(
                """
                  a:
                    b:
                      c:
                        d: true
                  """
              )
            );
        }

        @Test
        void dotSeparatedPropertyEqualToNewPropertyKey() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("a.b.c", "a.b.c.d", null, null, null)),
              yaml("a.b.c.d: true")
            );
        }

        @Test
        void dotSeparatedPropertyIncludingNewPropertyKey() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("a.b.c", "a.b.c.d", null, null, null)),
              yaml("a.b.c.d.x: true")
            );
        }

        @Test
        @Issue("https://github.com/openrewrite/rewrite/issues/1114")
        void changePathToOnePathLonger() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("a.b.c", "a.b.c.d", null, null, null)),
              yaml("a.b.c: true",
                "a.b.c.d: true"
              )
            );
        }

        @Test
        void changePathToOnePathShorter() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("a.b.c.d", "a.b.c", null, null, null)),
              yaml("a.b.c.d: true",
                "a.b.c: true"
              )
            );
        }

        @Test
        void changeOnlyMatchingFile() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey(
                "management.metrics.binders.files.enabled",
                "management.metrics.enable.process.files",
                null,
                "**/a.yml",
                null
              )),
              yaml(
                "management.metrics.binders.files.enabled: true",
                "management.metrics.enable.process.files: true",
                spec -> spec.path("a.yml")
              ),
              yaml(
                "management.metrics.binders.files.enabled: true",
                spec -> spec.path("b.yml")
              )
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "acme.my-project.person.first-name",
      "acme.myProject.person.firstName",
      "acme.my_project.person.first_name"
    })
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void relaxedBinding(String propertyKey) {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey(propertyKey, "acme.my-project.person.changed-first-name-key", true, null, null)),
          yaml(
            """
                  unrelated.root: true
                  acme.my-project:
                      unrelated: true
                      person:
                          unrelated: true
                          first-name: example
              """,
            """
                  unrelated.root: true
                  acme.my-project:
                      unrelated: true
                      person:
                          unrelated: true
                          changed-first-name-key: example
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void exactMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey(
            "acme.my-project.person.first-name",
            "acme.my-project.person.changed-first-name-key",
            false,
            null,
            null
          )),
          yaml(
            """
              acme.myProject.person.firstName: example
              acme.my_project.person.first_name: example
              acme.my-project.person.first-name: example
              """,
            """
              acme.myProject.person.firstName: example
              acme.my_project.person.first_name: example
              acme.my-project.person.changed-first-name-key: example
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1249")
    @Test
    void updateKeyAndDoesNotMergeToSibling() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey(
            "i",
            "a.b.c",
            false,
            null,
            null
          )),
          yaml(
            """
                  a:
                    b:
                      f0: v0
                      f1: v1
                  i:
                    f0: v0
                    f1: v1
              """,
            """
                  a:
                    b:
                      f0: v0
                      f1: v1
                  a.b.c:
                    f0: v0
                    f1: v1
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1249")
    @Test
    void updateKeyAndDoesNotMergeToSiblingWithCoalescedProperty() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey(
            "old-property",
            "new-property.sub-property.super-sub",
            true,
            null,
            null
          )),
          yaml(
            """
                  newProperty.subProperty:
                      superSub:
                        f0: v0
                        f1: v1
                  oldProperty:
                    f0: v0
                    f1: v1
              """,
            """
                  newProperty.subProperty:
                      superSub:
                        f0: v0
                        f1: v1
                  new-property.sub-property.super-sub:
                    f0: v0
                    f1: v1
              """
          )
        );
    }

    @Test
    void doesNotChangeKeyWithSequenceInPath() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey(
            "a.b.c.a0",
            "a.b.a0",
            true,
            null,
            null
          )),
          yaml(
            """
                  a:
                    b:
                      c:
                        - a0: x
                          a1: 'y'
                        - aa1: x
                          a1: 'y'
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/434")
    void doesNotChangePropertyOrdering() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey(
            "description",
            "newDescription",
            false,
            null,
            null
          )),
          yaml(
            """
              id: something
              description: desc
              other: whatever
              """,
            """
              id: something
              newDescription: desc
              other: whatever
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1744")
    @Test
    void updatePropertyWithMapping() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey("app.foo.change.from", "app.bar.change.to", null, null, null)),
          yaml(
            """
                  app:
                    foo.change.from: hi
                    bar:
                      other:
                        property: bye
              """,
            """
                  app:
                    bar.change.to: hi
                    bar:
                      other:
                        property: bye
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1841")
    void doesNotReformatUnrelatedProperties() {
        rewriteRun(
          yaml(
            """
                  unrelated:
                    property: true
                  management.metrics:
                    binders.files.enabled: true
                  other:
                    property: true
              """,
            """
                  unrelated:
                    property: true
                  management.metrics:
                    enable.process.files: true
                  other:
                    property: true
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1841")
    void relocatesPropertyIfNothingElseInFamily() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey("a.b.c", "x.y.z", true, null, null)),
          yaml(
            """
                  a:
                    b:
                      c: abc
                  something:
                    else: qwe
              """,
            """
              something:
                else: qwe
              x.y.z: abc
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2016")
    void relocatesPropertyWithSamePrefix() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey(
            "spring.elasticsearch.rest.sniffer.interval",
            "spring.elasticsearch.restclient.sniffer.interval",
            true,
            null,
            null
          )),
          yaml(
            """
                  spring:
                    elasticsearch:
                      rest:
                        sniffer:
                          interval: 1
              """,
            """
                  spring:
                    elasticsearch:
                        restclient.sniffer.interval: 1
              """
          )
        );
    }

    @Nested
    @Issue("https://github.com/openrewrite/rewrite-spring/issues/189")
    class WhenOldPropertyKeyIsPrefixOfDotSeparatedKeyTest implements RewriteTest {
        @Test
        void scalarValue() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, null)),
              yaml(
                """
                  spring.profiles.group.prod: proddb,prodmq,prodmetrics
                  """,
                """
                  spring.config.activate.on-profile.group.prod: proddb,prodmq,prodmetrics
                  """
              )
            );
        }

        @Test
        void mappingValue() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, null)),
              yaml(
                """
                      spring.profiles.group:
                        prod: proddb,prodmq,prodmetrics
                  """,
                """
                      spring.config.activate.on-profile.group:
                        prod: proddb,prodmq,prodmetrics
                  """
              )
            );
        }

        @Test
        void matchSplitAcrossParentEntries() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, null)),
              yaml(
                """
                      spring:
                        profiles.group:
                          prod: proddb,prodmq,prodmetrics
                  """,
                """
                      spring:
                        config.activate.on-profile.group:
                          prod: proddb,prodmq,prodmetrics
                  """
              )
            );
        }
    }

    @Nested
    @Issue("https://github.com/openrewrite/rewrite-spring/issues/189")
    class ExceptTest implements RewriteTest {

        @Nested
        class DotAndIndentCombinationsTest implements RewriteTest {
            @Test
            void dotDotDot() {
                rewriteRun(
                  spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
                  yaml(
                    """
                      spring.profiles.group.prod: proddb,prodmq,prodmetrics
                      """
                  )
                );
            }

            @Test
            void dotDotIndent() {
                rewriteRun(
                  spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
                  yaml(
                    """
                          spring.profiles.group:
                            prod: proddb,prodmq,prodmetrics
                      """
                  )
                );
            }

            @Test
            void dotIndentDot() {
                rewriteRun(
                  spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
                  yaml(
                    """
                          spring.profiles:
                            group.prod: proddb,prodmq,prodmetrics
                      """
                  )
                );
            }

            @Test
            void dotIndentIndent() {
                rewriteRun(
                  spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
                  yaml(
                    """
                          spring.profiles:
                            group:
                              prod: proddb,prodmq,prodmetrics
                      """
                  )
                );
            }

            @Test
            void indentDotDot() {
                rewriteRun(
                  spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
                  yaml(
                    """
                          spring:
                            profiles.group.prod: proddb,prodmq,prodmetrics
                      """
                  )
                );
            }

            @Test
            void indentDotIndent() {
                rewriteRun(
                  spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
                  yaml(
                    """
                          spring:
                            profiles.group:
                              prod: proddb,prodmq,prodmetrics
                      """
                  )
                );
            }

            @Test
            void indentIndentDot() {
                rewriteRun(
                  spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
                  yaml(
                    """
                          spring:
                            profiles:
                              group.prod: proddb,prodmq,prodmetrics
                      """
                  )
                );
            }

            @Test
            void indentIndentIndent() {
                rewriteRun(
                  spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
                  yaml(
                    """
                          spring:
                            profiles:
                              group:
                                prod: proddb,prodmq,prodmetrics
                      """
                  )
                );
            }
        }

        @Test
        void multipleExcludedEntries() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group", "active", "include"))),
              yaml(
                """
                      spring:
                        profiles:
                          active: allEnvs
                          include: baseProfile
                          foo: bar
                          group:
                            prod: proddb,prodmq,prodmetrics
                  """,
                """
                      spring:
                        profiles:
                          active: allEnvs
                          include: baseProfile
                          group:
                            prod: proddb,prodmq,prodmetrics
                        config.activate.on-profile:
                          foo: bar
                  """
              )
            );
        }

        @Test
        void targetMappingIncludesNonExcludedEntryWithScalarValue() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
              yaml(
                """
                      spring:
                        profiles:
                          foo: bar
                          group:
                            prod: proddb,prodmq,prodmetrics
                  """,
                """
                      spring:
                        profiles:
                          group:
                            prod: proddb,prodmq,prodmetrics
                        config.activate.on-profile:
                          foo: bar
                  """
              )
            );
        }

        @Test
        void targetMappingIncludesNonExcludedEntryWithMappingValue() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
              yaml(
                """
                      spring:
                        profiles:
                          foo:
                            bar: qwe
                          group:
                            prod: proddb,prodmq,prodmetrics
                  """,
                """
                      spring:
                        profiles:
                          group:
                            prod: proddb,prodmq,prodmetrics
                        config.activate.on-profile:
                          foo:
                            bar: qwe
                  """
              )
            );
        }

        @Test
        void targetMappingHasScalarValue() {
            rewriteRun(
              spec -> spec.recipe(new ChangePropertyKey("spring.profiles", "spring.config.activate.on-profile", null, null, List.of("group"))),
              yaml(
                """
                      spring:
                        profiles: foo
                  """,
                """
                      spring:
                        config.activate.on-profile: foo
                  """
              )
            );
        }
    }

    @Test
    void doesNotBreakOnKeysWhichIncludeRegexSpecialCharacters() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey("foo", "foo.bar", null, null, null)),
          yaml(
            """
            swagger:
              paths:
                /api/v1/business-objects/{id}:
                  verb: GET
            """
          )
        );
    }

}
