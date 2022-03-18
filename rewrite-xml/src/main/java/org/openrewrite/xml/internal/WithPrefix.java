package org.openrewrite.xml.internal;

import org.openrewrite.xml.tree.Xml;

public final class WithPrefix {
    private WithPrefix() {
    }

    public static <X extends Xml> X onlyIfNotEqual(X x, String prefix) {
        if (prefix.equals(x.getPrefix())) {
            return x;
        }
        //noinspection unchecked
        return (X) x.withPrefixUnsafe(prefix);
    }
}
