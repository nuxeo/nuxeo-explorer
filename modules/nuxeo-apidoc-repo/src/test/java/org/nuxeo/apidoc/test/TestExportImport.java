/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.repository.SnapshotPersister;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestExportImport {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    protected File tempExportFile;

    @Before
    public void saveRuntimeSnapshotForSampleExport() throws IOException {
        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session);
        Map<String, DistributionSnapshot> snapshots = snapshotManager.getPersistentSnapshots(session);
        assertNotNull(snapshots);
        assertEquals(1, snapshots.size());
        checkSnapshot(snapshots.values().iterator().next(), "Nuxeo", "unknown", "Nuxeo-unknown", Arrays.asList(),
                false);

        tempExportFile = Framework.createTempFile("testExportImport", snapshot.getKey());
        try (OutputStream out = new FileOutputStream(tempExportFile)) {
            snapshotManager.exportSnapshot(session, snapshot.getKey(), out);
        }
    }

    @After
    public void cleanup() {
        if (tempExportFile != null) {
            tempExportFile.delete();
        }
        // cleanup all persisted distribs
        session.removeDocument(new PathRef(SnapshotPersister.Root_PATH + SnapshotPersister.Root_NAME));
    }

    protected Map<String, Serializable> getDistribProps() {
        Map<String, Serializable> res = new HashMap<>();
        res.put(DistributionSnapshot.PROP_NAME, "server");
        res.put(DistributionSnapshot.PROP_VERSION, "42.66");
        res.put(DistributionSnapshot.PROP_KEY, "server-42.66");
        res.put(DistributionSnapshot.PROP_ALIASES, "latest\n11.x");
        return res;
    }

    protected void checkSnapshot(DistributionSnapshot snapshot, String name, String version, String key,
            List<String> aliases, boolean hidden) {
        assertNotNull(snapshot);
        assertEquals(name, snapshot.getName());
        assertEquals(version, snapshot.getVersion());
        assertEquals(key, snapshot.getKey());
        assertEquals(aliases, snapshot.getAliases());
        assertEquals(hidden, snapshot.isHidden());
    }

    protected void checkFinalSnapshots() {
        Map<String, DistributionSnapshot> snapshots = snapshotManager.getPersistentSnapshots(session);
        assertNotNull(snapshots);
        assertEquals(4, snapshots.size());
        assertTrue(snapshots.containsKey("Nuxeo-unknown"));
        checkSnapshot(snapshots.get("Nuxeo-unknown"), "Nuxeo", "unknown", "Nuxeo-unknown", Arrays.asList(), false);
        for (String distrib : Arrays.asList("server-42.66", "11.x", "latest")) {
            assertTrue(snapshots.containsKey(distrib));
            checkSnapshot(snapshots.get(distrib), "server", "42.66", "server-42.66", Arrays.asList("latest", "11.x"),
                    false);
        }
    }

    @Test
    public void testExportImportTmp() throws IOException {
        String distribDocId = null;
        try (InputStream in = new FileInputStream(tempExportFile)) {
            DocumentModel doc = snapshotManager.importTmpSnapshot(session, in);
            assertNotNull(doc);
            distribDocId = doc.getId();
            assertNotNull(distribDocId);
        }
        Map<String, DistributionSnapshot> snapshots = snapshotManager.getPersistentSnapshots(session);
        assertEquals(1, snapshots.size());
        assertTrue(snapshots.containsKey("Nuxeo-unknown"));
        checkSnapshot(snapshots.get("Nuxeo-unknown"), "Nuxeo", "unknown", "Nuxeo-unknown", Arrays.asList(), true);

        // check validation on it, updating some properties
        Map<String, Serializable> updateProps = new HashMap<>(getDistribProps());
        updateProps.put(DistributionSnapshot.PROP_KEY, SnapshotManager.DISTRIBUTION_ALIAS_CURRENT);
        try {
            snapshotManager.validateImportedSnapshot(session, distribDocId, updateProps,
                    Arrays.asList(SnapshotManager.DISTRIBUTION_ALIAS_CURRENT));
            fail("Should have raised a DocumentValidationException");
        } catch (IllegalArgumentException e) {
            assertEquals("Distribution key or alias is reserved: 'current'", e.getMessage());
        }

        // try again with original test props
        snapshotManager.validateImportedSnapshot(session, distribDocId, getDistribProps(), null);

        checkFinalSnapshots();
    }

    @Test
    public void testExportImport() throws IOException {
        try (InputStream in = new FileInputStream(tempExportFile)) {
            snapshotManager.importSnapshot(session, in, getDistribProps(), null);
        }
        checkFinalSnapshots();
    }

    @Test
    public void testExportImportMultiple() throws IOException {
        String key = "server-42.66";
        assertTrue(snapshotManager.getPersistentSnapshots(session, key, false).isEmpty());
        try (InputStream in = new FileInputStream(tempExportFile)) {
            snapshotManager.importSnapshot(session, in, getDistribProps(), null);
        }
        assertEquals(1, snapshotManager.getPersistentSnapshots(session, key, false).size());
        // duplicate same import
        try (InputStream in = new FileInputStream(tempExportFile)) {
            snapshotManager.importSnapshot(session, in, getDistribProps(), null);
        }
        // check there is one more
        assertEquals(2, snapshotManager.getPersistentSnapshots(session, key, false).size());
    }

}
