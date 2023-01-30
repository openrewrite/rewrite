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
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link Protobuf2Parser}.
 */
public interface Protobuf2ParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#proto}.
	 * @param ctx the parse tree
	 */
	void enterProto(Protobuf2Parser.ProtoContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#proto}.
	 * @param ctx the parse tree
	 */
	void exitProto(Protobuf2Parser.ProtoContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(Protobuf2Parser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(Protobuf2Parser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#identOrReserved}.
	 * @param ctx the parse tree
	 */
	void enterIdentOrReserved(Protobuf2Parser.IdentOrReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#identOrReserved}.
	 * @param ctx the parse tree
	 */
	void exitIdentOrReserved(Protobuf2Parser.IdentOrReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#syntax}.
	 * @param ctx the parse tree
	 */
	void enterSyntax(Protobuf2Parser.SyntaxContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#syntax}.
	 * @param ctx the parse tree
	 */
	void exitSyntax(Protobuf2Parser.SyntaxContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#importStatement}.
	 * @param ctx the parse tree
	 */
	void enterImportStatement(Protobuf2Parser.ImportStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#importStatement}.
	 * @param ctx the parse tree
	 */
	void exitImportStatement(Protobuf2Parser.ImportStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#packageStatement}.
	 * @param ctx the parse tree
	 */
	void enterPackageStatement(Protobuf2Parser.PackageStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#packageStatement}.
	 * @param ctx the parse tree
	 */
	void exitPackageStatement(Protobuf2Parser.PackageStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#optionName}.
	 * @param ctx the parse tree
	 */
	void enterOptionName(Protobuf2Parser.OptionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#optionName}.
	 * @param ctx the parse tree
	 */
	void exitOptionName(Protobuf2Parser.OptionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#option}.
	 * @param ctx the parse tree
	 */
	void enterOption(Protobuf2Parser.OptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#option}.
	 * @param ctx the parse tree
	 */
	void exitOption(Protobuf2Parser.OptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#optionDef}.
	 * @param ctx the parse tree
	 */
	void enterOptionDef(Protobuf2Parser.OptionDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#optionDef}.
	 * @param ctx the parse tree
	 */
	void exitOptionDef(Protobuf2Parser.OptionDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#optionList}.
	 * @param ctx the parse tree
	 */
	void enterOptionList(Protobuf2Parser.OptionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#optionList}.
	 * @param ctx the parse tree
	 */
	void exitOptionList(Protobuf2Parser.OptionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#topLevelDef}.
	 * @param ctx the parse tree
	 */
	void enterTopLevelDef(Protobuf2Parser.TopLevelDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#topLevelDef}.
	 * @param ctx the parse tree
	 */
	void exitTopLevelDef(Protobuf2Parser.TopLevelDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#ident}.
	 * @param ctx the parse tree
	 */
	void enterIdent(Protobuf2Parser.IdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#ident}.
	 * @param ctx the parse tree
	 */
	void exitIdent(Protobuf2Parser.IdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#message}.
	 * @param ctx the parse tree
	 */
	void enterMessage(Protobuf2Parser.MessageContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#message}.
	 * @param ctx the parse tree
	 */
	void exitMessage(Protobuf2Parser.MessageContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#messageField}.
	 * @param ctx the parse tree
	 */
	void enterMessageField(Protobuf2Parser.MessageFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#messageField}.
	 * @param ctx the parse tree
	 */
	void exitMessageField(Protobuf2Parser.MessageFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#messageBody}.
	 * @param ctx the parse tree
	 */
	void enterMessageBody(Protobuf2Parser.MessageBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#messageBody}.
	 * @param ctx the parse tree
	 */
	void exitMessageBody(Protobuf2Parser.MessageBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#extend}.
	 * @param ctx the parse tree
	 */
	void enterExtend(Protobuf2Parser.ExtendContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#extend}.
	 * @param ctx the parse tree
	 */
	void exitExtend(Protobuf2Parser.ExtendContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#enumDefinition}.
	 * @param ctx the parse tree
	 */
	void enterEnumDefinition(Protobuf2Parser.EnumDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#enumDefinition}.
	 * @param ctx the parse tree
	 */
	void exitEnumDefinition(Protobuf2Parser.EnumDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#enumBody}.
	 * @param ctx the parse tree
	 */
	void enterEnumBody(Protobuf2Parser.EnumBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#enumBody}.
	 * @param ctx the parse tree
	 */
	void exitEnumBody(Protobuf2Parser.EnumBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#enumField}.
	 * @param ctx the parse tree
	 */
	void enterEnumField(Protobuf2Parser.EnumFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#enumField}.
	 * @param ctx the parse tree
	 */
	void exitEnumField(Protobuf2Parser.EnumFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#service}.
	 * @param ctx the parse tree
	 */
	void enterService(Protobuf2Parser.ServiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#service}.
	 * @param ctx the parse tree
	 */
	void exitService(Protobuf2Parser.ServiceContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#serviceBody}.
	 * @param ctx the parse tree
	 */
	void enterServiceBody(Protobuf2Parser.ServiceBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#serviceBody}.
	 * @param ctx the parse tree
	 */
	void exitServiceBody(Protobuf2Parser.ServiceBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#rpc}.
	 * @param ctx the parse tree
	 */
	void enterRpc(Protobuf2Parser.RpcContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#rpc}.
	 * @param ctx the parse tree
	 */
	void exitRpc(Protobuf2Parser.RpcContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#rpcInOut}.
	 * @param ctx the parse tree
	 */
	void enterRpcInOut(Protobuf2Parser.RpcInOutContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#rpcInOut}.
	 * @param ctx the parse tree
	 */
	void exitRpcInOut(Protobuf2Parser.RpcInOutContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#rpcBody}.
	 * @param ctx the parse tree
	 */
	void enterRpcBody(Protobuf2Parser.RpcBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#rpcBody}.
	 * @param ctx the parse tree
	 */
	void exitRpcBody(Protobuf2Parser.RpcBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#reserved}.
	 * @param ctx the parse tree
	 */
	void enterReserved(Protobuf2Parser.ReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#reserved}.
	 * @param ctx the parse tree
	 */
	void exitReserved(Protobuf2Parser.ReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#ranges}.
	 * @param ctx the parse tree
	 */
	void enterRanges(Protobuf2Parser.RangesContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#ranges}.
	 * @param ctx the parse tree
	 */
	void exitRanges(Protobuf2Parser.RangesContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#range}.
	 * @param ctx the parse tree
	 */
	void enterRange(Protobuf2Parser.RangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#range}.
	 * @param ctx the parse tree
	 */
	void exitRange(Protobuf2Parser.RangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#fieldNames}.
	 * @param ctx the parse tree
	 */
	void enterFieldNames(Protobuf2Parser.FieldNamesContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#fieldNames}.
	 * @param ctx the parse tree
	 */
	void exitFieldNames(Protobuf2Parser.FieldNamesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PrimitiveType}
	 * labeled alternative in {@link Protobuf2Parser#type}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(Protobuf2Parser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PrimitiveType}
	 * labeled alternative in {@link Protobuf2Parser#type}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(Protobuf2Parser.PrimitiveTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FullyQualifiedType}
	 * labeled alternative in {@link Protobuf2Parser#type}.
	 * @param ctx the parse tree
	 */
	void enterFullyQualifiedType(Protobuf2Parser.FullyQualifiedTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FullyQualifiedType}
	 * labeled alternative in {@link Protobuf2Parser#type}.
	 * @param ctx the parse tree
	 */
	void exitFullyQualifiedType(Protobuf2Parser.FullyQualifiedTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(Protobuf2Parser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(Protobuf2Parser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#oneOf}.
	 * @param ctx the parse tree
	 */
	void enterOneOf(Protobuf2Parser.OneOfContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#oneOf}.
	 * @param ctx the parse tree
	 */
	void exitOneOf(Protobuf2Parser.OneOfContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#mapField}.
	 * @param ctx the parse tree
	 */
	void enterMapField(Protobuf2Parser.MapFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#mapField}.
	 * @param ctx the parse tree
	 */
	void exitMapField(Protobuf2Parser.MapFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#keyType}.
	 * @param ctx the parse tree
	 */
	void enterKeyType(Protobuf2Parser.KeyTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#keyType}.
	 * @param ctx the parse tree
	 */
	void exitKeyType(Protobuf2Parser.KeyTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#reservedWord}.
	 * @param ctx the parse tree
	 */
	void enterReservedWord(Protobuf2Parser.ReservedWordContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#reservedWord}.
	 * @param ctx the parse tree
	 */
	void exitReservedWord(Protobuf2Parser.ReservedWordContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#fullIdent}.
	 * @param ctx the parse tree
	 */
	void enterFullIdent(Protobuf2Parser.FullIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#fullIdent}.
	 * @param ctx the parse tree
	 */
	void exitFullIdent(Protobuf2Parser.FullIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStatement(Protobuf2Parser.EmptyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStatement(Protobuf2Parser.EmptyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link Protobuf2Parser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(Protobuf2Parser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link Protobuf2Parser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(Protobuf2Parser.ConstantContext ctx);
}