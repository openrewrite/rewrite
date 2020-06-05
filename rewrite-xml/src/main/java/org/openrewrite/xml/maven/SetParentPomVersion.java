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
package org.openrewrite.xml.maven;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Validated;
import org.openrewrite.xml.ChangeTagValue;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlRefactorVisitor;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.Validated.required;

public class SetParentPomVersion extends XmlRefactorVisitor {
    private final XPathMatcher parentVersion = new XPathMatcher("/project/parent/version");

    private String whenGroupId;
    private String whenArtifactId;
    private String version;

    public SetParentPomVersion() {
        setCursoringOn();
    }

    public void setWhenGroupId(String whenGroupId) {
        this.whenGroupId = whenGroupId;
    }

    public void setWhenArtifactId(String whenArtifactId) {
        this.whenArtifactId = whenArtifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Validated validate() {
        return required("whenGroupId", whenGroupId)
                .and(required("whenArtifactId", whenArtifactId))
                .and(required("version", version));
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("when.group", whenGroupId, "when.artifact", whenArtifactId,
                "version", version);
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        if (parentVersion.matches(getCursor()) &&
                tag.getSibling("groupId", getCursor())
                        .flatMap(Xml.Tag::getValue)
                        .map(groupId -> groupId.equals(whenGroupId))
                        .orElse(false) &&
                tag.getSibling("artifactId", getCursor())
                        .flatMap(Xml.Tag::getValue)
                        .map(artifactId -> artifactId.equals(whenArtifactId))
                        .orElse(false) &&
                tag.getValue().map(v -> !v.equals(version)).orElse(true)) {
            andThen(new ChangeTagValue.Scoped(tag, version));
        }

        return super.visitTag(tag);
    }
}
