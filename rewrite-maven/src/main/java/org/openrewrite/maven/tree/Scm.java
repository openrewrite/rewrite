/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.maven.tree;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * The {@code <scm>} element of a POM: where the source that an artifact was built from is kept.
 * <p>
 * On {@link Pom#getScm()} the values are exactly as that POM writes them. On {@link ResolvedPom#getScm()} and
 * {@link ResolvedDependency#getScm()} they are the effective values: inherited through the parent chain and with
 * property placeholders resolved, but without the child's artifactId that Maven appends to an inherited value.
 * See {@link ResolvedPom#getScm()} for why.
 */
@Value
@With
public class Scm implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Read access to the source repository, as an SCM URL such as {@code scm:git:https://github.com/openrewrite/rewrite.git}.
     */
    @Nullable
    String connection;

    /**
     * Like {@link #connection}, but for write access.
     */
    @Nullable
    String developerConnection;

    /**
     * Where the source repository can be browsed, such as {@code https://github.com/openrewrite/rewrite}.
     */
    @Nullable
    String url;

    /**
     * The tag of the source repository that this version was built from.
     */
    @Nullable
    String tag;
}
