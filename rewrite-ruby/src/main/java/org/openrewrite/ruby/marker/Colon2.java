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
package org.openrewrite.ruby.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * When calling a method, you may also use :: to designate the receiver,
 * but this is rarely used due to the potential for confusion with :: for namespaces.
 * For example both of these are the same:<br/>
 * {@code Nokogiri::XML(response.body)}<br/>
 * {@code Nokogiri.XML(response.body)}
 */
@Value
@With
public class Colon2 implements Marker {
    UUID id;
}
