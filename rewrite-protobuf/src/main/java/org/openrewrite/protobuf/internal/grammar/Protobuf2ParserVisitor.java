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
// Generated from java-escape by ANTLR 4.11.1
package org.openrewrite.protobuf.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link Protobuf2Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface Protobuf2ParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#proto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProto(Protobuf2Parser.ProtoContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#stringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(Protobuf2Parser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#identOrReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentOrReserved(Protobuf2Parser.IdentOrReservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#syntax}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSyntax(Protobuf2Parser.SyntaxContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#importStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportStatement(Protobuf2Parser.ImportStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#packageStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackageStatement(Protobuf2Parser.PackageStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#optionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionName(Protobuf2Parser.OptionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOption(Protobuf2Parser.OptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#optionDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionDef(Protobuf2Parser.OptionDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#optionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionList(Protobuf2Parser.OptionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#topLevelDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTopLevelDef(Protobuf2Parser.TopLevelDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#ident}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdent(Protobuf2Parser.IdentContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#message}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMessage(Protobuf2Parser.MessageContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#messageField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMessageField(Protobuf2Parser.MessageFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#messageBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMessageBody(Protobuf2Parser.MessageBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#extend}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtend(Protobuf2Parser.ExtendContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#enumDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumDefinition(Protobuf2Parser.EnumDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#enumBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumBody(Protobuf2Parser.EnumBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#enumField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumField(Protobuf2Parser.EnumFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#service}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitService(Protobuf2Parser.ServiceContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#serviceBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitServiceBody(Protobuf2Parser.ServiceBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#rpc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRpc(Protobuf2Parser.RpcContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#rpcInOut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRpcInOut(Protobuf2Parser.RpcInOutContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#rpcBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRpcBody(Protobuf2Parser.RpcBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#reserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReserved(Protobuf2Parser.ReservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#ranges}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRanges(Protobuf2Parser.RangesContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#range}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange(Protobuf2Parser.RangeContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#fieldNames}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldNames(Protobuf2Parser.FieldNamesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PrimitiveType}
	 * labeled alternative in {@link Protobuf2Parser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveType(Protobuf2Parser.PrimitiveTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FullyQualifiedType}
	 * labeled alternative in {@link Protobuf2Parser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFullyQualifiedType(Protobuf2Parser.FullyQualifiedTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(Protobuf2Parser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#oneOf}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOneOf(Protobuf2Parser.OneOfContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#mapField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapField(Protobuf2Parser.MapFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#keyType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyType(Protobuf2Parser.KeyTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#reservedWord}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReservedWord(Protobuf2Parser.ReservedWordContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#fullIdent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFullIdent(Protobuf2Parser.FullIdentContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#emptyStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyStatement(Protobuf2Parser.EmptyStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Protobuf2Parser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(Protobuf2Parser.ConstantContext ctx);
}