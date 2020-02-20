/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/**
 * Sourceforgery elects to include this software in this distribution under the CDDL license.
 *
 * Modifications:
 * Repackaged original source
 * Removed dependency on javax.ws.rs interfaces
 * Converted to Kotlin
 */

package com.sourceforgery.jersey.uribuilder

import java.util.NoSuchElementException

/**
 * Iterator which iterates through the input string and returns characters from that string.
 *
 * @author Miroslav Fuksa
 */
internal class CharacterIterator(
    /**
     * Returns the input String on which this [iterator][CharacterIterator] operates.
     *
     * @return String which initialized this iterator.
     */
    val input: String
) {
    private var pos: Int = 0

    init {
        this.pos = -1
    }

    /**
     * Determines whether there is next character in the iteration chain.
     *
     * @return True if there is a character which can be retrieved by [.next], false otherwise.
     */
    operator fun hasNext(): Boolean {
        return pos < input.length - 1
    }

    /**
     * Returns next character in the iteration chain and increase the current position.
     *
     * @return Next character.
     * @throws RuntimeException The method might throw exception when there is no more character to be retrieved.
     */
    operator fun next(): Char {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        return input[++pos]
    }

    /**
     * Returns the next character without increasing the position. The method does the same as [.next] but
     * the position is not changed by calling this method.
     *
     * @return Next character.
     */
    fun peek(): Char {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        return input[pos + 1]
    }

    /**
     * Returns the current internal position of the iterator.
     *
     * @return current position of the iterator
     */
    fun pos(): Int {
        return pos
    }

    /**
     * Changes the current position to the position.
     *
     * @param newPosition New position for the iterator.
     */
    fun setPosition(newPosition: Int) {
        if (newPosition > this.input.length - 1) {
            throw IndexOutOfBoundsException("Given position $newPosition is outside the input string range.")
        }
        this.pos = newPosition
    }

    /**
     * Returns character at the current position.
     *
     * @return Character from current position.
     */
    fun current(): Char {
        if (pos == -1) {
            throw IllegalStateException("Iterator not used yet.")
        }
        return input[pos]
    }
}