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
namespace Rewrite.Core.Rpc;

public class Reference
{
    private object? _value;

    public object? Value => _value;

    public static Reference AsRef(object? t)
    {
        return new Reference { _value = t };
    }

    public static T? GetValue<T>(object? maybeRef)
    {
        return (T?)(maybeRef is Reference reference ? reference.Value : maybeRef);
    }

    public static T GetValueNonNull<T>(object? maybeRef)
    {
        return GetValue<T>(maybeRef) ?? throw new ArgumentNullException(nameof(maybeRef));
    }
}
