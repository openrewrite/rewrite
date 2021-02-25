package org.openrewrite.config;

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;

@Value
public class ParameterDescriptor {

    String name;

    String type;

    @Nullable
    String displayName;

    @Nullable
    String description;

}
