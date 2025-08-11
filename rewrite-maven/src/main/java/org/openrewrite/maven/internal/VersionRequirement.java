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

import lombok.Data;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.*;

import java.util.List;

import static java.util.stream.Collectors.joining;

public abstract class VersionRequirement {

    @Nullable
    protected volatile transient String selected;

    public static VersionRequirement fromVersion(String requested, ResolutionStrategy resolutionStrategy, int depth) {
        if (resolutionStrategy == ResolutionStrategy.NEAREST_WINS) {
            return new NearestWins(null, NearestWins.buildVersionSpec(requested, depth == 0));
        }
        return new NewestWins(NewestWins.buildVersionSpec(requested));
    }

    public abstract VersionRequirement addRequirement(String requested);

    public @Nullable String resolve(GroupArtifact groupArtifact, MavenPomDownloader downloader, List<MavenRepository> repositories) throws MavenDownloadingException {
        return resolve(() -> {
            MavenMetadata metadata = downloader.downloadMetadata(groupArtifact, null, repositories);
            return metadata.getVersioning().getVersions();
        });
    }

    public @Nullable String resolve(DownloadOperation<Iterable<String>> availableVersions) throws MavenDownloadingException {
        if (selected == null) {
            //TODO Is it bad that LATEST and RELEASE are also cached -> if a new version is released, it won't be picked up in the current cached state. Existing behavior to cache them though.
            selected = cacheResolved(availableVersions);
        }
        return selected;
    }

    protected abstract @Nullable String cacheResolved(DownloadOperation<Iterable<String>> availableVersions) throws MavenDownloadingException;

    interface VersionSpec {

        boolean matches(Version version);
    }

    @Data
    protected static class SoftRequirement implements VersionSpec {
        final String version;

        public boolean matches(Version version) {
            return this.version.equals(version.toString());
        }

        @Override
        public String toString() {
            return "Soft version='" + version + "'";
        }
    }

    protected enum DynamicVersion implements VersionSpec {
        LATEST,
        RELEASE;

        public boolean matches(Version version) {
            return this == LATEST || !version.toString().endsWith("-SNAPSHOT");
        }

        @Override
        public String toString() {
            return "Dynamic='" + name() + "'";
        }

    }

    @Value
    protected static class RangeSet implements VersionSpec {
        List<Range> ranges;

        public boolean matches(Version version) {
            for (Range range : ranges) {
                boolean lowerMatches = true;
                if (range.lower != null) {
                    int lowComp = range.lower.compareTo(version);
                    lowerMatches = lowComp == 0 ? range.lowerClosed : lowComp < 0;
                }
                if (!lowerMatches) {
                    return false;
                }

                boolean upperMatches = true;
                if (range.upper != null) {
                    int upperComp = range.upper.compareTo(version);
                    upperMatches = upperComp == 0 ? range.upperClosed : upperComp > 0;
                }
                if (upperMatches) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return ranges.stream().map(Range::toString).collect(joining(",", "RangeSet={", "}"));
        }
    }

    @Value
    protected static class Range {
        boolean lowerClosed;

        @Nullable
        Version lower;

        boolean upperClosed;

        @Nullable
        Version upper;

        @Override
        public String toString() {
            return (lowerClosed ? "[" : "(") + lower + "," + upper + (upperClosed ? ']' : ')');
        }
    }
}
