
package org.openrewrite.maven.cache;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class CacheResult<T> {
    public enum State {
        Cached,
        Updated,
        Unavailable
    }

    State state;

    @Nullable
    T data;
}
