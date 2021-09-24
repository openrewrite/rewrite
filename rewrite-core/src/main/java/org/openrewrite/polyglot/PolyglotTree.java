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
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

import java.util.Arrays;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class PolyglotTree<T extends Tree> implements Tree, ProxyObject {

    private static final String[] MEMBERS = new String[]{"getId", "accept"};

    private final T delegate;

    public PolyglotTree(T delegate) {
        this.delegate = delegate;
    }

    @Override
    public UUID getId() {
        return delegate.getId();
    }

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return delegate.isAcceptable(v, p);
    }

    @Override
    public Object getMember(String key) {
        switch (key) {
            case "getId":
                return (ProxyExecutable) args -> {
                    assert args.length == 0;
                    return delegate.getId();
                };
            case "accept":
                return (ProxyExecutable) args -> {
                    assert args.length == 2;
                    return Value.asValue(delegate.accept(args[0].as(TreeVisitor.class), args[1]));
                };
        }
        return null;
    }

    @Override
    public Object getMemberKeys() {
        return Value.asValue(MEMBERS);
    }

    @Override
    public boolean hasMember(String key) {
        return Arrays.binarySearch(MEMBERS, key) > -1;
    }

    @Override
    public void putMember(String key, org.graalvm.polyglot.Value value) {

    }
}
