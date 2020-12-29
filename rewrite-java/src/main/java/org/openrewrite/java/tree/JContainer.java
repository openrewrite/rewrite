package org.openrewrite.java.tree;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * AST elements that contain lists of trees with some delimiter like parentheses, e.g. method arguments,
 * annotation arguments, catch variable declarations.
 *
 * Sometimes the delimiter surrounds the list. Parentheses surround method arguments. Sometimes the delimiter only
 * precedes the list. Throws statements on method declarations are preceded by the "throws" keyword.
 *
 * Sometimes containers are optional in the grammar, as in the
 * case of annotation arguments. Sometimes they are required, as in the case of method invocation arguments.
 *
 * @param <T> The type of the inner list of elements.
 */
public class JContainer<T> {
    private static final JContainer<?> EMPTY = new JContainer<>(Space.EMPTY, emptyList());

    private final Space before;
    private final List<JRightPadded<T>> elem;

    private JContainer(Space before, List<JRightPadded<T>> elem) {
        this.before = before;
        this.elem = elem;
    }

    public static <T> JContainer<T> build(Space before, List<JRightPadded<T>> elem) {
        if (before.isEmpty() && elem.isEmpty()) {
            return empty();
        }
        return new JContainer<>(before, elem);
    }

    @SuppressWarnings("unchecked")
    public static <T> JContainer<T> empty() {
        return (JContainer<T>) EMPTY;
    }

    public JContainer<T> withBefore(Space before) {
        return build(before, elem);
    }

    public JContainer<T> withElem(List<JRightPadded<T>> elem) {
        return build(getBefore(), elem);
    }

    public List<JRightPadded<T>> getElem() {
        return elem;
    }

    public Space getBefore() {
        return before;
    }
}
