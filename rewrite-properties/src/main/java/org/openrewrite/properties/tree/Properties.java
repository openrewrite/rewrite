package org.openrewrite.properties.tree;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.properties.PropertiesRefactorVisitor;
import org.openrewrite.properties.PropertiesSourceVisitor;
import org.openrewrite.properties.internal.PrintProperties;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Properties extends Serializable, Tree {
    @Override
    default String print() {
        return new PrintProperties().visit(this);
    }

    @Override
    default <R> R accept(SourceVisitor<R> v) {
        return v instanceof PropertiesSourceVisitor ?
                acceptProperties((PropertiesSourceVisitor<R>) v) : v.defaultTo(null);
    }

    default <R> R acceptProperties(PropertiesSourceVisitor<R> v) {
        return v.defaultTo(null);
    }

    @Override
    default String getTreeType() {
        return "properties";
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class File implements Properties, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        String sourcePath;

        @With
        Map<Metadata, String> metadata;

        @With
        List<Content> content;

        @With
        Formatting formatting;

        @Override
        public Formatting getFormatting() {
            return formatting;
        }

        public Refactor<Properties.File> refactor() {
            return new Refactor<>(this);
        }

        @Override
        public <R> R acceptProperties(PropertiesSourceVisitor<R> v) {
            return v.visitFile(this);
        }
    }

    interface Content extends Properties {
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Entry implements Content {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String key;

        @With
        String value;

        @With
        Formatting equalsFormatting;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptProperties(PropertiesSourceVisitor<R> v) {
            return v.visitEntry(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Comment implements Content {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String message;

        @With
        Formatting formatting;
    }
}
