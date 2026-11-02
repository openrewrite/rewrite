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
package org.openrewrite.hcl;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class JsonPathMatcherTest implements RewriteTest {

    private boolean anyBlockMatch(Hcl hcl, JsonPathMatcher matcher) {
        var matches = new AtomicBoolean(false);
        new HclVisitor<Integer>() {
            @Override
            public Hcl visitBlock(Hcl.Block block, Integer p) {
                var b = super.visitBlock(block, p);
                matches.set(matcher.matches(getCursor()) || matches.get());
                return b;
            }
        }.visit(hcl, 0);
        return matches.get();
    }

    @Test
    void match() {
        rewriteRun(
          hcl(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
                somethingElse {
                }
                attr = 1
              }
              """,
            spec ->
              spec.beforeRecipe(configFile -> {
                  assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.key_vault"))).isTrue();
                  assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.dne"))).isFalse();
              })
          )
        );
    }

    @Test
    void binaryExpression() {
        rewriteRun(
          hcl(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
              }
              """,
            spec -> spec.beforeRecipe(configFile -> {
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.key_vault[?(@.purge_soft_delete_on_destroy == true)]"))).isTrue();
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.key_vault[?(@.purge_soft_delete_on_destroy == false)]"))).isFalse();
            })
          )
        );
    }

    @Test
    void unaryExpression() {
        rewriteRun(
          hcl(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
              }
              """,
            spec -> spec.beforeRecipe(configFile -> {
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.key_vault[?(@.purge_soft_delete_on_destroy)]"))).isTrue();
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.key_vault[?(@.no_match)]"))).isFalse();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2197")
    @Test
    void unaryExpressionOnBlockName() {
        rewriteRun(
          hcl(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
                somethingElse {
                }
                attr = 1
              }
              """,
            spec -> spec.beforeRecipe(configFile -> {
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.*[?(@.features)]"))).isTrue();
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.*[?(@.no_match)]"))).isFalse();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2198")
    @Test
    void matchPropertyFromWildCardOnBlock() {
        rewriteRun(
          hcl(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
                somethingElse {
                }
                attr = 1
              }
              """,
            spec -> spec.beforeRecipe(configFile -> {
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.*.features"))).isTrue();
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.*.no_match"))).isFalse();
            })
          )
        );
    }

    @Test
    void negateUnaryExpression() {
        rewriteRun(
          hcl(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
              }
              """,
            spec -> spec.beforeRecipe(configFile -> {
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.key_vault[?(!@.no_match)]"))).isTrue();
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.key_vault[?(!@.purge_soft_delete_on_destroy)]"))).isFalse();
            })
          )
        );
    }

    @Test
    void negateBinaryExpression() {
        rewriteRun(
          hcl(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
              }
              """,
            spec -> spec.beforeRecipe(configFile -> {
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.key_vault[?(!(@.purge_soft_delete_on_destroy == false))]"))).isTrue();
                assertThat(anyBlockMatch(configFile, new JsonPathMatcher("$.provider.features.key_vault[?(!(@.purge_soft_delete_on_destroy == true))]"))).isFalse();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2198")
    @Test
    void matchParentBlockWithWildCard() {
        assertMatched(
          List.of(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
                somethingElse {
                }
                attr = 1
              }
              """
          ),
          List.of(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
                somethingElse {
                }
                attr = 1
              }
              """.trim()
          ),
          "$.*",
          false
        );
    }

    private static final String RESOURCES = """
      resource "aws_db_instance" "primary" {
        engine_version = "5.7"
        multi_az       = true
      }
      resource "aws_db_instance" "replica" {
        engine_version = "8.0"
        multi_az       = false
      }
      resource "aws_rds_cluster" "cluster" {
        engine_version = "5.6"
        multi_az       = true
      }
      """;

    @Test
    void matchByTypeLabel() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"5.7\"", "engine_version = \"8.0\""),
          "$.resource.aws_db_instance.engine_version",
          false
        );
    }

    @Test
    void matchByTypeAndNameLabels() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"8.0\""),
          "$.resource.aws_db_instance.replica.engine_version",
          false
        );
    }

    @Test
    void matchByWildcardNameLabel() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"5.7\"", "engine_version = \"8.0\""),
          "$.resource.aws_db_instance.*.engine_version",
          false
        );
    }

    @Test
    void pathEndingAtLabelMatchesBlock() {
        assertMatched(
          List.of(RESOURCES),
          List.of(
            """
              resource "aws_rds_cluster" "cluster" {
                engine_version = "5.6"
                multi_az       = true
              }
              """.trim()
          ),
          "$.resource.aws_rds_cluster",
          false
        );
        assertMatched(
          List.of(RESOURCES),
          List.of(
            """
              resource "aws_db_instance" "primary" {
                engine_version = "5.7"
                multi_az       = true
              }
              """.trim()
          ),
          "$.resource.aws_db_instance.primary",
          false
        );
    }

    @Test
    void pathWithoutLabelsStillMatchesAllBlocksOfType() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"5.7\"", "engine_version = \"8.0\"", "engine_version = \"5.6\""),
          "$.resource.engine_version",
          false
        );
    }

    @Test
    void matchByLabelsIntoNestedBlock() {
        assertMatched(
          List.of(
            """
              resource "aws_instance" "web" {
                ebs_block_device {
                  volume_size = 10
                }
              }
              resource "aws_instance" "db" {
                ebs_block_device {
                  volume_size = 20
                }
              }
              """
          ),
          List.of("volume_size = 10"),
          "$.resource.aws_instance.web.ebs_block_device.volume_size",
          false
        );
    }

    @Test
    void matchByLabelsOfNestedBlock() {
        assertMatched(
          List.of(
            """
              resource "aws_security_group" "sg" {
                dynamic "ingress" {
                  content {
                    from_port = 80
                  }
                }
                dynamic "egress" {
                  content {
                    from_port = 0
                  }
                }
              }
              """
          ),
          List.of("from_port = 80"),
          "$.resource.aws_security_group.sg.dynamic.ingress.content.from_port",
          false
        );
    }

    @Test
    void matchByUnquotedLabels() {
        assertMatched(
          List.of(
            """
              resource aws_db_instance primary {
                engine_version = "5.7"
              }
              resource aws_rds_cluster cluster {
                engine_version = "5.6"
              }
              """
          ),
          List.of("engine_version = \"5.7\""),
          "$.resource.aws_db_instance.primary.engine_version",
          false
        );
    }

    @Test
    void matchByLabelInBracketNotation() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"5.7\"", "engine_version = \"8.0\""),
          "$.resource['aws_db_instance'].engine_version",
          false
        );
    }

    @Test
    void filterAfterLabels() {
        assertMatched(
          List.of(RESOURCES),
          List.of(
            """
              resource "aws_db_instance" "primary" {
                engine_version = "5.7"
                multi_az       = true
              }
              """.trim()
          ),
          "$.resource.aws_db_instance[?(@.multi_az == true)]",
          false
        );
    }

    @Test
    void bodyKeyTakesPrecedenceOverLabel() {
        assertMatched(
          List.of(
            """
              resource "aws_instance" "settings" {
                settings {
                  size = 1
                }
                size = 2
              }
              """
          ),
          List.of("size = 1"),
          "$.resource.aws_instance.settings.size",
          false
        );
    }

    @Test
    void attributeNamedLikeLabelStillMatchesAttribute() {
        assertMatched(
          List.of(
            """
              variable "default" {
                default = 1
              }
              variable "other" {
                default = 2
              }
              """
          ),
          List.of("default = 1", "default = 2"),
          "$.variable.default",
          false
        );
    }

    @Test
    void labelsCannotBeSkipped() {
        assertMatched(
          List.of(RESOURCES),
          List.of(),
          "$.resource.replica.engine_version",
          false
        );
    }

    @Test
    void noMatchOnWrongLabel() {
        assertMatched(
          List.of(RESOURCES),
          List.of(),
          "$.resource.aws_db_instance.dne.engine_version",
          false
        );
        assertMatched(
          List.of(RESOURCES),
          List.of(),
          "$.resource.aws_s3_bucket.engine_version",
          false
        );
    }

    private static final String SECURITY_GROUPS = """
      resource "aws_security_group" "web" {
        dynamic "ingress" {
          content {
            from_port = 80
          }
        }
        dynamic "egress" {
          content {
            from_port = 0
          }
        }
      }
      """;

    @Test
    void recursiveDescentMatchesTypeLabel() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"5.7\"", "engine_version = \"8.0\""),
          "$..aws_db_instance.engine_version",
          false
        );
    }

    @Test
    void recursiveDescentMatchesNameLabel() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"8.0\""),
          "$..replica.engine_version",
          false
        );
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"8.0\""),
          "$..['replica'].engine_version",
          false
        );
    }

    @Test
    void recursiveDescentMatchesLabelOfNestedBlock() {
        assertMatched(
          List.of(SECURITY_GROUPS),
          List.of("from_port = 80"),
          "$..ingress.content.from_port",
          false
        );
    }

    @Test
    void scopedRecursiveDescentMatchesLabel() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"8.0\""),
          "$.resource..replica.engine_version",
          false
        );
        assertMatched(
          List.of(SECURITY_GROUPS),
          List.of("from_port = 80"),
          "$.resource..ingress.content.from_port",
          false
        );
    }

    @Test
    void scopedRecursiveDescentEndingAtLabelMatchesBlock() {
        assertMatched(
          List.of(SECURITY_GROUPS),
          List.of(
            """
              dynamic "ingress" {
                content {
                  from_port = 80
                }
              }
              """.trim()
          ),
          "$.resource..ingress",
          false
        );
    }

    @Test
    void recursiveDescentPrefersBodyKeyOverLabel() {
        assertMatched(
          List.of(
            """
              resource "aws_instance" "settings" {
                settings {
                  size = 1
                }
                size = 2
              }
              """
          ),
          List.of("size = 1"),
          "$..settings.size",
          false
        );
    }

    private static final String TFVARS_AND_RESOURCES = """
      region = "a"
      resource "x" "y" {
        engine = "mysql"
        v = 1
      }
      resource "x" "z" {
        engine = "postgres"
        v = 2
      }
      """;

    @Test
    void matchTopLevelAttribute() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of("region = \"a\""),
          "$.region",
          false
        );
    }

    @Test
    void filterOnNumericAttributeSelectsBlock() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of(
            """
              resource "x" "y" {
                engine = "mysql"
                v = 1
              }
              """.trim()
          ),
          "$.resource[?(@.v == 1)]",
          false
        );
    }

    @Test
    void filterOnNumericAttributeFollowedByChild() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of("v = 2"),
          "$.resource[?(@.v == 2)].v",
          false
        );
    }

    @Test
    void filterOnQuotedStringFollowedByChild() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of("v = 1"),
          "$.resource[?(@.engine == 'mysql')].v",
          false
        );
    }

    @Test
    void filterOnQuotedStringSelectsBlock() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of(
            """
              resource "x" "z" {
                engine = "postgres"
                v = 2
              }
              """.trim()
          ),
          "$.resource[?(@.engine == 'postgres')]",
          false
        );
    }

    @Test
    void filterComparesOnlyTheNamedAttribute() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of(),
          "$.resource[?(@.v == 'mysql')]",
          false
        );
    }

    @Test
    void notEqualFilterSelectsOtherBlocks() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of("v = 2"),
          "$.resource[?(@.engine != 'mysql')].v",
          false
        );
    }

    @Test
    void regexFilterOnQuotedString() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of("v = 2"),
          "$.resource[?(@.engine =~ 'post.*')].v",
          false
        );
    }

    @Test
    void containsFilterOnQuotedString() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of("v = 1"),
          "$.resource[?(@.engine contains 'my')].v",
          false
        );
    }

    @Test
    void negatedFilterOnBlock() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of("v = 2"),
          "$.resource[?(!(@.engine == 'mysql'))].v",
          false
        );
    }

    @Test
    void logicalFilterOnBlock() {
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of("engine = \"postgres\""),
          "$.resource[?(@.engine == 'postgres' && @.v == 2)].engine",
          false
        );
        assertMatched(
          List.of(TFVARS_AND_RESOURCES),
          List.of(),
          "$.resource[?(@.engine == 'postgres' && @.v == 1)].engine",
          false
        );
    }

    @Test
    void filterAfterLabelsFollowedByChild() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"8.0\""),
          "$.resource.aws_db_instance[?(@.multi_az == false)].engine_version",
          false
        );
    }

    @Test
    void filterOnNestedBlockPresence() {
        assertMatched(
          List.of(
            """
              resource "aws_instance" "web" {
                ebs_block_device {
                  volume_size = 10
                }
                ami = "a"
              }
              resource "aws_instance" "db" {
                ami = "b"
              }
              """
          ),
          List.of("ami = \"a\""),
          "$.resource[?(@.ebs_block_device)].ami",
          false
        );
    }

    @Test
    void recursiveDescentFollowedByFilter() {
        assertMatched(
          List.of(RESOURCES),
          List.of("engine_version = \"5.7\""),
          "$..aws_db_instance[?(@.multi_az == true)].engine_version",
          false
        );
    }

    @Test
    void recursiveDescentMatchesNestedBlock() {
        assertMatched(
          List.of(
            """
              resource "x" "y" {
                versioning {
                  enabled = true
                }
                lifecycle_rule {
                  versioning {
                    enabled = false
                  }
                }
              }
              """
          ),
          List.of(
            """
              versioning {
                enabled = true
              }
              """.trim(),
            """
              versioning {
                enabled = false
              }
              """.trim()
          ),
          "$..versioning",
          false
        );
    }

    @Test
    void recursiveDescentMatchesSingleLineNestedBlock() {
        assertMatched(
          List.of("resource \"x\" \"y\" { versioning { enabled = true } }"),
          List.of("versioning { enabled = true }"),
          "$..versioning",
          false
        );
    }

    @Disabled("Test enables a simple way to test JsonPatchMatches on HCL.")
    @Test
    void findJsonPathMatches() {
        assertMatched(
          List.of(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
                somethingElse {
                }
                attr = 1
              }
              """
          ),
          List.of(
            """
              """
          ),
          "$.*",
          true
        );
    }

    @SuppressWarnings("SameParameterValue")
    private void assertMatched(List<String> before, List<String> after, String jsonPath,
                               boolean printMatches) {
        var results = visit(before, jsonPath, printMatches);
        assertThat(results).hasSize(after.size());
        for (int i = 0; i < results.size(); i++) {
            assertThat(results.get(i)).isEqualTo(after.get(i));
        }
    }

    private List<String> visit(List<String> before, String jsonPath, boolean printMatches) {
        var ctx = new InMemoryExecutionContext();
        var documents = HclParser.builder().build().parse(ctx, before.toArray(new String[0]))
          .map(Hcl.ConfigFile.class::cast)
          .toList();
        if (documents.isEmpty()) {
            return emptyList();
        }
        var matcher = new JsonPathMatcher(jsonPath);
        var results = new ArrayList<String>();
        for (Hcl.ConfigFile document : documents) {
            new HclIsoVisitor<List<String>>() {
                @Override
                public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, List<String> p) {
                    var a = super.visitAttribute(attribute, p);
                    if (matcher.matches(getCursor())) {
                        var match = a.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return a;
                }

                @Override
                public Hcl.AttributeAccess visitAttributeAccess(Hcl.AttributeAccess attributeAccess, List<String> p) {
                    var a = super.visitAttributeAccess(attributeAccess, p);
                    if (matcher.matches(getCursor())) {
                        var match = a.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return a;
                }

                @Override
                public Hcl.Binary visitBinary(Hcl.Binary binary, List<String> p) {
                    var b = super.visitBinary(binary, p);
                    if (matcher.matches(getCursor())) {
                        var match = b.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return b;
                }

                @Override
                public Hcl.Block visitBlock(Hcl.Block block, List<String> p) {
                    var b = super.visitBlock(block, p);
                    if (matcher.matches(getCursor())) {
                        var match = b.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return b;
                }

                @Override
                public Hcl.Conditional visitConditional(Hcl.Conditional conditional, List<String> p) {
                    var c = super.visitConditional(conditional, p);
                    if (matcher.matches(getCursor())) {
                        var match = c.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return c;
                }

                @Override
                public Hcl.ConfigFile visitConfigFile(Hcl.ConfigFile configFile, List<String> p) {
                    var c = super.visitConfigFile(configFile, p);
                    if (matcher.matches(getCursor())) {
                        var match = c.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return c;
                }

                @Override
                public Hcl.Empty visitEmpty(Hcl.Empty empty, List<String> p) {
                    var e = super.visitEmpty(empty, p);
                    if (matcher.matches(getCursor())) {
                        var match = e.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return e;
                }

                @Override
                public Hcl.ForIntro visitForIntro(Hcl.ForIntro forIntro, List<String> p) {
                    var f = super.visitForIntro(forIntro, p);
                    if (matcher.matches(getCursor())) {
                        var match = f.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return f;
                }

                @Override
                public Hcl.ForObject visitForObject(Hcl.ForObject forObject, List<String> p) {
                    var f = super.visitForObject(forObject, p);
                    if (matcher.matches(getCursor())) {
                        var match = f.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return f;
                }

                @Override
                public Hcl.ForTuple visitForTuple(Hcl.ForTuple forTuple, List<String> p) {
                    var f = super.visitForTuple(forTuple, p);
                    if (matcher.matches(getCursor())) {
                        var match = f.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return f;
                }

                @Override
                public Hcl.FunctionCall visitFunctionCall(Hcl.FunctionCall functionCall, List<String> p) {
                    var f = super.visitFunctionCall(functionCall, p);
                    if (matcher.matches(getCursor())) {
                        var match = f.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return f;
                }

                @Override
                public Hcl.HeredocTemplate visitHeredocTemplate(Hcl.HeredocTemplate heredocTemplate, List<String> p) {
                    var h = super.visitHeredocTemplate(heredocTemplate, p);
                    if (matcher.matches(getCursor())) {
                        var match = h.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return h;
                }

                @Override
                public Hcl.Identifier visitIdentifier(Hcl.Identifier identifier, List<String> p) {
                    var i = super.visitIdentifier(identifier, p);
                    if (matcher.matches(getCursor())) {
                        var match = i.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return i;
                }

                @Override
                public Hcl.Index visitIndex(Hcl.Index index, List<String> p) {
                    var i = super.visitIndex(index, p);
                    if (matcher.matches(getCursor())) {
                        var match = i.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return i;
                }

                @Override
                public Hcl.Index.Position visitIndexPosition(Hcl.Index.Position indexPosition, List<String> p) {
                    var i = super.visitIndexPosition(indexPosition, p);
                    if (matcher.matches(getCursor())) {
                        var match = i.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return i;
                }

                @Override
                public Hcl.Literal visitLiteral(Hcl.Literal literal, List<String> p) {
                    var l = super.visitLiteral(literal, p);
                    if (matcher.matches(getCursor())) {
                        var match = l.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return l;
                }

                @Override
                public Hcl.ObjectValue visitObjectValue(Hcl.ObjectValue objectValue, List<String> p) {
                    var o = super.visitObjectValue(objectValue, p);
                    if (matcher.matches(getCursor())) {
                        var match = o.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return o;
                }

                @Override
                public Hcl.Parentheses visitParentheses(Hcl.Parentheses parentheses, List<String> p) {
                    var pp = super.visitParentheses(parentheses, p);
                    if (matcher.matches(getCursor())) {
                        var match = pp.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return pp;
                }

                @Override
                public Hcl.QuotedTemplate visitQuotedTemplate(Hcl.QuotedTemplate template, List<String> p) {
                    var q = super.visitQuotedTemplate(template, p);
                    if (matcher.matches(getCursor())) {
                        var match = q.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return q;
                }

                @Override
                public Hcl.Splat visitSplat(Hcl.Splat splat, List<String> p) {
                    var s = super.visitSplat(splat, p);
                    if (matcher.matches(getCursor())) {
                        var match = s.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return s;
                }

                @Override
                public Hcl.Splat.Operator visitSplatOperator(Hcl.Splat.Operator splatOperator, List<String> p) {
                    var s = super.visitSplatOperator(splatOperator, p);
                    if (matcher.matches(getCursor())) {
                        var match = s.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return s;
                }

                @Override
                public Hcl.TemplateInterpolation visitTemplateInterpolation(Hcl.TemplateInterpolation template, List<String> p) {
                    var t = super.visitTemplateInterpolation(template, p);
                    if (matcher.matches(getCursor())) {
                        var match = t.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return t;
                }

                @Override
                public Hcl.Tuple visitTuple(Hcl.Tuple tuple, List<String> p) {
                    var t = super.visitTuple(tuple, p);
                    if (matcher.matches(getCursor())) {
                        var match = t.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return t;
                }

                @Override
                public Hcl.Unary visitUnary(Hcl.Unary unary, List<String> p) {
                    var u = super.visitUnary(unary, p);
                    if (matcher.matches(getCursor())) {
                        var match = u.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return u;
                }

                @Override
                public Hcl.VariableExpression visitVariableExpression(Hcl.VariableExpression variableExpression, List<String> p) {
                    var v = super.visitVariableExpression(variableExpression, p);
                    if (matcher.matches(getCursor())) {
                        var match = v.printTrimmed(getCursor().getParentOrThrow());
                        if (printMatches) {
                        }
                        p.add(match);
                    }
                    return v;
                }
            }.visit(document, results);
        }
        return results;
    }
}
