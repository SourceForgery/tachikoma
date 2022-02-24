package se.transmode.gradle.plugins.docker

import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

private fun collectOutput(inputStream: InputStream): Future<String> =
    CompletableFuture<String>()
        .completeAsync {
            inputStream.reader()
                .readText()
        }

/**
 * @return exitCode (should be zero), standard output, error output
 */
fun executeAndWait(cmdLine: List<String>): Triple<Int, String, String> {
    val process = ProcessBuilder(cmdLine)
        .start()
    val stdOut = collectOutput(process.inputStream)
    val stdErr = collectOutput(process.errorStream)
    process.waitFor()
    return Triple(
        process.waitFor(),
        stdOut.get(),
        stdErr.get()
    )
}
