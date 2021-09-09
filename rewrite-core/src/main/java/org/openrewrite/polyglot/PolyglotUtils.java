package org.openrewrite.polyglot;

import org.graalvm.polyglot.Value;

import java.util.Optional;

public class PolyglotUtils {

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

}
