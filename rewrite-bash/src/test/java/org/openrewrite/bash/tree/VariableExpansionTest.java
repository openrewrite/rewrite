/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.bash.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.bash.Assertions.bash;

class VariableExpansionTest implements RewriteTest {

    @Test
    void simpleVariable() {
        rewriteRun(
          bash(
            "echo $VAR\n"
          )
        );
    }

    @Test
    void bracedVariable() {
        rewriteRun(
          bash(
            "echo ${VAR}\n"
          )
        );
    }

    @Test
    void specialVariables() {
        rewriteRun(
          bash(
            "echo $? $! $$ $# $@ $* $0 $1 $- $9\n"
          )
        );
    }

    @Test
    void defaultValue() {
        rewriteRun(
          bash(
            "echo ${VAR:-default}\n"
          )
        );
    }

    @Test
    void assignDefault() {
        rewriteRun(
          bash(
            "echo ${VAR:=default}\n"
          )
        );
    }

    @Test
    void alternateValue() {
        rewriteRun(
          bash(
            "echo ${VAR:+alternate}\n"
          )
        );
    }

    @Test
    void errorIfUnset() {
        rewriteRun(
          bash(
            "echo ${VAR:?\"error message\"}\n"
          )
        );
    }

    @Test
    void stringLength() {
        rewriteRun(
          bash(
            "echo ${#VAR}\n"
          )
        );
    }

    @Test
    void prefixRemoval() {
        rewriteRun(
          bash(
            "echo ${VAR#pattern}\n"
          )
        );
    }

    @Test
    void greedyPrefixRemoval() {
        rewriteRun(
          bash(
            "echo ${VAR##*/}\n"
          )
        );
    }

    @Test
    void suffixRemoval() {
        rewriteRun(
          bash(
            "echo ${VAR%pattern}\n"
          )
        );
    }

    @Test
    void greedySuffixRemoval() {
        rewriteRun(
          bash(
            "echo ${VAR%%.*}\n"
          )
        );
    }

    @Test
    void substitution() {
        rewriteRun(
          bash(
            "echo ${VAR/old/new}\n"
          )
        );
    }

    @Test
    void globalSubstitution() {
        rewriteRun(
          bash(
            "repo=\"${repo//\\\"/}\"\n"
          )
        );
    }

    @Test
    void arrayLength() {
        rewriteRun(
          bash(
            "echo ${#arr[@]}\n"
          )
        );
    }

    @Test
    void arrayElement() {
        rewriteRun(
          bash(
            "echo ${arr[0]}\n"
          )
        );
    }

    @Test
    void arrayAllElements() {
        rewriteRun(
          bash(
            "echo \"${arr[@]}\"\n"
          )
        );
    }

    @Test
    void indirectExpansion() {
        rewriteRun(
          bash(
            "echo ${!var_name}\n"
          )
        );
    }

    @Test
    void indirectWithDefault() {
        rewriteRun(
          bash(
            "local var_value=\"${!var_name:-}\"\n"
          )
        );
    }

    @Test
    void nestedArithmeticInBraceExpansion() {
        rewriteRun(
          bash(
            "echo ${sorted[$((${#sorted[@]} - 1))]}\n"
          )
        );
    }

    @Test
    void caseModification() {
        rewriteRun(
          bash(
            "echo ${VAR^^}\n"
          )
        );
    }

    @Test
    void indirectArrayIteration() {
        rewriteRun(
          bash(
            "for i in \"${!arr[@]}\"; do\n  echo $i\ndone\n"
          )
        );
    }

    @Test
    void chainedParameterExpansion() {
        rewriteRun(
          bash(
            "without_proto=\"${line#*://}\"\nuserinfo=\"${without_proto%@*}\"\npassword=\"${userinfo#*:}\"\n"
          )
        );
    }

    @Test
    void arraySubscriptInDoubleQuotes() {
        rewriteRun(
          bash(
            "echo \"${FUNCNAME[0]}\"\n"
          )
        );
    }

    @Test
    void dollarSingleQuoteInExpansion() {
        rewriteRun(
          bash(
            "short_sha=\"${repo_info##*$'\\n'}\"\n"
          )
        );
    }

    @Test
    void dollarSingleQuoteInSubstitution() {
        rewriteRun(
          bash(
            "message=\"${message//$'\\n'/ }\"\n"
          )
        );
    }

    @Test
    void dollarSingleQuoteSuffixRemoval() {
        rewriteRun(
          bash(
            "line=\"${line%$'\\r'}\"\n"
          )
        );
    }

    @Test
    void quotedPatternInSubstitution() {
        rewriteRun(
          bash(
            "pkg_body=\"${pkg_body//\"$OLD\"/\"$NEW\"}\"\n"
          )
        );
    }

    @Test
    void indirectWithSubstitution() {
        rewriteRun(
          bash(
            "_value=\"${!var//$'\\n'/' '}\"\n"
          )
        );
    }

    @Test
    void escapedDollarExpansion() {
        rewriteRun(
          bash(
            "eval x=\"\\$${ref:-}\"\n"
          )
        );
    }

    @Test
    void indirectPrefixExpansion() {
        rewriteRun(
          bash(
            "echo \"${!prefix@}\"\n"
          )
        );
    }

    @Test
    void parameterExpansionDoubleHash() {
        rewriteRun(
          bash(
            "if [ -z \"${i##*/liblto*}\" ]; then continue; fi\n"
          )
        );
    }

    @Test
    void printfWithArrayExpansion() {
        rewriteRun(
          bash(
            "printf \"Running ${FUNCNAME[0]}\\n\"\n"
          )
        );
    }

    @Test
    void arrayLengthInConditional() {
        rewriteRun(
          bash(
            "if [[ ${#FDINITRDS[@]} -gt 0 ]]; then\n  echo found\nfi\n"
          )
        );
    }
}
