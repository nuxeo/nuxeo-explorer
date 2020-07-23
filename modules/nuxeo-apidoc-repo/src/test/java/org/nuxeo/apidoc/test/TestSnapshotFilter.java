/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.PersistSnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.ecm.core.api.CoreSession;

public class TestSnapshotFilter extends AbstractApidocTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    @Before
    public void init() throws PackageException, IOException {
        mockPackageServices();
    }

    protected void checkApiDoc(DistributionSnapshot snapshot) {
        assertEquals(List.of("org.nuxeo.apidoc.core", "org.nuxeo.apidoc.repo"), snapshot.getBundleIds());
        assertEquals(List.of("org.nuxeo.apidoc.adapterContrib", "org.nuxeo.apidoc.doctypeContrib",
                "org.nuxeo.apidoc.lifecycle.contrib", "org.nuxeo.apidoc.listener.contrib",
                "org.nuxeo.apidoc.schemaContrib", "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                "org.nuxeo.apidoc.test.works"), snapshot.getComponentIds());
        assertEquals(List.of("org.nuxeo.apidoc.search.ArtifactSearcher", "org.nuxeo.apidoc.snapshot.SnapshotManager"),
                snapshot.getServiceIds());
        assertEquals(List.of("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins"),
                snapshot.getExtensionPointIds());
        assertEquals(
                List.of("org.nuxeo.apidoc.adapterContrib--adapters", "org.nuxeo.apidoc.doctypeContrib--doctype",
                        "org.nuxeo.apidoc.lifecycle.contrib--types", "org.nuxeo.apidoc.listener.contrib--listener",
                        "org.nuxeo.apidoc.schemaContrib--schema",
                        "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration",
                        "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration1",
                        "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration2",
                        "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration3",
                        "org.nuxeo.apidoc.test.works--queues", "org.nuxeo.apidoc.test.works--queues1"),
                snapshot.getContributionIds());
        assertEquals(0, snapshot.getOperations().size());
        assertEquals(List.of(MOCK_PACKAGE_ID),
                snapshot.getPackages().stream().map(PackageInfo::getId).collect(Collectors.toList()));
    }

    @Test
    public void testFilterBundlePrefix() throws IOException {
        PersistSnapshotFilter filter = new PersistSnapshotFilter("apidoc-bundle");
        filter.addBundlePrefix("org.nuxeo.apidoc");

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        assertNotNull(snapshot);

        assertEquals(List.of("apidoc-bundle"),
                snapshot.getBundleGroups().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        checkApiDoc(snapshot);
    }

    @Test
    public void testFilterNuxeoPackagePrefix() throws IOException {
        PersistSnapshotFilter filter = new PersistSnapshotFilter("apidoc-pack");
        filter.addNuxeoPackagePrefix(MOCK_PACKAGE_NAME);

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        assertNotNull(snapshot);

        assertEquals(List.of("apidoc-pack"),
                snapshot.getBundleGroups().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        checkApiDoc(snapshot);
    }

    @Test
    public void testFilterJavaPackagePrefix() throws IOException {
        PersistSnapshotFilter filter = new PersistSnapshotFilter("apidoc-java");
        filter.addPackagesPrefix("org.nuxeo.ecm.automation.core.operations.services.query");
        filter.addPackagesPrefix("org.nuxeo.ecm.automation.core.operations.services.workmanager");

        DistributionSnapshot snapshot = snapshotManager.persistRuntimeSnapshot(session, "apidoc", null, filter);
        assertNotNull(snapshot);

        assertEquals(List.of("apidoc-java"),
                snapshot.getBundleGroups().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        assertEquals(0, snapshot.getBundleIds().size());
        assertEquals(0, snapshot.getComponentIds().size());
        assertEquals(0, snapshot.getServiceIds().size());
        assertEquals(0, snapshot.getExtensionPointIds().size());
        assertEquals(0, snapshot.getContributionIds().size());
        assertEquals(List.of("Repository.Query", "Repository.ResultSetQuery", "WorkManager.RunWorkInFailure"),
                snapshot.getOperations().stream().map(OperationInfo::getName).collect(Collectors.toList()));
        assertEquals(0, snapshot.getPackages().size());
    }

}
