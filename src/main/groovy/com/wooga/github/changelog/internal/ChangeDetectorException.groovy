/*
 * Copyright 2019 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.wooga.github.changelog.internal

import groovy.transform.InheritConstructors
import org.kohsuke.github.GHCommit
import org.kohsuke.github.GHTag
import org.kohsuke.github.GHTagObject

@InheritConstructors
class ChangeDetectorException extends Exception {
}

class CommitNotReachableException extends ChangeDetectorException {
    final GHCommit commit

    CommitNotReachableException(GHCommit commit, String message) {
        super(message)
        this.commit = commit
    }
}

class TagNotReachableException extends ChangeDetectorException {
    final GHTag tag

    TagNotReachableException(GHTag tag, String message, Throwable cause) {
        super(message, cause)
        this.tag = tag
    }
}

