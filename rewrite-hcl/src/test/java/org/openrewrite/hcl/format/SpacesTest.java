/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.hcl.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.hcl.style.SpacesStyle;
import org.openrewrite.hcl.tree.Comment;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class SpacesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Spaces());
    }

    @DocumentExample
    @Test
    void columnarAlignment() {
        rewriteRun(
          hcl(
            """
              resource "aws_ebs_volume" {
                size =1
                encrypted =true
              }
              """,
            """
              resource "aws_ebs_volume" {
                size      = 1
                encrypted = true
              }
              """
          )
        );
    }

    @Test
    void nonColumnarAlignment() {
        rewriteRun(
          spec -> spec.parser(HclParser.builder().styles(spaces(new SpacesStyle(new SpacesStyle.BodyContent(false))))),
          hcl(
            """
              resource "aws_ebs_volume" {
                size      = 1
                encrypted = true
              }
              """,
            """
              resource "aws_ebs_volume" {
                size = 1
                encrypted = true
              }
              """
          )
        );
    }

    @Test
    void blockOpen() {
        rewriteRun(
          hcl(
            """
              resource "aws_ebs_volume"{
                size = 1
              }
              """,
            """
              resource "aws_ebs_volume" {
                size = 1
              }
              """
          )
        );
    }

    @Test
    void attributeGroups() {
        rewriteRun(
          hcl(
            """
              resource "custom_resource" {
                size = 1
                x = 1
                
                longerattribute = "long"
                y = 2
              }
              """,
            """
              resource "custom_resource" {
                size = 1
                x    = 1
                
                longerattribute = "long"
                y               = 2
              }
              """
          )
        );
    }

    @Test
    void attributeGroupMultilineValue() {
        rewriteRun(
          hcl(
            """
              variable myvar {
                description = "Sample Variable"
                type = object({
                  string_var = string
                  string_var_2 = string
                  multiline_var = object({
                    x = string
                    foo = string
                    
                    y = string
                  })
                  another_string_var = string
                  another_string_var_2 = string
                })
              }
              """,
            """ 
              variable myvar {
                description = "Sample Variable"
                type = object({
                  string_var   = string
                  string_var_2 = string
                  multiline_var = object({
                    x   = string
                    foo = string
                    
                    y = string
                  })
                  another_string_var   = string
                  another_string_var_2 = string
                })
              }
              """
          )
        );
    }

    @Test
    void objectAttributeGroupMultilineValue() {
        rewriteRun(
          hcl(
            """
              locals {
                 myvar = {
                   description = "Sample Variable"
                   type = {
                     string_var = "value"
                     string_var_2 = "value"
                     multiline_var = {
                       x = "value"
                       foo = "value"
                    
                       y = "value"
                     }
                     another_string_var = "value"
                     another_string_var_2 = "value"
                  }
                }
              }
              """,
            """ 
              locals {
                 myvar = {
                   description = "Sample Variable"
                   type = {
                     string_var   = "value"
                     string_var_2 = "value"
                     multiline_var = {
                       x   = "value"
                       foo = "value"
                    
                       y = "value"
                     }
                     another_string_var   = "value"
                     another_string_var_2 = "value"
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void noAttributeGroups() {
        rewriteRun(
          hcl(
            """
              resource "custom_resource" {
                size   = 1

                x = 1
                
                longerattribute   = "long"

                y = 2
              }
              """,
            """
              resource "custom_resource" {
                size = 1

                x = 1
                
                longerattribute = "long"

                y = 2
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/974")
    @Test
    void lineHashComment() {
        rewriteRun(
          hcl(
            """
              # a hash comment with inline # or // is still 1 line.
              resource{}
              """,
            """
              # a hash comment with inline # or // is still 1 line.
              resource {}
              """,
            spec -> spec.afterRecipe(c -> {
              assertThat(c.getPrefix().getComments().size()).isEqualTo(1);
              assertThat(c.getPrefix().getComments().getFirst().getStyle()).isEqualTo(Comment.Style.LINE_HASH);
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/974")
    @Test
    void lineSlashComment() {
        rewriteRun(
          hcl(
            """
              // a slash comment with inline # or // is still 1 line.
              resource{}
              """,
            """
              // a slash comment with inline # or // is still 1 line.
              resource {}
              """,
            spec -> spec.afterRecipe(c -> {
              assertThat(c.getPrefix().getComments().size()).isEqualTo(1);
              assertThat(c.getPrefix().getComments().getFirst().getStyle()).isEqualTo(Comment.Style.LINE_SLASH);
            })
          )
        );
    }

    private List<NamedStyles> spaces(SpacesStyle style) {
        return List.of(
          new NamedStyles(
            Tree.randomId(), "test", "test", "test", emptySet(), List.of(style)
          )
        );
    }
}
