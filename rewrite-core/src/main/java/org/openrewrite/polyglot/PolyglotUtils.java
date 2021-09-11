/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.polyglot;

import org.graalvm.polyglot.Value;

import java.util.Optional;
import java.util.Set;

public class PolyglotUtils {

    public static final String JS = "js";

    public static Optional<Value> getOption(Value value, String memberKey) {
        return getValue(value, "options")
                .flatMap(v -> Optional.ofNullable(v.getMember(memberKey)));
    }

    public static Optional<Value> getValue(Value value, String memberKey) {
        return Optional.ofNullable(value.getMember(memberKey));
    }

    public static Optional<Value> invokeMember(Value value, String memberKey, Object... args) {
        return getValue(value, memberKey)
                .filter(Value::canExecute)
                .map(v -> v.execute(args));
    }

    public static Value invokeMemberOrElse(Value value, String memberKey, Value orElse, Object... args) {
        return getValue(value, memberKey)
                .filter(Value::canExecute)
                .map(v -> v.execute(args))
                .orElse(orElse);
    }

    public static Optional<Value> jsExtendConstructor(Value value, String memberKey, Object obj) {
        Value bindings = value.getContext().getBindings(JS);
        Set<String> members = bindings.getMemberKeys();
        if (members == null || members.isEmpty()) {
            return Optional.empty();
        }
        String thisName = members.iterator().next();
        Value prototype = value.getContext().eval(JS, "this." + thisName + ".default.prototype");
        prototype.putMember(memberKey, Value.asValue(obj));
        return Optional.of(value);
    }

}
