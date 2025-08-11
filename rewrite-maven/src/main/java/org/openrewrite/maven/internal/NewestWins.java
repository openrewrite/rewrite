/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.internal;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.grammar.VersionRangeLexer;
import org.openrewrite.maven.internal.grammar.VersionRangeParser;
import org.openrewrite.maven.internal.grammar.VersionRangeParserBaseVisitor;
import org.openrewrite.maven.tree.Version;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class NewestWins extends VersionRequirement {

    private final Set<VersionSpec> requestedVersions = new HashSet<>();

    NewestWins(VersionSpec requestedVersions) {
        this.requestedVersions.add(requestedVersions);
    }

    @Override
    public String toString() {
        return requestedVersions.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    public NewestWins addRequirement(String requested) {
        VersionSpec requestedSpec = buildVersionSpec(requested);
        if (requestedVersions.add(requestedSpec)) {
            super.selected = null;
        }
        return this;
    }

    static VersionSpec buildVersionSpec(String requested) {
        if ("LATEST".equals(requested)) {
            return DynamicVersion.LATEST;
        } else if ("RELEASE".equals(requested)) {
            return DynamicVersion.RELEASE;
        } else if (requested.contains("[") || requested.contains("(")) {
            // for things like the profile activation block of where the range is unclosed but maven still handles it, e.g.
            // https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.12.0-rc2/jackson-databind-2.12.0-rc2.pom
            if (!(requested.contains("]") || requested.contains(")"))) {
                requested += "]";
            }

            VersionRangeParser parser = new VersionRangeParser(new CommonTokenStream(new VersionRangeLexer(
                    CharStreams.fromString(requested))));

            parser.removeErrorListeners();
            parser.addErrorListener(new PrintingErrorListener());

            return new VersionRangeParserBaseVisitor<VersionSpec>() {
                @Override
                public VersionSpec visitVersionRequirement(VersionRangeParser.VersionRequirementContext ctx) {
                    return new RangeSet(ctx.range().stream()
                            .map(range -> {
                                Version lower, upper;
                                if (range.bounds().boundedLower() != null) {
                                    Iterator<TerminalNode> versionIter = range.bounds().boundedLower().Version().iterator();
                                    lower = versionIter.hasNext() ? toVersion(versionIter.next()) : null;
                                    upper = versionIter.hasNext() ? toVersion(versionIter.next()) : null;
                                } else if (range.bounds().unboundedLower() != null) {
                                    TerminalNode upperVersionNode = range.bounds().unboundedLower().Version();
                                    lower = null;
                                    upper = upperVersionNode != null ? toVersion(upperVersionNode) : null;
                                } else {
                                    lower = toVersion(range.bounds().exactly().Version());
                                    upper = toVersion(range.bounds().exactly().Version());
                                }
                                return new Range(
                                        range.CLOSED_RANGE_OPEN() != null, lower,
                                        range.CLOSED_RANGE_CLOSE() != null, upper
                                );
                            })
                            .collect(toList())
                    );
                }

                private Version toVersion(TerminalNode version) {
                    return new Version(version.getText());
                }
            }.visit(parser.versionRequirement());
        }
        return new SoftRequirement(requested);
    }

    protected @Nullable String cacheResolved(DownloadOperation<Iterable<String>> availableVersions) throws MavenDownloadingException {
        Version latestVersion = null;
        for (String availableVersion : availableVersions.call()) {
            Version version = new Version(availableVersion);
            if (requestedVersions.stream().anyMatch(spec -> spec.matches(version))) {
                if (latestVersion == null || version.compareTo(latestVersion) > 0) {
                    latestVersion = version;
                }
            }
        }
        return latestVersion != null ? latestVersion.toString() : null;
    }

    private static class PrintingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            System.out.printf("Syntax error at line %d:%d %s%n", line, charPositionInLine, msg);
        }
    }
}
