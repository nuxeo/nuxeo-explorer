/*
 * (C) Copyright 2020 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.export.api.Exporter;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.PersistSnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 20.0.0
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeSnaphotFeature.class })
public class TestGraphExport extends AbstractApidocTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    @Before
    public void initMocks() throws PackageException, IOException {
        mockPackageServices();
    }

    @Test
    public void testGetExporters() throws Exception {
        assertNull(snapshotManager.getExporter("foo"));
        List<Exporter> exporters = snapshotManager.getExporters();
        assertEquals(2, exporters.size());
        assertEquals("jsonGraph", exporters.get(0).getName());
        assertEquals("dotGraph", exporters.get(1).getName());
    }

    @Test
    public void testGetExporter() throws Exception {
        assertNull(snapshotManager.getExporter("foo"));

        Exporter exporter = snapshotManager.getExporter("jsonGraph");
        assertNotNull(exporter);
        assertEquals("jsonGraph", exporter.getName());
        assertEquals("Json Graph", exporter.getTitle());
        assertEquals("Json dependency graph", exporter.getDescription());
        assertEquals("graph.json", exporter.getFilename());
        assertEquals("application/json", exporter.getMimetype());
        assertTrue(exporter.getProperties().isEmpty());

        exporter = snapshotManager.getExporter("dotGraph");
        assertNotNull(exporter);
        assertEquals("dotGraph", exporter.getName());
        assertEquals("DOT Graph", exporter.getTitle());
        assertEquals("Dependency graph exported in DOT format", exporter.getDescription());
        assertEquals("graph.dot", exporter.getFilename());
        assertEquals("application/octet-stream", exporter.getMimetype());
        assertTrue(exporter.getProperties().isEmpty());
    }

    protected void checkDefaultExports(DistributionSnapshot snapshot) throws Exception {
        assertNotNull(snapshot);

        PersistSnapshotFilter filter = new PersistSnapshotFilter("apidoc");
        filter.addNuxeoPackage(MOCK_PACKAGE_NAME);

        Exporter exporter = snapshotManager.getExporter("jsonGraph");
        try (ByteArrayOutputStream sinkJson = new ByteArrayOutputStream()) {
            exporter.export(sinkJson, snapshot, filter, Collections.singletonMap("pretty", "true"));
            checkJsonContentEquals("export/graphs/basic_graph.json", sinkJson.toString());
        }

        exporter = snapshotManager.getExporter("dotGraph");
        try (ByteArrayOutputStream sinkDot = new ByteArrayOutputStream()) {
            exporter.export(sinkDot, snapshot, filter, null);
            checkJsonContentEquals("export/graphs/jgrapht.dot", sinkDot.toString());
        }
    }

    @Test
    public void testExportLive() throws Exception {
        checkDefaultExports(snapshotManager.getRuntimeSnapshot());
    }

    @Test
    public void testExportPersisted() throws Exception {
        checkDefaultExports(snapshotManager.persistRuntimeSnapshot(session));
    }

}
