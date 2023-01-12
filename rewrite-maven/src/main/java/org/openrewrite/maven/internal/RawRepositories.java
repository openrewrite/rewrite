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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class RawRepositories {
    @JacksonXmlProperty(localName = "repository")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<Repository> repositories = emptyList();

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Repository {
        @Nullable
        String id;

        @With
        String url;

        @Nullable
        ArtifactPolicy releases;

        @Nullable
        ArtifactPolicy snapshots;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode
    @Getter
    public static class ArtifactPolicy {

        @Nullable
        String enabled;

        public ArtifactPolicy(@Nullable String enabled) {
            this.enabled = enabled;
        }

        /**
         * Used by Jackson in the event there is an empty tag in the POM.
         */
        @SuppressWarnings("unused")
        public ArtifactPolicy() {
            this("true");
        }
    }
}
