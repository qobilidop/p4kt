package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4TypeDeclTest {
  @Test
  fun parserTypeDeclaration() {
    val decl =
      P4TypeDecl(
        kind = "parser",
        name = "Parser",
        typeParams = listOf("H"),
        params =
          listOf(
            P4Param("b", P4Type.Named("packet_in"), null),
            P4Param("parsedHeaders", P4Type.Var("H"), Direction.OUT),
          ),
      )
    assertEquals("parser Parser<H>(packet_in b, out H parsedHeaders);", decl.toP4())
  }

  @Test
  fun controlTypeDeclaration() {
    val decl =
      P4TypeDecl(
        kind = "control",
        name = "Pipe",
        typeParams = listOf("H"),
        params =
          listOf(
            P4Param("headers", P4Type.Var("H"), Direction.INOUT),
            P4Param("parseError", P4Type.Error, Direction.IN),
            P4Param("inCtrl", P4Type.Named("InControl"), Direction.IN),
            P4Param("outCtrl", P4Type.Named("OutControl"), Direction.OUT),
          ),
      )
    assertEquals(
      "control Pipe<H>(inout H headers, in error parseError, in InControl inCtrl, out OutControl outCtrl);",
      decl.toP4(),
    )
  }

  @Test
  fun typeDeclDslInLibrary() {
    @Suppress("UnusedPrivateProperty")
    val lib =
      object : P4.Library() {
        val Parser by parserTypeDecl {
          val H by typeParam()
          val b by param(P4.typeName("packet_in"))
          val parsedHeaders by param(H, P4.OUT)
        }
        val Pipe by controlTypeDecl {
          val H by typeParam()
          val headers by param(H, P4.INOUT)
          val parseError by param(P4.errorType, P4.IN)
        }
        val VSS by packageTypeDecl {
          val H by typeParam()
          val p by param(P4.typeName("Parser"))
          val map by param(P4.typeName("Pipe"))
        }
      }
    assertEquals(
      """
          parser Parser<H>(packet_in b, out H parsedHeaders);

          control Pipe<H>(inout H headers, in error parseError);

          package VSS<H>(Parser p, Pipe map);
      """
        .trimIndent(),
      lib.toP4(),
    )
  }

  @Test
  fun packageTypeDeclaration() {
    val decl =
      P4TypeDecl(
        kind = "package",
        name = "VSS",
        typeParams = listOf("H"),
        params =
          listOf(
            P4Param("p", P4Type.Named("Parser"), null),
            P4Param("map", P4Type.Named("Pipe"), null),
            P4Param("d", P4Type.Named("Deparser"), null),
          ),
      )
    assertEquals("package VSS<H>(Parser p, Pipe map, Deparser d);", decl.toP4())
  }
}
