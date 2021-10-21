/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: fkrueger
 */
package org.openrewrite.maven;

import lombok.Data;
import org.openrewrite.ExecutionContext;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

@Data
public class DependencyExistVisitor extends MavenVisitor {

    private static final XPathMatcher DEPENDENCY_MATCHER = new XPathMatcher("//dependency");
    private static final String FOUND_DEPENDENCY_MSG = "plugin-dependency-found";

    private final Xml.Tag scope;
    private String groupId;
    private String artifactId;

    public DependencyExistVisitor(Xml.Tag tag) {
        this.scope = tag;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
        if (getCursor().isScopeInPath(scope)) {
            // Only for each dependency tag
            if (DEPENDENCY_MATCHER.matches(getCursor()) && hasGroupAndArtifact(groupId, artifactId)) {
                ctx.putMessage(FOUND_DEPENDENCY_MSG, true);
            }
        }
        return super.visitTag(tag, ctx);
    }

    private boolean hasGroupAndArtifact(String groupId, String artifactId) {
        Xml.Tag tag = getCursor().getValue();
        return groupId.equals(tag.getChildValue("groupId").orElse(model.getGroupId())) &&
                tag.getChildValue("artifactId")
                        .map(a -> a.equals(artifactId))
                        .orElse(artifactId == null);
    }

}