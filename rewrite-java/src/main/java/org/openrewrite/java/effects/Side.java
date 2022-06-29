package org.openrewrite.java.effects;

public enum Side {
    LVALUE, // refers to an expression being assigned to (e.g. left side of an assignment)
    RVALUE; // refers to an expression being evaluated (e.g. right side of an assignment)
}
