/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock;

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * What npm reads from the machine it runs on: {@code process.version}, {@code process.platform}, {@code process.arch}
 * and the libc it detects; a null member is unknown, and the checks that read it count as satisfied.
 */
@Value
@Builder(toBuilder = true)
public class Host {

    public static final Host UNKNOWN = Host.builder().build();

    @Nullable
    public String nodeVersion;

    @Nullable
    public String os;

    @Nullable
    public String cpu;

    @Nullable
    public String libc;
}
