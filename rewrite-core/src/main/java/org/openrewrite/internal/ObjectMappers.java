package org.openrewrite.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.jspecify.annotations.Nullable;

public class ObjectMappers {
    private ObjectMappers() {
    }

    public static ObjectMapper propertyBasedMapper(@Nullable ClassLoader classLoader) {
        ClassLoader cl = classLoader == null ? ObjectMappers.class.getClassLoader() : classLoader;
        ObjectMapper m = JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build()
                .registerModule(new ParameterNamesModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        TypeFactory tf = TypeFactory.defaultInstance().withClassLoader(cl);
        m.setTypeFactory(tf);
        return m;
    }
}
