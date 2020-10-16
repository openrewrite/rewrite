package org.openrewrite.maven.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;

public class JacksonMapdbSerializer<T> implements Serializer<T> {
    private static final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Class<T> tClass;

    public JacksonMapdbSerializer(Class<T> tClass) {
        this.tClass = tClass;
    }

    @Override
    public void serialize(@NotNull DataOutput2 out, @NotNull T value) throws IOException {
        out.writeUTF(mapper.writeValueAsString(value));
    }

    @Override
    public T deserialize(@NotNull DataInput2 input, int available) throws IOException {
        return mapper.readValue(input.readUTF(), tClass);
    }
}
