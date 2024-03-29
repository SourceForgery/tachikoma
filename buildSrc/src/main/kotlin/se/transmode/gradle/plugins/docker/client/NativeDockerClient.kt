/*
 * Copyright 2014 Transmode AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.transmode.gradle.plugins.docker.client

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import se.transmode.gradle.plugins.docker.executeAndWait

internal class NativeDockerClient(
    private val binary: String,
    private val project: Project
) : DockerClient {
    override fun buildImage(buildDir: File, tag: String) {
        project.logger.info("Docker build $tag")
        project.executeAndWait(
            listOf(
                binary,
                "build",
                "-t",
                tag,
                buildDir.toString(),
            )
        )
    }

    override fun pushImage(tag: String) {
        project.logger.info("Docker push $tag")
        project.executeAndWait(
            listOf(
                binary,
                "push",
                tag,
            )
        )
    }
}
