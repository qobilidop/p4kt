package p4kt

sealed class P4Type {
    data class Bit(val width: Int) : P4Type()
}

enum class Direction { IN, OUT, INOUT }

data class P4Param(val name: String, val type: P4Type, val direction: Direction)

sealed class P4Expr {
    data class Ref(val name: String) : P4Expr()
}

sealed class P4Statement {
    data class Return(val expr: P4Expr) : P4Statement()
}

data class P4Function(
    val name: String,
    val returnType: P4Type,
    val params: List<P4Param>,
    val body: List<P4Statement>,
)
