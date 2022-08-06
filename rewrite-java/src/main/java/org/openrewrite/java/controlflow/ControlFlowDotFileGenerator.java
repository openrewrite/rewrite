package org.openrewrite.java.controlflow;

interface ControlFlowDotFileGenerator {
    String visualizeAsDotfile(String name, ControlFlowSummary summary);

    enum Type {
        DOT {
            @Override
            ControlFlowDotFileGenerator create() {
                return new ControlFlowSummaryDotVisualizer();
            }

            @Override
            boolean isAvailable() {
                return true;
            }
        };

        abstract ControlFlowDotFileGenerator create();

        abstract boolean isAvailable();
    }

    static ControlFlowDotFileGenerator create() {
        for (Type visualizer : Type.values()) {
            if (visualizer.isAvailable()) {
                return visualizer.create();
            }
        }
        throw new RuntimeException("No available control flow visualizer");
    }
}
