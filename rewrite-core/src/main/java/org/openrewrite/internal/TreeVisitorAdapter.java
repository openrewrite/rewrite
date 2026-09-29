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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.invoke.MethodType.methodType;

public class TreeVisitorAdapter {
    private static final Object classCreationLock = new Object();
    private static final Map<ClassLoader, TreeVisitorAdapterClassLoader> classLoaders = new ConcurrentHashMap<>();

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

        TreeVisitor<?, ?> mixin = mixins.length == 1 ? mixins[0] : null;
        if (mixin != null && !adaptTo.isAssignableFrom(mixin.getClass())) {
            throw new IllegalArgumentException(
                    "Mixin " + mixin.getClass().getName() + " is not assignable to " + adaptTo.getName() +
                    "; the mixin must extend (or be a subtype of) the adaptTo class so that proxy generation can extend it.");
        }

        ClassLoader parent = delegate.getClass().getClassLoader();
        TreeVisitorAdapterClassLoader cl = classLoaders.get(parent);
        if (cl == null) {
            cl = classLoaders.computeIfAbsent(parent, TreeVisitorAdapterClassLoader::new);
        }

        // adapt() runs for every foreign-language node a visitor reaches, so the reflective work is
        // done once per (delegate class, adaptTo, mixin class) and the hot path only instantiates.
        AdapterKey key = new AdapterKey(delegate.getClass(), adaptTo, mixin == null ? null : mixin.getClass());
        Adapter adapter = cl.getAdapter(key);
        if (adapter == null) {
            adapter = cl.putAdapterIfAbsent(key, createAdapter(cl, delegate, adaptTo, mixin));
        }

        try {
            //noinspection unchecked
            return (Adapted) adapter.newInstance(delegate, mixin);
        } catch (Throwable t) {
            throw adaptFailed(adaptTo, t);
        }
    }

    private static Adapter createAdapter(TreeVisitorAdapterClassLoader cl, TreeVisitor<?, ?> delegate,
                                         Class<?> adaptTo, @Nullable TreeVisitor<?, ?> mixin) {
        Class<?> registeredMixinClass = mixin == null ? cl.mixinClass(delegate.getClass(), adaptTo).orElse(null) : null;

        //noinspection rawtypes
        Class<? extends TreeVisitor> delegateType = adapterDelegateType(delegate);
        Class<?> mixinClass = mixin == null ? registeredMixinClass : mixin.getClass();
        Class<?> proxySuper = mixinClass == null ? adaptTo : mixinClass;

        String adaptedName = delegate.getClass().getName().trim().replace('$', '_') +
                             "_" + adaptTo.getSimpleName() +
                             (mixinClass == null ? "" : "_" + mixinClass.getName().trim().replace('$', '_'));

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

                        // Collect the user's visit-like overrides from the leaf up to (but not into) the
                        // iso-visitor, so overrides declared on a shared superclass are forwarded too.
                        // Leaf-first, so the most-derived override wins.
                        Map<MethodKey, Method> methodsByKey = new LinkedHashMap<>();
                        Class<?> leafClass = delegate.getClass();
                        for (Class<?> c = leafClass;
                             c != null && c != Object.class && !TreeVisitor.class.equals(c);
                             c = c.getSuperclass()) {
                            if (c == delegateType && c != leafClass) {
                                break;
                            }
                            for (Method m : c.getDeclaredMethods()) {
                                if (!isVisitLike(m) || Modifier.isStatic(m.getModifiers()) || Modifier.isPrivate(m.getModifiers())) {
                                    continue;
                                }
                                methodsByKey.putIfAbsent(new MethodKey(m.getName(), m.getParameterTypes(), m.getReturnType()), m);
                            }
                            if (c == delegateType) {
                                break;
                            }
                        }

                        // If the mixin declares its own visitX override, don't proxy the delegate's
                        // same-name/arity methods (typed or bridge) — let dispatch fall through to it.
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
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return new Adapter(
                    lookup.findConstructor(a, methodType(void.class)).asType(methodType(Object.class)),
                    lookup.findVirtual(a, "setDelegate", methodType(void.class, delegateType))
                            .asType(methodType(void.class, Object.class, TreeVisitor.class)),
                    registeredMixinClass == null ? null :
                            lookup.findConstructor(registeredMixinClass, methodType(void.class))
                                    .asType(methodType(TreeVisitor.class)),
                    mixinClass == null ? new Field[0] : mixinInstanceFields(mixinClass)
            );
        } catch (ReflectiveOperationException e) {
            throw adaptFailed(adaptTo, e);
        }
    }

    private static RuntimeException adaptFailed(Class<?> adaptTo, Throwable t) {
        Timer.start().stop(MetricsHelper.errorTags(Timer.builder("rewrite.visitor.adapt")
                .tag("adapt.to", adaptTo.getSimpleName()), t).register(Metrics.globalRegistry));
        return new RuntimeException(t);
    }

    private static boolean declaresUserOverrideByNameAndArity(Class<?> mixinClass, Class<?> adaptTo, Method baseMethod) {
        // Only methods the user actually declared on the mixin count; inherited iso-visitor/adaptTo
        // methods are ambient, not real overrides.
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
                   Arrays.equals(paramTypes, other.paramTypes);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + returnType.hashCode() + Arrays.hashCode(paramTypes);
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

    private static Field[] mixinInstanceFields(Class<?> mixinClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> c = mixinClass;
        while (c != null && c != Object.class && c != TreeVisitor.class) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                f.setAccessible(true);
                fields.add(f);
            }
            c = c.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    static Optional<Class<?>> discoverRegisteredMixinClass(Class<?> delegateClass, Class<?> adaptTo) {
        // Mixins are registered in META-INF/rewrite/mixins/<base-visitor-fqn>. Not ServiceLoader:
        // its subtype check rejects mixins, which extend the language iso-visitor (e.g.
        // KotlinIsoVisitor) rather than the base visitor they compose with. Anchor lookup to
        // adaptTo's classloader so the language module's own registry files are visible.
        ClassLoader cl = adaptTo.getClassLoader();
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        if (cl == null) {
            return Optional.empty();
        }
        String resource = "META-INF/rewrite/mixins/" + delegateClass.getName();
        try {
            Enumeration<URL> urls = cl.getResources(resource);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
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
                                        "Registered mixin " + line + " is not a TreeVisitor (registered at " + url + ").");
                            }
                            if (!adaptTo.isAssignableFrom(mixinClass)) {
                                continue;
                            }
                            return Optional.of(mixinClass);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException(
                                    "Failed to load registered mixin " + line + " (from " + url + ").", e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Couldn't enumerate mixin registry resources; treat as no registration.
        }
        return Optional.empty();
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

    static final class AdapterKey {
        private final Class<?> delegateClass;
        private final Class<?> adaptTo;
        private final @Nullable Class<?> mixinClass;

        AdapterKey(Class<?> delegateClass, Class<?> adaptTo, @Nullable Class<?> mixinClass) {
            this.delegateClass = delegateClass;
            this.adaptTo = adaptTo;
            this.mixinClass = mixinClass;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AdapterKey)) {
                return false;
            }
            AdapterKey other = (AdapterKey) o;
            return delegateClass == other.delegateClass && adaptTo == other.adaptTo && mixinClass == other.mixinClass;
        }

        @Override
        public int hashCode() {
            return (delegateClass.hashCode() * 31 + adaptTo.hashCode()) * 31 + Objects.hashCode(mixinClass);
        }
    }

    static final class Adapter {
        private final MethodHandle constructor;
        private final MethodHandle setDelegate;
        private final @Nullable MethodHandle registeredMixinConstructor;
        private final Field[] mixinFields;

        Adapter(MethodHandle constructor, MethodHandle setDelegate,
                @Nullable MethodHandle registeredMixinConstructor, Field[] mixinFields) {
            this.constructor = constructor;
            this.setDelegate = setDelegate;
            this.registeredMixinConstructor = registeredMixinConstructor;
            this.mixinFields = mixinFields;
        }

        Object newInstance(TreeVisitor<?, ?> delegate, @Nullable TreeVisitor<?, ?> mixin) throws Throwable {
            Object adapted = constructor.invokeExact();
            if (mixin == null && registeredMixinConstructor != null) {
                // A fresh registered mixin for every proxy, as its fields are copied onto the proxy
                mixin = (TreeVisitor<?, ?>) registeredMixinConstructor.invokeExact();
            }
            if (mixin != null) {
                for (Field f : mixinFields) {
                    f.set(adapted, f.get(mixin));
                }
            }
            setDelegate.invokeExact(adapted, delegate);
            return adapted;
        }
    }
}
