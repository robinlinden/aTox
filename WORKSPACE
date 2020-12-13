workspace(name = "atox")

android_sdk_repository(name = "androidsdk")

android_ndk_repository(
    name = "androidndk",
    api_level = 19,
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "rules_pkg",
    sha256 = "352c090cc3d3f9a6b4e676cf42a6047c16824959b438895a76c2989c6d7c246a",
    url = "https://github.com/bazelbuild/rules_pkg/releases/download/0.2.5/rules_pkg-0.2.5.tar.gz",
)

http_archive(
    name = "rules_proto",
    sha256 = "602e7161d9195e50246177e7c55b2f39950a9cf7366f74ed5f22fd45750cd208",
    strip_prefix = "rules_proto-97d8af4dc474595af3900dd85cb3a29ad28cc313",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
        "https://github.com/bazelbuild/rules_proto/archive/97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
    ],
)

http_archive(
    name = "rules_android",
    urls = ["https://github.com/bazelbuild/rules_android/archive/v0.1.1.zip"],
    sha256 = "cd06d15dd8bb59926e4d65f9003bfc20f9da4b2519985c27e190cddc8b7a7806",
    strip_prefix = "rules_android-0.1.1",
)

http_archive(
    name = "android_test_support",
    urls = ["https://github.com/android/android-test/archive/androidx-test-1.3.0.zip"],
    sha256 = "40b122cd3d47b8cb4bf2cb39eb8c6a6b7d8da6595bff17661a64aa88747dbd4e",
    strip_prefix = "android-test-androidx-test-1.3.0",
)

load("@android_test_support//:repo.bzl", "android_test_repositories")

android_test_repositories()

http_archive(
    name = "robolectric",
    urls = ["https://github.com/robolectric/robolectric-bazel/archive/4.4.tar.gz"],
    sha256 = "d4f2eb078a51f4e534ebf5e18b6cd4646d05eae9b362ac40b93831bdf46112c7",
    strip_prefix = "robolectric-bazel-4.4",
)

load("@robolectric//bazel:robolectric.bzl", "robolectric_repositories")

robolectric_repositories()

STARDOC_TAG = "0.4.0"

STARDOC_SHA = "36b8d6c2260068b9ff82faea2f7add164bf3436eac9ba3ec14809f335346d66a"

http_archive(
    name = "io_bazel_stardoc",
    sha256 = STARDOC_SHA,
    strip_prefix = "stardoc-%s" % STARDOC_TAG,
    url = "https://github.com/bazelbuild/stardoc/archive/%s.zip" % STARDOC_TAG,
)

RULES_JVM_EXTERNAL_TAG = "3.3"

RULES_JVM_EXTERNAL_SHA = "d85951a92c0908c80bd8551002d66cb23c3434409c814179c0ff026b53544dab"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "androidx.room:room-ktx:2.2.5",
        "androidx.room:room-runtime:2.2.5",
        "androidx.room:room-testing:2.2.5",
        "androidx.test.ext:junit:1.1.2",
        "com.google.dagger:dagger:2.30.1",
        "com.google.guava:guava:19.0",
        "com.typesafe.scala-logging:scala-logging_2.11:3.7.2",
        "javax.inject:javax.inject:1",
        "junit:junit:4.13.1",
        "io.mockk:mockk-android:1.10.0",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2",
        "org.jetbrains:annotations:13.0",
        "org.slf4j:slf4j-api:1.7.25",
        "org.scala-lang:scala-library:2.11.12",
        "androidx.lifecycle:lifecycle-extensions:2.2.0",
        "androidx.lifecycle:lifecycle-livedata-ktx:2.2.0",
        "org.robolectric:robolectric:4.4",
        "com.google.code.gson:gson:2.8.6",
    ],
    repositories = [
        "https://jcenter.bintray.com/",
        "https://dl.google.com/dl/android/maven2/",
    ],
)

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "io_bazel_rules_kotlin",
    commit = "95b642f10fb9c73312edd59bcf6ba6250ef8a2cb",
    remote = "https://github.com/bazelbuild/rules_kotlin.git",
    shallow_since = "1605641205 -0500",
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")

kt_register_toolchains()

kotlin_repositories()

git_repository(
    name = "io_bazel_rules_scala",
    commit = "73c0dbb55d1ab2905c3d97923efc415623f67ac6",
    remote = "https://github.com/bazelbuild/rules_scala.git",
    shallow_since = "1588140396 +0300",
)

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")
load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")
load("@io_bazel_rules_scala//scala_proto:scala_proto.bzl", "scala_proto_repositories")
load("@io_bazel_rules_scala//scala_proto:toolchains.bzl", "scala_proto_register_enable_all_options_toolchain")

scala_register_toolchains()

scala_repositories()

scala_proto_repositories()

scala_proto_register_enable_all_options_toolchain()

load("@bazel_tools//tools/build_defs/repo:git.bzl", "new_git_repository")

new_git_repository(
    name = "jvm-toxcore-api",
    build_file = "//bazel:BUILD.jvm-toxcore-api",
    commit = "adb835597e1eac8d2ca80b938b4f37d260cfde36",
    remote = "https://github.com/TokTok/jvm-toxcore-api.git",
    shallow_since = "1587772287 +0000",
)

new_git_repository(
    name = "jvm-toxcore-c",
    build_file = "//bazel:BUILD.jvm-toxcore-c",
    commit = "50d9a6b565de348c00daab83575498fdaec853a8",
    remote = "https://github.com/TokTok/jvm-toxcore-c.git",
    shallow_since = "1588255275 +0100",
)

new_git_repository(
    name = "jvm-macros",
    build_file = "//bazel:BUILD.jvm-macros",
    commit = "f22e243a3192b5d808fac3b1135bb6b8cefef6b3",
    remote = "https://github.com/TokTok/jvm-macros.git",
    shallow_since = "1587772287 +0000",
)

http_archive(
    name = "libsodium",
    build_file = "//bazel:BUILD.libsodium",
    sha256 = "1b72c0cdbc535ce42e14ac15e8fc7c089a3ee9ffe5183399fd77f0f3746ea794",
    strip_prefix = "libsodium-1.0.18",
    url = "https://github.com/jedisct1/libsodium/archive/1.0.18.zip",
)

http_archive(
    name = "c-toxcore",
    build_file = "//bazel:BUILD.c-toxcore",
    patch_cmds = [
        "echo toxcore/ > .bazelignore",
        "echo toxencryptsave/ >> .bazelignore",
        "echo toxav/ >> .bazelignore",
    ],
    sha256 = "6d21fcd8d505e03dcb302f4c94b4b4ef146a2e6b79d4e649f99ce4d9a4c0281f",
    strip_prefix = "c-toxcore-0.2.12",
    url = "https://github.com/TokTok/c-toxcore/archive/v0.2.12.zip",
)

http_archive(
    name = "opus",
    build_file = "//bazel:BUILD.opus",
    sha256 = "09366bf588b02b76bda3fd1428a30b55ca995d6d2eac509a39919f337690329e",
    strip_prefix = "opus-5c94ec3205c30171ffd01056f5b4622b7c0ab54c",
    url = "https://github.com/xiph/opus/archive/5c94ec3205c30171ffd01056f5b4622b7c0ab54c.zip",
)

http_archive(
    name = "libvpx",
    build_file = "//bazel:BUILD.libvpx",
    sha256 = "27d082899b60dea79c596affc68341522db1f72c241f6d6096fc46bcf774f217",
    strip_prefix = "libvpx-3d28ff98039134325cf689d8d08996fc8dabb225",
    url = "https://github.com/webmproject/libvpx/archive/3d28ff98039134325cf689d8d08996fc8dabb225.zip",
)
