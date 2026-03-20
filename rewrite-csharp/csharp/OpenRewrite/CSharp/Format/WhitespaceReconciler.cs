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
using System.Collections;
using System.Collections.Concurrent;
using System.Reflection;
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Format;

/// <summary>
/// Walks two structurally identical trees in parallel, copying Space and Markers
/// from the formatted tree to the original. Preserves the original tree's IDs,
/// types, and all non-whitespace state.
///
/// If the trees diverge structurally, returns the original unchanged and sets
/// <see cref="IsCompatible"/> to false.
/// </summary>
public class WhitespaceReconciler
{
    private static readonly ConcurrentDictionary<Type, PropertyInfo[]> PropertyCache = new();
    private static readonly ConcurrentDictionary<(Type, string), MethodInfo?> WithMethodCache = new();

    private static readonly HashSet<string> SkipProperties =
        ["Id", "SourcePath"];

    private bool _compatible = true;
    private J? _targetSubtree;
    private J? _stopAfter;
    private ReconcileState _state;

    private enum ReconcileState
    {
        Searching,
        Reconciling,
        Done
    }

    public bool IsCompatible => _compatible;

    public J Reconcile(J original, J formatted, J? targetSubtree = null, J? stopAfter = null)
    {
        _compatible = true;
        _targetSubtree = targetSubtree;
        _stopAfter = stopAfter;
        _state = targetSubtree != null ? ReconcileState.Searching : ReconcileState.Reconciling;

        var result = VisitTree(original, formatted);
        return result as J ?? original;
    }

    private bool ShouldReconcile() => _state == ReconcileState.Reconciling;

    private object? StructureMismatch(object? original)
    {
        _compatible = false;
        return original;
    }

    private object? VisitProperty(object? original, object? formatted)
    {
        if (!_compatible) return original;

        // Handle null
        if (original == null || formatted == null)
        {
            if (!ReferenceEquals(original, formatted) && (original != null || formatted != null))
                return StructureMismatch(original);
            return original;
        }

        // Space — copy from formatted when reconciling
        if (original is Space)
        {
            if (!ShouldReconcile()) return original;
            return formatted is Space ? formatted : StructureMismatch(original);
        }

        // Markers — copy from formatted when reconciling
        if (original is Markers)
        {
            if (!ShouldReconcile()) return original;
            return formatted is Markers ? formatted : StructureMismatch(original);
        }

        // J nodes — recurse
        if (original is J origJ)
        {
            if (formatted is not J fmtJ)
                return StructureMismatch(original);
            return VisitTree(origJ, fmtJ);
        }

        // Padded wrappers — recurse into their structure
        if (IsGenericOf(original, typeof(JRightPadded<>)))
            return VisitRightPadded(original, formatted);

        if (IsGenericOf(original, typeof(JLeftPadded<>)))
            return VisitLeftPadded(original, formatted);

        if (IsGenericOf(original, typeof(JContainer<>)))
            return VisitContainer(original, formatted);

        // Lists (of padded elements, child nodes, etc.)
        if (original is IList origList)
        {
            if (formatted is not IList fmtList)
                return StructureMismatch(original);
            return VisitList(origList, fmtList);
        }

        // Primitive values, enums, etc. — keep original
        return original;
    }

    private object? VisitTree(J original, J formatted)
    {
        if (!_compatible) return original;

        // Check structural type compatibility
        if (original.GetType() != formatted.GetType())
            return StructureMismatch(original);

        // Track target subtree
        var isTarget = _targetSubtree != null && ReferenceEquals(original, _targetSubtree);
        var isStopAfter = _stopAfter != null && ReferenceEquals(original, _stopAfter);
        var previousState = _state;

        if (isTarget && _state == ReconcileState.Searching)
            _state = ReconcileState.Reconciling;

        try
        {
            var type = original.GetType();
            var properties = GetReconcilableProperties(type);
            var result = original;

            foreach (var prop in properties)
            {
                var origVal = prop.GetValue(original);
                var fmtVal = prop.GetValue(formatted);

                var visited = VisitProperty(origVal, fmtVal);
                if (!_compatible) return original;

                if (!ReferenceEquals(visited, origVal))
                {
                    result = SetProperty(result, type, prop, visited);
                    if (result == null) return original;
                }
            }

            return result;
        }
        finally
        {
            if (isTarget && previousState == ReconcileState.Searching)
                _state = ReconcileState.Done;
            if (isStopAfter && previousState == ReconcileState.Reconciling)
                _state = ReconcileState.Done;
        }
    }

    private object? VisitRightPadded(object original, object formatted)
    {
        if (!_compatible) return original;

        var origType = original.GetType();
        var fmtType = formatted.GetType();
        if (origType != fmtType) return StructureMismatch(original);

        var elementProp = origType.GetProperty("Element")!;
        var afterProp = origType.GetProperty("After")!;
        var markersProp = origType.GetProperty("Markers")!;

        var origElement = elementProp.GetValue(original);
        var fmtElement = elementProp.GetValue(formatted);
        var visitedElement = VisitProperty(origElement, fmtElement);
        if (!_compatible) return original;

        var origAfter = afterProp.GetValue(original) as Space;
        var fmtAfter = afterProp.GetValue(formatted) as Space;
        var visitedAfter = ShouldReconcile() ? fmtAfter : origAfter;

        var origMarkers = markersProp.GetValue(original) as Markers;
        var fmtMarkers = markersProp.GetValue(formatted) as Markers;
        var visitedMarkers = ShouldReconcile() ? fmtMarkers : origMarkers;

        if (ReferenceEquals(visitedElement, origElement) &&
            ReferenceEquals(visitedAfter, origAfter) &&
            ReferenceEquals(visitedMarkers, origMarkers))
            return original;

        var withElement = origType.GetMethod("WithElement")!;
        var withAfter = origType.GetMethod("WithAfter")!;
        var withMarkers = origType.GetMethod("WithMarkers")!;

        var result = original;
        if (!ReferenceEquals(visitedElement, origElement))
            result = withElement.Invoke(result, [visitedElement])!;
        if (!ReferenceEquals(visitedAfter, origAfter))
            result = withAfter.Invoke(result, [visitedAfter])!;
        if (!ReferenceEquals(visitedMarkers, origMarkers))
            result = withMarkers.Invoke(result, [visitedMarkers])!;

        return result;
    }

    private object? VisitLeftPadded(object original, object formatted)
    {
        if (!_compatible) return original;

        var origType = original.GetType();
        var fmtType = formatted.GetType();
        if (origType != fmtType) return StructureMismatch(original);

        var beforeProp = origType.GetProperty("Before")!;
        var elementProp = origType.GetProperty("Element")!;

        var origBefore = beforeProp.GetValue(original) as Space;
        var fmtBefore = beforeProp.GetValue(formatted) as Space;
        var visitedBefore = ShouldReconcile() ? fmtBefore : origBefore;

        var origElement = elementProp.GetValue(original);
        var fmtElement = elementProp.GetValue(formatted);
        var visitedElement = VisitProperty(origElement, fmtElement);
        if (!_compatible) return original;

        if (ReferenceEquals(visitedBefore, origBefore) &&
            ReferenceEquals(visitedElement, origElement))
            return original;

        var withBefore = origType.GetMethod("WithBefore")!;
        var withElement = origType.GetMethod("WithElement")!;

        var result = original;
        if (!ReferenceEquals(visitedBefore, origBefore))
            result = withBefore.Invoke(result, [visitedBefore])!;
        if (!ReferenceEquals(visitedElement, origElement))
            result = withElement.Invoke(result, [visitedElement])!;

        return result;
    }

    private object? VisitContainer(object original, object formatted)
    {
        if (!_compatible) return original;

        var origType = original.GetType();
        var fmtType = formatted.GetType();
        if (origType != fmtType) return StructureMismatch(original);

        var beforeProp = origType.GetProperty("Before")!;
        var elementsProp = origType.GetProperty("Elements")!;
        var markersProp = origType.GetProperty("Markers")!;

        var origBefore = beforeProp.GetValue(original) as Space;
        var fmtBefore = beforeProp.GetValue(formatted) as Space;
        var visitedBefore = ShouldReconcile() ? fmtBefore : origBefore;

        var origElements = elementsProp.GetValue(original) as IList;
        var fmtElements = elementsProp.GetValue(formatted) as IList;
        var visitedElements = VisitList(origElements!, fmtElements!);
        if (!_compatible) return original;

        var origMarkers = markersProp.GetValue(original) as Markers;
        var fmtMarkers = markersProp.GetValue(formatted) as Markers;
        var visitedMarkers = ShouldReconcile() ? fmtMarkers : origMarkers;

        if (ReferenceEquals(visitedBefore, origBefore) &&
            ReferenceEquals(visitedElements, origElements) &&
            ReferenceEquals(visitedMarkers, origMarkers))
            return original;

        var withBefore = origType.GetMethod("WithBefore")!;
        var withElements = origType.GetMethod("WithElements")!;
        var withMarkers = origType.GetMethod("WithMarkers")!;

        var result = original;
        if (!ReferenceEquals(visitedBefore, origBefore))
            result = withBefore.Invoke(result, [visitedBefore])!;
        if (!ReferenceEquals(visitedElements, origElements))
            result = withElements.Invoke(result, [visitedElements])!;
        if (!ReferenceEquals(visitedMarkers, origMarkers))
            result = withMarkers.Invoke(result, [visitedMarkers])!;

        return result;
    }

    private object? VisitList(IList original, IList formatted)
    {
        if (original.Count != formatted.Count)
            return StructureMismatch(original);

        var changed = false;
        var newList = new List<object?>(original.Count);

        for (var i = 0; i < original.Count; i++)
        {
            var visited = VisitProperty(original[i], formatted[i]);
            if (!_compatible) return original;
            newList.Add(visited);
            if (!ReferenceEquals(visited, original[i]))
                changed = true;
        }

        if (!changed) return original;

        // Create a new typed list matching the original's element type
        var listType = original.GetType();
        if (listType.IsGenericType)
        {
            var elementType = listType.GetGenericArguments()[0];
            var typedList = (IList)Activator.CreateInstance(typeof(List<>).MakeGenericType(elementType))!;
            foreach (var item in newList)
                typedList.Add(item);
            return typedList;
        }

        return newList;
    }

    private static bool IsJavaType(Type type)
    {
        var t = Nullable.GetUnderlyingType(type) ?? type;
        return typeof(JavaType).IsAssignableFrom(t);
    }

    private static PropertyInfo[] GetReconcilableProperties(Type type)
    {
        return PropertyCache.GetOrAdd(type, t =>
            t.GetProperties(BindingFlags.Public | BindingFlags.Instance)
                .Where(p => p.CanRead && !SkipProperties.Contains(p.Name) && !IsJavaType(p.PropertyType))
                .ToArray());
    }

    private static J? SetProperty(J node, Type type, PropertyInfo prop, object? value)
    {
        var key = (type, prop.Name);
        var withMethod = WithMethodCache.GetOrAdd(key, k =>
        {
            // Look for With{PropertyName} method
            var methodName = $"With{k.Item2}";
            return k.Item1.GetMethod(methodName, [prop.PropertyType]);
        });

        if (withMethod == null)
        {
            // No With* method — can't set this property, skip
            return node;
        }

        return withMethod.Invoke(node, [value]) as J;
    }

    private static bool IsGenericOf(object obj, Type genericTypeDef)
    {
        var type = obj.GetType();
        return type.IsGenericType && type.GetGenericTypeDefinition() == genericTypeDef;
    }
}
