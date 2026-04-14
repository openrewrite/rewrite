/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.hcl.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class AutodetectTest implements RewriteTest {

    @Test
    void detectsTwoSpaceIndent() {
        rewriteRun(
          withDetectedStyle(TabsAndIndentsStyle.class, style -> {
              assertThat(style.getUseTabCharacter()).isFalse();
              assertThat(style.getIndentSize()).isEqualTo(2);
          }),
          hcl(
            """
            resource "aws_instance" "example" {
              ami           = "abc-123"
              instance_type = "t2.micro"
            }
            """
          )
        );
    }

    @Test
    void detectsFourSpaceIndent() {
        rewriteRun(
          withDetectedStyle(TabsAndIndentsStyle.class, style -> {
              assertThat(style.getUseTabCharacter()).isFalse();
              assertThat(style.getIndentSize()).isEqualTo(4);
          }),
          hcl(
            """
            resource "aws_instance" "example" {
                ami           = "abc-123"
                instance_type = "t2.micro"
            }
            """
          )
        );
    }

    @Test
    void detectsTabIndent() {
        rewriteRun(
          withDetectedStyle(TabsAndIndentsStyle.class, style ->
            assertThat(style.getUseTabCharacter()).isTrue()),
          hcl(
            """
            resource "aws_instance" "example" {
            TABami           = "abc-123"
            TABinstance_type = "t2.micro"
            }
            """.replaceAll("TAB", "\t")
          )
        );
    }

    @Test
    void detectsNestedBlockIndent() {
        rewriteRun(
          withDetectedStyle(TabsAndIndentsStyle.class, style -> {
              assertThat(style.getUseTabCharacter()).isFalse();
              assertThat(style.getIndentSize()).isEqualTo(2);
          }),
          hcl(
            """
            resource "aws_instance" "example" {
              ami           = "abc-123"
              instance_type = "t2.micro"

              provisioner "local-exec" {
                command = "echo hello"
              }

              tags = {
                Name = "example"
              }
            }
            """
          )
        );
    }

    @Test
    void detectsIndentAcrossMultipleFiles() {
        rewriteRun(
          withDetectedStyle(TabsAndIndentsStyle.class, style -> {
              assertThat(style.getUseTabCharacter()).isFalse();
              assertThat(style.getIndentSize()).isEqualTo(2);
          }),
          hcl(
            """
            resource "aws_instance" "a" {
              ami = "abc-123"
            }
            """
          ),
          hcl(
            """
            resource "aws_instance" "b" {
              ami = "def-456"
              instance_type = "t2.micro"
            }
            """
          )
        );
    }

    @Test
    void spacesWinOverTabsWhenMajority() {
        rewriteRun(
          withDetectedStyle(TabsAndIndentsStyle.class, style ->
            assertThat(style.getUseTabCharacter()).isFalse()),
          hcl(
            """
            resource "aws_instance" "a" {
              ami = "abc-123"
              instance_type = "t2.micro"
            }
            """
          ),
          hcl(
            """
            resource "aws_instance" "b" {
              ami = "def-456"
            }
            """
          ),
          hcl(
            """
            resource "aws_instance" "c" {
            TABami = "ghi-789"
            }
            """.replaceAll("TAB", "\t")
          )
        );
    }

    @Test
    void tabsWinOverSpacesWhenMajority() {
        rewriteRun(
          withDetectedStyle(TabsAndIndentsStyle.class, style ->
            assertThat(style.getUseTabCharacter()).isTrue()),
          hcl(
            """
            resource "aws_instance" "a" {
            TABami = "abc-123"
            TABinstance_type = "t2.micro"
            }
            """.replaceAll("TAB", "\t")
          ),
          hcl(
            """
            resource "aws_instance" "b" {
            TABami = "def-456"
            }
            """.replaceAll("TAB", "\t")
          ),
          hcl(
            """
            resource "aws_instance" "c" {
              ami = "ghi-789"
            }
            """
          )
        );
    }

    @Test
    void defaultsToTwoSpaceWhenNoIndentsDetected() {
        rewriteRun(
          withDetectedStyle(TabsAndIndentsStyle.class, style -> {
              assertThat(style.getUseTabCharacter()).isFalse();
              assertThat(style.getIndentSize()).isEqualTo(TabsAndIndentsStyle.DEFAULT.getIndentSize());
          }),
          hcl(
            """
            variable "name" {}
            """
          )
        );
    }

    @Test
    void detectsLFLineEndings() {
        rewriteRun(
          withDetectedStyle(GeneralFormatStyle.class, style ->
            assertThat(style.isUseCRLFNewLines()).isFalse()),
          hcl(
            """
            resource "aws_instance" "example" {
              ami = "abc-123"
            }
            """
          )
        );
    }

    @Test
    void detectsCRLFLineEndings() {
        rewriteRun(
          withDetectedStyle(GeneralFormatStyle.class, style ->
            assertThat(style.isUseCRLFNewLines()).isTrue()),
          hcl(
            "resource \"aws_instance\" \"example\" {\r\n  ami = \"abc-123\"\r\n}\r\n"
          )
        );
    }

    private static <S extends Style> Consumer<RecipeSpec> withDetectedStyle(Class<S> styleClass, Consumer<S> fn) {
        return spec -> spec.beforeRecipe(sources -> {
            Autodetect.Detector detector = Autodetect.detector();
            sources.forEach(detector::sample);

            @SuppressWarnings("unchecked")
            var foundStyle = (S) detector.build().getStyles().stream()
              .filter(styleClass::isInstance)
              .findAny().orElseThrow();
            fn.accept(foundStyle);
        });
    }
}
