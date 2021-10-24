package org.openrewrite.java.internal.cache;

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ClasspathJavaTypeCache implements JavaTypeCache {
    private final Map<Path, Map<String, JavaType.Class>> classCache = new HashMap<>();
    private final Map<Path, Map<ShallowMethodSignature, JavaType.Method>> methodCache = new HashMap<>();
    private final Map<Path, Map<ShallowParameterizedSignature, JavaType.Parameterized>> parameterizedCache = new HashMap<>();
    private final Map<Path, Map<ShallowVariable, JavaType.Variable>> variableCache = new HashMap<>();
    private final Map<Path, Map<ShallowGeneric, JavaType.GenericTypeVariable>> genericCache = new HashMap<>();

    @Override
    public JavaType.Class computeClass(Path classpathElement, String fullyQualifiedName, Supplier<JavaType.Class> fq) {
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
                                         List<String> resolvedArgumentTypeSignatures, Supplier<JavaType.Method> m) {
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
    public JavaType.GenericTypeVariable computeGeneric(Path classpathElement, String name, @Nullable String fullyQualifiedName, Supplier<JavaType.GenericTypeVariable> g) {
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
    public JavaType.Parameterized computeParameterized(Path classpathElement, String fullyQualifiedName, List<String> typeVariableSignatures, Supplier<JavaType.Parameterized> p) {
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
        List<String> argumentTypes;
    }

    @Value
    private static class ShallowParameterizedSignature {
        String fullyQualifiedName;
        List<String> typeVariableSignatures;
    }

    @Value
    private static class ShallowVariable {
        String fullyQualifiedName;
        String variable;
    }
}
