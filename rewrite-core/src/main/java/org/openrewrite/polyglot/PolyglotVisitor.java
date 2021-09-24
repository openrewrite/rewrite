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

import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;

public class PolyglotVisitor<T> extends TreeVisitor<Polyglot, T> {

    @Override
    public @Nullable Polyglot visit(@Nullable Tree tree, T ctx) {
        if (tree instanceof Polyglot.Instantiable) {
            return visitInstantiable((Polyglot.Instantiable) tree, ctx);
        }

        if (tree instanceof Polyglot.Executable) {
            return visitExecutable((Polyglot.Executable) tree, ctx);
        }

        if (tree instanceof Polyglot.Members) {
            return visitMembers((Polyglot.Members) tree, ctx);
        }

        if (tree instanceof Polyglot.HashEntries) {
            return visitHashEntries((Polyglot.HashEntries) tree, ctx);
        }

        if (tree instanceof Polyglot.ArrayElements) {
            return visitArrayElements((Polyglot.ArrayElements) tree, ctx);
        }

        if (tree instanceof Polyglot.StringValue) {
            return visitStringValue((Polyglot.StringValue) tree, ctx);
        }

        if (tree instanceof Polyglot.NumberValue) {
            return visitNumberValue((Polyglot.NumberValue) tree, ctx);
        }

        return super.visit(tree, ctx);
    }

    public Polyglot visitSource(Polyglot.Source source, T ctx) {
        return source
                .withMembers(visitAndCast(source.getMembers(), ctx))
                .withMarkers(visitMarkers(source.getMarkers(), ctx));
    }

    public Polyglot visitMembers(Polyglot.Members members, T ctx) {
        return members
                .withMembers(ListUtils.map(members.getMembers(), m -> visitAndCast(m, ctx)))
                .withMarkers(visitMarkers(members.getMarkers(), ctx));
    }

    public Polyglot visitMember(Polyglot.Member member, T ctx) {
        return member
                .withMarkers(visitMarkers(member.getMarkers(), ctx));

    }

    public Polyglot visitStringValue(Polyglot.StringValue stringValue, T ctx) {
        return stringValue
                .withMarkers(visitMarkers(stringValue.getMarkers(), ctx));
    }

    public Polyglot visitNumberValue(Polyglot.NumberValue numberValue, T ctx) {
        return numberValue
                .withMarkers(visitMarkers(numberValue.getMarkers(), ctx));
    }

    public Polyglot visitInstantiable(Polyglot.Instantiable instantiable, T ctx) {
        return instantiable
                .withValue(visitAndCast(instantiable, ctx))
                .withMarkers(visitMarkers(instantiable.getMarkers(), ctx));
    }

    public Polyglot visitHashEntries(Polyglot.HashEntries hashEntries, T ctx) {
        return hashEntries
                .withEntries(ListUtils.map(hashEntries.getEntries(), m -> visitAndCast(m, ctx)))
                .withMarkers(visitMarkers(hashEntries.getMarkers(), ctx));
    }

    public Polyglot visitHashEntry(Polyglot.HashEntry hashEntry, T ctx) {
        return hashEntry
                .withMarkers(visitMarkers(hashEntry.getMarkers(), ctx));
    }

    public Polyglot visitArrayElements(Polyglot.ArrayElements arrayElements, T ctx) {
        return arrayElements
                .withElements(ListUtils.map(arrayElements.getElements(), e -> visitAndCast(e, ctx)))
                .withMarkers(visitMarkers(arrayElements.getMarkers(), ctx));
    }

    public Polyglot visitArrayElement(Polyglot.ArrayElement arrayElement, T ctx) {
        return arrayElement
                .withMarkers(visitMarkers(arrayElement.getMarkers(), ctx));
    }

    public Polyglot visitExecutable(Polyglot.Executable executable, T ctx) {
        return executable
                .withMarkers(visitMarkers(executable.getMarkers(), ctx));
    }

    public Polyglot visitError(Polyglot.Error error, T ctx) {
        return error
                .withMarkers(visitMarkers(error.getMarkers(), ctx));
    }

    public Polyglot visitInstance(Polyglot.Instance instance, T ctx) {
        return instance
                .withMarkers(visitMarkers(instance.getMarkers(), ctx));
    }

}
