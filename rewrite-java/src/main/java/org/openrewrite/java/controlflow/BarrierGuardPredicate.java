package org.openrewrite.java.controlflow;

@FunctionalInterface
public interface BarrierGuardPredicate {
    boolean isBarrierGuard(Guard guard, boolean branch);
}
