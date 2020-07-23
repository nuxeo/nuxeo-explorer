/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.export.graphs.api;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 20.0.0
 */
public enum EdgeType {

    UNDEFINED(-1, false), CONTAINS(1, true), REQUIRES(2, true), SOFT_REQUIRES(50, false), REFERENCES(100, false);

    private int index;

    private boolean directed;

    private EdgeType(int index, boolean directed) {
        this.index = index;
    }

    @Override
    public String toString() {
        return name();
    }

    public int getIndex() {
        return index;
    }

    public boolean isDirected() {
        return directed;
    }

    public String getLabel() {
        return StringUtils.capitalize(name().toLowerCase(Locale.ENGLISH));
    }

    public static EdgeType getType(String type) {
        for (EdgeType etype : EdgeType.values()) {
            if (etype.name().equalsIgnoreCase(type)) {
                return etype;
            }
        }
        return UNDEFINED;
    }

}
