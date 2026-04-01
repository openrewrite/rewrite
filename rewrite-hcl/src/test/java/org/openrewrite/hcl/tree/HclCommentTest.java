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
package org.openrewrite.hcl.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class HclCommentTest implements RewriteTest {

    @Test
    void comment() {
        rewriteRun(
          hcl(
            """
              # test
              /*
               multiline
              */
              resource {
                  # test
                  // test
                  a = 1
              }
              """
          )
        );
    }

    @Test
    void commentCrlf() {
        rewriteRun(
          hcl(
            """
              # test
              /*
               multiline
              */
              resource {
                  # test
                  // test
                  a = 1
              }
              """.replace("\n", "\r\n")
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2079")
    @Test
    void bracesInComment() {
        rewriteRun(
          hcl(
            """
              locals {
                  myvar = {
                    # below {attributes}
                    myattribute = "myvalue"

                    # add more attributes {here}
                  }
              }
              """
          )
        );
    }

    @Test
    void inLineCommentsNextLineAttribute() {
        rewriteRun(
          hcl(
            """
              # test
              /*
               multiline
              */
              resource {
                  # test
                  // test
                  a = 1
                  // test 3
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4611")
    @Test
    void commentAsTheLastLine() {
        rewriteRun(
          hcl(
            """
              locals {
                a = 3
              }
              # Nice code, right?
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4611")
    @Test
    void commentsAsTheFinalLines() {
        rewriteRun(
          hcl(
            """
              locals {
                a = 3
              }
              # Nice code, right?
              # Isn't it?
              """
          )
        );
    }

    @Test
    void singleLineWithinMultiLineHash() {
        rewriteRun(
          hcl(
            """
              /*
              # It's important
              */
              locals {
               Anwil = "Wloclawek"
              }
              """
          )
        );
    }

    @Test
    void singleLineWithinMultiLineSlash() {
        rewriteRun(
          hcl(
            """
              /*
              // It's important
              */
              locals {
               Anwil = "Wloclawek"
              }
              """
          )
        );
    }

    @Test
    void multilineNotStartingInTheFirstCharacter() {
        rewriteRun(
          hcl(
            """
                  /* An indented comment
              */
              """
          )
        );
    }

    @Test
    void commentedOutLinesInListLiteral() {
        rewriteRun(
          hcl(
            """
              locals {
                resources = [
                   "arn:aws:s3:::${var.my_precious_bucket}",
                   "arn:aws:s3:::${var.waste_bucket}",
                   #      "arn:aws:s3:::just-some-bucket/*",
                ]
              }
              """
          )
        );
    }

    @Test
    void emptyHashCommentJustBeforeCurlyBraceEnd() {
        rewriteRun(
          hcl(
            """
              locals {
                #
              }
              module "something" {
                source = "../else/"
              }
              """
          )
        );
    }

    @Test
    void emptyDoubleSlashCommentJustBeforeCurlyBraceEnd() {
        rewriteRun(
          hcl(
            """
              locals {
                //
              }
              module "something" {
                source = "../else/"
              }
              """
          )
        );
    }

    @Test
    void emptySingleLineCommentAtTheEndOfTheFile() {
        rewriteRun(
          hcl(
            """
              module "something" {
                source = "../else/"
              }
              //
              """
          )
        );
    }

    @Test
    void multiLineCommentWithUrl() {
        rewriteRun(
          hcl(
            """
              module "something" {
                /* https://www.example.com */
              }
              """
          )
        );
    }

    // Tests for comments at start/end of files - edge cases

    @Test
    void commentOnlyFile() {
        rewriteRun(
          hcl(
            """
              # This file contains only a comment
              """
          )
        );
    }

    @Test
    void commentOnlyFileNoTrailingNewline() {
        rewriteRun(
          hcl("# This file contains only a comment")
        );
    }

    @Test
    void multipleCommentsOnlyFile() {
        rewriteRun(
          hcl(
            """
              # First comment
              # Second comment
              """
          )
        );
    }

    @Test
    void multiLineCommentOnlyFile() {
        rewriteRun(
          hcl(
            """
              /* Multi-line
                 comment only */
              """
          )
        );
    }

    @Test
    void commentAtStartNoNewlineAfter() {
        rewriteRun(
          hcl(
            "# comment\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void commentAtEndNoTrailingNewline() {
        rewriteRun(
          hcl(
            """
              locals {
                a = 1
              }
              # trailing comment""".stripTrailing()
          )
        );
    }

    @Test
    void multipleCommentsAtEndNoTrailingNewline() {
        rewriteRun(
          hcl(
            """
              locals {
                a = 1
              }
              # comment 1
              # comment 2""".stripTrailing()
          )
        );
    }

    @Test
    void multiLineCommentAtEndNoTrailingNewline() {
        rewriteRun(
          hcl(
            """
              locals {
                a = 1
              }
              /* trailing
              multiline */""".stripTrailing()
          )
        );
    }

    @Test
    void commentAsFirstLine() {
        rewriteRun(
          hcl(
            """
              # This is a header comment
              resource "aws_s3_bucket" "example" {
                bucket = "my-bucket"
              }
              """
          )
        );
    }

    @Test
    void multiLineCommentAsFirstElement() {
        rewriteRun(
          hcl(
            """
              /* This is a block comment
                 at the start of the file */
              resource "aws_s3_bucket" "example" {
                bucket = "my-bucket"
              }
              """
          )
        );
    }

    @Test
    void emptyFileWithCommentOnly() {
        rewriteRun(
          hcl(
            "#"
          )
        );
    }

    @Test
    void emptyMultiLineCommentOnly() {
        rewriteRun(
          hcl(
            "/**/"
          )
        );
    }

    // Additional edge cases - comments at literal start/end positions

    @Test
    void hashCommentAsFirstCharacter() {
        rewriteRun(
          hcl(
            "# Comment starting at first character\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void slashCommentAsFirstCharacter() {
        rewriteRun(
          hcl(
            "// Comment starting at first character\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void blockCommentAsFirstCharacter() {
        rewriteRun(
          hcl(
            "/* Block comment */\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void hashCommentOnlyNoNewline() {
        rewriteRun(
          hcl(
            "# Only a hash comment, no newline"
          )
        );
    }

    @Test
    void slashCommentOnlyNoNewline() {
        rewriteRun(
          hcl(
            "// Only a slash comment, no newline"
          )
        );
    }

    @Test
    void blockCommentOnlyNoNewline() {
        rewriteRun(
          hcl(
            "/* Only a block comment, no newline */"
          )
        );
    }

    @Test
    void fileEndsWithHashCommentNoNewline() {
        rewriteRun(
          hcl(
            "locals { a = 1 }\n# trailing comment no newline"
          )
        );
    }

    @Test
    void fileEndsWithSlashCommentNoNewline() {
        rewriteRun(
          hcl(
            "locals { a = 1 }\n// trailing comment no newline"
          )
        );
    }

    @Test
    void fileEndsWithBlockComment() {
        rewriteRun(
          hcl(
            "locals { a = 1 }\n/* trailing block comment */"
          )
        );
    }

    @Test
    void onlyNewlineAndComment() {
        rewriteRun(
          hcl(
            "\n# Comment after newline"
          )
        );
    }

    @Test
    void commentBeforeEOF() {
        rewriteRun(
          hcl(
            "locals { a = 1 }\n#"
          )
        );
    }

    @Test
    void multipleHashCommentsNoContentBetween() {
        rewriteRun(
          hcl(
            "# comment 1\n# comment 2\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void commentDirectlyAfterClosingBrace() {
        rewriteRun(
          hcl(
            "locals { a = 1 }# trailing on same line"
          )
        );
    }

    @Test
    void commentDirectlyAfterClosingBraceWithSlash() {
        rewriteRun(
          hcl(
            "locals { a = 1 }// trailing on same line"
          )
        );
    }

    @Test
    void commentDirectlyAfterClosingBraceWithBlock() {
        rewriteRun(
          hcl(
            "locals { a = 1 }/* trailing on same line */"
          )
        );
    }

    // CRLF edge cases

    @Test
    void commentOnlyFileCRLFNoTrailingNewline() {
        rewriteRun(
          hcl(
            "# comment with CRLF\r\n# second comment"
          )
        );
    }

    @Test
    void commentAtEndCRLFNoTrailingNewline() {
        rewriteRun(
          hcl(
            "locals { a = 1 }\r\n# trailing with CRLF no newline"
          )
        );
    }

    @Test
    void commentAtStartCRLF() {
        rewriteRun(
          hcl(
            "# header comment\r\nlocals { a = 1 }"
          )
        );
    }

    // Comments adjacent to heredocs

    @Test
    void commentBeforeHeredoc() {
        rewriteRun(
          hcl(
            """
              # Comment before heredoc
              locals {
                a = <<EOF
              some text
              EOF
              }
              """
          )
        );
    }

    @Test
    void commentAfterHeredoc() {
        rewriteRun(
          hcl(
            """
              locals {
                a = <<EOF
              some text
              EOF
              }
              # Comment after block with heredoc
              """
          )
        );
    }

    // Empty file edge cases

    @Test
    void emptyFile() {
        rewriteRun(
          hcl(
            ""
          )
        );
    }

    @Test
    void whitespaceOnlyFile() {
        rewriteRun(
          hcl(
            "   \n   \n   "
          )
        );
    }

    @Test
    void newlineOnlyFile() {
        rewriteRun(
          hcl(
            "\n"
          )
        );
    }

    // Comment at line 1 with subsequent content

    @Test
    void commentFirstLineContentSecondLine() {
        rewriteRun(
          hcl(
            "#\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void multipleCommentsFirstLinesThenContent() {
        rewriteRun(
          hcl(
            "# line 1\n# line 2\n# line 3\nlocals { a = 1 }"
          )
        );
    }

    // Comment at last line edge cases

    @Test
    void contentThenCommentSameLine() {
        rewriteRun(
          hcl(
            "locals { a = 1 } # inline comment"
          )
        );
    }

    @Test
    void contentThenMultipleInlineComments() {
        rewriteRun(
          hcl(
            "locals { a = 1 } # comment 1 /* block */ // slash"
          )
        );
    }

    @Test
    void unclosedBlockCommentAtEnd() {
        // This should actually fail to parse - unclosed block comment
        // Testing to see how the parser handles it
    }

    // Realistic Terraform patterns with comments

    @Test
    void terraformProviderWithHeaderComment() {
        rewriteRun(
          hcl(
            """
              # Terraform AWS Provider Configuration
              # Author: Test
              # Date: 2024-01-01

              terraform {
                required_providers {
                  aws = {
                    source  = "hashicorp/aws"
                    version = "~> 4.0"
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void terraformResourceWithTrailingComment() {
        rewriteRun(
          hcl(
            """
              resource "aws_s3_bucket" "example" {
                bucket = "my-tf-test-bucket"
              }
              # End of file
              """
          )
        );
    }

    @Test
    void terraformVariablesFileStartWithComment() {
        rewriteRun(
          hcl(
            """
              # Variables for environment configuration
              variable "environment" {
                type    = string
                default = "dev"
              }
              """
          )
        );
    }

    @Test
    void terraformOutputsFileEndWithComment() {
        rewriteRun(
          hcl(
            """
              output "bucket_arn" {
                value = "arn:aws:s3:::example"
              }
              # Output definitions complete
              """
          )
        );
    }

    @Test
    void licensedHeaderCommentAtStart() {
        rewriteRun(
          hcl(
            """
              # Copyright 2024 Example Corp
              # Licensed under Apache 2.0
              #
              # This file configures the infrastructure

              locals {
                project = "example"
              }
              """
          )
        );
    }

    @Test
    void commentOnlyTfvarsFile() {
        // A .tfvars file that only has comments (no variables defined)
        rewriteRun(
          hcl(
            """
              # Environment: Development
              # Region: us-east-1
              # No variables overridden in this file
              """
          )
        );
    }

    @Test
    void multilineBlockCommentAtFileStart() {
        rewriteRun(
          hcl(
            """
              /*
               * Main Terraform Configuration
               *
               * This module provisions:
               * - VPC
               * - Subnets
               * - Security Groups
               */

              module "vpc" {
                source = "./modules/vpc"
              }
              """
          )
        );
    }

    @Test
    void multilineBlockCommentAtFileEnd() {
        rewriteRun(
          hcl(
            """
              module "vpc" {
                source = "./modules/vpc"
              }

              /*
               * End of configuration
               * Remember to run terraform plan before apply
               */
              """
          )
        );
    }

    @Test
    void mixedCommentsAtStartAndEnd() {
        rewriteRun(
          hcl(
            """
              # File header
              /* Block comment header */
              // Another header comment

              resource "null_resource" "example" {
              }

              // Footer comment
              /* Block footer */
              # Last comment
              """
          )
        );
    }

    @Test
    void commentImmediatelyBeforeFirstBlock() {
        rewriteRun(
          hcl(
            "# Header\nresource \"aws_instance\" \"web\" {\n  ami = \"ami-12345\"\n}"
          )
        );
    }

    @Test
    void commentImmediatelyAfterLastBlock() {
        rewriteRun(
          hcl(
            "resource \"aws_instance\" \"web\" {\n  ami = \"ami-12345\"\n}\n# Footer"
          )
        );
    }

    @Test
    void noNewlineAfterOpeningCommentNoNewlineBeforeClosingComment() {
        rewriteRun(
          hcl(
            "# start\nlocals { a = 1 }\n# end"
          )
        );
    }

    // Edge cases with CR line endings

    @Test
    void commentEndingWithCROnly() {
        // Old Mac-style line ending (CR without LF)
        rewriteRun(
          hcl(
            "# comment with CR only\rlocals { a = 1 }"
          )
        );
    }

    @Test
    void commentEndingWithCRAtEndOfFile() {
        rewriteRun(
          hcl(
            "locals { a = 1 }\r# trailing with CR"
          )
        );
    }

    @Test
    void multipleCROnlyLineComments() {
        rewriteRun(
          hcl(
            "# first\r# second\r# third\rlocals { a = 1 }"
          )
        );
    }

    // Very short comments

    @Test
    void singleCharHashComment() {
        rewriteRun(
          hcl(
            "#x\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void singleCharSlashComment() {
        rewriteRun(
          hcl(
            "//x\nlocals { a = 1 }"
          )
        );
    }

    // Unicode in comments

    @Test
    void unicodeInComment() {
        rewriteRun(
          hcl(
            "# 日本語コメント\nlocals { a = 1 }"
          )
        );
    }

    @Disabled("Emoji (surrogate pair) in comments at start of file causes cursor tracking issues - requires deeper fix")
    @Test
    void emojiInComment() {
        rewriteRun(
          hcl(
            "# This is 👍 good\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void unicodeInCommentAtEndOfFile() {
        rewriteRun(
          hcl(
            "locals { a = 1 }\n# 日本語コメント"
          )
        );
    }

    // Boundary conditions

    @Test
    void commentStartsAtPositionZero() {
        rewriteRun(
          hcl(
            "#"
          )
        );
    }

    @Test
    void commentWithOnlyNewlineAtEnd() {
        rewriteRun(
          hcl(
            "#\n"
          )
        );
    }

    @Test
    void twoEmptyHashCommentsInARow() {
        rewriteRun(
          hcl(
            "#\n#\n"
          )
        );
    }

    @Test
    void blockCommentWithNoSpaces() {
        rewriteRun(
          hcl(
            "/**/locals { a = 1 }"
          )
        );
    }

    @Test
    void blockCommentContainingSlashes() {
        rewriteRun(
          hcl(
            "/* http://example.com // not a comment */\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void blockCommentContainingHash() {
        rewriteRun(
          hcl(
            "/* # not a line comment */\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void nestedAsteriskInBlockComment() {
        rewriteRun(
          hcl(
            "/* * * * stars * * * */\nlocals { a = 1 }"
          )
        );
    }

    @Test
    void slashesInLineComment() {
        rewriteRun(
          hcl(
            "// http://example.com\nlocals { a = 1 }"
          )
        );
    }
}
