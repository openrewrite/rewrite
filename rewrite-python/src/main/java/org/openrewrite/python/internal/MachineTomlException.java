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
package org.openrewrite.python.internal;

/**
 * Thrown by {@link MachineTomlReader} when input falls outside the machine-emitted TOML subset it
 * accepts. Lock-file readers wrap this into their own format exception at the parse boundary.
 */
public class MachineTomlException extends RuntimeException {
    public MachineTomlException(String message) {
        super(message);
    }
}
