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
package org.openrewrite.maven.tree;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;

@SuppressWarnings("JavadocReference")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@RequiredArgsConstructor
public class MavenRepository implements Serializable {

    public static final MavenRepository MAVEN_LOCAL_USER_NEUTRAL = new MavenRepository("local", new File("~/.m2/repository").toString(), "true", "true", true, null, null, null, false);
    public static final MavenRepository MAVEN_LOCAL_DEFAULT = new MavenRepository("local", Paths.get(System.getProperty("user.home"), ".m2", "repository").toUri().toString(), "true", "true", true, null, null, null, false);
    public static final MavenRepository MAVEN_CENTRAL = new MavenRepository("central", "https://repo.maven.apache.org/maven2", "true", "false", true, null, null, null, true);

    @EqualsAndHashCode.Include
    @With
    @Nullable
    String id;

    /**
     * Not a {@link URI} because this could be a property reference.
     */
    @EqualsAndHashCode.Include
    @With
    String uri;

    @EqualsAndHashCode.Include
    @With
    @Nullable
    String releases;

    @EqualsAndHashCode.Include
    @With
    @Nullable
    String snapshots;

    @EqualsAndHashCode.Include
    @With
    @NonFinal
    boolean knownToExist;

    // Prevent user credentials from being inadvertently serialized
    @With
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Nullable
    String username;

    @With
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Nullable
    String password;

    @With
    @Nullable
    Duration timeout;

    @Nullable
    @NonFinal
    Boolean deriveMetadataIfMissing;

    /**
     * Constructor required by {@link org.openrewrite.maven.tree.OpenRewriteModelSerializableTest}.
     *
     * @deprecated Use {@link #MavenRepository(String, String, String, String, boolean, String, String, Duration, Boolean)}
     */
    @Deprecated
    @JsonIgnore
    public MavenRepository(
            @Nullable String id, String uri, @Nullable String releases, @Nullable String snapshots,
            @Nullable String username, @Nullable String password
    ) {
        this(id, uri, releases, snapshots, false, username, password, null, null);
    }

    /**
     * Constructor required by {@link org.openrewrite.gradle.marker.GradleProject}.
     *
     * @deprecated Use {@link #MavenRepository(String, String, String, String, boolean, String, String, Duration, Boolean)}
     */
    @Deprecated
    @JsonIgnore
    public MavenRepository(
            @Nullable String id, String uri, @Nullable String releases, @Nullable String snapshots, boolean knownToExist,
            @Nullable String username, @Nullable String password, @Nullable Boolean deriveMetadataIfMissing
    ) {
        this(id, uri, releases, snapshots, knownToExist, username, password, null, deriveMetadataIfMissing);
    }

    @JsonIgnore
    public MavenRepository(
            @Nullable String id, String uri, @Nullable String releases, @Nullable String snapshots, boolean knownToExist,
            @Nullable String username, @Nullable String password, @Nullable Duration timeout, @Nullable Boolean deriveMetadataIfMissing
    ) {
        this.id = id;
        this.uri = uri;
        this.releases = releases;
        this.snapshots = snapshots;
        this.knownToExist = knownToExist;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.deriveMetadataIfMissing = deriveMetadataIfMissing;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Accessors(fluent = true, chain = true)
    public static class Builder {
        String id;
        String uri;

        String releases;
        String snapshots;
        boolean knownToExist;
        String username;
        String password;
        Boolean deriveMetadataIfMissing;
        Duration timeout;

        private Builder() {
        }

        public MavenRepository build() {
            return new MavenRepository(id, uri, releases, snapshots, knownToExist, username, password, timeout, deriveMetadataIfMissing);
        }

        public Builder releases(boolean releases) {
            this.releases = Boolean.toString(releases);
            return this;
        }

        public Builder releases(String releases) {
            this.releases = releases;
            return this;
        }

        public Builder snapshots(boolean snapshots) {
            this.snapshots = Boolean.toString(snapshots);
            return this;
        }

        public Builder snapshots(String snapshots) {
            this.snapshots = snapshots;
            return this;
        }

        public Builder username(String username) {
            if (username.startsWith("${env.")) {
                this.username = resolveEnvironmentProperty(username);
                return this;
            }

            this.username = username;
            return this;
        }

        public Builder password(String password) {
            if (password.startsWith("${env.")) {
                this.password = resolveEnvironmentProperty(password);
                return this;
            }

            this.password = password;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        private static @Nullable String resolveEnvironmentProperty(@Nullable String rawProperty) {
            if (rawProperty == null) {
                return null;
            }
            String propertyName = rawProperty.replace("${env.", "").replace("}", "");
            return System.getenv(propertyName);
        }
    }
}
