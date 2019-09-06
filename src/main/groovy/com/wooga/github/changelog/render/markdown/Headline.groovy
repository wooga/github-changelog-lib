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

package com.wooga.github.changelog.render.markdown

class Headline {
    String text
    int level
    HeadlineType type

    Headline(String text, int level) {
        this(text, level, HeadlineType.atx)
    }

    Headline(String text, int level, HeadlineType type) {
        this.text = text
        this.level = level
        this.type = type
    }

    @Override
    String toString() {
        def value
        switch(type) {
            case HeadlineType.setext:
                value = renderSetextStyle(text, level)
                break
            case HeadlineType.atx:
                value = renderATXStyle(text, level)
                break
        }
        value
    }

    static renderSetextStyle(String text, Integer level) {
        if(level <= 2) {
            def length = text.length()
            def character = (level == 1) ? "=" : "-"
            return "${text}\n${"".padLeft(length,character)}\n\n"

        } else {
            return renderATXStyle(text, level)
        }
    }

    static renderATXStyle(String text, Integer level) {
        "${"".padLeft(Math.max(1, Math.min(6, level)),"#")} ${text}\n\n"
    }
}

