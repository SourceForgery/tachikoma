/**
 * Copyright 2014 Transmode AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.transmode.gradle.plugins.docker

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.get

abstract class DockerPluginExtension {
    abstract val baseImage: Property<String>
    abstract val registry: Property<String>

    // path to the docker binary
    abstract val dockerBinary: Property<String>

    // use docker REST api (with docker-java)
    abstract val useApi: Property<Boolean>

    // docker host url & credentials
    abstract val hostUrl: Property<String>
    abstract val apiUsername: Property<String>
    abstract val apiEmail: Property<String>
    abstract val apiPassword: Property<String>

    init {
        with(this) {
            dockerBinary.convention(DOCKER_BINARY)
            useApi.convention(false)
            baseImage.convention(DEFAULT_IMAGE)
        }
    }

    companion object {
        private const val DOCKER_BINARY = "docker"
        const val DEFAULT_IMAGE: String = "ubuntu"
    }
}