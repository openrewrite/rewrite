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
