package org.openrewrite.maven.internal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;

public class JacksonMapdbSerializer<T> implements Serializer<T> {
    private static final ObjectMapper mapper;

    static {
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        ObjectMapper m = new ObjectMapper(f)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper = m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }

    private final Class<T> tClass;

    public JacksonMapdbSerializer(Class<T> tClass) {
        this.tClass = tClass;
    }

    @Override
    public void serialize(@NotNull DataOutput2 out, @NotNull T value) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(value);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    @Override
    public T deserialize(@NotNull DataInput2 input, int available) throws IOException {
        byte[] bytes = new byte[input.readInt()];
        input.readFully(bytes);
        return mapper.readValue(bytes, tClass);
    }
}
