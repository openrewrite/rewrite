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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.grammar.VersionRangeLexer;
import org.openrewrite.maven.internal.grammar.VersionRangeParser;
import org.openrewrite.maven.internal.grammar.VersionRangeParserBaseVisitor;
import org.openrewrite.maven.tree.Version;

import java.util.Iterator;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class NearestWins extends VersionRequirement {

    @Nullable
    private final NearestWins nearer;

    private final VersionSpec versionSpec;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (nearer != null) {
            builder.append(nearer).append(", ");
        }
        builder.append(versionSpec);
        return builder.toString();
    }

    public NearestWins addRequirement(String requested) {
        if (versionSpec instanceof DirectRequirement) {
            return this;
        }

        VersionSpec newRequirement = buildVersionSpec(requested, false);

        NearestWins next = this;
        while (next != null) {
            if (next.versionSpec.equals(newRequirement)) {
                return this;
            }
            next = next.nearer;
        }

        return new NearestWins(this, newRequirement);
    }

    static VersionSpec buildVersionSpec(String requested, boolean direct) {
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
        return direct ?
                new DirectRequirement(requested) :
                new SoftRequirement(requested);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class DirectRequirement extends SoftRequirement {
        private DirectRequirement(String version) {
            super(version);
        }

        @Override
        public String toString() {
            return "Version='" + version + "'";
        }
    }

    protected @Nullable String cacheResolved(DownloadOperation<Iterable<String>> availableVersions) throws MavenDownloadingException {
        String nearestSoftRequirement = null;
        NearestWins next = this;
        NearestWins nearestHardRequirement = null;

        while (next != null) {
            VersionSpec spec = next.versionSpec;
            if (spec instanceof DirectRequirement) {
                // dependencies defined in the project POM always win
                return ((DirectRequirement) spec).getVersion();
            } else if (spec instanceof SoftRequirement) {
                nearestSoftRequirement = ((SoftRequirement) spec).version;
            } else {
                nearestHardRequirement = next;
            }
            next = next.nearer;
        }

        if (nearestHardRequirement == null) {
            return nearestSoftRequirement;
        }
        VersionSpec hardRequirement = nearestHardRequirement.versionSpec;
        Version latest = null;
        for (String availableVersion : availableVersions.call()) {
            Version version = new Version(availableVersion);

            if ((hardRequirement instanceof DynamicVersion || hardRequirement instanceof RangeSet) && hardRequirement.matches(version)) {
                if (latest == null || version.compareTo(latest) > 0) {
                    latest = version;
                }
            }
        }

        if (latest == null) {
            // No version matches the hard requirement.
            return null;
        }

        return latest.toString();
    }

    private static class PrintingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            System.out.printf("Syntax error at line %d:%d %s%n", line, charPositionInLine, msg);
        }
    }
}
