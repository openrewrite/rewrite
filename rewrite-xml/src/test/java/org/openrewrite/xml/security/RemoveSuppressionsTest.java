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
package org.openrewrite.xml.security;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.time.LocalDate;

import static org.openrewrite.xml.Assertions.xml;

class RemoveSuppressionsTest implements RewriteTest {

    @Test
    void removesSuppressionsAtLeastOneDayAgo() {
        LocalDate today = LocalDate.now();
        LocalDate dayBeforeYesterday = today.minusDays(2);

        // yesterday in YYYY-MM-DD format + Z to indicate UTC
        String dayBeforeYesterdayString = dayBeforeYesterday + "Z";

        rewriteRun(
                spec -> spec.recipe(new RemoveSuppressions()),
                xml("" +
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                        "<suppressions>\n" +
                        "    <suppress until=\"" + dayBeforeYesterdayString + "\">\n" +
                        "        <notes>\n" +
                        "        </notes>\n" +
                        "    </suppress>\n" +
                        "</suppressions>", """
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions>
                        </suppressions>""")
        );
    }

    @Test
    void onlyRemovesSuppressionsInThePast() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayBeforeYesterday = today.minusDays(2);
        String tomorrowString = tomorrow.toString().substring(0, 10);
        String dayBeforeYesterdayString = dayBeforeYesterday.toString().substring(0, 10);

        rewriteRun(
                spec -> spec.recipe(new RemoveSuppressions()),
                xml("" +
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                        "<suppressions>\n" +
                        "    <suppress until=\"" + tomorrowString + "\">\n" +
                        "        <notes>\n" +
                        "        </notes>\n" +
                        "    </suppress>\n" +
                        "    <suppress until=\"" + dayBeforeYesterdayString + "\">\n" +
                        "        <notes>\n" +
                        "        </notes>\n" +
                        "    </suppress>\n" +
                        "</suppressions>", "" +
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                        "<suppressions>\n" +
                        "    <suppress until=\"" + tomorrowString + "\">\n" +
                        "        <notes>\n" +
                        "        </notes>\n" +
                        "    </suppress>\n" +
                        "</suppressions>")
        );
    }
    
    @Test
    void removesSuppressionsWithAndWithoutZ() {
        LocalDate today = LocalDate.now();
        LocalDate dayBeforeYesterday = today.minusDays(2);
        String dayBeforeYesterdayString = dayBeforeYesterday + "Z";
        String dayBeforeYesterdayStringNoZ = dayBeforeYesterday.toString();

        rewriteRun(
                spec -> spec.recipe(new RemoveSuppressions()),
                xml("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                        "<suppressions>\n" +
                        "    <suppress until=\"" + dayBeforeYesterdayString + "\">\n" +
                        "        <notes>\n" +
                        "        </notes>\n" +
                        "    </suppress>\n" +
                        "    <suppress until=\"" + dayBeforeYesterdayStringNoZ + "\">\n" +
                        "        <notes>\n" +
                        "        </notes>\n" +
                        "    </suppress>\n" +
                        "</suppressions>", """
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <suppressions>
                        </suppressions>""")
        );
    }
}
