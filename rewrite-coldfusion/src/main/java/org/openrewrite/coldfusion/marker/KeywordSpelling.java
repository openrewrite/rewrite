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
package org.openrewrite.coldfusion.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * A keyword written in other than lower case, {@code If} or {@code RETURN}, since CFML keywords are
 * case-insensitive. It sits on the element whose keyword it is: a {@code J.If}, the
 * {@code J.ForEachLoop.Control} for {@code in}, the {@code JLeftPadded} of a do-while condition for
 * its {@code while} and of a try's {@code finally}.
 */
@Value
@With
public class KeywordSpelling implements Marker {
    UUID id;
    String text;
}
