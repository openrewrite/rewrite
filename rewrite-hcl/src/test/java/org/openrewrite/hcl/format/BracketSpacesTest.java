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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class BracketSpacesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AutoFormat());
    }

    @Test
    void blockBraces() {
        rewriteRun(
          hcl(
            """
              resource "aws_ebs_volume"    {    size      = 1
              encrypted = true   }
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
    void objectValues() {
        rewriteRun(
          hcl(
            """
              resource "aws_ebs_volume" {
              foo = { bar = "bar"
              baz = "baz"
              }
              }
              """,
            """
              resource "aws_ebs_volume" {
                foo = {
                  bar = "bar"
                  baz = "baz"
                }
              }
              """
          )
        );
    }


    @Test
    void objectValueBracesMulti() {
        rewriteRun(
          hcl(
            """
              resource "aws_ebs_volume" {
                foo = { bar = "bar"
                      bazzz = "bazzz"}
              }
              """,
            """
              resource "aws_ebs_volume" {
                foo = {
                  bar   = "bar"
                  bazzz = "bazzz"
                }
              }
              """
          )
        );
    }

    @Test
    void objectValueBracesSingle() {
        rewriteRun(
          hcl(
            """
              resource "aws_ebs_volume" {
                foo = {    bar = "bar"   ,     baz = "baz"     }
              }
              """,
            """
              resource "aws_ebs_volume" {
                foo = { bar = "bar", baz = "baz" }
              }
              """
          )
        );
    }

    @Test
    void objectValueBracesComplex() {
        rewriteRun(
          hcl(
            """
              resource "aws_ebs_volume" {
                multiline_var = { xxx = "xxx"
                      yyy = "yyy"
                      zzz = {    x = "x"   ,   y = "y"   }
                      nested = {                    xx = "xx"
                      yy = "yy"                    }
               }
              }
              """,
            """
              resource "aws_ebs_volume" {
                multiline_var = {
                  xxx = "xxx"
                  yyy = "yyy"
                  zzz = { x = "x", y = "y" }
                  nested = {
                    xx = "xx"
                    yy = "yy"
                  }
                }
              }
              """
          )
        );
    }
}
