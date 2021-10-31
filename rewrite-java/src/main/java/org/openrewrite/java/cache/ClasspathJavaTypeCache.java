/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.cache;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ClasspathJavaTypeCache implements JavaTypeCache {
    @With
    @Nullable
    private final JavaTypeCache next;

    @Nullable
    private final Predicate<Path> pathPredicate;

    private final Map<Path, Map<String, JavaType.Class>> classCache;
    private final Map<Path, Map<ShallowMethodSignature, JavaType.Method>> methodCache;
    private final Map<Path, Map<ShallowParameterizedSignature, JavaType.Parameterized>> parameterizedCache;
    private final Map<Path, Map<ShallowVariable, JavaType.Variable>> variableCache;
    private final Map<Path, Map<ShallowGeneric, JavaType.GenericTypeVariable>> genericCache;

    public ClasspathJavaTypeCache(JavaTypeCache next, Predicate<Path> pathPredicate) {
        this(next, pathPredicate, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public ClasspathJavaTypeCache() {
        this(null, null);
    }

    @Override
    public JavaType.Class computeClass(Path classpathElement, String fullyQualifiedName, Supplier<JavaType.Class> fq) {
        if (pathPredicate != null && next != null && !pathPredicate.test(classpathElement)) {
            return next.computeClass(classpathElement, fullyQualifiedName, fq);
        }

        if (!classCache.containsKey(classpathElement)) {
            classCache.put(classpathElement, new HashMap<>());
        }

        JavaType.Class ct = classCache.get(classpathElement).get(fullyQualifiedName);
        if (ct == null) {
            ct = fq.get();
            classCache.get(classpathElement).put(fullyQualifiedName, ct);
        }
        return ct;
    }

    @Override
    public JavaType.Method computeMethod(Path classpathElement, String fullyQualifiedName, String method, String resolvedReturnType,
                                         String resolvedArgumentTypeSignatures, Supplier<JavaType.Method> m) {
        if (pathPredicate != null && next != null && !pathPredicate.test(classpathElement)) {
            return next.computeMethod(classpathElement, fullyQualifiedName, method, resolvedReturnType,
                    resolvedArgumentTypeSignatures, m);
        }

        if (!methodCache.containsKey(classpathElement)) {
            methodCache.put(classpathElement, new HashMap<>());
        }

        ShallowMethodSignature key = new ShallowMethodSignature(fullyQualifiedName, method, resolvedReturnType,
                resolvedArgumentTypeSignatures);
        JavaType.Method mt = methodCache.get(classpathElement).get(key);
        if (mt == null) {
            mt = m.get();
            methodCache.get(classpathElement).put(key, mt);
        }
        return mt;
    }

    @Override
    public JavaType.GenericTypeVariable computeGeneric(Path classpathElement, String name, @Nullable String fullyQualifiedName,
                                                       Supplier<JavaType.GenericTypeVariable> g) {
        if (pathPredicate != null && next != null && !pathPredicate.test(classpathElement)) {
            return next.computeGeneric(classpathElement, name, fullyQualifiedName, g);
        }

        if (!genericCache.containsKey(classpathElement)) {
            genericCache.put(classpathElement, new HashMap<>());
        }

        ShallowGeneric key = new ShallowGeneric(name, fullyQualifiedName);
        JavaType.GenericTypeVariable gtv = genericCache.get(classpathElement).get(key);
        if (gtv == null) {
            gtv = g.get();
            genericCache.get(classpathElement).put(key, gtv);
        }
        return gtv;
    }

    @Override
    public JavaType.Parameterized computeParameterized(Path classpathElement, String fullyQualifiedName, String typeVariableSignatures, Supplier<JavaType.Parameterized> p) {
        if (pathPredicate != null && next != null && !pathPredicate.test(classpathElement)) {
            return next.computeParameterized(classpathElement, fullyQualifiedName, typeVariableSignatures, p);
        }

        if (!parameterizedCache.containsKey(classpathElement)) {
            parameterizedCache.put(classpathElement, new HashMap<>());
        }

        ShallowParameterizedSignature key = new ShallowParameterizedSignature(fullyQualifiedName, typeVariableSignatures);
        JavaType.Parameterized pt = parameterizedCache.get(classpathElement).get(key);
        if (pt == null) {
            pt = p.get();
            parameterizedCache.get(classpathElement).put(key, pt);
        }
        return pt;
    }

    @Override
    public JavaType.Variable computeVariable(Path classpathElement, String fullyQualifiedName, String variable, Supplier<JavaType.Variable> v) {
        if (pathPredicate != null && next != null && !pathPredicate.test(classpathElement)) {
            return next.computeVariable(classpathElement, fullyQualifiedName, variable, v);
        }

        if (!variableCache.containsKey(classpathElement)) {
            variableCache.put(classpathElement, new HashMap<>());
        }

        ShallowVariable key = new ShallowVariable(fullyQualifiedName, variable);
        JavaType.Variable vt = variableCache.get(classpathElement).get(key);
        if (vt == null) {
            vt = v.get();
            variableCache.get(classpathElement).put(key, vt);
        }
        return vt;
    }

    public void clear() {
        classCache.clear();
        genericCache.clear();
        methodCache.clear();
        parameterizedCache.clear();
        variableCache.clear();
    }

    @Value
    private static class ShallowGeneric {
        String name;
        String fullyQualifiedName;
    }

    @Value
    private static class ShallowMethodSignature {
        String fullyQualifiedName;
        String method;
        String returnType;
        String argumentTypes;
    }

    @Value
    private static class ShallowParameterizedSignature {
        String fullyQualifiedName;
        String typeVariableSignatures;
    }

    @Value
    private static class ShallowVariable {
        String fullyQualifiedName;
        String variable;
    }
}
