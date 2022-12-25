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
import java.util.Collection;
import java.util.ArrayList;
import java.util.function.Supplier;

@SuppressWarnings("RedundantSuppression")
abstract class Test<P extends Collection> implements Supplier<P> {
    Object field;
    Object ternaryStrangeness = conditional() ? get() : get().stream();
    static Boolean conditional() {
        return null;
    }
    static {
        Supplier s = new Test<Collection>() {
            @Override
            public Collection get() {
                return new ArrayList<>();
            }
        };
    }
    Test() {
        Collection c = new ArrayList();
        c.add(1);
        //noinspection UnusedAssignment
        field = c;
        this.field = "Over achievements!";
    }

    @Override
    public P get() {
        return null;
    }

    void test() {
        String n = "42";
        String o = n;
        System.out.println(o);
        String p = o;
    }
}
