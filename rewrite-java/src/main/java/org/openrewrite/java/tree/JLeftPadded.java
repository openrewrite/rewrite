package org.openrewrite.java.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;

/**
 * A Java element that could have space preceding some delimiter.
 * For example an array dimension could have space before the opening
 * bracket, and the containing {@link #elem} could have a prefix that occurs
 * after the bracket.
 *
 * @param <T>
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class JLeftPadded<T> {
    @With
    Space before;

    @With
    T elem;
}
