package org.openrewrite.maven.internal;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.grammar.VersionRangeLexer;
import org.openrewrite.maven.internal.grammar.VersionRangeParser;
import org.openrewrite.maven.internal.grammar.VersionRangeParserBaseVisitor;
import org.openrewrite.maven.tree.GroupArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class RequestedVersion {
    private static final Logger logger = LoggerFactory.getLogger(RequestedVersion.class);

    private final GroupArtifact groupArtifact;

    @Nullable
    private final RequestedVersion nearer;

    private final VersionSpec versionSpec;

    /**
     * @param groupArtifact The group and artifact of the requested version.
     * @param nearer        A version in the same group and artifact that is nearer the root, if any.
     * @param requested     Any valid version text that can be written in a POM
     *                      including a fixed version, a range, LATEST, or RELEASE.
     */
    public RequestedVersion(@Nullable URI uri, GroupArtifact groupArtifact, @Nullable RequestedVersion nearer, String requested) {
        this.groupArtifact = groupArtifact;
        this.nearer = nearer;

        VersionRangeParser parser = new VersionRangeParser(new CommonTokenStream(new VersionRangeLexer(
                CharStreams.fromString(requested))));
        parser.removeErrorListeners();
        parser.addErrorListener(new LoggingErrorListener(uri));

        this.versionSpec = new VersionRangeParserBaseVisitor<VersionSpec>() {
            @Override
            public VersionSpec visitRequestedVersion(VersionRangeParser.RequestedVersionContext ctx) {
                if (ctx.version() != null) {
                    return new SoftRequirement(new Version(ctx.version().getText()));
                }

                return new RangeSet(ctx.range().stream()
                        .map(range -> {
                            Version lower, upper;
                            if (range.bounds().boundedLower() != null) {
                                Iterator<TerminalNode> versionIter = range.bounds().boundedLower().Version().iterator();
                                lower = toVersion(versionIter.next());
                                upper = versionIter.hasNext() ? toVersion(versionIter.next()) : null;
                            } else {
                                lower = null;
                                upper = toVersion(range.bounds().unboundedLower().Version());
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
        }.visit(parser.requestedVersion());
    }

    public boolean isRange() {
        return versionSpec instanceof RangeSet && (nearer == null || nearer.isRange());
    }

    public boolean isDynamic() {
        return !isRange() && (nearer == null ? versionSpec instanceof DynamicVersion : nearer.isDynamic());
    }

    /**
     * When the requested version is not a range, select the nearest version.
     */
    @Nullable
    public String nearestVersion() {
        if (isRange() || isDynamic()) {
            return null;
        } else if (nearer != null) {
            return nearer.nearestVersion();
        }
        return ((SoftRequirement) versionSpec).version.toString();
    }

    /**
     * When the requested version is a range set or dynamic, select the latest matching version.
     *
     * @param availableVersions The other versions listed in maven metadata.
     * @return The latest version matching the range set.
     */
    @Nullable
    public String selectFrom(Iterable<String> availableVersions) {
        Stream<Version> versionStream = StreamSupport.stream(availableVersions.spliterator(), false)
                .map(Version::new);
        return (isRange() ?
                versionStream.filter(this::rangeMatch) :
                versionStream
                        .filter(v -> ((DynamicVersion) versionSpec).kind.equals(DynamicVersion.Kind.LATEST) || !v.toString().endsWith("-SNAPSHOT"))

        ).max(Comparator.naturalOrder()).map(Version::toString).orElse(null);
    }

    private boolean rangeMatch(Version version) {
        if (!(versionSpec instanceof RangeSet)) {
            return true;
        }

        return ((RangeSet) versionSpec).ranges.stream()
                .anyMatch(range -> {
                    boolean lowerMatches = true;
                    if (range.lower != null) {
                        int lowComp = range.lower.compareTo(version);
                        lowerMatches = lowComp == 0 ? range.lowerClosed : lowComp < 0;
                    }

                    boolean upperMatches = true;
                    if (range.upper != null) {
                        int upperComp = range.upper.compareTo(version);
                        upperMatches = upperComp == 0 ? range.upperClosed : upperComp > 0;
                    }

                    return lowerMatches && upperMatches;
                }) && (nearer == null || nearer.rangeMatch(version));
    }

    interface VersionSpec {
    }

    private static class SoftRequirement implements VersionSpec {
        private final Version version;

        private SoftRequirement(Version version) {
            this.version = version;
        }
    }

    private static class DynamicVersion implements VersionSpec {
        private final Kind kind;

        private DynamicVersion(Kind kind) {
            this.kind = kind;
        }

        enum Kind {
            LATEST,
            RELEASE
        }
    }

    private static class RangeSet implements VersionSpec {
        private final List<Range> ranges;

        private RangeSet(List<Range> ranges) {
            this.ranges = ranges;
        }
    }

    private static class Range {
        private final boolean lowerClosed;
        private final boolean upperClosed;

        @Nullable
        private final Version lower;

        @Nullable
        private final Version upper;

        private Range(boolean lowerClosed,
                      @Nullable Version lower,
                      boolean upperClosed,
                      @Nullable Version upper) {
            this.lowerClosed = lowerClosed;
            this.lower = lower;
            this.upperClosed = upperClosed;
            this.upper = upper;
        }

        @Override
        public String toString() {
            return (lowerClosed ? "[" : "(") + lower + "," + upper +
                    (upperClosed ? ']' : ')');
        }
    }

    public String resolve(RawPomDownloader downloader, List<RawPom.Repository> repositories) {
        String selectedVersion;
        if (isRange() || isDynamic()) {
            RawMavenMetadata metadata = downloader.downloadMetadata(groupArtifact.getGroupId(),
                    groupArtifact.getArtifactId(), repositories);
            selectedVersion = selectFrom(metadata.getVersioning().getVersions());
        } else {
            selectedVersion = nearestVersion();
        }

        // for debugging...
        //noinspection RedundantIfStatement
        if (selectedVersion == null) {
            //noinspection ConstantConditions
            assert selectedVersion != null;
        }

        return selectedVersion;
    }

    private static class LoggingErrorListener extends BaseErrorListener {
        @Nullable
        private final URI uri;

        private LoggingErrorListener(@Nullable URI uri) {
            this.uri = uri;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            logger.warn("Syntax error at line {}:{} {} {}", line, charPositionInLine, msg, uri);
        }
    }
}
