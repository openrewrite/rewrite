/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

/**
 * Auto-detected styles for JavaScript/TypeScript code.
 * <p>
 * This marker is added when Prettier configuration is not available in the project.
 * It collects formatting statistics from existing source files to detect patterns
 * like tabs vs spaces, indent size, and brace spacing preferences.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class Autodetect extends NamedStyles implements RpcCodec<Autodetect> {
    private static final String NAME = "org.openrewrite.javascript.Autodetect";
    private static final String DISPLAY_NAME = "Auto-detected";
    private static final String DESCRIPTION = "Automatically detect styles from a repository's existing code.";

    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id, NAME, DISPLAY_NAME, DESCRIPTION, Collections.emptySet(), styles);
    }

    @Override
    public Autodetect withId(UUID id) {
        return id == getId() ? this : new Autodetect(id, getStyles());
    }

    @Override
    public Autodetect withStyles(Collection<Style> styles) {
        return styles == getStyles() ? this : new Autodetect(getId(), styles);
    }

    private static List<Style> stylesAsList(Autodetect a) {
        Collection<Style> styles = a.getStyles();
        //noinspection ConstantValue
        return styles == null ? emptyList() : new ArrayList<>(styles);
    }

    @Override
    public void rpcSend(Autodetect after, RpcSendQueue q) {
        q.getAndSend(after, NamedStyles::getId);
        q.getAndSendList(after, Autodetect::stylesAsList, s -> s.getClass().getName(), null);
    }

    @Override
    public Autodetect rpcReceive(Autodetect before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withStyles(q.receiveList(stylesAsList(before), null));
    }
}
