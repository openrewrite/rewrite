package org.openrewrite.maven;

import org.openrewrite.xml.RemoveContent;
import org.openrewrite.xml.tree.Xml;

public class RemoveDependency extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;

    public RemoveDependency() {
        setCursoringOn();
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        if (isDependencyTag(groupId, artifactId)) {
            andThen(new RemoveContent.Scoped(tag, true));
        }

        return super.visitTag(tag);
    }
}
