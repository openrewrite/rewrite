/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;

public abstract class DynamicDispatchRpcCodec<T> implements RpcCodec<T> {
    private static final Map<String, List<DynamicDispatchRpcCodec<?>>> CODEC_BY_TYPE = new ConcurrentHashMap<>();

    /**
     * Classloaders we've already scanned for {@code ServiceLoader} providers. We re-scan
     * any new thread context classloader we see, so codecs from plugin/recipe classloaders
     * are picked up even when {@link DynamicDispatchRpcCodec} itself was loaded by a parent
     * classloader that didn't have them visible at class-init time.
     */
    private static final Set<ClassLoader> SCANNED_CLASSLOADERS =
            Collections.newSetFromMap(new WeakHashMap<>());

    static {
        // Scan with this class's defining classloader so codecs co-located with rewrite-core
        // are always found, even before any caller triggers the per-context-CL discovery below.
        discoverFrom(DynamicDispatchRpcCodec.class.getClassLoader());
    }

    private static synchronized void discoverFrom(@Nullable ClassLoader cl) {
        if (cl == null || !SCANNED_CLASSLOADERS.add(cl)) {
            return;
        }
        @SuppressWarnings({"unchecked", "rawtypes"}) ServiceLoader<DynamicDispatchRpcCodec<?>> loader =
                (ServiceLoader<DynamicDispatchRpcCodec<?>>) (ServiceLoader) ServiceLoader.load(DynamicDispatchRpcCodec.class, cl);
        for (DynamicDispatchRpcCodec<?> provider : loader) {
            List<DynamicDispatchRpcCodec<?>> bucket = CODEC_BY_TYPE.computeIfAbsent(
                    provider.getSourceFileType(), k -> new ArrayList<>());
            // Dedup by concrete codec class so re-scans don't accumulate duplicates.
            boolean alreadyPresent = false;
            for (DynamicDispatchRpcCodec<?> existing : bucket) {
                if (existing.getClass() == provider.getClass()) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) {
                bucket.add(provider);
            }
        }
    }

    /**
     * Verifies that at least one {@link DynamicDispatchRpcCodec} is registered for the
     * given source file type. Call this at startup to fail fast when the ServiceLoader
     * has not discovered the expected codec implementations (e.g. missing module on the
     * classpath).
     *
     * @param sourceFileType The fully-qualified class name of the source file type
     *                       (e.g. {@code "org.openrewrite.json.tree.Json$Document"}).
     * @throws IllegalStateException if no codec is registered for the given type.
     */
    public static void requireCodecFor(String sourceFileType) {
        if (!CODEC_BY_TYPE.containsKey(sourceFileType)) {
            throw new IllegalStateException(
                    "No DynamicDispatchRpcCodec registered for '" + sourceFileType + "'. " +
                    "The ServiceLoader found zero DynamicDispatchRpcCodec implementations for this type. " +
                    "Ensure that the module providing this codec is on the classpath.");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable RpcCodec<T> getCodec(Object t, @Nullable String sourceFileType) {
        if (sourceFileType == null) {
            return null;
        }
        // Discover codecs from any classloader we haven't seen yet. Covers plugin/recipe
        // classloaders that weren't visible when this class's static initializer ran.
        discoverFrom(Thread.currentThread().getContextClassLoader());
        discoverFrom(t.getClass().getClassLoader());
        for (DynamicDispatchRpcCodec<?> codec : CODEC_BY_TYPE.getOrDefault(sourceFileType, emptyList())) {
            if (codec.getType().isAssignableFrom(t.getClass())) {
                return (RpcCodec<T>) codec;
            }
        }
        // Defense-in-depth: the keyed bucket missed. This happens when {@code sourceFileType}
        // is a proxy/subclass runtime class name (e.g. a moderne-cli V3 lazy proxy) rather
        // than the canonical registered type, including when such a string arrives from a
        // remote. Scan all registered codecs for one assignable from the instance's runtime
        // type. {@code getType()} is a language marker interface (e.g. Xml, Json), so at most
        // one language's codec is assignable from a given tree instance.
        for (List<DynamicDispatchRpcCodec<?>> bucket : CODEC_BY_TYPE.values()) {
            for (DynamicDispatchRpcCodec<?> codec : bucket) {
                if (codec.getType().isAssignableFrom(t.getClass())) {
                    return (RpcCodec<T>) codec;
                }
            }
        }
        return null;
    }

    /**
     * Memoized per {@link Class}: see {@link #canonicalSourceFileType(Class)}.
     */
    private static final ClassValue<String> CANONICAL_SOURCE_FILE_TYPE = new ClassValue<String>() {
        @Override
        protected String computeValue(Class<?> type) {
            // Mirror getCodec's discovery so a proxy's codec (carried by the proxy's own
            // classloader, since the proxy extends the real tree type) is registered before
            // we walk. The result is then cached for the life of the class.
            discoverFrom(Thread.currentThread().getContextClassLoader());
            discoverFrom(type.getClassLoader());
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                if (CODEC_BY_TYPE.containsKey(c.getName())) {
                    return c.getName();
                }
            }
            return type.getName();
        }
    };

    /**
     * Resolves the canonical, codec-registered source file type name for a (possibly
     * subclassed/proxied) runtime class. A moderne-cli V3 LST is a lazy-loading proxy
     * generated as {@code Lazy_X extends <realTreeType>}; its {@code getClass().getName()}
     * is the proxy name, which is not a key in the codec map. Walking the superclass chain
     * lands on the concrete registered type (e.g. {@code Xml$Document}), whose name equals
     * the codec's {@link #getSourceFileType()} key. Normal LSTs match on the first iteration,
     * so behavior is unchanged for them.
     * <p>
     * The canonical name must be derived here rather than worked around downstream, because
     * it is also sent over the wire and the remote (e.g. the Python receiver) does an exact
     * string lookup that cannot perform Java-class assignability.
     *
     * @param type the runtime class of a {@code SourceFile} (possibly a proxy/subclass).
     * @return the registered supertype's name, or {@code type.getName()} if none is registered.
     */
    public static String canonicalSourceFileType(Class<?> type) {
        return CANONICAL_SOURCE_FILE_TYPE.get(type);
    }

    public abstract String getSourceFileType();

    public abstract Class<? extends T> getType();
}
