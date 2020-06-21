package org.openrewrite.maven;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

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

    class Pom implements Maven, SourceFile {
        private final MavenModel model;
        private final Xml.Document document;

        @JsonIgnore
        private final Object memoizationLock = new Object();

        @JsonIgnore
        private List<Dependency> memoizedDependencies;

        @JsonIgnore
        private List<Property> memoizedProperties;

        public Pom(MavenModel model, Xml.Document document) {
            this.model = model;
            this.document = document;
        }

        Xml.Document getDocument() {
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

        public Pom withDependencies(List<Dependency> dependencies) {
            synchronized (memoizationLock) {
                if (dependencies != memoizedDependencies) {
                    memoizedDependencies = dependencies;
                    Xml.Tag dependenciesTag = document.getRoot().getChild("dependencies").orElse(null);
                    if(dependenciesTag == null) {
                        throw new IllegalStateException("Expecting <dependencies> to already exist in POM");
                    }

                    return new Pom(model, document.withRoot(document.getRoot()
                            .withContent(document.getRoot().getContent().stream()
                                    .map(tag -> {
                                        if (tag == dependenciesTag) {
                                            return ((Xml.Tag) tag).withContent(dependencies.stream()
                                                    .map(d -> d.tag)
                                                    .collect(toList()));
                                        }
                                        return tag;
                                    })
                                    .collect(toList()))));
                }
            }
            return this;
        }

        public List<Dependency> getDependencies() {
            return memoizeDependencies();
        }

        private List<Dependency> memoizeDependencies() {
            if (memoizedDependencies == null) {
                List<Dependency> deps = document.getRoot().getChild("dependencies")
                        .map(dependencies -> dependencies.getChildren("dependency"))
                        .map(dependencies -> dependencies.stream()
                                .map(dependencyTag -> new Dependency(
                                        model.getDependencies().stream()
                                                .filter(d -> d.getModuleVersion().getGroupId().equals(dependencyTag.getChildValue("groupId").orElse(null)) &&
                                                        d.getModuleVersion().getArtifactId().equals(dependencyTag.getChildValue("artifactId").orElse(null)))
                                                .findAny()
                                                .orElse(null),
                                        dependencyTag
                                )).collect(toList())
                        )
                        .orElse(emptyList());

                synchronized (memoizationLock) {
                    if (memoizedDependencies == null) {
                        memoizedDependencies = deps;
                    }
                }
            }

            return memoizedDependencies;
        }

        public Pom withProperties(List<Property> properties) {
            synchronized (memoizationLock) {
                if (properties != memoizedProperties) {
                    memoizedProperties = properties;
                    Xml.Tag propertiesTag = document.getRoot().getChild("properties").orElse(null);
                    if(propertiesTag == null) {
                        throw new IllegalStateException("Expecting <properties> to already exist in POM");
                    }

                    return new Pom(model, document.withRoot(document.getRoot()
                            .withContent(document.getRoot().getContent().stream()
                                    .map(tag -> {
                                        if (tag == propertiesTag) {
                                            return ((Xml.Tag) tag).withContent(properties.stream()
                                                    .map(d -> d.tag)
                                                    .collect(toList()));
                                        }
                                        return tag;
                                    })
                                    .collect(toList()))));
                }
            }
            return this;
        }

        public List<Property> getProperties() {
            return memoizeProperties();
        }

        private List<Property> memoizeProperties() {
            if(memoizedProperties == null) {
                List<Property> props = document.getRoot().getChild("properties")
                        .map(properties -> properties.getContent().stream()
                                .filter(c -> c instanceof Xml.Tag)
                                .map(Xml.Tag.class::cast)
                                .map(Property::new)
                                .collect(toList()))
                        .orElse(emptyList());

                synchronized (memoizationLock) {
                    if(memoizedProperties == null) {
                        memoizedProperties = props;
                    }
                }
            }

            return memoizedProperties;
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
        public String getFileType() {
            return "POM";
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
    }

    class Dependency implements Maven {
        private final MavenModel.Dependency model;
        final Xml.Tag tag;

        public Dependency(MavenModel.Dependency model, Xml.Tag tag) {
            this.model = model;
            this.tag = tag;
        }

        public MavenModel.Dependency getModel() {
            return model;
        }

        public String getGroupId() {
            return tag.getChildValue("groupId").orElse(null);
        }

        public Dependency withGroupId(String groupId) {
            return new Dependency(model.withModuleVersion(model.getModuleVersion().withGroupId(groupId)),
                    tag.withChildValue("groupId", groupId));
        }

        public String getArtifactId() {
            return tag.getChildValue("artifactId").orElse(null);
        }

        public Dependency withArtifactId(String artifactId) {
            return new Dependency(model.withModuleVersion(model.getModuleVersion().withArtifactId(artifactId)),
                    tag.withChildValue("artifactId", artifactId));
        }

        @Nullable
        public String getVersion() {
            return tag.getChildValue("version").orElse(null);
        }

        public Dependency withVersion(String version) {
            return new Dependency(model.withModuleVersion(model.getModuleVersion().withVersion(version)),
                    tag.withChildValue("version", version));
        }

        @Override
        public Formatting getFormatting() {
            return tag.getFormatting();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Dependency withFormatting(Formatting fmt) {
            return new Dependency(model, tag.withFormatting(fmt));
        }

        @Override
        public UUID getId() {
            return tag.getId();
        }

        @Override
        public <R> R acceptMaven(MavenSourceVisitor<R> v) {
            return v.visitDependency(this);
        }
    }

    class Property implements Maven {
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
    }
}
