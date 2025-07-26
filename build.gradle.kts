import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.NoSuchElementException

plugins {
    id("java-library")
    id("de.undercouch.download") version("5.6.0")
}

group = "net.janrupf"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-tree:9.8")

    testImplementation("com.fasterxml.jackson.core:jackson-core:2.19.2")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
}

fun findProgram(name: String, env: String?): Path? {
    val localPrograms = file("local-programs.properties")
    if (localPrograms.exists()) {
        val properties = Properties()
        localPrograms.inputStream().use { properties.load(it) }

        if (properties.containsKey(env)) {
            return Paths.get(properties.getProperty(env))
        }
    }

    val sysEnv = System.getenv()

    if (env != null && sysEnv.containsKey(env)) {
        return Paths.get(sysEnv[env])
    }

    val sysPath = sysEnv["PATH"]
    val pathExt = sysEnv["PATHEXT"]?.split(File.pathSeparator) ?: listOf("")

    if (sysPath != null) {
        for (dir in sysPath.split(File.pathSeparator)) {
            for (ext in pathExt) {
                val file = Paths.get(dir, "$name$ext")
                if (Files.exists(file)) {
                    return file
                }
            }
        }
    }

    return null
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val wasmTestSuiteRepositoryName = "testsuite"
val wasmTestSuiteCommit = "88e97b0f742f4c3ee01fea683da130f344dd7b02"

val wasmTestSuiteDirectory = layout.buildDirectory.dir("wasm-testsuite")
val wasmTestSuiteZip = wasmTestSuiteDirectory.map { it.file("wasm-testsuite.zip") }
val wasmTestSuiteSource = wasmTestSuiteDirectory.map { it.dir("extracted") }

sourceSets {
    test {
        resources {
            srcDir(wasmTestSuiteSource)
        }
    }
}

tasks {
    val downloadWasmTestsuite = register<Download>("downloadWasmTestsuite") {
        src("https://github.com/WebAssembly/${wasmTestSuiteRepositoryName}/archive/${wasmTestSuiteCommit}.zip")
        dest(wasmTestSuiteZip)
    }

    val extractWasmTestsuite = register<Copy>("extractWasmTestsuite") {
        dependsOn(downloadWasmTestsuite)

        from(zipTree(wasmTestSuiteZip))
        into(wasmTestSuiteSource)

        exclude("**/proposals/**")
    }

    val compileTestWasm = register("compileTestWasm") {
        data class WasmOutput(
            val wasm: RegularFile?,
            val testJson: RegularFile?
        )

        dependsOn(extractWasmTestsuite)

        val wat2wasm = findProgram("wat2wasm", "WAT2WASM")
        val wast2json = findProgram("wast2json", "WAST2JSON")
        val collectedTestManifests = mutableListOf<String>()

        if (wat2wasm != null && wast2json != null) {
            val outputDir = layout.buildDirectory.dir("wasm/test")

            val inputFileProvider = sourceSets.test.map { set ->
                val files = set.resources.filter { it.isFile && (it.extension == "wat" || it.extension == "wast") }
                val rootDirs = set.resources.sourceDirectories

                rootDirs to files
            }

            val fileMappingProvider = inputFileProvider.map {
                val (rootDirs, files) = it
                files.map outputFiles@{ inputFile ->
                    for (rootDir in rootDirs) {
                        if (inputFile.startsWith(rootDir)) {
                            val relativeOutput = inputFile.relativeTo(rootDir)
                            val relativeOutputWasm = if (inputFile.extension == "wat")
                                File(relativeOutput.parent, "${relativeOutput.nameWithoutExtension}.wasm")
                            else null
                            val relativeTestJson = if (inputFile.extension == "wast")
                                File(relativeOutput.parent, "${relativeOutput.nameWithoutExtension}.json")
                            else null

                            relativeTestJson?.let { j -> collectedTestManifests.add(j.path) }

                            val outputFileProvider = outputDir.map { out ->
                                WasmOutput(
                                    relativeOutputWasm?.let { w -> out.file(w.path) },
                                    relativeTestJson?.let { j -> out.file(j.path) }
                                )
                            }
                            return@outputFiles (inputFile to outputFileProvider)
                        }
                    }

                    throw NoSuchElementException("Failed to find root directory for $inputFile")
                }
            }

            inputs.files(inputFileProvider.map { it.second })
            inputs.file(wat2wasm)
            outputs.dir(outputDir)

            doLast {
                fileMappingProvider.get().forEach { (inputFile, output) ->
                    val wasmFile = output.get().wasm?.asFile
                    val testJson = output.get().testJson?.asFile

                    val outputParent = wasmFile?.parentFile ?: testJson?.parentFile
                    if (outputParent?.exists() == false) {
                        outputParent.mkdirs()
                    }

                    if (wasmFile != null) {
                        exec {
                            executable(wat2wasm)
                            args(
                                inputFile.absolutePath,
                                "-o", wasmFile.absolutePath,
                                "--enable-annotations"
                            )
                        }
                    } else if (testJson != null) {
                        exec {
                            executable(wast2json)
                            args(
                                inputFile.absolutePath,
                                "-o", testJson.absolutePath
                            )
                        }
                    }
                }

                outputDir.map { it.file("wast-tests.txt") }.get()
                    .asFile.writeText(collectedTestManifests.joinToString("\n"))
            }
        } else {
            doFirst {
                if (wat2wasm == null) {
                    throw GradleScriptException("wat2wasm not found", NoSuchFileException("wat2wasm"))
                }

                if (wast2json == null) {
                    throw GradleScriptException("wast2json not found", NoSuchFileException("wast2json"))
                }
            }
        }
    }

    processTestResources {
        from(compileTestWasm)
    }

    test {
        useJUnitPlatform()

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
        }

        // jvmArgs(
        //     "-XX:+UnlockDiagnosticVMOptions",
        //     "-XX:CompileCommand=compileonly,*TestModule0.*",
        //     "-XX:CompileCommand=print,*TestModule0.*",
        //     "-Xcomp",
        //     "-XX:PrintAssemblyOptions=intel,mpad=10,cpad=10,code"
        // )
        environment("LD_LIBRARY_PATH" to "/home/janrupf/Downloads")
    }
}
