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
package org.openrewrite.properties

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.marker.Range
import org.openrewrite.properties.tree.Properties

class UpdateSourcePositionsTest : PropertiesRecipeTest {

    @Test
    fun singleEntry() {
        val props = PropertiesParser().parse("key=value\n")[0]
        Assertions.assertThat(props.printAll()).isEqualTo("key=value\n");

        val p = UpdateSourcePositions().run(listOf(props)).results[0].after as Properties.File;
        Assertions.assertThat(p).isNotNull;
        val entries = p.content.map { it as Properties.Entry }
        var r = entries[0].markers.findFirst(Range::class.java).get()
        Assertions.assertThat(r.start.line).isEqualTo(1);
        Assertions.assertThat(r.start.column).isEqualTo(0);
        Assertions.assertThat(r.end.line).isEqualTo(1);
        Assertions.assertThat(r.end.column).isEqualTo(9);

        r = entries[0].value.markers.findFirst(Range::class.java).get()
        Assertions.assertThat(r.start.line).isEqualTo(1);
        Assertions.assertThat(r.start.column).isEqualTo(4);
        Assertions.assertThat(r.end.line).isEqualTo(1);
        Assertions.assertThat(r.end.column).isEqualTo(9);
    }

    @Test
    fun singleEntryMultilines() {
        val props = PropertiesParser().parse("\n\n   key=value\n")[0]
        Assertions.assertThat(props.printAll()).isEqualTo("\n\n   key=value\n");

        val p = UpdateSourcePositions().run(listOf(props)).results[0].after as Properties.File;
        Assertions.assertThat(p).isNotNull;
        val entries = p.content.map { it as Properties.Entry }
        var r = entries[0].markers.findFirst(Range::class.java).get()
        Assertions.assertThat(r.start.line).isEqualTo(3);
        Assertions.assertThat(r.start.column).isEqualTo(3);
        Assertions.assertThat(r.end.line).isEqualTo(3);
        Assertions.assertThat(r.end.column).isEqualTo(12);

        r = entries[0].value.markers.findFirst(Range::class.java).get()
        Assertions.assertThat(r.start.line).isEqualTo(3);
        Assertions.assertThat(r.start.column).isEqualTo(7);
        Assertions.assertThat(r.end.line).isEqualTo(3);
        Assertions.assertThat(r.end.column).isEqualTo(12);
    }

    @Test
    fun multiEntryMultilines() {
        val props = PropertiesParser().parse("\n\n   key1=value1\n   \n  \n  key2   = value2\n\n\n")[0]
        Assertions.assertThat(props.printAll()).isEqualTo("\n\n   key1=value1\n   \n  \n  key2   = value2\n\n\n");

        val p = UpdateSourcePositions().run(listOf(props)).results[0].after as Properties.File;
        Assertions.assertThat(p).isNotNull;
        val entries = p.content.map { it as Properties.Entry }

        // entry 1
        var r = entries[0].markers.findFirst(Range::class.java).get()
        Assertions.assertThat(r.start.line).isEqualTo(3);
        Assertions.assertThat(r.start.column).isEqualTo(3);
        Assertions.assertThat(r.end.line).isEqualTo(3);
        Assertions.assertThat(r.end.column).isEqualTo(14);
        // value 1
        r = entries[0].value.markers.findFirst(Range::class.java).get()
        Assertions.assertThat(r.start.line).isEqualTo(3);
        Assertions.assertThat(r.start.column).isEqualTo(8);
        Assertions.assertThat(r.end.line).isEqualTo(3);
        Assertions.assertThat(r.end.column).isEqualTo(14);

        // entry 2
        r = entries[1].markers.findFirst(Range::class.java).get()
        Assertions.assertThat(r.start.line).isEqualTo(6);
        Assertions.assertThat(r.start.column).isEqualTo(2);
        Assertions.assertThat(r.end.line).isEqualTo(6);
        Assertions.assertThat(r.end.column).isEqualTo(17);
        // value 2
        r = entries[1].value.markers.findFirst(Range::class.java).get()
        Assertions.assertThat(r.start.line).isEqualTo(6);
        Assertions.assertThat(r.start.column).isEqualTo(11);
        Assertions.assertThat(r.end.line).isEqualTo(6);
        Assertions.assertThat(r.end.column).isEqualTo(17);
    }
}