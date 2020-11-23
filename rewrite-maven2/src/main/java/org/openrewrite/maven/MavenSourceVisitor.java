package org.openrewrite.maven;

import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.XmlSourceVisitor;

public interface MavenSourceVisitor<R> extends XmlSourceVisitor<R> {
    R visitMaven(Maven maven);
}
