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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.JsYaml;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.list;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * config/reader/test/projectConfig.test.ts of pnpm 11.2.2.
 */
class ProjectConfigsTest {

    private static @Nullable String record(@Nullable Object packageConfigs) {
        Map<String, Map<String, @Nullable Object>> record = ProjectConfigs.createProjectConfigRecord(packageConfigs);
        return record == null ? null : json(record);
    }

    private static void throwsCode(@Nullable Object packageConfigs, String code, String received) {
        assertThatThrownBy(() -> ProjectConfigs.createProjectConfigRecord(packageConfigs))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_" + code);
              assertThat(e.getMessage()).endsWith(received);
          });
    }

    private static Map<String, @Nullable Object> project1(@Nullable Object config) {
        Map<String, @Nullable Object> record = new LinkedHashMap<>();
        record.put("project-1", config);
        return record;
    }

    private static Map<String, @Nullable Object> matching(String field, @Nullable Object value) {
        Map<String, @Nullable Object> item = new LinkedHashMap<>();
        item.put("match", singletonList("project-1"));
        item.put(field, value);
        return item;
    }

    @Test
    void returnsUndefinedForUndefined() {
        assertThat(record(null)).isNull();
        assertThat(record(Js.UNDEFINED)).isNull();
    }

    @Test
    void errorsOnInvalidPackageConfigs() {
        throwsCode(0, "PROJECT_CONFIGS_IS_NEITHER_OBJECT_NOR_ARRAY", "received 0");
        throwsCode("some string", "PROJECT_CONFIGS_IS_NEITHER_OBJECT_NOR_ARRAY", "received \"some string\"");
        throwsCode(true, "PROJECT_CONFIGS_IS_NEITHER_OBJECT_NOR_ARRAY", "received true");
    }

    @Test
    void recordReturnsAnEmptyRecordForAnEmptyRecord() {
        assertThat(record(obj("{}"))).isEqualTo("{}");
    }

    @Test
    void recordReturnsAValidRecordForAValidRecord() {
        String packageConfigs = "{'project-1':{'modulesDir':'foo'},'project-2':{'saveExact':true}," +
                                "'project-3':{'savePrefix':'~'}}";
        assertThat(record(obj(packageConfigs))).isEqualTo(json(obj(packageConfigs)));
    }

    @Test
    void recordExplicitlySetsHoistPatternToUndefinedWhenHoistIsFalse() {
        Map<String, Map<String, @Nullable Object>> record =
          ProjectConfigs.createProjectConfigRecord(obj("{'project-1':{'hoist':false}}"));
        assertThat(record).isNotNull();
        assertThat(record.get("project-1")).containsEntry("hoist", false).containsEntry("hoistPattern",
          Js.UNDEFINED);
    }

    @Test
    void recordErrorsOnInvalidProjectConfig() {
        throwsCode(project1(0), "PROJECT_CONFIG_NOT_AN_OBJECT", "received 0");
        throwsCode(project1("some string"), "PROJECT_CONFIG_NOT_AN_OBJECT", "received \"some string\"");
        throwsCode(project1(true), "PROJECT_CONFIG_NOT_AN_OBJECT", "received true");
        throwsCode(project1(null), "PROJECT_CONFIG_NOT_AN_OBJECT", "received null");
        throwsCode(project1(list("[0,1,2]")), "PROJECT_CONFIG_NOT_AN_OBJECT", "received [0,1,2]");
    }

    @Test
    void recordErrorsOnInvalidFieldTypes() {
        throwsCode(obj("{'project-1':{'hoist':'invalid'}}"), "PROJECT_CONFIG_INVALID_VALUE_TYPE",
          "Expecting a value of type boolean but received a value of type string: \"invalid\"");
        throwsCode(obj("{'project-1':{'hoist':0}}"), "PROJECT_CONFIG_INVALID_VALUE_TYPE", "type number: 0");
        throwsCode(obj("{'project-1':{'modulesDir':0}}"), "PROJECT_CONFIG_INVALID_VALUE_TYPE",
          "Expecting a value of type string but received a value of type number: 0");
        throwsCode(obj("{'project-1':{'modulesDir':true}}"), "PROJECT_CONFIG_INVALID_VALUE_TYPE", "boolean: true");
        throwsCode(obj("{'project-1':{'saveExact':'invalid'}}"), "PROJECT_CONFIG_INVALID_VALUE_TYPE",
          "type string: \"invalid\"");
        throwsCode(obj("{'project-1':{'saveExact':0}}"), "PROJECT_CONFIG_INVALID_VALUE_TYPE", "type number: 0");
        throwsCode(obj("{'project-1':{'savePrefix':0}}"), "PROJECT_CONFIG_INVALID_VALUE_TYPE", "type number: 0");
        throwsCode(obj("{'project-1':{'savePrefix':false}}"), "PROJECT_CONFIG_INVALID_VALUE_TYPE",
          "type boolean: false");
    }

    @Test
    void recordErrorsOnUnsupportedFields() {
        throwsCode(obj("{'project-1':{'ignoreScripts':true}}"), "PROJECT_CONFIG_UNSUPPORTED_FIELD",
          "Field ignoreScripts is not supported but was specified");
        throwsCode(obj("{'project-1':{'hoistPattern':['*']}}"), "PROJECT_CONFIG_UNSUPPORTED_FIELD",
          "Field hoistPattern is not supported but was specified");
    }

    @Test
    void recordDoesNotErrorOnUnsupportedButUndefinedFields() {
        Map<String, @Nullable Object> config = new LinkedHashMap<>();
        config.put("ignoreScripts", Js.UNDEFINED);
        config.put("hoistPattern", Js.UNDEFINED);
        Map<String, Map<String, @Nullable Object>> record = ProjectConfigs.createProjectConfigRecord(project1(config));
        assertThat(record).isNotNull();
        assertThat(record.get("project-1")).isEqualTo(config);
    }

    @Test
    void arrayReturnsAnEmptyRecordForAnEmptyArray() {
        assertThat(record(list("[]"))).isEqualTo("{}");
    }

    @Test
    void arrayReturnsAMapOfProjectSpecificSettingsForANonEmptyArray() {
        String record = record(list("[{'match':['project-1'],'modulesDir':'foo'}," +
                                    "{'match':['project-2','project-3'],'saveExact':true}," +
                                    "{'match':['project-4','project-5','project-6'],'savePrefix':'~'}]"));
        String expected = "{'project-1':{'modulesDir':'foo'},'project-2':{'saveExact':true}," +
                          "'project-3':{'saveExact':true},'project-4':{'savePrefix':'~'}," +
                          "'project-5':{'savePrefix':'~'},'project-6':{'savePrefix':'~'}}";
        assertThat(record).isEqualTo(json(obj(expected)));
        assertThat(record(obj(expected))).isEqualTo(record);
    }

    @Test
    void arrayExplicitlySetsHoistPatternToUndefinedWhenHoistIsFalse() {
        Map<String, Map<String, @Nullable Object>> record =
          ProjectConfigs.createProjectConfigRecord(list("[{'match':['project-1'],'hoist':false}]"));
        assertThat(record).isNotNull();
        assertThat(record.get("project-1")).containsEntry("hoist", false).containsEntry("hoistPattern",
          Js.UNDEFINED).doesNotContainKey("match");
    }

    @Test
    void arrayErrorsOnInvalidArrayItems() {
        throwsCode(list("[0]"), "PROJECT_CONFIGS_ARRAY_ITEM_IS_NOT_AN_OBJECT", "received 0");
        throwsCode(list("['some string']"), "PROJECT_CONFIGS_ARRAY_ITEM_IS_NOT_AN_OBJECT", "received \"some string\"");
        throwsCode(list("[true]"), "PROJECT_CONFIGS_ARRAY_ITEM_IS_NOT_AN_OBJECT", "received true");
        throwsCode(list("[null]"), "PROJECT_CONFIGS_ARRAY_ITEM_IS_NOT_AN_OBJECT", "received null");
    }

    @Test
    void arrayErrorsOnUndefinedMatch() {
        throwsCode(list("[{}]"), "PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_DEFINED",
          "A packageConfigs match is not defined");
    }

    @Test
    void arrayErrorsOnNonArrayMatch() {
        throwsCode(list("[{'match':0}]"), "PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_AN_ARRAY", "received 0");
        throwsCode(list("[{'match':'some string'}]"), "PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_AN_ARRAY",
          "received \"some string\"");
        throwsCode(list("[{'match':true}]"), "PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_AN_ARRAY", "received true");
        throwsCode(singletonList(matchOnly(Js.UNDEFINED)), "PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_AN_ARRAY",
          "received undefined");
        throwsCode(list("[{'match':null}]"), "PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_AN_ARRAY", "received null");
    }

    private static Map<String, @Nullable Object> matchOnly(@Nullable Object match) {
        Map<String, @Nullable Object> item = new LinkedHashMap<>();
        item.put("match", match);
        return item;
    }

    @Test
    void arrayErrorsOnNonStringMatchItem() {
        throwsCode(list("[{'match':[0]}]"), "PROJECT_CONFIGS_MATCH_ITEM_IS_NOT_A_STRING", "received 0");
        throwsCode(list("[{'match':[null]}]"), "PROJECT_CONFIGS_MATCH_ITEM_IS_NOT_A_STRING", "received null");
        throwsCode(list("[{'match':[{}]}]"), "PROJECT_CONFIGS_MATCH_ITEM_IS_NOT_A_STRING", "received {}");
    }

    @Test
    void arrayErrorsOnInvalidFieldTypes() {
        throwsCode(singletonList(matching("hoist", "invalid")), "PROJECT_CONFIG_INVALID_VALUE_TYPE",
          "type string: \"invalid\"");
        throwsCode(singletonList(matching("hoist", 0)), "PROJECT_CONFIG_INVALID_VALUE_TYPE", "type number: 0");
        throwsCode(singletonList(matching("modulesDir", 0)), "PROJECT_CONFIG_INVALID_VALUE_TYPE", "type number: 0");
        throwsCode(singletonList(matching("modulesDir", true)), "PROJECT_CONFIG_INVALID_VALUE_TYPE",
          "type boolean: true");
        throwsCode(singletonList(matching("saveExact", "invalid")), "PROJECT_CONFIG_INVALID_VALUE_TYPE",
          "type string: \"invalid\"");
        throwsCode(singletonList(matching("saveExact", 0)), "PROJECT_CONFIG_INVALID_VALUE_TYPE", "type number: 0");
        throwsCode(singletonList(matching("savePrefix", 0)), "PROJECT_CONFIG_INVALID_VALUE_TYPE", "type number: 0");
        throwsCode(singletonList(matching("savePrefix", false)), "PROJECT_CONFIG_INVALID_VALUE_TYPE",
          "type boolean: false");
    }

    @Test
    void arrayErrorsOnUnsupportedFields() {
        throwsCode(singletonList(matching("ignoreScripts", true)), "PROJECT_CONFIG_UNSUPPORTED_FIELD",
          "Field ignoreScripts is not supported but was specified");
        throwsCode(singletonList(matching("hoistPattern", list("['*']"))), "PROJECT_CONFIG_UNSUPPORTED_FIELD",
          "Field hoistPattern is not supported but was specified");
    }

    /**
     * A YAML timestamp is a Date, which {@code typeof} calls an object and which has no enumerable properties.
     */
    @Test
    void aDateIsAnObjectWithoutProperties() {
        Object date = JsYaml.load("2026-01-01");
        assertThat(date).isInstanceOf(JsYaml.Timestamp.class);
        assertThat(record(date)).isEqualTo("{}");
        assertThat(record(project1(date))).isEqualTo(json(obj("{'project-1':{}}")));
        throwsCode(singletonList(date), "PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_DEFINED", "is not defined");
        throwsCode(singletonList(matching("match", date)), "PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_AN_ARRAY",
          "received \"2026-01-01T00:00:00.000Z\"");
        Map<String, Map<String, @Nullable Object>> record =
          ProjectConfigs.createProjectConfigRecord(singletonList(matching("overrides", date)));
        assertThat(record).isNotNull();
        assertThat(record.get("project-1")).containsOnlyKeys("overrides").containsEntry("overrides", date);
    }

    @Test
    void arrayDoesNotErrorOnUnsupportedButUndefinedFields() {
        Map<String, @Nullable Object> item = matching("ignoreScripts", Js.UNDEFINED);
        item.put("hoistPattern", Js.UNDEFINED);
        Map<String, Map<String, @Nullable Object>> record =
          ProjectConfigs.createProjectConfigRecord(singletonList(item));
        assertThat(record).isNotNull();
        assertThat(record.get("project-1")).containsOnlyKeys("ignoreScripts", "hoistPattern");
    }
}
