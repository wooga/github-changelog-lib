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

package com.wooga.github.changelog.changeSet

class LabelChangeSet<B extends ChangeSet, L> implements ChangeSet, Labels<L>, Compound<B> {
    final List<L> labels

    @Delegate
    final B inner

    LabelChangeSet(B base, List<L> labels) {
        this.inner = base
        this.labels = labels
    }

    @Override
    void setLabels(Iterable<L> labels) {
        this.labels.clear()
        this.labels.addAll(labels)
    }

    @Override
    void mutate(Closure mutator) {
        this.with mutator
    }


    @Override
    <M> M map(Closure<M> map) {
        map.delegate = this
        map.resolveStrategy = Closure.DELEGATE_ONLY
        map.call(this)
    }
}