package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4StatementTest {
  @Test
  fun varDeclWithInit() {
    val fn =
      P4.function("test", P4.void_) {
        val x by param(P4.bit(8), P4.IN)
        val tmp by varDecl(P4.bit(8), x)
      }

    assertEquals(
      """
            function void test(in bit<8> x) {
                bit<8> tmp = x;
            }
            """
        .trimIndent(),
      fn.toP4(),
    )
  }

  @Test
  fun varDeclWithoutInit() {
    val fn =
      P4.function("test", P4.void_) {
        val tmp by varDecl(P4.bit(8))
      }

    assertEquals(
      """
            function void test() {
                bit<8> tmp;
            }
            """
        .trimIndent(),
      fn.toP4(),
    )
  }

  @Test
  fun assignment() {
    val fn =
      P4.function("swap", P4.void_) {
        val x by param(P4.bit(8), P4.INOUT)
        val y by param(P4.bit(8), P4.INOUT)
        val tmp by varDecl(P4.bit(8), x)
        assign(x, y)
        assign(y, tmp)
      }

    assertEquals(
      """
            function void swap(inout bit<8> x, inout bit<8> y) {
                bit<8> tmp = x;
                x = y;
                y = tmp;
            }
            """
        .trimIndent(),
      fn.toP4(),
    )
  }

  @Test
  fun ifStatement() {
    val fn =
      P4.function("test", P4.void_) {
        val x by param(P4.bit(8), P4.IN)
        if_(x eq P4.lit(0)) { return_(P4.lit(1)) }
      }

    assertEquals(
      """
            function void test(in bit<8> x) {
                if (x == 0) {
                    return 1;
                }
            }
            """
        .trimIndent(),
      fn.toP4(),
    )
  }

  @Test
  fun ifElseStatement() {
    val fn =
      P4.function("max", P4.bit(8)) {
        val a by param(P4.bit(8), P4.IN)
        val b by param(P4.bit(8), P4.IN)
        if_(a eq b) { return_(a) }.else_ { return_(b) }
      }

    assertEquals(
      """
            function bit<8> max(in bit<8> a, in bit<8> b) {
                if (a == b) {
                    return a;
                } else {
                    return b;
                }
            }
            """
        .trimIndent(),
      fn.toP4(),
    )
  }

  @Test
  fun verifyStatement() {
    val stmt =
      P4Statement.Verify(
        P4Expr.BinOp(BinOpKind.EQ, P4Expr.Ref("version"), P4Expr.TypedLit(4, 4)),
        P4Expr.ErrorMember("IPv4IncorrectVersion"),
      )
    assertEquals("verify(version == 4w4, error.IPv4IncorrectVersion);", stmt.toP4())
  }

  @Test
  fun transitionStatement() {
    val stmt = P4Statement.Transition("accept")
    assertEquals("transition accept;", stmt.toP4())
  }

  @Test
  fun transitionSelectStatement() {
    val stmt =
      P4Statement.TransitionSelect(
        P4Expr.FieldAccess(P4Expr.Ref("p"), "etherType"),
        listOf(P4Expr.TypedLit(16, 0x0800) to "parse_ipv4"),
      )
    assertEquals(
      """
            transition select(p.etherType) {
                16w2048 : parse_ipv4;
            }
      """
        .trimIndent(),
      stmt.toP4(),
    )
  }

  @Test
  fun verifyDsl() {
    val fn =
      P4.function("test", P4.void_) {
        val x by param(P4.bit(4), P4.IN)
        verify(x eq P4.lit(4, 4), P4.error_("BadVersion"))
      }
    assertEquals(
      """
            function void test(in bit<4> x) {
                verify(x == 4w4, error.BadVersion);
            }
      """
        .trimIndent(),
      fn.toP4(),
    )
  }

  @Test
  fun functionCallStatement() {
    val stmt = P4Statement.FunctionCall("Drop_action", emptyList())
    assertEquals("Drop_action();", stmt.toP4())
  }

  @Test
  fun functionCallDsl() {
    val fn = P4.function("test", P4.void_) { call("Drop_action") }
    assertEquals(
      """
            function void test() {
                Drop_action();
            }
      """
        .trimIndent(),
      fn.toP4(),
    )
  }

  @Test
  fun nestedIfInFunction() {
    val fn =
      P4.function("test", P4.void_) {
        val x by param(P4.bit(8), P4.IN)
        if_(x eq P4.lit(0)) { if_(x ne P4.lit(1)) { return_(x) } }
      }

    assertEquals(
      """
            function void test(in bit<8> x) {
                if (x == 0) {
                    if (x != 1) {
                        return x;
                    }
                }
            }
            """
        .trimIndent(),
      fn.toP4(),
    )
  }
}
