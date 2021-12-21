package org.openrewrite.java;

import java.util.List;

public interface JavaTypeGoat {
    void clazz(Integer n);
    void primitive(int n);
    void array(Integer[] n);
    void parameterized(List<String> n);
}
