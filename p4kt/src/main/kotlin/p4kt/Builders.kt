@file:Suppress("TooManyFunctions")

package p4kt

import kotlin.properties.ReadOnlyProperty

@DslMarker annotation class P4DslMarker

// Internal IR types

data class P4Param(val name: String, val type: P4Type, val direction: Direction? = null)

sealed class P4Statement {
  data class Return(val expr: P4Expr?) : P4Statement()

  data class MethodCall(val expr: P4Expr, val method: String, val args: List<P4Expr>) :
    P4Statement()

  data class VarDecl(val name: String, val type: P4Type, val init: P4Expr?) : P4Statement()

  data class Assign(val target: P4Expr, val value: P4Expr) : P4Statement()

  data class If(
    val condition: P4Expr,
    val thenBody: List<P4Statement>,
    val elseBody: List<P4Statement>,
  ) : P4Statement()

  data class Verify(val condition: P4Expr, val error: P4Expr) : P4Statement()

  data class Transition(val stateName: String) : P4Statement()

  data class TransitionSelect(val expr: P4Expr, val cases: List<Pair<P4Expr, String>>) :
    P4Statement()

  data class FunctionCall(val name: String, val args: List<P4Expr>) : P4Statement()
}

data class P4Field(val name: String, val type: P4Type)

enum class BinOpKind {
  SUB,
  EQ,
  NE,
}

data class P4KeyEntry(val expr: P4Expr, val matchKind: P4MatchKindRef)

data class P4LocalVar(val name: String, val type: P4Type) : P4Declaration

data class P4Function(
  val name: String,
  val returnType: P4Type,
  val params: List<P4Param>,
  val body: List<P4Statement>,
) : P4Declaration

data class P4ExternFunction(
  val name: String,
  val returnType: P4Type,
  val params: List<P4Param>,
  val typeParams: List<String> = emptyList(),
) : P4Declaration

data class P4ExternMethod(
  val name: String,
  val returnType: P4Type,
  val params: List<P4Param>,
  val typeParams: List<String> = emptyList(),
)

data class P4ExternInstance(val typeName: String, val name: String) : P4Declaration

data class P4ParserState(val name: String, val body: List<P4Statement>)

// Property delegates

class ParamDelegate(
  private val params: MutableList<P4Param>,
  private val type: P4Type,
  private val direction: Direction? = null,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr.Ref> {
    params.add(P4Param(property.name, type, direction))
    return ReadOnlyProperty { _, _ -> P4Expr.Ref(property.name) }
  }
}

class TypedParamDelegate<T : P4.StructRef>(
  private val params: MutableList<P4Param>,
  private val factory: (P4Expr) -> T,
  private val direction: Direction? = null,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, T> {
    val instance = factory(P4Expr.Ref(property.name))
    params.add(P4Param(property.name, P4Type.Named(instance::class.simpleName!!), direction))
    return ReadOnlyProperty { _, _ -> instance }
  }
}

class VarDeclDelegate(
  private val body: MutableList<P4Statement>,
  private val type: P4Type,
  private val init: P4Expr?,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr.Ref> {
    body.add(P4Statement.VarDecl(property.name, type, init))
    return ReadOnlyProperty { _, _ -> P4Expr.Ref(property.name) }
  }
}

class ConstDelegate(
  private val type: P4Type,
  private val value: P4Expr,
  private val register: (P4Const) -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr.Ref> {
    register(P4Const(property.name, type, value))
    return ReadOnlyProperty { _, _ -> P4Expr.Ref(property.name) }
  }
}

class DeclDelegate<T : P4Declaration>(
  private val factory: (String) -> T,
  private val register: (T) -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, T> {
    val decl = factory(property.name)
    register(decl)
    return ReadOnlyProperty { _, _ -> decl }
  }
}

class ControlVarDeclDelegate(
  private val declarations: MutableList<P4Declaration>,
  private val type: P4Type,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr.Ref> {
    declarations.add(P4LocalVar(property.name, type))
    return ReadOnlyProperty { _, _ -> P4Expr.Ref(property.name) }
  }
}

class TableDeclDelegate(
  private val factory: (String) -> P4Table,
  private val register: (P4Table) -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4.TableRef> {
    val table = factory(property.name)
    register(table)
    return ReadOnlyProperty { _, _ -> P4.TableRef(property.name) }
  }
}

class ExternInstanceDelegate(
  private val declarations: MutableList<P4Declaration>,
  private val typeName: String,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr.Ref> {
    declarations.add(P4ExternInstance(typeName, property.name))
    return ReadOnlyProperty { _, _ -> P4Expr.Ref(property.name) }
  }
}

class StateDeclDelegate(
  private val deferredStates: MutableList<Pair<String, StateBuilder.() -> Unit>>,
  private val block: StateBuilder.() -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4.StateRef> {
    deferredStates.add(property.name to block)
    return ReadOnlyProperty { _, _ -> P4.StateRef(property.name) }
  }
}

// Statement builder

@P4DslMarker
open class StatementBuilder {
  protected val body = mutableListOf<P4Statement>()

  fun varDecl(type: P4Type, init: P4Expr? = null) = VarDeclDelegate(body, type, init)

  fun assign(target: P4Expr, value: P4Expr) {
    body.add(P4Statement.Assign(target, value))
  }

  fun if_(condition: P4Expr, block: StatementBuilder.() -> Unit): IfBuilder {
    val thenBuilder = StatementBuilder()
    thenBuilder.block()
    body.add(P4Statement.If(condition, thenBuilder.statements(), emptyList()))
    return IfBuilder(body, body.size - 1)
  }

  fun return_(expr: P4Expr) {
    body.add(P4Statement.Return(expr))
  }

  fun return_() {
    body.add(P4Statement.Return(null))
  }

  fun stmt(statement: P4Statement) {
    body.add(statement)
  }

  fun call(expr: P4Expr, method: String, vararg args: P4Expr) {
    body.add(P4Statement.MethodCall(expr, method, args.toList()))
  }

  fun call(obj: P4.StructRef, method: String, vararg args: P4Expr) {
    call(obj.expr, method, *args)
  }

  fun call(expr: P4Expr, method: String, arg: P4.StructRef) {
    body.add(P4Statement.MethodCall(expr, method, listOf(arg.expr)))
  }

  fun call(name: String, vararg args: P4Expr) {
    body.add(P4Statement.FunctionCall(name, args.toList()))
  }

  fun verify(condition: P4Expr, error: P4Expr) {
    body.add(P4Statement.Verify(condition, error))
  }

  fun P4.TableRef.apply_() {
    body.add(P4Statement.MethodCall(P4Expr.Ref(name), "apply", emptyList()))
  }

  fun statements() = body.toList()
}

class IfBuilder(private val parentBody: MutableList<P4Statement>, private val index: Int) {
  infix fun else_(block: StatementBuilder.() -> Unit) {
    val elseBuilder = StatementBuilder()
    elseBuilder.block()
    val oldIf = parentBody[index] as P4Statement.If
    parentBody[index] = oldIf.copy(elseBody = elseBuilder.statements())
  }
}

// Declaration builders

@P4DslMarker
class FieldsBuilder {
  private val fields = mutableListOf<P4Field>()

  fun field(name: String, type: P4Type) {
    fields.add(P4Field(name, type))
  }

  fun field(name: String, type: P4TypeReference) {
    fields.add(P4Field(name, type.typeRef))
  }

  fun build() = fields.toList()
}

class ActionBuilder : StatementBuilder() {
  private val params = mutableListOf<P4Param>()

  fun param(type: P4Type) = ParamDelegate(params, type)

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference) = ParamDelegate(params, type.typeRef)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  fun <T : P4.StructRef> param(factory: (P4Expr) -> T) = TypedParamDelegate(params, factory)

  fun <T : P4.StructRef> param(factory: (P4Expr) -> T, direction: Direction) =
    TypedParamDelegate(params, factory, direction)

  fun build(name: String) = P4Action(name, params, body)
}

class FunctionBuilder(private val name: String, private val returnType: P4Type) :
  StatementBuilder() {
  private val params = mutableListOf<P4Param>()

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  fun <T : P4.StructRef> param(factory: (P4Expr) -> T) = TypedParamDelegate(params, factory)

  fun <T : P4.StructRef> param(factory: (P4Expr) -> T, direction: Direction) =
    TypedParamDelegate(params, factory, direction)

  fun build() = P4Function(name, returnType, params, body)
}

@P4DslMarker
class TableBuilder {
  private val keys = mutableListOf<P4KeyEntry>()
  private val actions = mutableListOf<String>()
  private var size: Int? = null
  private var defaultAction: String = ""
  private var isDefaultActionConst: Boolean = false

  fun key(expr: P4Expr, matchKind: P4MatchKindRef) {
    keys.add(P4KeyEntry(expr, matchKind))
  }

  fun actions(vararg actionRefs: P4Action) {
    actions.addAll(actionRefs.map { it.name })
  }

  fun actionByName(name: String) {
    actions.add(name)
  }

  fun size(size: Int) {
    this.size = size
  }

  @Suppress("FunctionParameterNaming")
  fun defaultAction(action: P4Action, const_: Boolean = false) {
    defaultAction = action.name
    isDefaultActionConst = const_
  }

  @Suppress("FunctionParameterNaming")
  fun defaultAction(name: String, const_: Boolean = false) {
    defaultAction = name
    isDefaultActionConst = const_
  }

  fun build(name: String) = P4Table(name, keys, actions, size, defaultAction, isDefaultActionConst)
}

class TypeParamDelegate(private val typeParams: MutableList<String>) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Type.Var> {
    typeParams.add(property.name)
    return ReadOnlyProperty { _, _ -> P4Type.Var(property.name) }
  }
}

@P4DslMarker
class ExternMethodBuilder {
  private val params = mutableListOf<P4Param>()
  private val typeParams = mutableListOf<String>()
  private var returnType: P4Type = P4Type.Void

  fun typeParam() = TypeParamDelegate(typeParams)

  fun returnType(type: P4Type) {
    returnType = type
  }

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4Type) = ParamDelegate(params, type)

  fun params() = params.toList()

  fun typeParams() = typeParams.toList()

  fun returnType() = returnType
}

@P4DslMarker
class TypeDeclBuilder {
  private val params = mutableListOf<P4Param>()
  private val typeParams = mutableListOf<String>()

  fun typeParam() = TypeParamDelegate(typeParams)

  fun param(type: P4Type) = ParamDelegate(params, type)

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference) = ParamDelegate(params, type.typeRef)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  fun <T : P4.StructRef> param(factory: (P4Expr) -> T) = TypedParamDelegate(params, factory)

  fun <T : P4.StructRef> param(factory: (P4Expr) -> T, direction: Direction) =
    TypedParamDelegate(params, factory, direction)

  fun params() = params.toList()

  fun typeParams() = typeParams.toList()
}

data class MethodRef(val name: String)

class MethodDelegate(
  private val methods: MutableList<P4ExternMethod>,
  private val returnType: P4Type,
  private val block: (ExternMethodBuilder.() -> Unit)?,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, MethodRef> {
    if (block != null) {
      val builder = ExternMethodBuilder()
      builder.block()
      methods.add(P4ExternMethod(property.name, returnType, builder.params(), builder.typeParams()))
    } else {
      methods.add(P4ExternMethod(property.name, returnType, emptyList()))
    }
    return ReadOnlyProperty { _, _ -> MethodRef(property.name) }
  }
}

class MethodDelegateWithReturnType(
  private val methods: MutableList<P4ExternMethod>,
  private val block: ExternMethodBuilder.() -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, MethodRef> {
    val builder = ExternMethodBuilder()
    builder.block()
    methods.add(
      P4ExternMethod(property.name, builder.returnType(), builder.params(), builder.typeParams())
    )
    return ReadOnlyProperty { _, _ -> MethodRef(property.name) }
  }
}

class OverloadDelegate(
  private val methods: MutableList<P4ExternMethod>,
  private val originalName: String,
  private val returnType: P4Type,
  private val block: ExternMethodBuilder.() -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, Unit> {
    val builder = ExternMethodBuilder()
    builder.block()
    methods.add(P4ExternMethod(originalName, returnType, builder.params(), builder.typeParams()))
    return ReadOnlyProperty { _, _ -> }
  }
}

@P4DslMarker
class ExternBuilder(private val name: String) {
  private val methods = mutableListOf<P4ExternMethod>()

  fun constructor_() {
    methods.add(P4ExternMethod(name, P4Type.Void, emptyList()))
  }

  fun method(returnType: P4Type) = MethodDelegate(methods, returnType, null)

  fun method(returnType: P4Type, block: ExternMethodBuilder.() -> Unit) =
    MethodDelegate(methods, returnType, block)

  fun method(block: ExternMethodBuilder.() -> Unit) = MethodDelegateWithReturnType(methods, block)

  fun overload(original: MethodRef, returnType: P4Type, block: ExternMethodBuilder.() -> Unit) =
    OverloadDelegate(methods, original.name, returnType, block)

  fun build() = P4Extern(name, methods)
}

class ControlBuilder : StatementBuilder() {
  private val params = mutableListOf<P4Param>()
  private val declarations = mutableListOf<P4Declaration>()

  fun param(type: P4Type) = ParamDelegate(params, type)

  fun param(type: P4TypeReference) = ParamDelegate(params, type.typeRef)

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  fun <T : P4.StructRef> param(factory: (P4Expr) -> T, direction: Direction) =
    TypedParamDelegate(params, factory, direction)

  fun action(block: ActionBuilder.() -> Unit) =
    DeclDelegate<P4Action>(
      factory = { name -> P4.action(name, block) },
      register = { declarations.add(it) },
    )

  fun varDecl(type: P4Type): ControlVarDeclDelegate = ControlVarDeclDelegate(declarations, type)

  fun varDecl(type: P4TypeReference): ControlVarDeclDelegate =
    ControlVarDeclDelegate(declarations, type.typeRef)

  fun externInstance(extern: P4Extern) = ExternInstanceDelegate(declarations, extern.name)

  fun externInstance(extern: P4TypeReference) =
    ExternInstanceDelegate(declarations, extern.typeRef.name)

  fun table(block: TableBuilder.() -> Unit) =
    TableDeclDelegate(
      factory = { name ->
        val builder = TableBuilder()
        builder.block()
        builder.build(name)
      },
      register = { declarations.add(it) },
    )

  fun apply(block: StatementBuilder.() -> Unit) {
    val applyBuilder = StatementBuilder()
    applyBuilder.block()
    body.addAll(applyBuilder.statements())
  }

  fun build(name: String) = P4Control(name, params, declarations, body)
}

class StateBuilder : StatementBuilder() {
  fun transition(stateRef: P4.StateRef) {
    body.add(P4Statement.Transition(stateRef.name))
  }

  fun select(expr: P4Expr, block: SelectBuilder.() -> Unit) {
    val builder = SelectBuilder()
    builder.block()
    body.add(P4Statement.TransitionSelect(expr, builder.cases()))
  }

  fun build(name: String) = P4ParserState(name, body)
}

@P4DslMarker
class SelectBuilder {
  private val cases = mutableListOf<Pair<P4Expr, String>>()

  infix fun P4Expr.to(stateRef: P4.StateRef) {
    cases.add(this to stateRef.name)
  }

  fun cases() = cases.toList()
}

@P4DslMarker
class ParserBuilder {
  private val params = mutableListOf<P4Param>()
  private val declarations = mutableListOf<P4Declaration>()
  private val deferredStates = mutableListOf<Pair<String, StateBuilder.() -> Unit>>()

  fun param(type: P4Type) = ParamDelegate(params, type)

  fun param(type: P4TypeReference) = ParamDelegate(params, type.typeRef)

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  fun <T : P4.StructRef> param(factory: (P4Expr) -> T, direction: Direction) =
    TypedParamDelegate(params, factory, direction)

  fun externInstance(extern: P4Extern) = ExternInstanceDelegate(declarations, extern.name)

  fun externInstance(extern: P4TypeReference) =
    ExternInstanceDelegate(declarations, extern.typeRef.name)

  fun state(block: StateBuilder.() -> Unit) = StateDeclDelegate(deferredStates, block)

  fun build(name: String): P4Parser {
    val states =
      deferredStates.map { (stateName, block) ->
        val builder = StateBuilder()
        builder.block()
        builder.build(stateName)
      }
    return P4Parser(name, params, declarations, states)
  }
}

// Program builder

@P4DslMarker
class ProgramBuilder {
  private val declarations = mutableListOf<P4Declaration>()

  fun declare(declaration: P4Declaration) {
    declarations.add(declaration)
  }

  fun typedef(type: P4Type) =
    DeclDelegate<P4Typedef>(
      factory = { name -> P4.typedef(name, type) },
      register = { declarations.add(it) },
    )

  fun header(block: FieldsBuilder.() -> Unit) =
    DeclDelegate<P4Header>(
      factory = { name -> P4.header(name, block) },
      register = { declarations.add(it) },
    )

  fun struct(block: FieldsBuilder.() -> Unit) =
    DeclDelegate<P4Struct>(
      factory = { name -> P4.struct(name, block) },
      register = { declarations.add(it) },
    )

  fun const_(type: P4Type, value: P4Expr) = ConstDelegate(type, value) { declarations.add(it) }

  fun const_(type: P4TypeReference, value: P4Expr) =
    ConstDelegate(type.typeRef, value) { declarations.add(it) }

  fun action(block: ActionBuilder.() -> Unit) =
    DeclDelegate<P4Action>(
      factory = { name -> P4.action(name, block) },
      register = { declarations.add(it) },
    )

  fun <T : P4.StructRef> struct(factory: (P4Expr) -> T) {
    val dummy = factory(P4Expr.Ref(""))
    declarations.add(P4Struct(dummy::class.simpleName!!, dummy.fields.toList()))
  }

  fun <T : P4.HeaderRef> header(factory: (P4Expr) -> T) {
    val dummy = factory(P4Expr.Ref(""))
    declarations.add(P4Header(dummy::class.simpleName!!, dummy.fields.toList()))
  }

  fun control(block: ControlBuilder.() -> Unit) =
    DeclDelegate<P4Control>(
      factory = { name -> P4.control(name, block) },
      register = { declarations.add(it) },
    )

  fun function(returnType: P4Type, block: FunctionBuilder.() -> Unit) =
    DeclDelegate<P4Function>(
      factory = { name -> P4.function(name, returnType, block) },
      register = { declarations.add(it) },
    )

  fun errors(vararg members: String) {
    declarations.add(P4Error(members.toList()))
  }

  fun parser(block: ParserBuilder.() -> Unit) =
    DeclDelegate<P4Parser>(
      factory = { name ->
        val builder = ParserBuilder()
        builder.block()
        builder.build(name)
      },
      register = { declarations.add(it) },
    )

  fun extern(block: ExternBuilder.() -> Unit) =
    DeclDelegate<P4Extern>(
      factory = { name ->
        val builder = ExternBuilder(name)
        builder.block()
        builder.build()
      },
      register = { declarations.add(it) },
    )

  fun packageInstance(typeName: String, name: String, vararg args: String) {
    declarations.add(P4PackageInstance(typeName, args.toList(), name))
  }

  fun build() = P4Program(declarations.toList())
}
