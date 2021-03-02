/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

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
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 22.0.0
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
@Deploy("org.nuxeo.apidoc.repo:apidoc-automation-test-contrib.xml")
public class TestStatExport extends AbstractApidocTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    @Before
    public void initMocks() throws PackageException, IOException {
        mockPackageServices();
    }

    @Test
    public void testGetExporter() {
        Exporter exporter = snapshotManager.getExporter("jsonContributionStats");
        assertNotNull(exporter);
        assertEquals("jsonContributionStats", exporter.getName());
        assertEquals("Json Contribution Stats", exporter.getTitle());
        assertEquals("Json statistics for contributions", exporter.getDescription());
        assertEquals("contribution_stats.json", exporter.getFilename());
        assertEquals("application/json", exporter.getMimetype());
        assertFalse(exporter.getProperties().isEmpty());
    }

    protected void checkExport(DistributionSnapshot snapshot) throws IOException {
        assertNotNull(snapshot);

        PersistSnapshotFilter filter = new PersistSnapshotFilter("apidoc");
        filter.addNuxeoPackage(MOCK_PACKAGE_NAME);

        Exporter exporter = snapshotManager.getExporter("jsonContributionStats");
        try (ByteArrayOutputStream sinkJson = new ByteArrayOutputStream()) {
            exporter.export(sinkJson, snapshot, filter, Map.of("pretty", "true"));
            checkJsonContentEquals("export/contribution_stats.json", sinkJson.toString());
        }
    }

    @Test
    public void testExportLive() throws IOException {
        checkExport(snapshotManager.getRuntimeSnapshot());
    }

    @Test
    public void testExportPersisted() throws IOException {
        checkExport(snapshotManager.persistRuntimeSnapshot(session));
    }

}
