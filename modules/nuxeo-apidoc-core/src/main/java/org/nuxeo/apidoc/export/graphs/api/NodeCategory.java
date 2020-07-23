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

import org.nuxeo.apidoc.api.BundleInfo;

/**
 * @since 20.0.0
 */
public enum NodeCategory {

    RUNTIME, CORE, PLATFORM, STUDIO;

    @Override
    public String toString() {
        return name();
    }

    public static NodeCategory guessCategory(BundleInfo bundle) {
        NodeCategory cat = guessCategory(bundle.getGroupId(), null);
        if (cat == null) {
            cat = guessCategory(bundle.getArtifactId(), null);
        }
        if (cat == null) {
            cat = guessCategory(bundle.getId(), PLATFORM);
        }
        return cat;
    }

    public static NodeCategory guess(String id) {
        return guessCategory(id, PLATFORM);
    }

    protected static NodeCategory guessCategory(String id, NodeCategory defaultValue) {
        NodeCategory cat = introspect(id);
        if (cat == null) {
            cat = defaultValue;
        }
        return cat;
    }

    protected static NodeCategory introspect(String source) {
        for (NodeCategory item : NodeCategory.values()) {
            if (contains(source, item.name())) {
                return item;
            }
        }
        return null;
    }

    protected static boolean contains(String source, String content) {
        if (source == null) {
            return false;
        }
        return source.toLowerCase(Locale.ENGLISH).contains(content.toLowerCase(Locale.ENGLISH));
    }

    public static NodeCategory getCategory(String cat, NodeCategory defaultValue) {
        for (NodeCategory ecat : NodeCategory.values()) {
            if (ecat.name().equalsIgnoreCase(cat)) {
                return ecat;
            }
        }
        return defaultValue;
    }

}
