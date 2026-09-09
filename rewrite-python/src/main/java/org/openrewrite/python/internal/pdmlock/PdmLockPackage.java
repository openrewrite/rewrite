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
package org.openrewrite.python.internal.pdmlock;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One {@code [[package]]} entry. Keys are emitted in the order name, version, extras, requires_python,
 * editable, subdirectory, the source keys ({@code <vcs>}/ref/revision, or path, or url), summary,
 * groups, marker, dependencies, files — matching {@code Candidate.as_lockfile_entry} +
 * {@code PDMLock.format_lockfile}.
 */
@Value
@With
public class PdmLockPackage {
    String name;
    String version;

    @Nullable List<String> extras;
    @Nullable String requiresPython;
    @Nullable Boolean editable;
    @Nullable String subdirectory;

    /** VCS kind ({@code git}/{@code hg}/{@code bzr}/{@code svn}) and its repository URL. */
    @Nullable String vcs;
    @Nullable String vcsUrl;
    @Nullable String ref;
    @Nullable String revision;

    @Nullable String path;
    @Nullable String url;

    String summary;

    /** Package-level group membership, present under the {@code inherit_metadata} strategy. */
    @Nullable List<String> groups;

    @Nullable String marker;

    /** Resolved dependency edges as normalized PEP 508 strings. */
    @Nullable List<String> dependencies;

    @Nullable List<PdmLockFile> files;
}
