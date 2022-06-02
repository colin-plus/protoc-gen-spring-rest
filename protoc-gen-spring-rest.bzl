_gRpcSpringRestToolchainInfo = provider(
    fields = [
        "java_toolchain",
        "protoc",
    ],
)

def _grpc_spring_rest_library_toolchain_impl(ctx):
    return [
        _gRpcSpringRestToolchainInfo(
            java_toolchain = ctx.attr._java_toolchain,
            protoc = ctx.executable._protoc,
        ),
        platform_common.ToolchainInfo(),
    ]

grpc_spring_rest_library_toolchain = rule(
    attrs = {
        "_protoc": attr.label(
            cfg = "host",
            default = Label("@com_google_protobuf//:protoc"),
            executable = True,
        ),
        "_java_toolchain": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_toolchain"),
        ),
    },
    provides = [
        _gRpcSpringRestToolchainInfo,
        platform_common.ToolchainInfo,
    ],
    implementation = _grpc_spring_rest_library_toolchain_impl,
)

def _grpc_spring_rest_library_impl(ctx):
    toolchain = ctx.attr._toolchain[_gRpcSpringRestToolchainInfo]
    srcs = ctx.attr.srcs[0][ProtoInfo].direct_sources
    descriptor_set_in = ctx.attr.srcs[0][ProtoInfo].transitive_descriptor_sets

    srcjar = ctx.actions.declare_file("%s-proto-gensrc.jar" % ctx.label.name)

    args = ctx.actions.args()
    args.add(ctx.attr._plugin.files_to_run.executable, format = "--plugin=protoc-gen-spring-rest-plugin=%s")
    args.add(srcjar.path, format = "--spring-rest-plugin_out=%s")
    args.add_joined("--descriptor_set_in", descriptor_set_in, join_with = ctx.host_configuration.host_path_separator)
    args.add_all(srcs)

    tools_input, tools_input_manifests = ctx.resolve_tools(tools = [ctx.attr._plugin])
    ctx.actions.run(
        inputs = depset(srcs, transitive = [descriptor_set_in]),
        outputs = [srcjar],
        executable = toolchain.protoc,
        arguments = [args],
        tools = tools_input,
        input_manifests = tools_input_manifests,
    )

    deps_java_info = java_common.merge([dep[JavaInfo] for dep in ctx.attr.deps])

    java_info = java_common.compile(
        ctx,
        java_toolchain = toolchain.java_toolchain[java_common.JavaToolchainInfo],
        source_jars = [srcjar],
        output = ctx.outputs.jar,
        output_source_jar = ctx.outputs.srcjar,
        deps = [java_common.make_non_strict(deps_java_info)] + [dep[JavaInfo] for dep in ctx.attr._runtime],
    )

    return [java_info]

_grpc_spring_rest_library = rule(
    attrs = {
        "srcs": attr.label_list(mandatory = True, allow_empty = False, providers = [ProtoInfo]),
        "deps": attr.label_list(mandatory = True, allow_empty = False, providers = [JavaInfo]),
        "_runtime": attr.label_list(default = []),
        "_plugin": attr.label(default = Label("//protoc-gen-spring-rest")),
        "_toolchain": attr.label(default = Label("//protoc-gen-spring-rest:grpc_spring_rest_library_toolchain")),
    },
    fragments = ["java"],
    outputs = {"jar": "lib%{name}.jar", "srcjar": "lib%{name}-src.jar"},
    provides = [JavaInfo],
    implementation = _grpc_spring_rest_library_impl,
)

def grpc_spring_rest_library(
        name,
        srcs,
        deps,
        **kwargs):
    _grpc_spring_rest_library(
        name = name,
        srcs = srcs,
        deps = deps,
        **kwargs
    )
