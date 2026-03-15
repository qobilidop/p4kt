package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4StatementTest {
  @Test
  fun varDeclWithInit() {
    val fn =
      p4Function("test", void_) {
        val x by param(bit(8), IN)
        val tmp by varDecl(bit(8), x)
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
      p4Function("test", void_) {
        val tmp by varDecl(bit(8))
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
      p4Function("swap", void_) {
        val x by param(bit(8), INOUT)
        val y by param(bit(8), INOUT)
        val tmp by varDecl(bit(8), x)
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
      p4Function("test", void_) {
        val x by param(bit(8), IN)
        if_(x eq lit(0)) { return_(lit(1)) }
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
      p4Function("max", bit(8)) {
        val a by param(bit(8), IN)
        val b by param(bit(8), IN)
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
  fun nestedIfInFunction() {
    val fn =
      p4Function("test", void_) {
        val x by param(bit(8), IN)
        if_(x eq lit(0)) { if_(x ne lit(1)) { return_(x) } }
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
