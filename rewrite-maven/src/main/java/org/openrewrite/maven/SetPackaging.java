/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Alex Boyko
 */
package org.openrewrite.maven;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Alex Boyko
 */
public class SetPackaging extends Recipe {

    private static final XPathMatcher PACKAGING_MATCHER = new XPathMatcher("/project/packaging");
    private static final XPathMatcher PROJECT_MATCHER = new XPathMatcher("/project");

    @Option(
            displayName = "Packaging",
            description = "The type of packaging to set",
            example = "jar"
    )
    @Nullable
    private final String packaging;

    public SetPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getDisplayName() {
        return "Set Maven project packaging type";
    }

    public String getDescription() {
        return "Sets the packaging type of the Maven project. Either adds the packaging tag if it is missing or changes its context if present. If 'null' specified the tag will be removed";
    }

    protected boolean canEqual(@Nullable final Object other) {
        return other instanceof SetPackaging;
    }

    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SetPackagingVisitor();
    }

    private class SetPackagingVisitor extends MavenVisitor {

        private Xml.Tag foundPackagingTag = null;

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            Maven m = super.visitMaven(maven, ctx);
            Pom pom = m.getModel();
            // TODO: Remove reflective access, pom.getEffectiveProperties() has been removed in 7.14.0
            Field reflectionOverridesField = ReflectionUtils.findField(Pom.class, "propertyOverrides");
            ReflectionUtils.makeAccessible(reflectionOverridesField);
            Map<String, String> effectiveProperties = (Map<String, String>) ReflectionUtils.getField(reflectionOverridesField, pom);
            Pom recreatedPom = Pom.build(
                    pom.getGroupId(),
                    pom.getArtifactId(),
                    pom.getVersion(),
                    pom.getDatedSnapshotVersion(),
                    pom.getName(),
                    pom.getDescription(),
                    packaging,
                    pom.getClassifier(),
                    pom.getParent(),
                    pom.getDependencies(),
                    pom.getDependencyManagement(),
                    pom.getLicenses(),
                    pom.getRepositories(),
                    pom.getProperties(),
                    effectiveProperties,
                    false
            );
            return m.withModel(recreatedPom);
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext context) {
            if (PROJECT_MATCHER.matches(getCursor())) {
                return visitProjectTag(tag, context);
            } else if (PACKAGING_MATCHER.matches(getCursor())) {
                return visitPackagingTag(tag, context);
            } else {
                return super.visitTag(tag, context);
            }
        }

        private Xml.Tag visitProjectTag(Xml.Tag tag, ExecutionContext context) {
            Xml.Tag t = (Xml.Tag) super.visitTag(tag, context);
            if (foundPackagingTag != null) {
                if (packaging == null) {
                    t = t.withContent(t.getContent().stream().filter(c -> c != foundPackagingTag).collect(Collectors.toList()));
                }
            } else {
                if (packaging != null) {
                    doAfterVisit(new AddToTagVisitor(t, Xml.Tag.build("<packaging>" + packaging + "</packaging>")));
                }
            }
            return t;
        }

        private Xml.Tag visitPackagingTag(Xml.Tag tag, ExecutionContext context) {
            foundPackagingTag = tag;
            if (packaging != null) {
                Optional<? extends Content> presentPackaging = tag.getContent().stream()
                        .filter(Xml.CharData.class::isInstance)
                        .filter(cd -> packaging.equalsIgnoreCase(((Xml.CharData) cd).getText()))
                        .findFirst();
                if (presentPackaging.isEmpty()) {
                    doAfterVisit(new ChangeTagValueVisitor(tag, packaging));
                }
            }
            return tag;
        }

    }

}
