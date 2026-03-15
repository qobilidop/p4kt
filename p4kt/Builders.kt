package p4kt

import kotlin.properties.ReadOnlyProperty

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

class TypedParamDelegate<T : StructRef>(
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
  ): ReadOnlyProperty<Any?, P4TableRef> {
    val table = factory(property.name)
    register(table)
    return ReadOnlyProperty { _, _ -> P4TableRef(property.name) }
  }
}

// Declaration builders

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

  fun <T : StructRef> param(factory: (P4Expr) -> T) = TypedParamDelegate(params, factory)

  fun <T : StructRef> param(factory: (P4Expr) -> T, direction: Direction) =
    TypedParamDelegate(params, factory, direction)

  fun build(name: String) = P4Action(name, params, body)
}

class FunctionBuilder(private val name: String, private val returnType: P4Type) :
  StatementBuilder() {
  private val params = mutableListOf<P4Param>()

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  fun <T : StructRef> param(factory: (P4Expr) -> T) = TypedParamDelegate(params, factory)

  fun <T : StructRef> param(factory: (P4Expr) -> T, direction: Direction) =
    TypedParamDelegate(params, factory, direction)

  fun build() = P4Function(name, returnType, params, body)
}

class TableBuilder {
  private val keys = mutableListOf<P4KeyEntry>()
  private val actions = mutableListOf<String>()
  private var size: Int? = null
  private var defaultAction: String = ""
  private var isDefaultActionConst: Boolean = false

  fun key(expr: P4Expr, matchKind: MatchKind) {
    keys.add(P4KeyEntry(expr, matchKind))
  }

  fun actions(vararg actionRefs: P4Action) {
    actions.addAll(actionRefs.map { it.name })
  }

  fun size(size: Int) {
    this.size = size
  }

  @Suppress("FunctionParameterNaming")
  fun defaultAction(action: P4Action, const_: Boolean = false) {
    defaultAction = action.name
    isDefaultActionConst = const_
  }

  fun build(name: String) = P4Table(name, keys, actions, size, defaultAction, isDefaultActionConst)
}

class ControlBuilder : StatementBuilder() {
  private val params = mutableListOf<P4Param>()
  private val declarations = mutableListOf<P4Declaration>()

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  fun <T : StructRef> param(factory: (P4Expr) -> T, direction: Direction) =
    TypedParamDelegate(params, factory, direction)

  fun action(block: ActionBuilder.() -> Unit) =
    DeclDelegate<P4Action>(
      factory = { name -> p4Action(name, block) },
      register = { declarations.add(it) },
    )

  fun varDecl(type: P4Type): ControlVarDeclDelegate = ControlVarDeclDelegate(declarations, type)

  fun varDecl(type: P4TypeReference): ControlVarDeclDelegate =
    ControlVarDeclDelegate(declarations, type.typeRef)

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

// Factory functions

fun p4Action(name: String, block: ActionBuilder.() -> Unit): P4Action {
  val builder = ActionBuilder()
  builder.block()
  return builder.build(name)
}

fun p4Function(name: String, returnType: P4Type, block: FunctionBuilder.() -> Unit): P4Function {
  val builder = FunctionBuilder(name, returnType)
  builder.block()
  return builder.build()
}

fun p4Table(name: String, block: TableBuilder.() -> Unit): P4Table {
  val builder = TableBuilder()
  builder.block()
  return builder.build(name)
}

fun p4Control(name: String, block: ControlBuilder.() -> Unit): P4Control {
  val builder = ControlBuilder()
  builder.block()
  return builder.build(name)
}

fun p4Const(name: String, type: P4Type, value: P4Expr) = P4Const(name, type, value)

fun p4Typedef(name: String, type: P4Type) = P4Typedef(name, type)

fun p4Header(name: String, block: FieldsBuilder.() -> Unit): P4Header {
  val builder = FieldsBuilder()
  builder.block()
  return P4Header(name, builder.build())
}

fun p4Struct(name: String, block: FieldsBuilder.() -> Unit): P4Struct {
  val builder = FieldsBuilder()
  builder.block()
  return P4Struct(name, builder.build())
}

// Program builder

class ProgramBuilder {
  private val declarations = mutableListOf<P4Declaration>()

  fun typedef(type: P4Type) =
    DeclDelegate<P4Typedef>(
      factory = { name -> p4Typedef(name, type) },
      register = { declarations.add(it) },
    )

  fun header(block: FieldsBuilder.() -> Unit) =
    DeclDelegate<P4Header>(
      factory = { name -> p4Header(name, block) },
      register = { declarations.add(it) },
    )

  fun struct(block: FieldsBuilder.() -> Unit) =
    DeclDelegate<P4Struct>(
      factory = { name -> p4Struct(name, block) },
      register = { declarations.add(it) },
    )

  fun const_(type: P4Type, value: P4Expr) = ConstDelegate(type, value) { declarations.add(it) }

  fun const_(type: P4TypeReference, value: P4Expr) =
    ConstDelegate(type.typeRef, value) { declarations.add(it) }

  fun action(block: ActionBuilder.() -> Unit) =
    DeclDelegate<P4Action>(
      factory = { name -> p4Action(name, block) },
      register = { declarations.add(it) },
    )

  fun <T : StructRef> struct(factory: (P4Expr) -> T) {
    val dummy = factory(P4Expr.Ref(""))
    declarations.add(P4Struct(dummy::class.simpleName!!, dummy.fields.toList()))
  }

  fun <T : HeaderRef> header(factory: (P4Expr) -> T) {
    val dummy = factory(P4Expr.Ref(""))
    declarations.add(P4Header(dummy::class.simpleName!!, dummy.fields.toList()))
  }

  fun control(block: ControlBuilder.() -> Unit) =
    DeclDelegate<P4Control>(
      factory = { name -> p4Control(name, block) },
      register = { declarations.add(it) },
    )

  fun function(returnType: P4Type, block: FunctionBuilder.() -> Unit) =
    DeclDelegate<P4Function>(
      factory = { name -> p4Function(name, returnType, block) },
      register = { declarations.add(it) },
    )

  fun build() = P4Program(declarations.toList())
}

fun p4Program(block: ProgramBuilder.() -> Unit): P4Program {
  val builder = ProgramBuilder()
  builder.block()
  return builder.build()
}
