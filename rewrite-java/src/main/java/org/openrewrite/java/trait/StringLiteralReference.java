package org.openrewrite.java.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.trait.Reference;
import org.openrewrite.trait.SimpleTraitMatcher;

import java.util.regex.Pattern;

@Value
public class StringLiteralReference implements Reference {
    Cursor cursor;
    Kind kind;
    Pattern referencePattern = Pattern.compile("\\p{javaJavaIdentifierStart}*\\.\\p{javaJavaIdentifierStart}*");

    @Override
    public String getValue() {
        if (getTree() instanceof J.Literal) {
            if (((J.Literal) getTree()).getType().equals(JavaType.Primitive.String)) {
                String literal = (String) ((J.Literal) getTree()).getValue();
                java.util.regex.Matcher match = referencePattern.matcher(literal);
                if (match.find()) {
                    return match.group(0);
                }
            }
        }
        throw new IllegalArgumentException("getTree() must be a J.Literal: " + getTree().getClass());
    }

    @Override
    public boolean supportsRename() {
        return true;
    }

    @Override
    public Tree rename(Renamer renamer, Cursor cursor, ExecutionContext ctx) {
        Tree tree = cursor.getValue();
        if (tree instanceof J.Literal) {
            J.Literal literal = (J.Literal) tree;
            if (((J.Literal) tree).getType().equals(JavaType.Primitive.String)) {
                return literal.withValue(((String)literal.getValue()).replace(getValue(), renamer.rename(this))).withValueSource(literal.getValueSource().replace(getValue(), renamer.rename(this)));
            }
        }
        return tree;
    }

    public static class Provider extends AbstractProvider<StringLiteralReference> {
        private static final SimpleTraitMatcher<StringLiteralReference> matcher = new SimpleTraitMatcher<StringLiteralReference>() {
            private final Pattern referencePattern = Pattern.compile("\\p{javaJavaIdentifierStart}*\\.\\p{javaJavaIdentifierStart}*");

            @Override
            protected @Nullable StringLiteralReference test(Cursor cursor) {
                Object value = cursor.getValue();
                if (value instanceof J.Literal) {
                    if (((J.Literal) value).getType().equals(JavaType.Primitive.String)) {
                        String literal = (String) ((J.Literal) value).getValue();
                        java.util.regex.Matcher match = referencePattern.matcher(literal);
                        if (match.find()) {
                            return new StringLiteralReference(cursor, determineKind(match.group(0)));
                        }
                    }
                }
                return null;
            }

            Reference.Kind determineKind(String value) {
                return Character.isUpperCase(value.charAt(value.lastIndexOf('.') + 1)) ? Reference.Kind.TYPE : Reference.Kind.PACKAGE;
            }
        };

        @Override
        protected SimpleTraitMatcher<StringLiteralReference> getMatcher() {
            return matcher;
        }

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            return sourceFile instanceof J.CompilationUnit;
        }
    }

}
