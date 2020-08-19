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
package org.nuxeo.apidoc.export.graphs.introspection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.apidoc.export.graphs.api.Node;
import org.nuxeo.apidoc.export.graphs.api.NodeFilter;

/**
 * Node filter based on node type(s).
 *
 * @since 20.0.0
 */
public class NodeTypeFilter implements NodeFilter {

    protected final List<String> types = new ArrayList<>();

    public NodeTypeFilter(List<String> types) {
        super();
        if (types != null) {
            this.types.addAll(types);
        }
    }

    public NodeTypeFilter(String... types) {
        this(Arrays.asList(types));
    }

    @Override
    public boolean accept(Node<?> node) {
        if (node == null) {
            return false;
        }
        if (types.contains(node.getType())) {
            return true;
        }
        return false;
    }

}
