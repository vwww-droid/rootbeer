import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    signing
}

apply(plugin = "stringfog")

val llmotivateOmvllEnabled = project.findBooleanPropertyOrDefault("llmotivateOmvllEnabled")
val llmotivateOmvllPlugin = project.findStringPropertyOrDefault("llmotivateOmvllPlugin")
val llmotivateOmvllConfig = project.findStringPropertyOrDefault("llmotivateOmvllConfig")
val llmotivateOmvllPythonPath = project.findStringPropertyOrDefault("llmotivateOmvllPythonPath")
val llmotivateOmvllOutputDir = project.findStringPropertyOrDefault("llmotivateOmvllOutputDir")
val llmotivateOmvllLauncher = project.findStringPropertyOrDefault("llmotivateOmvllLauncher")

if (llmotivateOmvllEnabled) {
    require(llmotivateOmvllPlugin.isNotBlank()) { "缺少 Gradle 属性 llmotivateOmvllPlugin." }
    require(llmotivateOmvllConfig.isNotBlank()) { "缺少 Gradle 属性 llmotivateOmvllConfig." }
    require(llmotivateOmvllPythonPath.isNotBlank()) { "缺少 Gradle 属性 llmotivateOmvllPythonPath." }
    require(llmotivateOmvllOutputDir.isNotBlank()) { "缺少 Gradle 属性 llmotivateOmvllOutputDir." }
    require(llmotivateOmvllLauncher.isNotBlank()) { "缺少 Gradle 属性 llmotivateOmvllLauncher." }
}

android {
    namespace = "com.scottyab.rootbeer"

    defaultConfig {
        ndk {
            abiFilters.addAll(setOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a"))
        }

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                arguments.add("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
                if (llmotivateOmvllEnabled) {
                    arguments.addAll(
                        listOf(
                            "-DLLMOTIVATE_OMVLL_ENABLE=ON",
                            "-DLLMOTIVATE_OMVLL_PLUGIN=${project.file(llmotivateOmvllPlugin).invariantSeparatorsPath}",
                            "-DLLMOTIVATE_OMVLL_CONFIG=${project.file(llmotivateOmvllConfig).invariantSeparatorsPath}",
                            "-DLLMOTIVATE_OMVLL_PYTHONPATH=${project.file(llmotivateOmvllPythonPath).invariantSeparatorsPath}",
                            "-DLLMOTIVATE_OMVLL_OUTPUT_DIR=${project.file(llmotivateOmvllOutputDir).invariantSeparatorsPath}",
                            "-DLLMOTIVATE_OMVLL_COMPILER_LAUNCHER=${project.file(llmotivateOmvllLauncher).invariantSeparatorsPath}",
                            "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
                        )
                    )
                }
                // added to improve security of binary #180
                cFlags("-fPIC")
                cppFlags("-fPIC")
            }
        }
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        targetSdk = libs.versions.android.target.sdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation("com.github.megatronking.stringfog:xor:5.0.0-llm3")

    testImplementation(libs.junit)
    testImplementation(libs.mockito)
}

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    enable = true
    fogPackages = arrayOf("com.scottyab.rootbeer")
    kg = RandomKeyGenerator()
    mode = StringFogMode.visible
    visiblePrefix = "商业软件禁止逆向"
    fogClassPackage = "o0O"
    fogClassName = "O00"
    generateFogClass = false
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = findStringPropertyOrDefault("POM_ARTIFACT_ID")

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name = findStringPropertyOrDefault("POM_NAME")
                packaging = findStringPropertyOrDefault("POM_PACKAGING")
                description = findStringPropertyOrDefault("POM_DESCRIPTION")
                url = findStringPropertyOrDefault("POM_URL")

                scm {
                    url = findStringPropertyOrDefault("POM_SCM_URL")
                    connection = findStringPropertyOrDefault("POM_SCM_CONNECTION")
                    developerConnection = findStringPropertyOrDefault("POM_SCM_DEV_CONNECTION")
                }

                licenses {
                    license {
                        name = findStringPropertyOrDefault("POM_LICENCE_NAME")
                        url = findStringPropertyOrDefault("POM_LICENCE_URL")
                        distribution = findStringPropertyOrDefault("POM_LICENCE_DIST")
                    }
                }

                developers {
                    developer {
                        id = findStringPropertyOrDefault("POM_DEVELOPER_ID")
                        name = findStringPropertyOrDefault("POM_DEVELOPER_NAME")
                        organizationUrl = findStringPropertyOrDefault("POM_URL")
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["release"])
}

private fun Project.findStringPropertyOrDefault(propertyName: String, default: String = ""): String =
    findProperty(propertyName)?.toString() ?: default

private fun Project.findBooleanPropertyOrDefault(propertyName: String, default: Boolean = false): Boolean =
    findProperty(propertyName)?.toString()?.equals("true", ignoreCase = true) ?: default
