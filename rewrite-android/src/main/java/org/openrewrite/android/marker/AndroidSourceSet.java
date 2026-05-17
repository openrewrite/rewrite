/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.android.marker;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A single AGP source set (e.g., {@code main}, {@code debug}, {@code paid},
 * {@code paidDebug}, {@code androidTest}). All path fields are project-root-relative
 * strings using forward slashes, mirroring the convention from
 * {@code org.openrewrite.maven.tree.Pom#getSourcePath()}. Storing strings rather
 * than {@link java.nio.file.Path} keeps markers serializable across machines without
 * leaking absolute paths from the host that ingested the project.
 */
@Value
@With
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class AndroidSourceSet implements Serializable {

    String name;

    List<String> javaSrcDirs;

    List<String> kotlinSrcDirs;

    List<String> resDirs;

    @Nullable
    String manifestFile;

    public List<String> getJavaSrcDirs() {
        return javaSrcDirs == null ? emptyList() : javaSrcDirs;
    }

    public List<String> getKotlinSrcDirs() {
        return kotlinSrcDirs == null ? emptyList() : kotlinSrcDirs;
    }

    public List<String> getResDirs() {
        return resDirs == null ? emptyList() : resDirs;
    }
}
