/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.trait.expr;

import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.java.trait.Top;
import org.openrewrite.java.trait.TraitFactory;
import org.openrewrite.java.trait.util.Either;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.trait.util.Validation;
import org.openrewrite.java.tree.J;

import java.util.Objects;
import java.util.UUID;

/**
 * A use of one of the keywords `this` or `super`, which may be qualified.
 */
public interface InstanceAccess extends Expr {

    /**
     * True if this instance access gets the value of `this`. That is, it is not
     * an enclosing instance.
     * This is never true for accesses in lambda expressions as they cannot access
     * their own instance directly.
     */
    boolean isOwnInstanceAccess();

    enum Factory implements TraitFactory<InstanceAccess> {
        F;

        @Override
        public Validation<TraitErrors, InstanceAccess> viewOf(Cursor cursor) {
            return ThisAccess.viewOf(cursor)
                    .map(t -> (InstanceAccess) t)
                    .f()
                    .bind(thisFail -> SuperAccess.viewOf(cursor)
                            .map(s -> (InstanceAccess) s)
                            .f()
                            .bind(superFail -> Validation.fail(TraitErrors.semigroup.sum(thisFail, superFail))));
        }
    }

    static Validation<TraitErrors, InstanceAccess> viewOf(Cursor cursor) {
        return Factory.F.viewOf(cursor);
    }
}

@AllArgsConstructor
class InstanceAccessBase implements InstanceAccess {
    final Cursor cursor;
    /**
     * Either a {@link J.Identifier} or a {@link J.FieldAccess} representing the instance access.
     */
    final Either<J.Identifier, J.FieldAccess> expression;

    @Override
    public UUID getId() {
        return expression.either(
                J.Identifier::getId,
                J.FieldAccess::getId
        );
    }

    public String getName() {
        return expression.either(
                J.Identifier::getSimpleName,
                fa -> fa.getName().getSimpleName()
        );
    }

    @Override
    public boolean isOwnInstanceAccess() {
        // TODO: Implement this
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return Top.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Top.hashCode(this);
    }

    static Validation<TraitErrors, InstanceAccessBase> viewOf(Cursor cursor) {
        Objects.requireNonNull(cursor, "cursor must not be null");
        Object maybeTree = cursor.getValue();
        if (maybeTree instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) maybeTree;
            if ("this".equals(fieldAccess.getName().getSimpleName()) || "super".equals(fieldAccess.getName().getSimpleName())) {
                return Validation.success(new InstanceAccessBase(cursor, Either.right(fieldAccess)));
            }
        } else if (maybeTree instanceof J.Identifier) {
            J.Identifier identifier = (J.Identifier) maybeTree;
            if ("this".equals(identifier.getSimpleName()) || "super".equals(identifier.getSimpleName())) {
                return Validation.success(new InstanceAccessBase(cursor, Either.left(identifier)));
            }
        }
        return TraitErrors.invalidTraitCreationType(InstanceAccess.class, cursor, J.Identifier.class, J.FieldAccess.class);
    }
}
