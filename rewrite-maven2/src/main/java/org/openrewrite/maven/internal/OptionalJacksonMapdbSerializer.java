package org.openrewrite.maven.internal;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.util.Optional;

public class OptionalJacksonMapdbSerializer<T> implements Serializer<Optional<T>> {
    private final JacksonMapdbSerializer<T> jacksonMapdbSerializer;

    public OptionalJacksonMapdbSerializer(Class<T> tClass) {
        this.jacksonMapdbSerializer = new JacksonMapdbSerializer<>(tClass);
    }

    @Override
    public void serialize(@NotNull DataOutput2 out, @NotNull Optional<T> value) throws IOException {
        if (value.isPresent()) {
            out.writeBoolean(true);
            jacksonMapdbSerializer.serialize(out, value.get());
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public Optional<T> deserialize(@NotNull DataInput2 input, int available) throws IOException {
        boolean present = input.readBoolean();
        return present ? Optional.of(jacksonMapdbSerializer.deserialize(input, available)) : Optional.empty();
    }
}
