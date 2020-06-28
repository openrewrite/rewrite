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
import lombok.Getter;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenSourceVisitor;
import org.openrewrite.maven.PrintMaven;
import org.openrewrite.xml.ChangeTagContent;
import org.openrewrite.xml.tree.Xml;

import java.io.Serializable;
import java.util.*;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Maven extends Serializable, Tree {
    @Override
    default String print() {
        return new PrintMaven().visit(this);
    }

    @Override
    default <R> R accept(SourceVisitor<R> v) {
        return v instanceof MavenSourceVisitor ?
                acceptMaven((MavenSourceVisitor<R>) v) : v.defaultTo(null);
    }

    default <R> R acceptMaven(MavenSourceVisitor<R> v) {
        return v.defaultTo(null);
    }

    @JsonIgnore
    @Override
    default String getTreeType() {
        return "pom";
    }

    class Pom implements Maven, SourceFile {
        private final MavenModel model;
        private final Xml.Document document;

        @Nullable
        @JsonIgnore
        private final DependencyManagement dependencyManagement;

        @JsonIgnore
        private final MemoizedTags<Dependency> memoizedDependencies;

        @JsonIgnore
        private final MemoizedTags<Property> memoizedProperties;

        public Pom(MavenModel model, Xml.Document document) {
            this.model = model;
            this.document = document;
            this.dependencyManagement = document.getRoot().getChild("dependencyManagement")
                    .map(dm -> new DependencyManagement(model.getDependencyManagement(), dm))
                    .orElse(null);

            this.memoizedDependencies = new MemoizedTags<>(document.getRoot(), "dependencies/dependency",
                    tag -> new Dependency(
                            false,
                            model.getDependencies().stream()
                                    .filter(d -> d.getModuleVersion().getGroupId().equals(tag.getChildValue("groupId").orElse(null)) &&
                                            d.getModuleVersion().getArtifactId().equals(tag.getChildValue("artifactId").orElse(null)))
                                    .findAny()
                                    .orElse(null),
                            tag
                    ), Dependency::getTag);

            this.memoizedProperties = new MemoizedTags<>(document.getRoot(), "properties/*",
                    Property::new, Property::getTag);
        }

        public Refactor<Pom> refactor() {
            return new Refactor<>(this);
        }

        public Xml.Document getDocument() {
            return document;
        }

        public MavenModel getModel() {
            return model;
        }

        @Nullable
        public String getGroupId() {
            return document.getRoot().getChildValue("groupId").orElse(null);
        }

        public Pom withGroupId(String groupId) {
            return new Pom(model, document.withRoot(document.getRoot()
                    .withChildValue("groupId", groupId)));
        }

        @Nullable
        public String getArtifactId() {
            return document.getRoot().getChildValue("artifactId").orElse(null);
        }

        public Pom withArtifactId(String artifactId) {
            return new Pom(model, document.withRoot(document.getRoot()
                    .withChildValue("artifactId", artifactId)));
        }

        @Nullable
        public String getVersion() {
            return document.getRoot().getChildValue("version").orElse(null);
        }

        public Pom withVersion(String version) {
            return new Pom(model, document.withRoot(document.getRoot()
                    .withChildValue("version", version)));
        }

        @Nullable
        public Parent getParent() {
            return model.getParent() == null ?
                    null :
                    new Parent(model.getParent(), document.getRoot().getChild("parent")
                            .orElse(null));
        }

        @Nullable
        public DependencyManagement getDependencyManagement() {
            return dependencyManagement;
        }

        public Pom withDependencyManagement(DependencyManagement dependencyManagement) {
            return document.getRoot().getChild("dependencyManagement")
                    .map(dm -> new Pom(model.withDependencyManagement(dependencyManagement.getModel()),
                            document.withRoot(new Refactor<>(document.getRoot())
                                    .visit(new ChangeTagContent(dm, dependencyManagement.tag.getContent()))
                                    .fix()
                                    .getFixed()))
                    )
                    .orElse(this);
        }

        public List<Dependency> getDependencies() {
            return memoizedDependencies.getModels();
        }

        public Pom withDependencies(List<Dependency> dependencies) {
            return memoizedDependencies.with(dependencies)
                    .map(root -> new Pom(
                            model.withDependencies(dependencies.stream().map(Dependency::getModel).collect(toList())),
                            document.withRoot(root))
                    )
                    .orElse(this);
        }

        public List<Property> getProperties() {
            return memoizedProperties.getModels();
        }

        public Pom withProperties(List<Property> properties) {
            return memoizedProperties.with(properties)
                    .map(root -> new Pom(
                            model.withProperties(properties.stream().collect(toMap(Property::getKey, Property::getValue))),
                            document.withRoot(root))
                    )
                    .orElse(this);
        }

        /**
         * @param maybePropertyReference A tag value (text content) that may represent a property.
         * @return A matching property, if the tag value is in fact a property reference
         * and such a property is defined.
         */
        public Optional<Property> getPropertyFromValue(String maybePropertyReference) {
            return getPropertyKey(maybePropertyReference)
                    .flatMap(key -> getProperties().stream()
                            .filter(prop -> prop.getKey().equals(key))
                            .findAny());
        }

        /**
         * Because Jackson will not place a polymorphic type tag on the root of the AST when we are serializing a list of ASTs together
         */
        protected final String jacksonPolymorphicTypeTag = ".Maven$Pom";

        @JsonProperty("@c")
        public String getJacksonPolymorphicTypeTag() {
            return jacksonPolymorphicTypeTag;
        }

        @Override
        public String getSourcePath() {
            return document.getSourcePath();
        }

        @Override
        public Map<Metadata, String> getMetadata() {
            return document.getMetadata();
        }

        @Override
        public Formatting getFormatting() {
            return document.getFormatting();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Pom withFormatting(Formatting fmt) {
            return new Pom(model, document.withFormatting(fmt));
        }

        @Override
        public UUID getId() {
            return document.getId();
        }

        @Override
        public <R> R acceptMaven(MavenSourceVisitor<R> v) {
            return v.visitPom(this);
        }
    }

    class Parent implements Maven {
        private final MavenModel model;

        @Getter
        @Nullable
        private final Xml.Tag tag;

        public Parent(MavenModel model, @Nullable Xml.Tag tag) {
            this.model = model;
            this.tag = tag;
        }

        public MavenModel getModel() {
            return model;
        }

        @Nullable
        public String getGroupId() {
            return tag == null ? null : tag.getChildValue("groupId").orElse(null);
        }

        public Parent withGroupId(String groupId) {
            return tag == null ? this : new Parent(model, tag.withChildValue("groupId", groupId));
        }

        @Nullable
        public String getArtifactId() {
            return tag == null ? null : tag.getChildValue("artifactId").orElse(null);
        }

        public Parent withArtifactId(String artifactId) {
            return tag == null ? this :
                    new Parent(model, tag.withChildValue("artifactId", artifactId));
        }

        @Nullable
        public String getVersion() {
            return tag == null ? null : tag.getChildValue("version").orElse(null);
        }

        public Parent withVersion(String version) {
            return tag == null ? this :
                    new Parent(model, tag.withChildValue("version", version));
        }

        @Override
        public Formatting getFormatting() {
            return tag == null ? Formatting.EMPTY : tag.getFormatting();
        }

        @Override
        public UUID getId() {
            return tag == null ? randomUUID() : tag.getId();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Parent withFormatting(Formatting fmt) {
            return tag == null ? this : new Parent(model, tag.withFormatting(fmt));
        }

        @Override
        public <R> R acceptMaven(MavenSourceVisitor<R> v) {
            return v.visitParent(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Parent parent = (Parent) o;
            return model.equals(parent.model) &&
                    tag == parent.tag;
        }

        @Override
        public int hashCode() {
            return Objects.hash(model, tag);
        }
    }

    class Dependency implements Maven {
        private final boolean isManaged;
        private final MavenModel.Dependency model;

        @Getter
        final Xml.Tag tag;

        public Dependency(boolean isManaged, MavenModel.Dependency model, Xml.Tag tag) {
            this.isManaged = isManaged;
            this.model = model;
            this.tag = tag;
        }

        public boolean isManaged() {
            return isManaged;
        }

        public MavenModel.Dependency getModel() {
            return model;
        }

        public String getGroupId() {
            return tag.getChildValue("groupId").orElse(null);
        }

        public Dependency withGroupId(String groupId) {
            return new Dependency(isManaged, model.withModuleVersion(model.getModuleVersion().withGroupId(groupId)),
                    tag.withChildValue("groupId", groupId));
        }

        public String getArtifactId() {
            return tag.getChildValue("artifactId").orElse(null);
        }

        public Dependency withArtifactId(String artifactId) {
            return new Dependency(isManaged, model.withModuleVersion(model.getModuleVersion().withArtifactId(artifactId)),
                    tag.withChildValue("artifactId", artifactId));
        }

        @Nullable
        public String getVersion() {
            return tag.getChildValue("version").orElse(null);
        }

        public Dependency withVersion(String version) {
            return new Dependency(isManaged, model.withModuleVersion(model.getModuleVersion().withVersion(version)),
                    tag.withChildValue("version", version));
        }

        @Override
        public Formatting getFormatting() {
            return tag.getFormatting();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Dependency withFormatting(Formatting fmt) {
            return new Dependency(isManaged, model, tag.withFormatting(fmt));
        }

        @Override
        public UUID getId() {
            return tag.getId();
        }

        @Override
        public <R> R acceptMaven(MavenSourceVisitor<R> v) {
            return v.visitDependency(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Dependency that = (Dependency) o;
            return model.equals(that.model) &&
                    tag == that.tag;
        }

        @Override
        public int hashCode() {
            return Objects.hash(model, tag);
        }
    }

    class Property implements Maven {
        @Getter
        final Xml.Tag tag;

        public Property(Xml.Tag tag) {
            this.tag = tag;
        }

        public String getKey() {
            return tag.getName();
        }

        public Property withKey(String key) {
            return new Property(tag.withName(key));
        }

        public String getValue() {
            return tag.getValue().orElse("");
        }

        public Property withValue(String value) {
            return new Property(tag.withValue(value));
        }

        @Override
        public Formatting getFormatting() {
            return tag.getFormatting();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Property withFormatting(Formatting fmt) {
            return new Property(tag.withFormatting(fmt));
        }

        @Override
        public UUID getId() {
            return tag.getId();
        }

        @Override
        public <R> R acceptMaven(MavenSourceVisitor<R> v) {
            return v.visitProperty(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Property property = (Property) o;
            return tag == property.tag;
        }

        @Override
        public int hashCode() {
            return Objects.hash(tag);
        }
    }

    class DependencyManagement implements Maven {
        private final MavenModel.DependencyManagement model;
        private final Xml.Tag tag;

        @JsonIgnore
        private final MemoizedTags<Dependency> memoizedDependencies;

        public DependencyManagement(MavenModel.DependencyManagement model, Xml.Tag tag) {
            this.model = model;
            this.tag = tag;

            this.memoizedDependencies = new MemoizedTags<>(tag, "dependencies/dependency",
                    dm -> new Dependency(
                            true,
                            model.getDependencies().stream()
                                    .filter(d -> d.getModuleVersion().getGroupId().equals(dm.getChildValue("groupId").orElse(null)) &&
                                            d.getModuleVersion().getArtifactId().equals(dm.getChildValue("artifactId").orElse(null)))
                                    .findAny()
                                    .orElse(null),
                            dm
                    ), Dependency::getTag);
        }

        public MavenModel.DependencyManagement getModel() {
            return model;
        }

        public Xml.Tag getTag() {
            return tag;
        }

        public DependencyManagement withDependencies(List<Dependency> dependencies) {
            return memoizedDependencies.with(dependencies)
                    .map(root -> new DependencyManagement(
                            model.withDependencies(dependencies.stream().map(Dependency::getModel).collect(toList())),
                            root)
                    )
                    .orElse(this);
        }

        public List<Dependency> getDependencies() {
            return memoizedDependencies.getModels();
        }

        @Override
        public Formatting getFormatting() {
            return tag.getFormatting();
        }

        @Override
        public UUID getId() {
            return tag.getId();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Tree> T withFormatting(Formatting fmt) {
            return (T) tag.withFormatting(fmt);
        }

        @Override
        public <R> R acceptMaven(MavenSourceVisitor<R> v) {
            return v.visitDependencyManagement(this);
        }
    }

    static Optional<String> getPropertyKey(String textValue) {
        if (textValue.startsWith("${") && textValue.endsWith("}")) {
            return Optional.of(textValue.substring(2, textValue.length() - 1));
        }
        return Optional.empty();
    }
}
