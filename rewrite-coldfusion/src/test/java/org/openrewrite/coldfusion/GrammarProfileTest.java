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
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openrewrite.coldfusion.internal.grammar.CFMLLexer;
import org.openrewrite.coldfusion.internal.grammar.CFMLParser;
import org.openrewrite.internal.EncodingDetectingInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Profiles the grammar on a sample of {@code -Dcoldfusion.corpus.dir} and lists, for each parser
 * decision, the most tokens SLL prediction looked ahead, with the tokens. A decision that looks past
 * a statement makes parsing slower and grows ANTLR's shared DFA with every file. For a directory of a
 * few files, it also lists the long lookaheads of each file.
 */
class GrammarProfileTest {

    private static final class Decision {
        long maxLook;
        long totalLook;
        long invocations;
        String example = "";
    }

    @Test
    @EnabledIfSystemProperty(named = "coldfusion.profile.report", matches = ".+")
    void profile() throws IOException {
        Path root = Paths.get(System.getProperty("coldfusion.corpus.dir"));
        int limit = Integer.getInteger("coldfusion.profile.limit", 2000);
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile).filter(ColdFusionParser.builder().build()::accept).sorted().forEach(paths::add);
        }
        Collections.shuffle(paths, new Random(42));
        paths = paths.subList(0, Math.min(limit, paths.size()));

        Map<Integer, Decision> decisions = new TreeMap<>();
        StringBuilder perFile = new StringBuilder();
        for (Path path : paths) {
            String source;
            try (InputStream in = Files.newInputStream(path); EncodingDetectingInputStream is = new EncodingDetectingInputStream(in)) {
                source = is.readFully();
            }
            boolean script = ColdFusionParser.isScriptComponent(path, source);
            CFMLLexer lexer = new CFMLLexer(CharStreams.fromString(source));
            if (script) {
                lexer.script();
            } else {
                lexer.template();
            }
            lexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CFMLParser parser = new CFMLParser(tokens);
            parser.removeErrorListeners();
            parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
            parser.setProfile(true);
            try {
                if (script) {
                    parser.scriptComponent();
                } else {
                    parser.template();
                }
            } catch (RuntimeException e) {
                continue;
            }
            perFile.append(root.relativize(path)).append(':');
            for (DecisionInfo info : parser.getParseInfo().getDecisionInfo()) {
                if (info.invocations == 0) {
                    continue;
                }
                if (info.SLL_MaxLook > 4) {
                    perFile.append(' ').append(rule(parser, info.decision)).append('=').append(info.SLL_MaxLook);
                }
                Decision decision = decisions.computeIfAbsent(info.decision, d -> new Decision());
                decision.totalLook += info.SLL_TotalLook;
                decision.invocations += info.invocations;
                if (info.SLL_MaxLook > decision.maxLook && info.SLL_MaxLookEvent != null) {
                    decision.maxLook = info.SLL_MaxLook;
                    StringBuilder text = new StringBuilder();
                    int start = info.SLL_MaxLookEvent.startIndex;
                    for (int i = start; i <= Math.min(info.SLL_MaxLookEvent.stopIndex, start + 40); i++) {
                        text.append(tokens.get(i).getText()).append(' ');
                    }
                    decision.example = root.relativize(path) + ": " + text.toString().replace("\n", "\\n");
                }
            }
            perFile.append('\n');
        }

        StringBuilder report = new StringBuilder();
        if (paths.size() <= 20) {
            report.append(perFile).append('\n');
        }
        CFMLParser parser = new CFMLParser(null);
        decisions.entrySet().stream()
          .sorted((a, b) -> Long.compare(b.getValue().maxLook, a.getValue().maxLook))
          .limit(25)
          .forEach(e -> report.append(String.format("%s: maxLook=%d totalLook=%d invocations=%d%n    %s%n",
            rule(parser, e.getKey()), e.getValue().maxLook, e.getValue().totalLook, e.getValue().invocations,
            e.getValue().example)));
        Files.write(Paths.get(System.getProperty("coldfusion.profile.report")), report.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String rule(CFMLParser parser, int decision) {
        return parser.getRuleNames()[parser.getATN().decisionToState.get(decision).ruleIndex] + "#" + decision;
    }
}
