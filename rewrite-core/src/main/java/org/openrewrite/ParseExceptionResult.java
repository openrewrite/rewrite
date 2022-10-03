/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class ParseExceptionResult implements Marker {
    UUID id;

    Throwable throwable;

    public ParseExceptionResult(Throwable t) {
        this(Tree.randomId(), t);
    }

    public ParseExceptionResult(UUID id, Throwable t) {
        this.id = id;
        this.throwable = t;
    }

    public String getDescription() {
        if(throwable == null) {
            return "Unknown parsing exception. Perhaps there was an issue deserializing the associated throwable.";
        } else {
            return throwable.toString();
        }
    }
}
