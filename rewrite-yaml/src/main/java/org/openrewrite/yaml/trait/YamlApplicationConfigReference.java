package org.openrewrite.yaml.trait;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.yaml.tree.Yaml;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class YamlApplicationConfigReference extends YamlReference {
    Cursor cursor;
    @Getter
    Kind kind;

    public static class Provider extends YamlProvider {
        private static final Predicate<String> applicationPropertiesMatcher = Pattern.compile("^application(-\\w+)?\\.(yaml|yml)$").asPredicate();

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            return super.isAcceptable(sourceFile) && applicationPropertiesMatcher.test(sourceFile.getSourcePath().getFileName().toString());
        }

        @Override
        SimpleTraitMatcher<YamlReference> getMatcher() {
            return new SimpleTraitMatcher<YamlReference>() {
                private final Predicate<String> javaFullyQualifiedTypePattern = Pattern.compile(
                                "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*")
                        .asPredicate();

                @Override
                protected @Nullable YamlReference test(Cursor cursor) {
                    Object value = cursor.getValue();
                    if (value instanceof Yaml.Scalar && javaFullyQualifiedTypePattern.test(((Yaml.Scalar) value).getValue())) {
                        return new YamlApplicationConfigReference(cursor, determineKind(((Yaml.Scalar) value).getValue()));
                    }
                    return null;
                }

                private Kind determineKind(String value) {
                    return Character.isUpperCase(value.charAt(value.lastIndexOf('.') + 1)) ? Kind.TYPE : Kind.PACKAGE;
                }
            };
        }
    }
}
