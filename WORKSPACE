workspace(name = "giraffe")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# grpc

http_archive(
    name = "io_grpc_grpc_java",
    sha256 = "7e9af0de77535f6b30df94ce63336aa42858c3bd2e9e9637c596b8f556a2d6ca",
    strip_prefix = "grpc-java-1.42.2",
    url = "https://github.com/grpc/grpc-java/archive/v1.42.2.zip",
)

load("@io_grpc_grpc_java//:repositories.bzl", "IO_GRPC_GRPC_JAVA_ARTIFACTS", "grpc_java_repositories")

grpc_java_repositories()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")

protobuf_deps()

# googleapis

http_archive(
    name = "com_google_googleapis",
    sha256 = "c0bf391e7db14ebcc359130fa9575d6ef9724b8dc0af82897dbd1b45d26afe68",
    strip_prefix = "googleapis-107a322d4c61b87221f89f627c73029330e17a5e",
    urls = ["https://github.com/googleapis/googleapis/archive/107a322d4c61b87221f89f627c73029330e17a5e.zip"],  # 2022.1.19
)

load("@com_google_googleapis//:repository_rules.bzl", "switched_rules_by_language")

switched_rules_by_language(
    name = "com_google_googleapis_imports",
    grpc = True,
    java = True,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = IO_GRPC_GRPC_JAVA_ARTIFACTS + [
        "com.github.spullara.mustache.java:compiler:0.9.4",
        "javax.servlet:javax.servlet-api:4.0.1",
        "org.projectlombok:lombok:1.18.22",
        "org.springframework:spring-web:5.3.16",
    ],
    excluded_artifacts = [
    ],
    fail_if_repin_required = True,
    fetch_javadoc = True,
    fetch_sources = True,
    generate_compat_repositories = True,
    maven_install_json = "//:maven_install.json",
    repositories = [
        "https://maven.aliyun.com/repository/public",
    ],
    version_conflict_policy = "pinned",
)

load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

load("@maven//:compat.bzl", "compat_repositories")

compat_repositories()
