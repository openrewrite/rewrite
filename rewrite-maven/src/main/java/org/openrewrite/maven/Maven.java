package org.openrewrite.maven;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

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
        private final Xml.Document document;

        public Pom(Xml.Document document) {
            this.document = document;
        }

        Xml.Document getDocument() {
            return document;
        }

        @Nullable
        public String getGroupId() {
            return document.getRoot().getChildValue("groupId")
                    .orElse(null);
        }

        public Pom withGroupId(String groupId) {
            return new Pom(document.withRoot(document.getRoot()
                    .getChildAndWithValue("groupId", groupId)));
        }

        @Nullable
        public String getArtifactId() {
            return document.getRoot().getChildValue("artifactId")
                    .orElse(null);
        }

        public Pom withArtifactId(String artifactId) {
            return new Pom(document.withRoot(document.getRoot()
                    .getChildAndWithValue("artifactId", artifactId)));
        }

        @Nullable
        public String getVersion() {
            return document.getRoot().getChildValue("version")
                    .orElse(null);
        }

        public Pom withVersion(String version) {
            return new Pom(document.withRoot(document.getRoot()
                    .getChildAndWithValue("version", version)));
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

        @Override
        public UUID getId() {
            return document.getId();
        }

        @Override
        public <R> R acceptMaven(MavenSourceVisitor<R> v) {
            return v.visitPom(this);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Pom withFormatting(Formatting fmt) {
            return new Pom(document.withFormatting(fmt));
        }
    }

    class Parent implements Maven {
        private final Xml.Tag tag;

        public Parent(Xml.Tag tag) {
            this.tag = tag;
        }

        @Nullable
        public String getGroupId() {
            return tag.getChildValue("groupId")
                    .orElse(null);
        }

        public Parent withGroupId(String groupId) {
            return new Parent(tag.getChildAndWithValue("groupId", groupId));
        }

        @Nullable
        public String getArtifactId() {
            return tag.getChildValue("artifactId")
                    .orElse(null);
        }

        public Parent withArtifactId(String artifactId) {
            return new Parent(tag.getChildAndWithValue("artifactId", artifactId));
        }

        @Nullable
        public String getVersion() {
            return tag.getChildValue("version")
                    .orElse(null);
        }

        public Parent withVersion(String version) {
            return new Parent(tag.getChildAndWithValue("version", version));
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
        public Parent withFormatting(Formatting fmt) {
            return new Parent(tag.withFormatting(fmt));
        }
    }
}
