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
package se.transmode.gradle.plugins.docker.image

import java.io.File

internal class Dockerfile(var instructions: MutableList<String> = mutableListOf()) {
    fun append(instruction: String): Dockerfile {
        instructions.add(instruction.toString())
        return this
    }

    fun appendAll(instructions: List<String>): Dockerfile {
        this.instructions.addAll(instructions).toString()
        return this
    }

    fun writeToFile(destination: File) {
        destination.writeText(
            instructions.joinToString("\n")
        )
    }

    companion object {
        fun fromExternalFile(source: File): Dockerfile {
            val dockerfile = Dockerfile()
            source.forEachLine {
                dockerfile.append(it)
            }
            return dockerfile
        }

        fun fromBaseImage(base: String): Dockerfile {
            return Dockerfile(mutableListOf("FROM $base"))
        }
    }
}
