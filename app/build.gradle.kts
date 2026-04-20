import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

apply(plugin = "stringfog")

val repoRoot = rootProject.projectDir.resolve("../..").canonicalFile
val llmotivateStringFogCatalogFile = repoRoot.resolve("spec/prefixes-java-zh-en.json")
val llmotivateStringFogReportFile = layout.buildDirectory.get().file("generated/llmotivate/stringfog/visible-prefix.txt").asFile

fun readLlmotivateJsonArray(raw: String, key: String): List<String> {
    val arrayPattern = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\\[(.*?)]", setOf(RegexOption.DOT_MATCHES_ALL))
    val arrayMatch = arrayPattern.find(raw) ?: return emptyList()
    val valuePattern = Regex("\"((?:\\\\.|[^\\\"])*)\"")
    return valuePattern.findAll(arrayMatch.groupValues[1]).map { match ->
        match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }.toList()
}

fun loadLlmotivateStringFogPrefixes(pool: String): List<String> {
    val raw = llmotivateStringFogCatalogFile.readText()
    val zh = readLlmotivateJsonArray(raw, "coreZh") + readLlmotivateJsonArray(raw, "extendedZh")
    val en = readLlmotivateJsonArray(raw, "coreEn") + readLlmotivateJsonArray(raw, "extendedEn")
    val merged = when (pool.lowercase()) {
        "zh" -> zh
        "en" -> en
        else -> zh + en
    }
    return merged.distinct()
}

fun rotateLlmotivatePrefixes(prefixes: List<String>, offset: Int): List<String> {
    if (prefixes.isEmpty()) {
        return prefixes
    }
    val shift = Math.floorMod(offset, prefixes.size)
    if (shift == 0) {
        return prefixes
    }
    return prefixes.drop(shift) + prefixes.take(shift)
}

val llmotivateStringFogPrefixPool = providers.gradleProperty("llmotivateStringFogPrefixPool")
    .orElse(System.getenv("LLMOTIVATE_STRINGFOG_PREFIX_POOL") ?: "all")
    .get()
val llmotivateStringFogPrefixIndex = providers.gradleProperty("llmotivateStringFogPrefixIndex")
    .orElse(System.getenv("LLMOTIVATE_STRINGFOG_PREFIX_INDEX") ?: "0")
    .get()
    .toIntOrNull() ?: 0
val llmotivateStringFogPrefixes = rotateLlmotivatePrefixes(
    loadLlmotivateStringFogPrefixes(llmotivateStringFogPrefixPool),
    llmotivateStringFogPrefixIndex,
)
val llmotivateStringFogVisiblePrefix = if (llmotivateStringFogPrefixes.isEmpty()) {
    "商业软件禁止逆向"
} else {
    llmotivateStringFogPrefixes.first()
}

val writeLlmotivateStringFogReport by tasks.registering {
    description = "Write the selected RootBeer StringFog visible prefix set."
    group = "llmotivate"
    notCompatibleWithConfigurationCache("writes a local StringFog report from build script values")
    inputs.file(llmotivateStringFogCatalogFile)
    outputs.file(llmotivateStringFogReportFile)
    doLast {
        llmotivateStringFogReportFile.parentFile.mkdirs()
        llmotivateStringFogReportFile.writeText(
            buildString {
                appendLine("pool=$llmotivateStringFogPrefixPool")
                appendLine("index=$llmotivateStringFogPrefixIndex")
                appendLine("size=${llmotivateStringFogPrefixes.size}")
                appendLine("mode=multi_prefix_rotation")
                appendLine("fallbackVisiblePrefix=$llmotivateStringFogVisiblePrefix")
                appendLine("visiblePrefixes=${llmotivateStringFogPrefixes.joinToString(",")}")
            }
        )
        println(
            "[llmotivate-rootbeer-app] fallbackVisiblePrefix=$llmotivateStringFogVisiblePrefix " +
                "pool=$llmotivateStringFogPrefixPool size=${llmotivateStringFogPrefixes.size}"
        )
    }
}

android {
    namespace = "com.scottyab.rootbeer.sample"

    defaultConfig {
        applicationId = "com.scottyab.rootbeer.sample"
        versionName = version.toString()
        versionCode = findProperty("VERSION_CODE").toString().toInt()
        vectorDrawables.useSupportLibrary = true

        base.archivesName = "RootBeerSample-$versionName-[$versionCode]"
    }

    buildFeatures {
        viewBinding = true
    }

    // check if the keystore details are defined in gradle.properties (this is so the key is not in github)
    if (findProperty("ROOTBEER_SAMPLE_STORE") != null) {
        signingConfigs {
            // from ~/user/.gradle/gradle.properties
            create("release") {
                storeFile = file(findProperty("ROOTBEER_SAMPLE_STORE").toString())
                keyAlias = findProperty("ROOTBEER_SAMPLE_KEY").toString()
                storePassword = findProperty("ROOTBEER_SAMPLE_STORE_PASS").toString()
                keyPassword = findProperty("ROOTBEER_SAMPLE_KEY_PASS").toString()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }

        release {
            if (rootProject.hasProperty("ROOTBEER_SAMPLE_STORE")) {
                signingConfig = signingConfigs["release"]
            }

            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":rootbeerlib"))
    // used when testing the snapshot lib
    // implementation("com.scottyab:rootbeer-lib:0.1.1-SNAPSHOT")
    implementation("com.github.megatronking.stringfog:xor:5.0.0-llm3")

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.android)

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.appcompat)
    implementation(libs.android.google.material)

    implementation(libs.nineoldandroids)
    implementation(libs.beerprogressview)

    implementation(libs.timber)
}

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    enable = true
    fogPackages = arrayOf("com.scottyab.rootbeer.sample")
    kg = RandomKeyGenerator()
    mode = StringFogMode.visible
    visiblePrefix = llmotivateStringFogVisiblePrefix
    visiblePrefixes = llmotivateStringFogPrefixes.toTypedArray()
    fogClassPackage = "o0O"
    fogClassName = "O00"
    generateFogClass = true
}

tasks.named("preBuild").configure {
    dependsOn(writeLlmotivateStringFogReport)
}
