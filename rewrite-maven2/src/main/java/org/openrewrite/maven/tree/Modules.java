package org.openrewrite.maven.tree;

import org.openrewrite.Metadata;

import java.util.Collection;

public class Modules implements Metadata {
    private final Collection<Pom> modules;

    public Modules(Collection<Pom> modules) {
        this.modules = modules;
    }

    public Collection<Pom> getModules() {
        return modules;
    }
}
