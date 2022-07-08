/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal;

import org.openrewrite.cobol.internal.registry.ASGElementRegistry;
import org.openrewrite.cobol.internal.registry.impl.ASGElementRegistryImpl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProgramImpl extends ASGElementImpl implements Program {

    protected final ASGElementRegistry asgElementRegistry = new ASGElementRegistryImpl();

    protected final Map<String, CompilationUnit> compilationUnits = new LinkedHashMap<String, CompilationUnit>();

    public ProgramImpl() {
        super(null, null);
    }

    @Override
    public ASGElementRegistry getASGElementRegistry() {
        return asgElementRegistry;
    }

    @Override
    public CompilationUnit getCompilationUnit() {
        final CompilationUnit result;

        if (getCompilationUnits().isEmpty()) {
            result = null;
        } else {
            result = getCompilationUnits().get(0);
        }

        return result;
    }

    @Override
    public CompilationUnit getCompilationUnit(final String name) {
        final String compilationUnitKey = getCompilationUnitKey(name);
        return compilationUnits.get(compilationUnitKey);
    }

    private String getCompilationUnitKey(final String name) {
        return name.toLowerCase();
    }

    @Override
    public List<CompilationUnit> getCompilationUnits() {
        return new ArrayList<>(compilationUnits.values());
    }

    @Override
    public void registerCompilationUnit(final CompilationUnit compilationUnit) {
        final String compilationUnitKey = getCompilationUnitKey(compilationUnit.getName());
        compilationUnits.put(compilationUnitKey, compilationUnit);
    }

    // Why is IntelliJ complaining about this ???
    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
