package org.openrewrite.java.controlflow;

import java.lang.reflect.InvocationTargetException;

interface ControlFlowDotFileGenerator {
    String visualizeAsDotfile(String name, ControlFlowSummary summary);

    enum Type {
        GRAPHVIZ {
            @Override
            boolean isAvailable() {
                return ControlFlowDotFileGenerator.class.getClassLoader().getResource("org/openrewrite/java/controlflow/GraphvizControlFlowVisualizer.class") != null;
            }

            @Override
            ControlFlowDotFileGenerator create() {
                if (isAvailable()) {
                    try {
                        Class<?> visualizer =
                                ControlFlowDotFileGenerator.class.getClassLoader().loadClass("org.openrewrite.java.controlflow.GraphvizControlFlowVisualizer");
                        return (ControlFlowDotFileGenerator) visualizer.getMethod("asControlFlowDotFileGenerator").invoke(null);
                    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to load class", e);
                    }
                }
                throw new RuntimeException("GraphvizControlFlowVisualizer not available");
            }
        },
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
