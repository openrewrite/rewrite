/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.internal;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.quarkus.gizmo.*;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

import java.lang.reflect.*;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class TreeVisitorAdapter {
    private static final Integer classCreationLock = 1;
    private static final Map<ClassLoader, TreeVisitorAdapterClassLoader> classLoaders = new IdentityHashMap<>();

    private TreeVisitorAdapter() {
    }

    public static void unload(ClassLoader parentClassLoader) {
        classLoaders.remove(parentClassLoader);
    }

    @SafeVarargs
    public static <T extends Tree, Adapted> Adapted adapt(TreeVisitor<T, ?> delegate, Class<Adapted> adaptTo,
                                                          TreeVisitor<? extends T, ?>... mixins) {
        if (mixins.length > 1) {
            throw new IllegalArgumentException(
                    "TreeVisitorAdapter currently supports at most one mixin per adapt() call; got " + mixins.length + ".");
        }

        TreeVisitor<?, ?> mixin = mixins.length == 1 ? mixins[0] : discoverMixinViaSpi(delegate, adaptTo);
        if (mixin != null && !adaptTo.isAssignableFrom(mixin.getClass())) {
            throw new IllegalArgumentException(
                    "Mixin " + mixin.getClass().getName() + " is not assignable to " + adaptTo.getName() +
                    "; the mixin must extend (or be a subtype of) the adaptTo class so that proxy generation can extend it.");
        }

        Timer.Sample timer = Timer.start();

        //noinspection rawtypes
        Class<? extends TreeVisitor> delegateType = adapterDelegateType(delegate);
        Class<?> mixinClass = mixin == null ? null : mixin.getClass();
        Class<?> proxySuper = mixinClass == null ? adaptTo : mixinClass;

        String adaptedName = delegate.getClass().getName().trim().replace('$', '_') +
                             "_" + adaptTo.getSimpleName() +
                             (mixinClass == null ? "" : "_" + mixinClass.getName().trim().replace('$', '_'));

        TreeVisitorAdapterClassLoader cl;
        synchronized (classLoaders) {
            cl = classLoaders.computeIfAbsent(delegate.getClass().getClassLoader(), TreeVisitorAdapterClassLoader::new);
        }

        if (!cl.hasClass(adaptedName)) {
            synchronized (classCreationLock) {
                if (!cl.hasClass(adaptedName)) {
                    try (ClassCreator creator = ClassCreator.builder()
                            .classOutput(cl)
                            .className(adaptedName)
                            .superClass(proxySuper)
                            .build()) {

                        FieldCreator delegateField = creator.getFieldCreator("delegate", delegateType);
                        delegateField.setModifiers(Modifier.PRIVATE);

                        MethodCreator setDelegate = creator.getMethodCreator("setDelegate", void.class, delegateType);
                        setDelegate.setModifiers(Modifier.PUBLIC);
                        setDelegate.writeInstanceField(delegateField.getFieldDescriptor(), setDelegate.getThis(),
                                setDelegate.getMethodParam(0));
                        setDelegate.invokeSpecialMethod(
                                MethodDescriptor.ofMethod(proxySuper, "setCursor", void.class, Cursor.class),
                                setDelegate.getThis(),
                                setDelegate.invokeVirtualMethod(MethodDescriptor.ofMethod(delegateType, "getCursor", Cursor.class),
                                        setDelegate.getMethodParam(0))
                        );
                        setDelegate.returnValue(null);

                        MethodCreator setCursor = creator.getMethodCreator("setCursor", void.class, Cursor.class);
                        setCursor.setModifiers(Modifier.PUBLIC);
                        setCursor.invokeSpecialMethod(
                                MethodDescriptor.ofMethod(proxySuper, "setCursor", void.class, Cursor.class),
                                setCursor.getThis(),
                                setCursor.getMethodParam(0)
                        );
                        setCursor.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(delegateType, "setCursor", void.class, Cursor.class),
                                setCursor.readInstanceField(delegateField.getFieldDescriptor(), setCursor.getThis()),
                                setCursor.getMethodParam(0)
                        );
                        setCursor.returnValue(null);

                        // Walk delegate's class hierarchy (down to but excluding TreeVisitor)
                        // and collect every visit-like method declared at each level — typed
                        // overrides AND synthetic bridges. JVM virtual dispatch picks based on
                        // erased descriptor; e.g., J.Annotation.acceptJava emits
                        // invokevirtual JavaVisitor.visitAnnotation(J.Annotation, Object) → J,
                        // and the proxy must define a method with that exact descriptor
                        // (not just the typed RemoveAnnotationVisitor.visitAnnotation(..., ExecutionContext))
                        // for the forwarding to actually be the dispatch target.
                        java.util.LinkedHashMap<MethodKey, Method> methodsByKey = new java.util.LinkedHashMap<>();
                        for (Method m : delegate.getClass().getDeclaredMethods()) {
                            if (!isVisitLike(m) || Modifier.isStatic(m.getModifiers()) || Modifier.isPrivate(m.getModifiers())) {
                                continue;
                            }
                            MethodKey key = new MethodKey(m.getName(), m.getParameterTypes(), m.getReturnType());
                            methodsByKey.putIfAbsent(key, m);
                        }

                        // For visit*(SpecificNode) override-skip: name+arity match against any
                        // user-declared (non-synthetic) mixin method. If the mixin overrides
                        // visitClassDeclaration at one signature, skip generating proxy methods
                        // for ALL same-name same-arity delegate methods (typed + bridges), so
                        // dispatch falls through to the mixin via inheritance.
                        for (Method method : methodsByKey.values()) {
                            boolean isPreOrPost = "preVisit".equals(method.getName()) || "postVisit".equals(method.getName());
                            boolean mixinOverridesByName = mixinClass != null && !isPreOrPost &&
                                    declaresUserOverrideByNameAndArity(mixinClass, adaptTo, method);
                            if (mixinOverridesByName) {
                                continue;
                            }
                            boolean mixinDeclaresSame = mixinClass != null && isPreOrPost &&
                                    declaresExactSignature(mixinClass, adaptTo, method);

                            MethodCreator visitMethod = creator.getMethodCreator(
                                    method.getName(),
                                    method.getReturnType(),
                                    (Object[]) method.getParameterTypes());
                            visitMethod.setModifiers(method.getModifiers());

                            int paramLength = method.getParameters().length;
                            ResultHandle[] args = new ResultHandle[paramLength];
                            for (int i = 0; i < paramLength; i++) {
                                args[i] = visitMethod.getMethodParam(i);
                            }

                            ResultHandle delegateCall = visitMethod.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(delegateType, method.getName(),
                                            DescriptorUtils.classToStringRepresentation(method.getReturnType()),
                                            (Object[]) method.getParameterTypes()),
                                    visitMethod.readInstanceField(delegateField.getFieldDescriptor(), visitMethod.getThis()),
                                    args
                            );

                            // preVisit/postVisit fan-out: after delegate, also fire the mixin's
                            // version via super (proxy.super == mixinClass).
                            if (mixinClass != null && isPreOrPost && mixinDeclaresSame) {
                                // If the delegate returned null, skip the mixin call and propagate null.
                                BranchResult nullCheck = visitMethod.ifNull(delegateCall);
                                BytecodeCreator nullBranch = nullCheck.trueBranch();
                                nullBranch.returnValue(nullBranch.loadNull());

                                BytecodeCreator nonNull = nullCheck.falseBranch();
                                ResultHandle[] superArgs = args.clone();
                                superArgs[0] = delegateCall;
                                ResultHandle superCall = nonNull.invokeSpecialMethod(
                                        MethodDescriptor.ofMethod(mixinClass, method.getName(),
                                                DescriptorUtils.classToStringRepresentation(method.getReturnType()),
                                                (Object[]) method.getParameterTypes()),
                                        nonNull.getThis(),
                                        superArgs
                                );
                                nonNull.returnValue(superCall);
                            } else {
                                visitMethod.returnValue(delegateCall);
                            }
                        }
                    }
                }
            }
        }

        try {
            Class<?> a = cl.loadClass(adaptedName);

            @SuppressWarnings("unchecked")
            Adapted adapted = (Adapted) a.getDeclaredConstructor().newInstance();

            if (mixin != null) {
                copyMixinInstanceFields(mixin, adapted);
            }
            a.getDeclaredMethod("setDelegate", delegateType).invoke(adapted, delegate);
            return adapted;
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 ClassNotFoundException e) {
            timer.stop(MetricsHelper.errorTags(Timer.builder("rewrite.visitor.adapt")
                    .tag("adapt.to", adaptTo.getSimpleName()), e).register(Metrics.globalRegistry));
            throw new RuntimeException(e);
        }
    }

    private static boolean declaresUserOverrideByNameAndArity(Class<?> mixinClass, Class<?> adaptTo, Method baseMethod) {
        // Only consider methods declared on the immediate mixin class —
        // that is, what the user wrote. Methods inherited from the language
        // iso-visitor (e.g., JavaIsoVisitor declares visitIdentifier) or from
        // adaptTo are ambient, not real overrides, and would otherwise cause
        // us to skip generating forwarding for nearly every visit method.
        for (Method m : mixinClass.getDeclaredMethods()) {
            if (m.isSynthetic() || m.isBridge()) {
                continue;
            }
            if (m.getName().equals(baseMethod.getName()) &&
                m.getParameterCount() == baseMethod.getParameterCount()) {
                return true;
            }
        }
        return false;
    }

    private static final class MethodKey {
        final String name;
        final Class<?>[] paramTypes;
        final Class<?> returnType;

        MethodKey(String name, Class<?>[] paramTypes, Class<?> returnType) {
            this.name = name;
            this.paramTypes = paramTypes;
            this.returnType = returnType;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MethodKey)) {
                return false;
            }
            MethodKey other = (MethodKey) o;
            return name.equals(other.name) &&
                   returnType.equals(other.returnType) &&
                   java.util.Arrays.equals(paramTypes, other.paramTypes);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + returnType.hashCode() + java.util.Arrays.hashCode(paramTypes);
        }
    }

    private static boolean isVisitLike(Method method) {
        return method.getName().startsWith("visit") ||
               "preVisit".equals(method.getName()) ||
               "postVisit".equals(method.getName());
    }

    private static boolean declaresExactSignature(Class<?> mixinClass, Class<?> adaptTo, Method baseMethod) {
        Class<?>[] baseParams = baseMethod.getParameterTypes();
        Class<?> c = mixinClass;
        while (c != null && c != Object.class && c != adaptTo && !TreeVisitor.class.equals(c)) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(baseMethod.getName()) ||
                    m.getParameterCount() != baseParams.length) {
                    continue;
                }
                Class<?>[] mParams = m.getParameterTypes();
                boolean match = true;
                for (int i = 0; i < baseParams.length; i++) {
                    if (!baseParams[i].equals(mParams[i])) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return true;
                }
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private static boolean declaresOverride(Class<?> mixinClass, Method baseMethod) {
        Class<?>[] baseParams = baseMethod.getParameterTypes();
        Class<?> c = mixinClass;
        while (c != null && c != Object.class && !TreeVisitor.class.equals(c)) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(baseMethod.getName()) ||
                    m.getParameterCount() != baseParams.length ||
                    m.isSynthetic()) {
                    continue;
                }
                Class<?>[] mParams = m.getParameterTypes();
                boolean match = true;
                for (int i = 0; i < baseParams.length; i++) {
                    if (!baseParams[i].equals(mParams[i])) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return true;
                }
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private static void copyMixinInstanceFields(TreeVisitor<?, ?> mixin, Object proxy) throws IllegalAccessException {
        Class<?> c = mixin.getClass();
        while (c != null && c != Object.class && c != TreeVisitor.class) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                f.setAccessible(true);
                f.set(proxy, f.get(mixin));
            }
            c = c.getSuperclass();
        }
    }

    private static @org.jspecify.annotations.Nullable TreeVisitor<?, ?> discoverMixinViaSpi(TreeVisitor<?, ?> delegate, Class<?> adaptTo) {
        // Each language module ships per-base-visitor SPI files at
        //   META-INF/services/<base-visitor-fqn>
        // listing mixin classes that should compose with that base. We parse
        // these files directly rather than using {@link ServiceLoader} because
        // ServiceLoader requires the registered class to be a subtype of the
        // service class — but a mixin extends the language module's iso-visitor
        // (e.g., KotlinIsoVisitor), not the base visitor it composes with
        // (e.g., RemoveAnnotationVisitor). They share the visitor role
        // structurally, not via inheritance.
        //
        // Anchor resource lookup to adaptTo's classloader so the language
        // module's own SPI files (shipped in the language module's JAR) are
        // visible.
        ClassLoader cl = adaptTo.getClassLoader();
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        if (cl == null) {
            return null;
        }
        String resource = "META-INF/services/" + delegate.getClass().getName();
        try {
            java.util.Enumeration<java.net.URL> urls = cl.getResources(resource);
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(url.openStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int hash = line.indexOf('#');
                        if (hash >= 0) {
                            line = line.substring(0, hash);
                        }
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue;
                        }
                        try {
                            Class<?> mixinClass = Class.forName(line, false, cl);
                            if (!TreeVisitor.class.isAssignableFrom(mixinClass)) {
                                throw new IllegalStateException(
                                        "SPI-registered mixin " + line + " is not a TreeVisitor (registered at " + url + ").");
                            }
                            return (TreeVisitor<?, ?>) mixinClass.getDeclaredConstructor().newInstance();
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalStateException(
                                    "Failed to instantiate SPI-registered mixin " + line + " (from " + url + ").", e);
                        }
                    }
                }
            }
        } catch (java.io.IOException e) {
            // Couldn't enumerate SPI resources; treat as no registration.
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private static Class<? extends TreeVisitor> adapterDelegateType(TreeVisitor<?, ?> delegate) {
        for (TypeVariable<? extends Class<? extends TreeVisitor>> tp : delegate.getClass().getTypeParameters()) {
            for (Type bound : tp.getBounds()) {
                if (bound instanceof Class && Tree.class.isAssignableFrom((Class<?>) bound)) {
                    return delegate.getClass();
                }
            }
        }

        Class<?> v2 = delegate.getClass();
        Type sup = v2.getGenericSuperclass();
        for (int i = 0; i < 20; i++) {
            if (sup instanceof ParameterizedType) {
                for (Type bound : ((ParameterizedType) sup).getActualTypeArguments()) {
                    if (bound instanceof Class && Tree.class.isAssignableFrom((Class<?>) bound)) {
                        if (delegate.getLanguage() == null) {
                            //noinspection unchecked
                            return (Class<? extends TreeVisitor>) ((ParameterizedType) sup).getRawType();
                        }
                        //noinspection unchecked
                        return (Class<? extends TreeVisitor>) v2;
                    }
                }
                sup = ((ParameterizedType) sup).getRawType();
            } else if (sup instanceof Class) {
                v2 = (Class<?>) sup;
                if (v2.getName().endsWith("IsoVisitor")) {
                    //noinspection unchecked
                    return (Class<? extends TreeVisitor>) v2;
                }
                sup = ((Class<?>) sup).getGenericSuperclass();
            }
        }
        throw new IllegalArgumentException("Expected to find a tree type somewhere in the type parameters of the " +
                                           "type hierarchy of visitor " + delegate.getClass().getName());
    }
}
