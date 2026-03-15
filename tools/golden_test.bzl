"""Macro for golden tests that compare P4kt output to expected .p4 files."""

load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")
load("@rules_shell//shell:sh_test.bzl", "sh_test")

def golden_test(name):
    """Creates a golden test that runs a P4kt example and compares output.

    Args:
        name: Base name of the example. Expects {name}.kt and {name}.p4 files.
    """
    main_class = "p4kt.examples." + name.title().replace("_", "") + "Kt"

    kt_jvm_binary(
        name = name + "_bin",
        srcs = [name + ".kt"],
        main_class = main_class,
        deps = ["//p4kt"],
    )

    sh_test(
        name = name + "_test",
        srcs = ["//tools:golden_test.sh"],
        args = [
            "$(location :{name}_bin)".format(name = name),
            "$(location {name}.p4)".format(name = name),
        ],
        data = [
            ":{name}_bin".format(name = name),
            "{name}.p4".format(name = name),
        ],
    )
