import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*
import kotlin.NoSuchElementException

plugins {
    id("java-library")
}

group = "net.janrupf"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.ow2.asm:asm:9.7")
}

fun findProgram(name: String, env: String?): Path? {
    val localPrograms = file("local-programs.properties")
    if (localPrograms.exists()) {
        val properties = Properties()
        localPrograms.inputStream().use { properties.load(it) }

        if (properties.containsKey(env)) {
            return Path.of(properties.getProperty(env))
        }
    }

    val sysEnv = System.getenv()

    if (env != null && sysEnv.containsKey(env)) {
        return Path.of(sysEnv[env])
    }

    val sysPath = sysEnv["PATH"]
    val pathExt = sysEnv["PATHEXT"]?.split(File.pathSeparator) ?: listOf("")

    if (sysPath != null) {
        for (dir in sysPath.split(File.pathSeparator)) {
            for (ext in pathExt) {
                val file = Path.of(dir, "$name$ext")
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

tasks {
    val compileTestWasm = register("compileTestWasm") {
        val wat2wasm = findProgram("wat2wasm", "WAT2WASM")

        if (wat2wasm != null) {
            val outputDir = layout.buildDirectory.dir("wasm/test")

            val inputFileProvider = sourceSets.test.map { set ->
                val files = set.resources.filter { it.isFile && it.extension == "wat" }
                val rootDirs = set.resources.sourceDirectories

                rootDirs to files
            }

            val fileMappingProvider = inputFileProvider.map {
                val (rootDirs, files) = it
                files.map outputFiles@{ inputFile ->
                    for (rootDir in rootDirs) {
                        if (inputFile.startsWith(rootDir)) {
                            val relativeOutput = inputFile.relativeTo(rootDir)
                            val relativeOutputWasm =
                                File(relativeOutput.parent, "${relativeOutput.nameWithoutExtension}.wasm")

                            val outputFileProvider = outputDir.map { out -> out.file(relativeOutputWasm.path) }
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
                fileMappingProvider.get().forEach { (inputFile, outputFile) ->
                    exec {
                        executable(wat2wasm)
                        args(
                            inputFile.absolutePath,
                            "-o", outputFile.get().asFile.absolutePath,
                            "--enable-annotations"
                        )
                    }
                }
            }
        } else {
            doFirst {
                throw GradleScriptException("wat2wasm not found", NoSuchFileException("wat2wasm"))
            }
        }
    }

    processTestResources {
        from(compileTestWasm)
    }

    test {
        useJUnitPlatform()

        /*jvmArgs(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:CompileCommand=compileonly,*TestModule0.*",
            "-Xcomp",
            "-XX:+PrintAssembly",
            "-XX:PrintAssemblyOptions=intel,mpad=10,cpad=10,code"
        )
        environment("LD_LIBRARY_PATH" to "/home/janrupf/Downloads")*/
    }
}
