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

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.openrewrite.maven.cache.CacheResult;

import java.io.IOException;

public class CacheResultJacksonMapdbSerializer<T> implements Serializer<CacheResult<T>> {
    private final JacksonMapdbSerializer<T> jacksonMapdbSerializer;

    public CacheResultJacksonMapdbSerializer(Class<T> tClass) {
        this.jacksonMapdbSerializer = new JacksonMapdbSerializer<>(tClass);
    }

    @Override
    public void serialize(DataOutput2 out, CacheResult<T> value) throws IOException {
        if (value.getData() != null) {
            out.writeBoolean(true);
            out.writeLong(value.getTtl());
            jacksonMapdbSerializer.serialize(out, value.getData());
        } else {
            out.writeBoolean(false);
            out.writeLong(value.getTtl());
        }
    }

    @Override
    public CacheResult<T> deserialize(DataInput2 input, int available) throws IOException {
        boolean present = input.readBoolean();
        long ttl = input.readLong();
        T data = present ? jacksonMapdbSerializer.deserialize(input, available) : null;
        return new CacheResult<>(CacheResult.State.Cached, data, ttl);
    }
}
