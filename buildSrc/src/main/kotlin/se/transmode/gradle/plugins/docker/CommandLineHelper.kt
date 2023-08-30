package se.transmode.gradle.plugins.docker

import org.gradle.api.Project
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

data class ExecResult(
    val statusCode: Int,
    val stdOut: String,
    val stdErr: String,
)

private fun collectOutput(inputStream: InputStream): Future<String> =
    CompletableFuture<String>()
        .completeAsync {
            inputStream.reader()
                .readText()
        }

/**
 * @return exitCode (should be zero), standard output, error output
 */
fun Project.executeAndWait(cmdLine: List<String>, failOnErrors: Boolean = true): ExecResult {
    val process = ProcessBuilder(cmdLine)
        .start()
    val stdOut = collectOutput(process.inputStream)
    val stdErr = collectOutput(process.errorStream)
    val execResult = ExecResult(
        process.waitFor(),
        stdOut.get(),
        stdErr.get()
    )
    if (failOnErrors && execResult.statusCode != 0) {
        logger.error(execResult.stdErr)
        logger.warn(execResult.stdOut)
        error("Failed to execute ${cmdLine.joinToString { "'$it'" }}")
    } else {
        logger.info(execResult.stdErr)
        logger.info(execResult.stdOut)
    }
    return execResult
}
