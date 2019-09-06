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

class Link {
    final String text
    final String url
    final String title

    Link(String text, String url, String title) {
        this.text = text
        this.url = url
        this.title = title
    }

    Link(String text, URL url, String title) {
        this(text, url.toString(), title)
    }

    Link(String text, URL url) {
        this(text, url.toString())
    }

    Link(String text, String url) {
        this(text,url, null)
    }

    String link(boolean inline = true) {
        if(inline) {
            return this.inlineLink()
        }

        this.referenceLink()
    }

    String referenceLink() {
        "[${this.text}]"
    }

    String inlineLink() {
        if(this.title) {
            "[${this.text}](${this.url} \"${this.title}\")"
        }
        "[${this.text}](${this.url})"
    }

    String reference() {
        if(this.title) {
            "[${this.text}]: ${this.url} \"${this.title}\""
        }
        "[${this.text}]: ${this.url}"
    }

    @Override
    int hashCode() {
        return "${this.text.hashCode()}-${this.url.hashCode()}".hashCode()
    }

    @Override
    boolean equals(Object obj) {
        if(!obj) {
            return false
        }

        if(Link.isInstance(obj)) {
            Link other = (Link) obj
            if(other.text == this.text && other.url == this.url) {
                return true
            }
        }
        return false
    }
}