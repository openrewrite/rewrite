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

import org.apache.commons.io.FilenameUtils;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.graalvm.polyglot.Value.asValue;

@SuppressWarnings("unchecked")
public class PolyglotUtils {

    public static final String JS = "js";
    public static final String PYTHON = "python";
    public static final String PY = "py";
    public static final String PYC = "pyc";
    public static final String LLVM = "llvm";

    public static String getLanguage(String source) {
        String suffix = FilenameUtils.getExtension(source);
        switch (suffix) {
            case JS:
                return JS;
            case PY:
            case PYC:
                return PYTHON;
            default:
                return LLVM;
        }
    }

    public static String getLanguage(Source source) {
        return getLanguage(source.getPath());
    }

    public static String getName(Value value) {
        if (value.getMetaObject() == null) {
            return "undefined";
        }
        try {
            return value.getMetaSimpleName();
        } catch (UnsupportedOperationException e) {
            return value.getMetaObject().toString();
        }
    }

    public static Optional<Value> getValue(Value value, String memberKey) {
        return ofNullable(value.getMember(memberKey));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <O> O invokeMemberOrElse(Value value,
                                           String member,
                                           @Nullable Supplier<O> superFn) {
        return value.canInvokeMember(member)
                ? (O) value.invokeMember(member)
                : superFn != null ? superFn.get() : null;
    }

    public static <TREE extends Tree, CTX> TREE invokeMemberOrElse(Value value,
                                                                   String member,
                                                                   @Nullable TREE tree,
                                                                   CTX ctx,
                                                                   BiFunction<TREE, CTX, TREE> superFn) {
        return value.canInvokeMember(member)
                ? (TREE) value.invokeMember(member, asValue(tree), asValue(ctx)).as(Tree.class)
                : superFn.apply(tree, ctx);
    }

}
