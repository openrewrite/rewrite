package org.openrewrite.java;

import java.util.List;

public interface JavaTypeGoat<T extends JavaTypeGoat<? extends T> & List<?>> {
    void clazz(Integer n);
    void primitive(int n);
    void array(Integer[] n);
    void parameterized(List<String> n);
    void generic(List<? extends java.lang.String> n);
    void genericContravariant(List<? super java.lang.String> n);
    <U> void genericUnbounded(List<U> n);
}
