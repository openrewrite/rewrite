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
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.List;
import java.util.Optional;

import static org.openrewrite.Validated.required;

public class UpgradeParentVersion extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;
    private String toVersion;

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

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("toVersion", toVersion))
                .and(Semver.validate(toVersion));
    }

    @Override
    public Maven visitPom(Maven.Pom pom) {
        versionComparator = Semver.validate(toVersion).getValue();
        return super.visitPom(pom);
    }

    @Override
    public Maven visitParent(Maven.Parent parent) {
        Maven.Parent p = refactor(parent, super::visitParent);

        List<String> newerVersions = p.getModel().getModuleVersion().getNewerVersions();

        Optional<String> newerVersion = newerVersions.stream()
                .filter(v -> versionComparator.isValid(v))
                .filter(v -> LatestRelease.INSTANCE.compare(parent.getModel().getModuleVersion().getVersion(), v) < 0)
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
