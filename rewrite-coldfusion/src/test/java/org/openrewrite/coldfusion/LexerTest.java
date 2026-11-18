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
package org.openrewrite.coldfusion;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.dfa.DFAState;
import org.junit.jupiter.api.Test;
import org.openrewrite.coldfusion.internal.grammar.CFMLLexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class LexerTest {

    @Test
    void firstCharactersLexFromTheCache() {
        Set<String> uncached = new TreeSet<>();
        for (boolean script : new boolean[]{true, false}) {
            List<int[]> starts = new ArrayList<>();
            CFMLLexer lexer = new CFMLLexer(CharStreams.fromString(script ?
              "x = a.b + 1 + .5; // note\n/* more */ if (x >= 2) { y = [1, 2]; s = \"a\"\"#b#\" & 'c''#d#'; }" :
              "<p>#a# ## <cfoutput>#a.b# and #c#</cfoutput> <!--- note ---> <cfset x = 1 /> 1 < 2</p>" +
              "<cfinclude template=a#b#.cfm><cfmail to=\"#t#\" subject='s' cc=##x /><cfif x EQ 1 >y</cfif >")) {
                @Override
                public Token nextToken() {
                    starts.add(new int[]{_mode, _input.LA(1)});
                    return super.nextToken();
                }
            };
            if (script) {
                lexer.script();
            } else {
                lexer.template();
            }
            new CommonTokenStream(lexer).fill();
            DFA[] dfa = lexer.getInterpreter().decisionToDFA;
            for (int[] start : starts) {
                // ANTLR caches steps on ASCII characters only
                if (start[1] < 0 || start[1] > 127) {
                    continue;
                }
                DFAState s0 = dfa[start[0]].s0;
                if (s0 == null || s0.edges == null || s0.edges[start[1]] == null) {
                    uncached.add(CFMLLexer.modeNames[start[0]] + " '" + (char) start[1] + "'");
                }
            }
        }
        assertThat(uncached).isEmpty();
    }

    @Test
    void digitsAreANameOnlyAfterAMember() {
        CFMLLexer lexer = new CFMLLexer(CharStreams.fromString("x = row.2 + 2 + a.5 + .5 + row.2e5 + row.2\u0663"));
        lexer.script();
        List<String> tokens = new ArrayList<>();
        for (Token token : lexer.getAllTokens()) {
            if (token.getChannel() == Token.DEFAULT_CHANNEL && token.getText().matches(".*[0-9].*")) {
                tokens.add(CFMLLexer.VOCABULARY.getSymbolicName(token.getType()) + " " + token.getText());
            }
        }
        assertThat(tokens).containsExactly(
          "IDENTIFIER 2", "NUMBER 2", "IDENTIFIER 5", "NUMBER .5", "IDENTIFIER 2e5", "IDENTIFIER 2\u0663");
    }
}
