package org.openrewrite.rpc;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;

public abstract class DynamicDispatchRpcCodec<T> implements RpcCodec<T> {
    private static final Map<String, List<DynamicDispatchRpcCodec<?>>> CODEC_BY_TYPE = new ConcurrentHashMap<>();

    static {
        @SuppressWarnings({"unchecked", "rawtypes"}) ServiceLoader<DynamicDispatchRpcCodec<?>> loader = (ServiceLoader<DynamicDispatchRpcCodec<?>>)
                (ServiceLoader) ServiceLoader.load(DynamicDispatchRpcCodec.class);
        for (DynamicDispatchRpcCodec<?> provider : loader) {
            CODEC_BY_TYPE.computeIfAbsent(provider.getSourceFileType(), p -> new ArrayList<>())
                    .add(provider);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable RpcCodec<T> getCodec(Object t, @Nullable String sourceFileType) {
        if (sourceFileType == null) {
            return null;
        }
        for (DynamicDispatchRpcCodec<?> codec : CODEC_BY_TYPE.getOrDefault(sourceFileType, emptyList())) {
            if (codec.getType().isAssignableFrom(t.getClass())) {
                return (RpcCodec<T>) codec;
            }
        }
        return null;
    }

    public abstract String getSourceFileType();

    public abstract Class<? extends T> getType();
}
