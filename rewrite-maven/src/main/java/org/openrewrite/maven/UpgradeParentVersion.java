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
package org.openrewrite.maven;

import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.openrewrite.Validated.required;
import static org.openrewrite.Validated.test;

public class UpgradeParentVersion extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;
    private String toVersion;

    @Nullable
    private String metadataPattern;

    private File localRepository = MavenParser.DEFAULT_LOCAL_REPOSITORY;

    @Nullable
    private File workspaceDir;

    private VersionComparator versionComparator;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setToVersion(String toVersion) {
        this.toVersion = toVersion;
    }

    public void setMetadataPattern(@Nullable String metadataPattern) {
        this.metadataPattern = metadataPattern;
    }

    public void setLocalRepository(File localRepository) {
        this.localRepository = localRepository;
    }

    public void setWorkspaceDir(@Nullable File workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    public UpgradeParentVersion() {
        setCursoringOn();
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("toVersion", toVersion))
                .and(Semver.validate(toVersion, metadataPattern))
                .and(test("localRepository", "must exist",
                        localRepository, File::exists))
                .and(test("workspaceDir", "must exist if set",
                        workspaceDir, w -> w == null || w.exists()));
    }

    @Override
    public Maven visitPom(Maven.Pom pom) {
        versionComparator = Semver.validate(toVersion, metadataPattern).getValue();
        return super.visitPom(pom);
    }

    @Override
    public Maven visitParent(Maven.Parent parent) {
        Maven.Parent p = refactor(parent, super::visitParent);

        Maven.Pom pom = getCursor().firstEnclosing(Maven.Pom.class);
        assert pom != null;

        List<String> newerVersions = p.getModel().getModuleVersion()
                .getNewerVersions(pom, localRepository, workspaceDir);

        LatestRelease latestRelease = new LatestRelease(metadataPattern);
        Optional<String> newerVersion = newerVersions.stream()
                .filter(v -> versionComparator.isValid(v))
                .filter(v -> latestRelease.compare(parent.getModel().getModuleVersion().getVersion(), v) < 0)
                .max(versionComparator);

        if (newerVersion.isPresent()) {
            ChangeParentVersion changeParentVersion = new ChangeParentVersion();
            changeParentVersion.setGroupId(groupId);
            changeParentVersion.setArtifactId(artifactId);
            changeParentVersion.setToVersion(newerVersion.get());
            andThen(changeParentVersion);
        }

        return p;
    }
}
