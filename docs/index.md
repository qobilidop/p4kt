# P4kt

P4kt (pronounced "packet") is an embedded domain-specific language (eDSL) for [P4](https://p4.org) in Kotlin.

## Quick example

Write P4 programs using Kotlin's type system and DSL builders:

<div class="grid" markdown>

```kotlin title="P4kt"
val id = p4Function("id", bit(8)) {
    val x by param(bit(8), IN)
    return_(x)
}
println(id.toP4())
```

```p4 title="P4 Output"
function bit<8> id(in bit<8> x) {
    return x;
}
```

</div>

See more in [Examples](examples.md).
