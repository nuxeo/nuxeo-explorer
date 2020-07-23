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
package org.nuxeo.apidoc.export.graphs.plugins;

import java.util.Map;

import org.nuxeo.apidoc.export.api.Export;
import org.nuxeo.apidoc.export.api.ExporterDescriptor;
import org.nuxeo.apidoc.export.graphs.api.GraphExport;
import org.nuxeo.apidoc.export.graphs.api.GraphType;
import org.nuxeo.apidoc.export.graphs.introspection.AbstractGraphExporter;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Basic Graph export using DOT format.
 *
 * @since 20.0.0
 */
public class DOTGraphExporter extends AbstractGraphExporter {

    public DOTGraphExporter(ExporterDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public Export getExport(DistributionSnapshot distribution, SnapshotFilter filter, Map<String, String> properties) {
        GraphExport baseGraph = getDefaultGraph(distribution, filter, properties);
        try {
            DOTContentGraphExport export = baseGraph.copy(DOTContentGraphExport.class, null);
            export.update(getName(), "DOT Graph", "Complete Graph exported in DOT format", GraphType.BASIC.name());
            return export;
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

}
