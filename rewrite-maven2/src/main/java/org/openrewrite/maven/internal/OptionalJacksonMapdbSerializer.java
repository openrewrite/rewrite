/*
 * Copyright 2020 the original author or authors.
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
