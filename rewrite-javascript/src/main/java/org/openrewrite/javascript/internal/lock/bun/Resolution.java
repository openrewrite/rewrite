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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;

/**
 * Port of bun's src/install/resolution.zig with versioned_url.zig (bun 1.3.10): how a package was resolved.
 */
final class Resolution {

    enum Tag {
        UNINITIALIZED(0), ROOT(1), NPM(2), FOLDER(4), LOCAL_TARBALL(8), GITHUB(16), GIT(32), SYMLINK(64),
        WORKSPACE(72), REMOTE_TARBALL(80), SINGLE_FILE_MODULE(100);

        final int value;

        Tag(int value) {
            this.value = value;
        }
    }

    static final class UnexpectedResolution extends IllegalArgumentException {
        UnexpectedResolution(String message) {
            super(message);
        }
    }

    static final Resolution UNINITIALIZED = new Resolution(Tag.UNINITIALIZED, null, "", "", null);
    static final Resolution ROOT = new Resolution(Tag.ROOT, null, "", "", null);

    final Tag tag;

    /**
     * npm: the version and the tarball url.
     */
    final @Nullable SemverVersion version;
    final String url;

    /**
     * folder, local or remote tarball, symlink, workspace, single file module.
     */
    final String value;
    final @Nullable Repository repository;

    private Resolution(Tag tag, @Nullable SemverVersion version, String url, String value,
                       @Nullable Repository repository) {
        this.tag = tag;
        this.version = version;
        this.url = url;
        this.value = value;
        this.repository = repository;
    }

    static Resolution npm(SemverVersion version, String url) {
        return new Resolution(Tag.NPM, version, url, "", null);
    }

    static Resolution of(Tag tag, String value) {
        return new Resolution(tag, null, "", value, null);
    }

    static Resolution repository(Tag tag, Repository repository) {
        return new Resolution(tag, null, "", "", repository);
    }

    Resolution withUrl(String url) {
        return new Resolution(tag, version, url, value, repository);
    }

    Resolution withRepository(Repository repository) {
        return new Resolution(tag, version, url, value, repository);
    }

    static Resolution fromTextLockfile(String resStr) {
        if (resStr.startsWith("root:")) {
            return ROOT;
        }
        if (resStr.startsWith("link:")) {
            return of(Tag.SYMLINK, resStr.substring("link:".length()));
        }
        if (resStr.startsWith("workspace:")) {
            return of(Tag.WORKSPACE, resStr.substring("workspace:".length()));
        }
        if (resStr.startsWith("file:")) {
            return of(Tag.FOLDER, resStr.substring("file:".length()));
        }
        switch (DependencyVersion.Tag.infer(resStr)) {
            case GIT:
                return repository(Tag.GIT, Repository.parseAppendGit(resStr));
            case GITHUB:
                return repository(Tag.GITHUB, Repository.parseAppendGithub(resStr));
            case TARBALL:
                if (Dependency.isRemoteTarball(resStr)) {
                    return of(Tag.REMOTE_TARBALL, resStr);
                }
                return of(Tag.LOCAL_TARBALL, resStr);
            case NPM: {
                SemverVersion.ParseResult parsed = SemverVersion.parse(resStr);
                if (!parsed.valid || parsed.version.major == null || parsed.version.minor == null ||
                    parsed.version.patch == null) {
                    throw new UnexpectedResolution("Unexpected resolution: " + resStr);
                }
                return npm(parsed.version.min(), "");
            }
            default:
                throw new UnexpectedResolution("Unexpected resolution: " + resStr);
        }
    }

    int order(Resolution rhs) {
        if (tag != rhs.tag) {
            return Integer.compare(tag.value, rhs.tag.value);
        }
        switch (tag) {
            case NPM:
                return version.order(rhs.version);
            case LOCAL_TARBALL:
            case FOLDER:
            case REMOTE_TARBALL:
            case WORKSPACE:
            case SYMLINK:
            case SINGLE_FILE_MODULE:
                return Strings.order(value, rhs.value);
            case GIT:
            case GITHUB:
                return repository.order(rhs.repository);
            default:
                return 0;
        }
    }

    boolean eql(Resolution rhs) {
        if (tag != rhs.tag) {
            return false;
        }
        switch (tag) {
            case ROOT:
                return true;
            case NPM:
                return version.eql(rhs.version);
            case GIT:
            case GITHUB:
                return repository.eql(rhs.repository);
            default:
                return value.equals(rhs.value);
        }
    }

    /**
     * {@code Resolution.Formatter} with posix separators.
     */
    @Override
    public String toString() {
        switch (tag) {
            case NPM:
                return version.toString();
            case LOCAL_TARBALL:
            case FOLDER:
            case REMOTE_TARBALL:
                return value;
            case GIT:
                return repository.format("git+");
            case GITHUB:
                return repository.format("github:");
            case WORKSPACE:
                return "workspace:" + value;
            case SYMLINK:
                return "link:" + value;
            case SINGLE_FILE_MODULE:
                return "module:" + value;
            default:
                return "";
        }
    }
}
