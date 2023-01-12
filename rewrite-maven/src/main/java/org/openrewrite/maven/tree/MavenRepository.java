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
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.internal.lang.Nullable;

import java.io.File;
import java.net.URI;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@RequiredArgsConstructor
public class MavenRepository {
    public static final MavenRepository MAVEN_LOCAL_USER_NEUTRAL = new MavenRepository("local", new File("~/.m2/repository").toString(), true, true, true, null, null, false);
    public static final MavenRepository MAVEN_LOCAL_DEFAULT = new MavenRepository("local", new File(System.getProperty("user.home") + "/.m2/repository").toURI().toString(), true, true, true, null, null, false);
    public static final MavenRepository MAVEN_CENTRAL = new MavenRepository("central", "https://repo.maven.apache.org/maven2", true, false, true, null, null, true);

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

    @Nullable
    @NonFinal
    Boolean deriveMetadataIfMissing;

    @JsonIgnore
    public MavenRepository(@Nullable String id, String uri, @Nullable String releases, @Nullable String snapshots, boolean knownToExist, @Nullable String username, @Nullable String password, @Nullable Boolean deriveMetadataIfMissing) {
        this.id = id;
        this.uri = uri;
        this.releases = releases;
        this.snapshots = snapshots;
        this.knownToExist = knownToExist;
        this.username = username;
        this.password = password;
        this.deriveMetadataIfMissing = deriveMetadataIfMissing;
    }

    // Two overloaded constructors to make it convient to use booleans for releases/snapshots
    @JsonIgnore
    public MavenRepository(@Nullable String id, String uri, boolean releases, boolean snapshots, @Nullable String username, @Nullable String password) {
        this.id = id;
        this.uri = uri;
        this.releases = Boolean.toString(releases);
        this.snapshots = Boolean.toString(snapshots);
        this.username = username;
        this.password = password;
    }

    @JsonIgnore
    public MavenRepository(@Nullable String id, String uri, boolean releases, boolean snapshots, boolean knownToExist, @Nullable String username, @Nullable String password, @Nullable Boolean deriveMetadataIfMissing) {
        this.id = id;
        this.uri = uri;
        this.releases = Boolean.toString(releases);
        this.snapshots = Boolean.toString(snapshots);
        this.knownToExist = knownToExist;
        this.username = username;
        this.password = password;
        this.deriveMetadataIfMissing = deriveMetadataIfMissing;
    }
}
