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
import spock.lang.Subject
import spock.lang.Unroll


class LinkSpec extends Specification {

    @Subject
    Link testLink

    @Unroll
    def ":inlineLink renders inline link #message optional title"() {
        given: "a link with 3 components"
        testLink = new Link(text, url, title)

        expect:
        if (title) {
            testLink.inlineLink() == "[${text}](${url} \"${title}\")"
        } else {
            testLink.inlineLink() == "[${text}](${url}"
        }

        where:
        text         | url                       | title
        "klick here" | new URL("http://test.sh") | "test domain"
        "klick here" | new URL("http://test.sh") | null
        message = title ? "with" : "without"
    }

    @Unroll
    def ":referenceLink renders reference link"() {
        given: "a link with 3 components"
        testLink = new Link(text, url, title)

        expect:
        testLink.referenceLink() == "[${text}]"

        where:
        text         | url              | title
        "klick here" | "http://test.sh" | "test domain"
    }

    @Unroll
    def ":reference renders a link reference declaration #message optional title"() {
        given: "a link with 3 components"
        testLink = new Link(text, url, title)

        expect:
        if (title) {
            testLink.reference() == "[${text}]: ${url} \"${title}\""
        } else {
            testLink.reference() == "[${text}]: ${url}"
        }

        where:
        text         | url              | title
        "klick here" | "http://test.sh" | "test domain"
        "klick here" | "http://test.sh" | null
        message = title ? "with" : "without"
    }

    @Unroll
    def ":link renders inline links  #message optional title when passed #inline"() {
        given: "a link with 3 components"
        testLink = new Link(text, url, title)

        expect:
        if (inline) {
            if (title) {
                testLink.link(inline) == "[${text}](${url} \"${title}\")"
            } else {
                testLink.link(inline) == "[${text}](${url}"
            }
        } else {
            testLink.link(inline) == "[${text}]"
        }


        where:
        text         | url              | title         | inline
        "klick here" | "http://test.sh" | "test domain" | true
        "klick here" | "http://test.sh" | null          | true
        "klick here" | "http://test.sh" | "test domain" | false
        "klick here" | "http://test.sh" | "test domain" | false
        message = title ? "with" : "without"

    }
}
