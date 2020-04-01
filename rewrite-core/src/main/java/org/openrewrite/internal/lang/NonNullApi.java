package org.openrewrite.internal.lang;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.*;

/**
 * A common annotation to declare that parameters and return values
 * are to be considered as non-nullable by default for a given package.
 * <p>Leverages JSR-305 meta-annotations to indicate nullability in Java to common
 * tools with JSR-305 support and used by Kotlin to infer nullability of the API.
 * <p>Should be used at package level in association with {@link Nullable}
 * annotations at parameter and return value level.
 *
 * @see NonNullFields
 * @see Nullable
 * @see NonNull
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
public @interface NonNullApi {
}
