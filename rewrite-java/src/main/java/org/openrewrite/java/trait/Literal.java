/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.trait;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A literal in Java is either a {@link J.Literal} or a {@link J.NewArray}
 * that has a non-null initializer that, itself, contains literals or new 
 * arrays that recursively contain these constraints. In other languages 
 * this trait is inclusive of constructs like list or map literals.
 */
@RequiredArgsConstructor
public class Literal implements Trait<Expression> {
    @Getter
    private final Cursor cursor;

    private final ObjectMapper mapper;

    public boolean isNull() {
        return getTree() instanceof J.Literal && ((J.Literal) getTree()).getValue() == null;
    }

    public boolean isNotNull() {
        return !isNull();
    }

    public String getString() {
        return getValue(String.class);
    }

    public <@Nullable T> T getValue(Class<T> type) {
        return getValue(mapper.constructType(type));
    }

    public <@Nullable T> T getValue(TypeReference<T> type) {
        return getValue(mapper.constructType(type));
    }

    public <@Nullable T> T getValue(JavaType type) {
        Expression lit = getTree();
        if (lit instanceof J.Literal) {
            J.Literal literal = (J.Literal) lit;
            if (literal.getValue() == null) {
                //noinspection DataFlowIssue
                return null;
            } else if (type.isCollectionLikeType()) {
                List<?> l = singletonList(literal.getValue());
                return mapper.convertValue(l, type);
            } else {
                return mapper.convertValue(literal.getValue(), type);
            }
        } else if (lit instanceof J.NewArray) {
            List<Object> untyped = untypedInitializerLiterals((J.NewArray) lit);
            return mapper.convertValue(untyped, type);
        }
        //noinspection DataFlowIssue
        return null;
    }

    private List<Object> untypedInitializerLiterals(J.NewArray newArray) {
        List<Object> acc = new ArrayList<>();
        for (Expression init : requireNonNull(newArray.getInitializer())) {
            if (init instanceof J.Literal) {
                acc.add(((J.Literal) init).getValue());
            } else {
                acc.add(untypedInitializerLiterals((J.NewArray) init));
            }
        }
        return acc;
    }

    public static class Matcher extends SimpleTraitMatcher<Literal> {
        private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

        private ObjectMapper mapper = DEFAULT_MAPPER;

        /**
         * @param mapper A customized mapper, which should be rare,
         *               but possibly when you want a custom type factory.
         * @return This matcher with a customized mapper set.
         */
        public Matcher mapper(ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        @Override
        protected @Nullable Literal test(Cursor cursor) {
            Object value = cursor.getValue();
            return value instanceof J.Literal ||
                   isNewArrayWithLiteralInitializer(value) ?
                    new Literal(cursor, mapper) :
                    null;
        }

        private boolean isNewArrayWithLiteralInitializer(Object value) {
            if (value instanceof J.NewArray) {
                List<Expression> init = ((J.NewArray) value).getInitializer();
                if (init == null) {
                    return false;
                }
                for (Expression expr : init) {
                    if (!(expr instanceof J.Literal) &&
                        !isNewArrayWithLiteralInitializer(expr)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }
}
