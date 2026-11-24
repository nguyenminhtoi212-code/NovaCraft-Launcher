import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9.x tích hợp sẵn Kotlin; giữ kotlin.android sẽ xung đột classpath plugin.
    // alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp.plugin)
    // Đã tắt plugin kotlin.serialization vì chưa khai báo trong libs.versions.toml và mã nguồn không dùng.
    // alias(libs.plugins.kotlin.serialization)
}

fun baseVersionName(): String =
    rootProject.file("VERSION").takeIf { it.isFile }
        ?.readText()?.trim()?.takeIf { it.isNotEmpty() }
        ?: "2.3.0"

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

// Kiểm tra khóa ký phát hành từ biến môi trường (hỗ trợ cả NOVACRAFT_* và AMBER_*)
val releaseKeystoreFile: File? = (System.getenv("NOVACRAFT_KEYSTORE_FILE") ?: System.getenv("AMBER_KEYSTORE_FILE"))
    ?.takeIf { it.isNotBlank() }
    ?.let { File(it) }
    ?.takeIf { it.isFile }
val releaseKeystorePassword: String? = (System.getenv("NOVACRAFT_KEYSTORE_PASSWORD") ?: System.getenv("AMBER_KEYSTORE_PASSWORD"))
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
val hasReleaseKey = releaseKeystoreFile != null && releaseKeystorePassword != null

val rootDebugKeystore = rootProject.file("debug.keystore")

android {
    namespace = "com.aistudio.novacraft.launcher"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.aistudio.novacraft.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.let { 200 + it } ?: 220
        versionName = (System.getenv("NOVACRAFT_VERSION_NAME") ?: System.getenv("AMBER_VERSION_NAME"))
            ?.trim()?.takeIf { it.isNotEmpty() } ?: baseVersionName()

        // Kunci API CurseForge cho tải mod: lấy từ biến môi trường CURSEFORGE_API_KEY hoặc local.properties (curseforge.api.key).
        val curseForgeApiKey = (System.getenv("CURSEFORGE_API_KEY") ?: localProperties.getProperty("curseforge.api.key") ?: "")
            .filter { !it.isWhitespace() && it != '"' && it != '\\' }
        buildConfigField("String", "CURSEFORGE_API_KEY", "\"" + curseForgeApiKey + "\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        multiDexEnabled = false

        // Chỉ hỗ trợ 64-bit (arm64-v8a); cấu hình 32-bit đã được loại bỏ.
        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        if (rootDebugKeystore.isFile) {
            create("debugConfig") {
                storeFile = rootDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
        if (hasReleaseKey) {
            create("amberRelease") {
                storeFile = releaseKeystoreFile
                storeType = "pkcs12"
                storePassword = releaseKeystorePassword
                keyAlias = "amber"
                keyPassword = releaseKeystorePassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    val resolvedDebugSigning = if (rootDebugKeystore.isFile) {
        signingConfigs.getByName("debugConfig")
    } else {
        signingConfigs.getByName("debug")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("amberRelease")
            } else {
                resolvedDebugSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("productionRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks.add("release")
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("amberRelease")
            } else {
                resolvedDebugSigning
            }
        }
        debug {
            isMinifyEnabled = true
            isShrinkResources = false
            isDebuggable = true
            signingConfig = resolvedDebugSigning
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // minSdk = 26 đã hỗ trợ sẵn java.time & java.nio.file; tắt desugaring giúp gộp trọn vào 1 classes.dex duy nhất.
        isCoreLibraryDesugaringEnabled = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        noCompress += listOf("zip", "xz")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf(
                "**/libc++_shared.so",
                "**/libhorizon_android.so",
                "**/libopenal.so",
                "**/libSDL2.so",
                "**/libSDL3.so",
                "**/libbytehook.so"
            )
        }

        resources {
            pickFirsts += setOf("**/kotlin/**", "**/org/**")
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES",
                "META-INF/com/android/build/**",
                "META-INF/version-control-info.textproto",
                "META-INF/*.version",
                "google/**",
                "kotlin/**",
                "okhttp3/internal/publicsuffix/publicsuffixes.gz",
                "org/mozilla/javascript/resources/Messages_*.properties",
                "DebugProbesKt.bin",
                "META-INF/**/LICENSE.txt",
                "META-INF/**/LICENSE",
                "META-INF/**/LICENSE.md",
                "META-INF/*.md",
                "/META-INF/CONTRIBUTORS.md",
                "/META-INF/README*",
                "/META-INF/INDEX.LIST",
                "**/*.proto"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

configurations.all {
    exclude(group = "androidx.privacysandbox.ads", module = "ads-adservices")
    exclude(group = "androidx.privacysandbox.ads", module = "ads-adservices-java")
    exclude(group = "com.google.android.gms", module = "play-services-ads-identifier")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-annotation-processing-gradle")
}

dependencies {
    // Tắt coreLibraryDesugaring (minSdk 26 hỗ trợ gốc NIO/Time) để tránh tách classes2.dex
    // coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.bytehook)
    // Klien proxy resmi mod TouchController (LGPL-3.0). Dipanggil lewat refleksi, lihat TouchControllerBridge.
    implementation(libs.proxy.client.android)
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    debugImplementation(libs.androidx.ui.tooling)

    // ---- Các thư viện không được mã nguồn sử dụng (đã chú thích để tối ưu dung lượng APK & tốc độ build) ----
    // implementation("androidx.multidex:multidex:2.0.1")
    // implementation("androidx.appcompat:appcompat:1.7.0")
    // implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    // implementation("androidx.datastore:datastore-preferences:1.1.1")
    // implementation("androidx.work:work-runtime-ktx:2.9.1")
    // implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    // implementation("androidx.navigation:navigation-compose:2.8.5")
    // implementation("androidx.media3:media3-exoplayer:1.5.1")
    // implementation("io.ktor:ktor-client-core:2.3.8")
    // implementation("io.coil-kt:coil-compose:2.7.0")
    // implementation("com.github.bumptech.glide:glide:4.16.0")
    // implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
    // implementation("com.google.guava:guava:33.3.1-android")
    // implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    // implementation("com.jakewharton.timber:timber:5.0.1")
    // implementation("com.materialkolor:material-kolor:1.4.0")
    // implementation("com.valentinilk.shimmer:compose-shimmer:1.2.0")
    // implementation("sh.calvin.reorderable:reorderable:1.3.2")
    // implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
    // implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")
    // implementation("com.github.yalantis:ucrop:2.2.8")
    // implementation("org.apache.commons:commons-lang3:3.14.0")
    // implementation("androidx.palette:palette-ktx:1.0.0")
    // implementation("androidx.graphics:graphics-shapes:1.0.0-alpha05")
    // implementation("androidx.mediarouter:mediarouter:1.7.0")
    // implementation("androidx.browser:browser:1.8.0")
    // implementation(files("libs/opennbt-1.6-full.jar"))
    // implementation(files("libs/horizon-natives-merged.aar")) // Trùng .so với jniLibs/arm64-v8a & kéo theo libspirv-cross 40MB + 32-bit
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

fun publishApkAtomically(src: File, destDirs: List<File>) {
    if (!src.isFile) return
    for (dir in destDirs) {
        dir.mkdirs()
        val tmp = File(dir, "${src.name}.tmp")
        val target = File(dir, src.name)
        src.copyTo(tmp, overwrite = true)
        Files.move(
            tmp.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
    }
}

tasks.register("copyDebugApkToOutputs") {
    val srcApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
    val outDir = rootProject.layout.projectDirectory.dir("build-outputs").asFile
    val dotOutDir = rootProject.layout.projectDirectory.dir(".build-outputs").asFile
    doLast {
        publishApkAtomically(srcApk.get().asFile, listOf(outDir, dotOutDir))
    }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy("copyDebugApkToOutputs")
}

tasks.register("copyReleaseApkToOutputs") {
    val releaseDir = layout.buildDirectory.dir("outputs/apk/release")
    val prodReleaseDir = layout.buildDirectory.dir("outputs/apk/productionRelease")
    val outDir = rootProject.layout.projectDirectory.dir("build-outputs").asFile
    val dotOutDir = rootProject.layout.projectDirectory.dir(".build-outputs").asFile
    doLast {
        for (dir in listOf(releaseDir.get().asFile, prodReleaseDir.get().asFile)) {
            dir.listFiles { f -> f.isFile && f.extension == "apk" }?.forEach { apk ->
                publishApkAtomically(apk, listOf(outDir, dotOutDir))
            }
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "assembleProductionRelease" }.configureEach {
    finalizedBy("copyReleaseApkToOutputs")
}

// ---- Pemangkasan simbol pustaka native (jaring pengaman ukuran) ----
fun findLlvmStrip(): File? {
    val exe = if (System.getProperty("os.name").lowercase().contains("win")) "llvm-strip.exe" else "llvm-strip"
    val roots = mutableListOf<File>()
    for (key in listOf("ANDROID_NDK_HOME", "ANDROID_NDK_ROOT", "ANDROID_NDK_LATEST_HOME")) {
        System.getenv(key)?.takeIf { it.isNotBlank() }?.let { roots.add(File(it)) }
    }
    for (key in listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
        System.getenv(key)?.takeIf { it.isNotBlank() }?.let { sdk ->
            File(sdk, "ndk").listFiles()?.sortedByDescending { it.name }?.let { roots.addAll(it) }
        }
    }
    for (root in roots) {
        val hosts = File(root, "toolchains/llvm/prebuilt").listFiles() ?: continue
        for (host in hosts) {
            val tool = File(host, "bin/$exe")
            if (tool.isFile) return tool
        }
    }
    for (dir in (System.getenv("PATH") ?: "").split(File.pathSeparator)) {
        if (dir.isBlank()) continue
        val tool = File(dir, exe)
        if (tool.isFile && tool.canExecute()) return tool
    }
    return null
}

// True bila berkas ELF 64-bit little-endian punya bagian tabel simbol (.symtab, tipe SHT_SYMTAB = 2).
fun elfHasSymbols(file: File): Boolean {
    try {
        RandomAccessFile(file, "r").use { raf ->
            if (raf.length() < 64) return false
            val header = ByteArray(64)
            raf.readFully(header)
            val isElf = header[0].toInt() == 0x7F && header[1].toInt() == 'E'.code &&
                header[2].toInt() == 'L'.code && header[3].toInt() == 'F'.code
            if (!isElf || header[4].toInt() != 2 || header[5].toInt() != 1) return false
            val head = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val shoff = head.getLong(0x28)
            val shentsize = head.getShort(0x3A).toInt() and 0xFFFF
            val shnum = head.getShort(0x3C).toInt() and 0xFFFF
            if (shoff <= 0L || shentsize < 64 || shnum == 0) return false
            if (shoff + shentsize.toLong() * shnum > raf.length()) return false
            val entry = ByteArray(8)
            for (i in 0 until shnum) {
                raf.seek(shoff + i.toLong() * shentsize)
                raf.readFully(entry)
                if (ByteBuffer.wrap(entry).order(ByteOrder.LITTLE_ENDIAN).getInt(4) == 2) return true
            }
        }
    } catch (_: Throwable) {
    }
    return false
}

tasks.configureEach {
    if (name == "stripReleaseDebugSymbols") {
        doLast {
            try {
                val unstripped = outputs.files.files
                    .flatMap { root -> if (root.isDirectory) root.walkTopDown().toList() else listOf(root) }
                    .filter { it.isFile && it.name.endsWith(".so") && elfHasSymbols(it) }
                if (unstripped.isNotEmpty()) {
                    val tool = findLlvmStrip()
                    if (tool == null) {
                        logger.warn("[NovaCraft] ${unstripped.size} pustaka native masih berisi simbol, tetapi llvm-strip tidak ditemukan (NDK belum terpasang).")
                    } else {
                        var saved = 0L
                        var done = 0
                        for (lib in unstripped) {
                            val before = lib.length()
                            val proc = ProcessBuilder(tool.absolutePath, "--strip-unneeded", lib.absolutePath)
                                .redirectErrorStream(true)
                                .start()
                            proc.inputStream.readBytes()
                            if (proc.waitFor() == 0) {
                                saved += before - lib.length()
                                done++
                            }
                        }
                        logger.lifecycle("[NovaCraft] Simbol dipangkas dari $done dari ${unstripped.size} pustaka native, hemat ${saved / 1024 / 1024} MB.")
                    }
                }
            } catch (e: Throwable) {
                logger.warn("[NovaCraft] Pemangkasan simbol dilewati: ${e.message}")
            }
        }
    }
}

// ---- Mesin Rust (Horizon): dipasang otomatis sebelum build ----

val localHorizonBundle = listOf(
    File(rootDir, "horizon-engine-arm64-v8a.zip"),
    File(rootDir, "horizon-engine/horizon-engine-arm64-v8a.zip"),
    File(rootDir, "dist/horizon-engine-arm64-v8a.zip")
).firstOrNull { it.isFile }

val horizonRequested = System.getenv("HORIZON_ENGINE")?.trim()?.lowercase()
val inCiBuild = System.getenv("GITHUB_ACTIONS")?.equals("true", ignoreCase = true) == true ||
    System.getenv("CI")?.equals("true", ignoreCase = true) == true

val fetchHorizonEngine by tasks.registering(Exec::class) {
    group = "novacraft"
    description = "Memasang mesin Rust (Horizon) dari Release resmi atau paket lokal ke APK"
    workingDir = rootDir
    if (localHorizonBundle != null) {
        commandLine("bash", "scripts/fetch-horizon-engine.sh", "--bundle", localHorizonBundle.absolutePath)
    } else {
        commandLine("bash", "scripts/fetch-horizon-engine.sh")
    }
    isIgnoreExitValue = true
    outputs.upToDateWhen { false }
    enabled = !System.getProperty("os.name").lowercase().contains("win") &&
        horizonRequested != "off" &&
        (localHorizonBundle != null || (!gradle.startParameter.isOffline && (horizonRequested == "on" || inCiBuild))) &&
        File(rootDir, "scripts/fetch-horizon-engine.sh").isFile
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(fetchHorizonEngine)
}
