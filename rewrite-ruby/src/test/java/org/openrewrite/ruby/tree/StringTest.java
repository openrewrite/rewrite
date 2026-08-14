/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class StringTest implements RewriteTest {

    @Disabled
    @Test
    void emoji() {
        rewriteRun(
          ruby(
            """
              expect { described_class.new("🤷") }
              """
          )
        );
    }

    @Test
    void escapingInsideComplexString() {
        rewriteRun(
          ruby(
            """
              PATTERN_RAW = "\\\\s*(#{OPERATORS})?\\\\s*v?(#{Gem::Version::VERSION_PATTERN})\\\\s*".freeze
              PATTERN = /\\A#{PATTERN_RAW}\\z/
              """
          )
        );
    }

    @Test
    void empty() {
        rewriteRun(
          ruby(
            """
              ''
              """
          )
        );
    }

    @Test
    void string() {
        rewriteRun(
          ruby(
            """
              "The programming language is"
              """
          )
        );
    }

    @Test
    void singleQuoteDelimiter() {
        rewriteRun(
          ruby(
            """
              'The programming language is'
              """
          )
        );
    }

    @Test
    void delimitedString() {
        rewriteRun(
          ruby(
            """
              "#{a1} The programming language is #{a1}"
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Q", "q"})
    void percentDelimitedString(String delim) {
        rewriteRun(
          ruby(
            """
              %%%s[#{a1} The programming language is #{a1}]
              """.formatted(delim)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "i", "x", "m", "u", "e", "s", "n"})
    void regex(String options) {
        rewriteRun(
          ruby(
            """
              /^[yY]/%s
              """.formatted(options)
          )
        );
    }

    @Test
    void regexRDString() {
        rewriteRun(
          ruby(
            """
              %r[^/usr/local/.*]
              """
          )
        );
    }

    @Test
    void regexRDStrings() {
        rewriteRun(
          ruby(
            """
              %r(
                ^/usr/#{folder}/.*    # test
                ^/usr/.*
              )m
              """
          )
        );
    }

    @Test
    void multilineRegexRDString() {
        rewriteRun(
          // note that the literal starts with parentheses, so the parsing code
          // needs to be careful to not interpret those as syntactic parentheses but
          // rather as part of the literal
          ruby(
            """
              %r{
                (((?<!required_)version\\s=\\s*["'].*["'])
                (\\s*source\\s*=\\s*["'](#{registry_host}/)?#{name}["']|\\s*#{name}\\s*=\\s*\\{.*))
              }mx
              """
          )
        );
    }

    @Test
    void multilineRegexRDString2() {
        rewriteRun(
          ruby(
            """
              %r{
                ((a#{name}b)
                (c))
              }mx
              """
          )
        );
    }

    @Test
    void regexDString() {
        rewriteRun(
          ruby(
            """
              /my name is #{name}/o
              """
          )
        );
    }

    @Test
    void backReferences() {
        rewriteRun(
          ruby(
            """
              /h/.match("hello")
              $&
              """
          )
        );
    }

    @Test
    void nthMatch() {
        rewriteRun(
          ruby(
            """
              /h/.match("hello")
              $1
              """
          )
        );
    }

    /**
     * Squiggly "here docs" remove the extra indentation.
     */
    @ParameterizedTest
    @ValueSource(strings = {"<<~", "<<-"})
    void hereDocuments(String startDelim) {
        rewriteRun(
          ruby(
            """
              document = %sHERE.chomp
                  This is a string literal.
                  It has two lines and abruptly ends...
              HERE
                            
              1 and 2
              """.formatted(startDelim)
          )
        );
    }

    @Disabled
    @Test
    void multipleHeredocs() {
        rewriteRun(
          ruby(
            """
              greeting = <<-HERE + <<-THERE + "World"
              Hello
              HERE
              There
              THERE
              """
          )
        );
    }

    @Test
    void xString() {
        rewriteRun(
          ruby(
            """
              `mkdir -p src/main/java`
              """
          )
        );
    }

    @Test
    void xStringPercentDelimited() {
        rewriteRun(
          ruby(
            """
              %x(echo 1)
              """
          )
        );
    }

    @Test
    void nonInterpolableStringArrayLiteral() {
        rewriteRun(
          ruby(
            """
              %w[foo bar baz]
              """
          )
        );
    }

    @Test
    void nonInterpolableStringArrayLiteralMultiline() {
        rewriteRun(
          ruby(
            """
              possible_architectures = %w(
                linux_amd64
                darwin_amd64
                windows_amd64
                darwin_arm64
                linux_arm64
              )
              puts "hello"
              """
          )
        );
    }

    @Test
    void interpolableStringArrayLiteral() {
        rewriteRun(
          ruby(
            """
              %W(#{1 + 1})
              """
          )
        );
    }

    @Test
    void characterLiteral() {
        rewriteRun(
          ruby(
            """
              ?A
              """
          )
        );
    }

    /**
     * Just to ensure we are grouping StrNode/DNode children of ArrayNode together correctly.
     */
    @Test
    void stringArray() {
        rewriteRun(
          ruby(
            """
              ["a", "b", "c"]
              """
          )
        );
    }

    @Test
    void implicitConcatenation() {
        rewriteRun(
          ruby(
            """
              DIGITS = 'AB' \\
                       'ab' \\
                       '01'
              """
          ),
          ruby(
            """
              DIGITS = 'AB' 'ab' '01'
              """
          )
        );
    }

    @Test
    void implicitConcatenationWithNewlines() {
        rewriteRun(
          ruby(
            """
              eq("always-auth = true\\n" \\
                 "strict-ssl = true\\n" \\
                 "//npm.fury.io/dependabot/:_authToken=secret_token\\n" \\
                 "registry = https://npm.fury.io/dependabot\\n" \\
                 "//npm.fury.io/dependabot/:_authToken=my_token\\n" \\
                 "always-auth = true\\n")
                 
              puts "hello"
              """
          )
        );
    }

    @Test
    void implicitConcatenationOfDStrings() {
        rewriteRun(
          ruby(
            """
              a = "Don't know how to update a #{new_req[:source][:type]} " \\
                   "declaration" \\
                   "!"
                            
              puts "hello"
              """
          )
        );
    }

    @Test
    void implicitConcatenationNotLastStatement() {
        rewriteRun(
          ruby(
            """
              expect(
                "This terraform provider syntax is now deprecated.\\n" \\
                "See https://www.terraform.io/docs/language/providers/requirements.html " \\
                "for the new Terraform v0.13+ provider syntax!"
              )
              """
          )
        );
    }

    @Test
    void heredocWithClosingParen() {
        rewriteRun(
          ruby(
            """
              expect(<<~HCL)
                module "s3-webapp"
              HCL
                            
              a = "hello world"
              """
          )
        );
    }

    @Test
    void heredocStartingOnNextLine() {
        rewriteRun(
          ruby(
            """
              expect(updated_file.content).to include(
                <<~DEP
                  module "origin_label" {
                    source     = "git::https://github.com/cloudposse/terraform-null-label.git?ref=tags/0.4.1"
                DEP
              )
                            
              1 and 2
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      \\",escape a character
      \\a,ring the console bell
      \\b,backspace
      \\e,escape
      \\f,form feed
      \\n,newline
      \\r,carriage return
      \\s,space
      \\t,tab
      \\u0000,Unicode character
      \\u{0000},Unicode character
      \\v,vertical tab
      \\000,three octal digits between 000 and 377
      \\00,two octal digits between 00 and 77
      \\0,one octal digit between 0 and 7
      \\x00,two hexadecimal digits between 00 and FF
      \\x0,hex digit between 0 and 7
      \\C-A,way to specify ctrl+A
      \\cA,alternative way to specify ctrl+A
      \\M-A,way to specify a meta character which is not part of the ASCII table
      \\r,carriage return
      """
    )
    void escaping(String escapeSequence) {
        rewriteRun(
          ruby(
            """
              a = "%s"
              1 and 2
              """.formatted(escapeSequence)
          )
        );
    }
}
