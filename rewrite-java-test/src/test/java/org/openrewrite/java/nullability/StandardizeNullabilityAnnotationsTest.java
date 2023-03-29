package org.openrewrite.java.nullability;

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.fail;

class StandardizeNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new StandardizeNullabilityAnnotations(Nullable.class.getName(), NonNull.class.getName()));
    }

    @Test
    void removesImportIfNecessaryNullable() {
        fail("not yet implemented");
    }

    @Test
    void removesImportIfNecessaryNonNull() {
        fail("not yet implemented");
    }

    @Test
    void addsImportIfNecessaryNullable() {
        fail("not yet implemented");
    }

    @Test
    void addsImportIfNecessaryNonNull() {
        fail("not yet implemented");
    }

    @Test
    void doesNotAddImportIfUnnecessaryNullable() {
        fail("not yet implemented");
    }

    @Test
    void doesNotAddImportIfUnnecessaryNonNull() {
        fail("not yet implemented");
    }

    @Test
    void unchangedWhenNoNullabilityAnnotationWasUsed() {
        fail("not yet implemented");
    }

    @Test
    void unchangedWhenOnlyTheConfiguredNullabilityAnnotationsWereUsed() {
        fail("not yet implemented");
    }

    @Test
    void replacesAnnotationIfNonConfiguredAnnotationWasUsedNullable() {
        fail("not yet implemented");
    }

    @Test
    void replacesAnnotationIfNonConfiguredAnnotationWasUsedNonNull() {
        fail("not yet implemented");
    }

    @Test
    void replacesAllAnnotationsIfDifferentNonConfiguredAnnotationWereUsed() {
        fail("not yet implemented");
    }

    @Test
    void recipeIsIdempotent() {
        fail("not yet implemented");
    }
}
