package org.openrewrite.kotlin.tree;

import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Space;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserAsserts {

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
