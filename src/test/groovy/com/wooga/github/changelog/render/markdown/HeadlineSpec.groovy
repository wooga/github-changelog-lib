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

import spock.lang.Specification
import spock.lang.Unroll

class HeadlineSpec extends Specification {

    @Unroll
    def ":toString renders headline based on provided style"() {
        given:
        def headline = new Headline(text, level, style)

        expect:
        headline.toString() == expectedHeadline

        where:
        text       | level | style               | expectedHeadline
        "Headline" | 1     | HeadlineType.setext | "Headline\n========\n\n"
        "Headline" | 2     | HeadlineType.setext | "Headline\n--------\n\n"
        "Headline" | 3     | HeadlineType.setext | "### Headline\n\n"
        "Headline" | 4     | HeadlineType.setext | "#### Headline\n\n"
        "Headline" | 5     | HeadlineType.setext | "##### Headline\n\n"
        "Headline" | 6     | HeadlineType.setext | "###### Headline\n\n"
        "Headline" | 7     | HeadlineType.setext | "###### Headline\n\n"
        "Headline" | 8     | HeadlineType.setext | "###### Headline\n\n"
        "Headline" | 90    | HeadlineType.setext | "###### Headline\n\n"
        "Headline" | 1     | HeadlineType.atx    | "# Headline\n\n"
        "Headline" | 2     | HeadlineType.atx    | "## Headline\n\n"
        "Headline" | 3     | HeadlineType.atx    | "### Headline\n\n"
        "Headline" | 4     | HeadlineType.atx    | "#### Headline\n\n"
        "Headline" | 5     | HeadlineType.atx    | "##### Headline\n\n"
        "Headline" | 6     | HeadlineType.atx    | "###### Headline\n\n"
        "Headline" | 7     | HeadlineType.atx    | "###### Headline\n\n"
        "Headline" | 8     | HeadlineType.atx    | "###### Headline\n\n"
        "Headline" | 90    | HeadlineType.atx    | "###### Headline\n\n"
    }


    def ":renderSetextStyle renders given headline setext style if possible"() {
        expect:
        Headline.renderSetextStyle(text, level) == expectedHeadline

        where:
        text       | level | expectedHeadline
        "Headline" | 1     | "Headline\n========\n\n"
        "Headline" | 2     | "Headline\n--------\n\n"
        "Headline" | 3     | "### Headline\n\n"
        "Headline" | 4     | "#### Headline\n\n"
        "Headline" | 5     | "##### Headline\n\n"
        "Headline" | 6     | "###### Headline\n\n"
        "Headline" | 7     | "###### Headline\n\n"
        "Headline" | 8     | "###### Headline\n\n"
        "Headline" | 90    | "###### Headline\n\n"
    }

    def ":renderATXStyle renders given headline atx style"() {
        expect:
        Headline.renderATXStyle(text, level) == expectedHeadline

        where:
        text       | level | expectedHeadline
        "Headline" | 1     | "# Headline\n\n"
        "Headline" | 2     | "## Headline\n\n"
        "Headline" | 3     | "### Headline\n\n"
        "Headline" | 4     | "#### Headline\n\n"
        "Headline" | 5     | "##### Headline\n\n"
        "Headline" | 6     | "###### Headline\n\n"
        "Headline" | 7     | "###### Headline\n\n"
        "Headline" | 8     | "###### Headline\n\n"
        "Headline" | 90    | "###### Headline\n\n"
    }
}
