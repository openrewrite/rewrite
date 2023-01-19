package org.openrewrite.kotlin.tree;

import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserAssertions {

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before) {
        SourceSpec<K.CompilationUnit> kotlin = new SourceSpec<>(K.CompilationUnit.class, null, KotlinParser.builder(), before, null);
        isFullyParsed().accept(kotlin);
        return kotlin;
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, Consumer<SourceSpec<K.CompilationUnit>> spec) {
        SourceSpec<K.CompilationUnit> kotlin = new SourceSpec<>(K.CompilationUnit.class, null, KotlinParser.builder(), before, null);
        spec.andThen(isFullyParsed()).accept(kotlin);
        return kotlin;
    }

    public static Consumer<SourceSpec<K.CompilationUnit>> isFullyParsed() {
        return spec -> spec.afterRecipe(cu -> new JavaVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer integer) {
                assertThat(space.getWhitespace().trim()).isEmpty();
                return super.visitSpace(space, loc, integer);
            }
        }.visit(cu, 0));
    }
}
