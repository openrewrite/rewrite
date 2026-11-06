/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.coldfusion.format;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.coldfusion.style.CFFormat;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * The visitors that change the characters of whitespace rather than where it is: trailing whitespace, line breaks,
 * and tabs or spaces.
 */
class LineAndCharacterTest {

    @Nested
    class RemoveTrailingWhitespace implements RewriteTest {

        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(toRecipe(() -> new RemoveTrailingWhitespaceVisitor<>()));
        }

        @Test
        void script() {
            rewriteRun(
              cfc(
                "component {   \n    function f() {\t\n        x = 1;  \n    \n    }\n}  ",
                "component {\n    function f() {\n        x = 1;\n\n    }\n}"
              )
            );
        }

        @Test
        void keepsIndentsOnEmptyLinesWhenTheStyleSays() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new RemoveTrailingWhitespaceVisitor<>(
                CFFormat.tabsAndIndents().withKeepIndentsOnEmptyLines(true), null))),
              cfc(
                "component {   \n    function f() {\n        x = 1;  \n        \n    }\n}",
                "component {\n    function f() {\n        x = 1;\n        \n    }\n}"
              )
            );
        }

        @Test
        void keepsTheTrailingWhitespaceATemplateWrites() {
            rewriteRun(
              cfm("<p>a</p>   \n<cfset x = 1>  \n<cfsilent>  \n<cfset y = 2>  \n</cfsilent>\n",
                "<p>a</p>   \n<cfset x = 1>  \n<cfsilent>\n<cfset y = 2>\n</cfsilent>\n")
            );
        }
    }

    @Nested
    class NormalizeLineBreaks implements RewriteTest {

        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(toRecipe(() -> new NormalizeLineBreaksVisitor<>(GeneralFormatStyle.DEFAULT)));
        }

        @Test
        void followsTheFilesOwnLineBreaks() {
            rewriteRun(
              cfc(
                "component {\r\n    function f() {\n        x = 1;\r\n    }\r\n}\r\n",
                "component {\r\n    function f() {\r\n        x = 1;\r\n    }\r\n}\r\n"
              )
            );
        }

        @Test
        void classicMacLineBreaks() {
            rewriteRun(
              cfm(
                "<cfscript>\r// a comment\nx = 1;\r</cfscript>\r",
                "<cfscript>\r// a comment\rx = 1;\r</cfscript>\r"
              )
            );
        }

        @Test
        void keepsTheLineBreaksATemplateWrites() {
            rewriteRun(
              cfm("<p>a</p>\r\n<cfset x = 1>\n<p>b</p>\r\n")
            );
        }
    }

    @Nested
    class NormalizeTabsOrSpaces implements RewriteTest {

        @Test
        void spaces() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new NormalizeTabsOrSpacesVisitor<>(CFFormat.tabsAndIndents()))),
              cfc(
                "component {\n\tfunction f() {\n\t\tx = 1;\n\t}\n}",
                "component {\n    function f() {\n        x = 1;\n    }\n}"
              )
            );
        }

        @Test
        void tabsKeepTheSpacesThatAlignAfterThem() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new NormalizeTabsOrSpacesVisitor<>(
                CFFormat.tabsAndIndents().withUseTabCharacter(true)))),
              cfc(
                "component {\n    function f() {\n\t\tx = foo(a,\n\t\t       b);\n    }\n}",
                "component {\n\tfunction f() {\n\t\tx = foo(a,\n\t\t       b);\n\t}\n}"
              )
            );
        }

        @Test
        void keepsTheIndentationATemplateWrites() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new NormalizeTabsOrSpacesVisitor<>(CFFormat.tabsAndIndents()))),
              cfm("<cfif x>\n\t<p>a</p>\n\t<cfset y = 1>\n</cfif>\n")
            );
        }
    }
}
