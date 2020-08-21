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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.jetbrains.annotations.NotNull;
import org.mapdb.*;
import org.openrewrite.internal.lang.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

public class CachingWorkspaceReader implements WorkspaceReader {
    HTreeMap<Artifact, List<String>> versionsByArtifact;

    public CachingWorkspaceReader(@Nullable File workspace) {
        if(workspace != null) {
            DB localRepositoryDiskDb = DBMaker
                    .fileDB(workspace)
                    .transactionEnable()
                    .make();

            // big map populated with data expired from cache
            versionsByArtifact = localRepositoryDiskDb
                    .hashMap("workspace.disk")
                    .keySerializer(ARTIFACT_SERIALIZER)
                    .valueSerializer(LIST_SERIALIZER)
                    .create();

            Metrics.gaugeMapSize("rewrite.maven.workspace.cache.size", Tags.of("layer", "disk"), versionsByArtifact);
        }
        else {
            DB inMemoryDb = DBMaker
                    .heapDB()
                    .make();

            // fast in-memory collection with limited size
            this.versionsByArtifact = inMemoryDb
                    .hashMap("workspace.inmem")
                    .keySerializer(ARTIFACT_SERIALIZER)
                    .valueSerializer(LIST_SERIALIZER)
                    .expireAfterCreate(10, TimeUnit.MINUTES)
                    .create();

            Metrics.gaugeMapSize("rewrite.maven.workspace.cache.size", Tags.of("layer", "memory"), versionsByArtifact);
        }
    }

    @Override
    public WorkspaceRepository getRepository() {
        return new WorkspaceRepository();
    }

    @Override
    public File findArtifact(Artifact artifact) {
        // this module loader should never need to access artifact files
        return null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return versionsByArtifact.getOrDefault(artifact, emptyList());
    }

    public void cacheVersions(Artifact artifact, List<String> versions) {
        if (!versionsByArtifact.containsKey(artifact)) {
            versionsByArtifact.put(artifact, versions);
        }
    }

    private static final Serializer<Artifact> ARTIFACT_SERIALIZER = new Serializer<Artifact>() {
        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull Artifact value) throws IOException {
            out.writeUTF(value.getGroupId());
            out.writeUTF(value.getArtifactId());
            out.writeUTF(value.getClassifier());
            out.writeUTF(value.getExtension());
            out.writeUTF(value.getVersion());
        }

        @Override
        public Artifact deserialize(@NotNull DataInput2 input, int available) throws IOException {
            return new DefaultArtifact(input.readUTF(),
                    input.readUTF(),
                    input.readUTF(),
                    input.readUTF(),
                    input.readUTF());
        }
    };

    private static Serializer<List<String>> LIST_SERIALIZER = new Serializer<List<String>>() {
        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull List<String> value) throws IOException {
            out.writeShort(value.size());
            for (String s : value) {
                out.writeUTF(s);
            }

        }

        @Override
        public List<String> deserialize(@NotNull DataInput2 input, int available) throws IOException {
            short size = input.readShort();
            List<String> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(input.readUTF());
            }
            return list;
        }
    };
}
