from typing import Optional, List

from _cbor2 import break_marker
from cbor2 import CBOREncoder, CBORDecoder
from rewrite_remote import RemotingContext, SerializationContext, DeserializationContext, INDEFINITE_MAP_START, \
    BREAK_MARKER

from ..tree import JavaType


def register_codecs():
    # SenderContext.register(J, JavaSender)
    # ReceiverContext.register(J, JavaReceiver)

    RemotingContext.register_value_deserializer(
        'org.openrewrite.java.tree.JavaType$Primitive',
        deserialize_java_primitive
    )
    RemotingContext.register_value_serializer(
        JavaType.Primitive,
        serialize_java_primitive
    )
    RemotingContext.register_value_deserializer(
        'org.openrewrite.java.tree.JavaType$Class',
        deserialize_java_class
    )
    RemotingContext.register_value_serializer(
        JavaType.Class,
        serialize_java_class
    )
    RemotingContext.register_value_deserializer(
        'org.openrewrite.java.tree.JavaType$Method',
        deserialize_java_method
    )
    RemotingContext.register_value_serializer(
        JavaType.Method,
        serialize_java_method
    )
    RemotingContext.register_value_deserializer(
        'org.openrewrite.java.tree.JavaType$Variable',
        deserialize_java_variable
    )
    RemotingContext.register_value_serializer(
        JavaType.Variable,
        serialize_java_variable
    )
    RemotingContext.register_value_deserializer(
        'org.openrewrite.java.tree.JavaType$Array',
        deserialize_java_array
    )
    RemotingContext.register_value_serializer(
        JavaType.Array,
        serialize_java_array
    )
    RemotingContext.register_value_deserializer(
        'org.openrewrite.java.tree.JavaType$Parameterized',
        deserialize_java_parameterized
    )
    RemotingContext.register_value_serializer(
        JavaType.Parameterized,
        serialize_java_parameterized
    )
    RemotingContext.register_value_deserializer(
        'org.openrewrite.java.tree.JavaType$GenericTypeVariable',
        deserialize_java_generic_type_variable
    )
    RemotingContext.register_value_serializer(
        JavaType.GenericTypeVariable,
        serialize_java_generic_type_variable
    )
    RemotingContext.register_value_deserializer(
        'org.openrewrite.java.tree.JavaType$Unknown',
        deserialize_java_unknown
    )
    RemotingContext.register_value_serializer(
        JavaType.Unknown,
        serialize_java_unknown
    )


def deserialize_java_class(type_: str, decoder: CBORDecoder, context: DeserializationContext) -> JavaType.Class:
    cls = JavaType.ShallowClass() if type_ == 'org.openrewrite.java.tree.JavaType$ShallowClass' else JavaType.Class()
    while not (key := decoder.decode()) == break_marker:
        if key == '@ref':
            context.remoting_context.add_by_id(decoder.decode(), cls)
        elif key == 'flagsBitMap':
            setattr(cls, '_flags_bit_map', decoder.decode())
        elif key == 'fullyQualifiedName':
            setattr(cls, '_fully_qualified_name', decoder.decode())
        elif key == 'kind':
            setattr(cls, '_kind', context.deserialize(JavaType.FullyQualified.Kind, decoder))
        elif key == 'typeParameters':
            setattr(cls, '_type_parameters', context.deserialize(List[JavaType], decoder))
        elif key == 'supertype':
            setattr(cls, '_supertype', context.deserialize(JavaType.FullyQualified, decoder))
        elif key == 'owningClass':
            setattr(cls, '_owning_class', context.deserialize(JavaType.FullyQualified, decoder))
        elif key == 'annotations':
            setattr(cls, '_annotations', context.deserialize(List[JavaType.FullyQualified], decoder))
        elif key == 'interfaces':
            setattr(cls, '_interfaces', context.deserialize(List[JavaType.FullyQualified], decoder))
        elif key == 'members':
            setattr(cls, '_members', context.deserialize(List[JavaType.Variable], decoder))
        elif key == 'methods':
            setattr(cls, '_methods', context.deserialize(List[JavaType.Method], decoder))
        else:
            raise ValueError(f"Unexpected key: {key}")
    return cls


def deserialize_java_method(_: str, decoder: CBORDecoder, context: DeserializationContext) -> JavaType.Method:
    method = JavaType.Method()
    while not (key := decoder.decode()) == break_marker:
        if key == '@ref':
            context.remoting_context.add_by_id(decoder.decode(), method)
        elif key == 'flagsBitMap':
            method._flags_bit_map = decoder.decode()
        elif key == 'declaringType':
            method._declaring_type = context.deserialize(JavaType.FullyQualified, decoder)
        elif key == 'name':
            method._name = decoder.decode()
        elif key == 'returnType':
            method._return_type = context.deserialize(JavaType, decoder)
        elif key == 'parameterNames':
            method._parameter_names = context.deserialize(List[str], decoder)
        elif key == 'parameterTypes':
            method._parameter_types = context.deserialize(List[JavaType], decoder)
        elif key == 'thrownExceptions':
            method._thrown_exceptions = context.deserialize(List[JavaType.FullyQualified], decoder)
        elif key == 'annotations':
            method._annotations = context.deserialize(List[JavaType.FullyQualified], decoder)
        elif key == 'defaultValue':
            method._default_value = context.deserialize(List[str], decoder)
        elif key == 'declaredFormalTypeNames':
            method._declared_formal_type_names = context.deserialize(List[str], decoder)
        else:
            raise ValueError(f"Unexpected key: {key}")
    return method


def deserialize_java_variable(_: str, decoder: CBORDecoder, context: DeserializationContext) -> JavaType.Variable:
    variable = JavaType.Variable()
    while not (key := decoder.decode()) == break_marker:
        if key == '@ref':
            context.remoting_context.add_by_id(decoder.decode(), variable)
        elif key == 'flagsBitMap':
            setattr(variable, '_flags_bit_map', decoder.decode())
        elif key == 'name':
            setattr(variable, '_name', decoder.decode())
        elif key == 'owner':
            setattr(variable, '_owner', context.deserialize(JavaType, decoder))
        elif key == 'type':
            setattr(variable, '_type', context.deserialize(JavaType, decoder))
        elif key == 'annotations':
            setattr(variable, '_annotations', context.deserialize(List[JavaType.FullyQualified], decoder))
        else:
            raise ValueError(f"Unexpected key: {key}")
    return variable


def deserialize_java_array(_: str, decoder: CBORDecoder, context: DeserializationContext) -> JavaType.Array:
    array = JavaType.Array()
    while not (key := decoder.decode()) == break_marker:
        if key == '@ref':
            context.remoting_context.add_by_id(decoder.decode(), array)
        elif key == 'elemType':
            setattr(array, '_elem_type', context.deserialize(JavaType, decoder))
        elif key == 'annotations':
            setattr(array, '_annotations', context.deserialize(List[JavaType.FullyQualified], decoder))
        else:
            raise ValueError(f"Unexpected key: {key}")
    return array


def deserialize_java_parameterized(_: str, decoder: CBORDecoder,
                                   context: DeserializationContext) -> JavaType.Parameterized:
    param = JavaType.Parameterized()
    while not (key := decoder.decode()) == break_marker:
        if key == '@ref':
            context.remoting_context.add_by_id(decoder.decode(), param)
        elif key == 'type':
            setattr(param, '_type', context.deserialize(JavaType.FullyQualified, decoder))
        elif key == 'typeParameters':
            setattr(param, '_type_parameters', context.deserialize(List[JavaType], decoder))
        else:
            raise ValueError(f"Unexpected key: {key}")
    return param


def deserialize_java_generic_type_variable(_: str, decoder: CBORDecoder,
                                           context: DeserializationContext) -> JavaType.GenericTypeVariable:
    type_variable = JavaType.GenericTypeVariable()
    while not (key := decoder.decode()) == break_marker:
        if key == '@ref':
            context.remoting_context.add_by_id(decoder.decode(), type_variable)
        elif key == 'name':
            setattr(type_variable, '_name', decoder.decode())
        elif key == 'variance':
            setattr(type_variable, '_variance', context.deserialize(JavaType.GenericTypeVariable.Variance, decoder))
        elif key == 'bounds':
            setattr(type_variable, '_bounds', context.deserialize(List[JavaType], decoder))
        else:
            raise ValueError(f"Unexpected key: {key}")
    return type_variable


def deserialize_java_primitive(_: str, decoder: CBORDecoder, context: DeserializationContext) -> JavaType.Primitive:
    kind = decoder.decode()
    assert decoder.decode() == break_marker
    return JavaType.Primitive(kind)


def deserialize_java_unknown(_: str, decoder: CBORDecoder, context: DeserializationContext) -> JavaType.Unknown:
    unknown = JavaType.Unknown()
    while not (key := decoder.decode()) == break_marker:
        if key == '@ref':
            context.remoting_context.add_by_id(decoder.decode(), unknown)
        else:
            decoder.decode()
    return unknown


def serialize_java_primitive(value: JavaType.Primitive, type_name: Optional[str], encoder: CBOREncoder,
                             context: SerializationContext) -> None:
    encoder.encode(['org.openrewrite.java.tree.JavaType$Primitive', value.value])


def serialize_java_class(value: JavaType.Class, type_name: Optional[str], encoder: CBOREncoder,
                         context: SerializationContext) -> None:
    pass


def serialize_java_method(value: JavaType.Method, type_name: Optional[str], encoder: CBOREncoder,
                          context: SerializationContext) -> None:
    if id := context.remoting_context.try_get_id(value):
        encoder.encode(id)
        return

    encoder.write(INDEFINITE_MAP_START)
    encoder.encode_string('@c')
    encoder.encode_string('org.openrewrite.java.tree.JavaType$Method')
    encoder.encode_string('@ref')
    encoder.encode_int(context.remoting_context.add(value))
    encoder.encode_string('flagsBitMap')
    encoder.encode_int(value.flags_bit_map)
    if value.declaring_type:
        encoder.encode_string('declaringType')
        context.serialize(value.declaring_type, None, encoder)
    encoder.encode_string('name')
    encoder.encode_string(value.name)
    if value.return_type:
        encoder.encode_string('returnType')
        context.serialize(value.return_type, None, encoder)
    if value.parameter_names:
        encoder.encode_string('parameterNames')
        context.serialize(value.parameter_names, None, encoder)
    if value.parameter_types:
        encoder.encode_string('parameterTypes')
        context.serialize(value.parameter_types, None, encoder)
    if value.thrown_exceptions:
        encoder.encode_string('thrownExceptions')
        context.serialize(value.thrown_exceptions, None, encoder)
    if value.annotations:
        encoder.encode_string('annotations')
        context.serialize(value.annotations, None, encoder)
    if value.default_value:
        encoder.encode_string('defaultValue')
        context.serialize(value.default_value, None, encoder)
    if value.declared_formal_type_names:
        encoder.encode_string('declaredFormalTypeNames')
        context.serialize(value.declared_formal_type_names, None, encoder)
    encoder.write(BREAK_MARKER)


def serialize_java_variable(value: JavaType.Variable, type_name: Optional[str], encoder: CBOREncoder,
                            context: SerializationContext) -> None:
    pass


def serialize_java_array(value: JavaType.Array, type_name: Optional[str], encoder: CBOREncoder,
                         context: SerializationContext) -> None:
    pass


def serialize_java_parameterized(value: JavaType.Parameterized, type_name: Optional[str], encoder: CBOREncoder,
                                 context: SerializationContext) -> None:
    pass


def serialize_java_generic_type_variable(value: JavaType.GenericTypeVariable, type_name: Optional[str],
                                         encoder: CBOREncoder, context: SerializationContext) -> None:
    pass


def serialize_java_union(value: JavaType.GenericTypeVariable, type_name: Optional[str], encoder: CBOREncoder,
                         context: SerializationContext) -> None:
    pass


def serialize_java_unknown(value: JavaType.GenericTypeVariable, type_name: Optional[str], encoder: CBOREncoder,
                           context: SerializationContext) -> None:
    id = context.remoting_context.try_get_id(value)
    if id is not None:
        encoder.encode(id)
        return
    encoder.encode({
        '@c': 'org.openrewrite.java.tree.JavaType$Unknown',
        '@ref': context.remoting_context.add(value)
    })
