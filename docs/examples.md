# Examples

Each example is a standalone P4kt program alongside the P4 code it produces.
Source files are in the [`examples/`](https://github.com/qobilidop/p4kt/tree/main/examples) directory.

## Identity

<div class="grid" markdown>

```kotlin title="P4kt"
package p4kt.examples

import p4kt.*

fun main() {
  val id =
    p4Function("id", bit(8)) {
      val x by param(bit(8), IN)
      return_(x)
    }
  println(id.toP4())
}
```

```p4 title="P4 Output"
function bit<8> id(in bit<8> x) {
    return x;
}
```

</div>

