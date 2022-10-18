/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven;

import lombok.Getter;
import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MavenDownloadingExceptions extends Exception {
    /**
     * Exceptions tied to one or more direct dependencies, parent, or
     * dependency management dependencies.
     */
    private final List<MavenDownloadingException> exceptions = new ArrayList<>();

    public static MavenDownloadingExceptions append(@Nullable MavenDownloadingExceptions current,
                                                    MavenDownloadingException exception) {
        if (current == null) {
            current = new MavenDownloadingExceptions();
        }
        current.exceptions.add(exception);
        return current;
    }

    public static MavenDownloadingExceptions append(@Nullable MavenDownloadingExceptions current,
                                                    MavenDownloadingExceptions exceptions) {
        if (current == null) {
            current = new MavenDownloadingExceptions();
        }
        current.exceptions.addAll(exceptions.getExceptions());
        return current;
    }
}
