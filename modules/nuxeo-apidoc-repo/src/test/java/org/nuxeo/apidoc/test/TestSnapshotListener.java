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
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.snapshot.SnapshotListener;
import org.nuxeo.runtime.model.ComponentStartOrders;

/**
 * @since 20.0.0
 */
public class TestSnapshotListener extends AbstractApidocTest {

    protected static final String SNAPSHOT_COMP = "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent";

    @Inject
    protected SnapshotListener listener;

    @Test
    public void testService() {
        assertNotNull(listener);
        // not listening anymore after runtime startup
        assertFalse(listener.isListening());
    }

    protected void check(int expected, Long actual) {
        assertEquals(Long.valueOf(expected), actual);
    }

    @Test
    public void testStartOrders() {
        check(208, listener.getStartTotal());
        check(142, listener.getStartOrder(SNAPSHOT_COMP));
        assertNull(listener.getDeclaredStartOrder(SNAPSHOT_COMP));
        // check XML comp
        assertNull(listener.getDeclaredStartOrder("org.nuxeo.apidoc.lifecycle.contrib"));
        // check Java comps with a declared order
        check(ComponentStartOrders.REPOSITORY,
                listener.getDeclaredStartOrder("org.nuxeo.ecm.core.repository.RepositoryServiceComponent"));
        check(-1000, listener.getDeclaredStartOrder("org.nuxeo.runtime.datasource"));
        check(-500, listener.getDeclaredStartOrder("org.nuxeo.runtime.kv.KeyValueService"));
    }

    @Test
    public void testExtensionPointOrders() {
        // single contributions use case
        check(2, listener.getExtensionPointTotal(
                ExtensionPointInfo.computeId("org.nuxeo.ecm.core.lifecycle.LifeCycleService", "types")));
        check(0, listener.getExtensionRegistrationOrder(
                ExtensionInfo.computeId("org.nuxeo.ecm.core.LifecycleCoreExtensions", "types")));
        check(1, listener.getExtensionRegistrationOrder(
                ExtensionInfo.computeId("org.nuxeo.apidoc.lifecycle.contrib", "types")));
        // multiple contributions use case
        check(27, listener.getExtensionPointTotal(
                ExtensionPointInfo.computeId("org.nuxeo.runtime.ConfigurationService", "configuration")));
        check(9, listener.getExtensionRegistrationOrder(ExtensionInfo.computeId(SNAPSHOT_COMP, "configuration", 0)));
        check(10, listener.getExtensionRegistrationOrder(ExtensionInfo.computeId(SNAPSHOT_COMP, "configuration", 1)));
        // self registration
        check(1, listener.getExtensionPointTotal(ExtensionPointInfo.computeId(SNAPSHOT_COMP, "exporters")));
        check(0, listener.getExtensionRegistrationOrder(ExtensionInfo.computeId(SNAPSHOT_COMP, "exporters")));
        // pending registration (non-regression test for NXP-29641)
        check(1, listener.getExtensionRegistrationOrder(
                ExtensionInfo.computeId("org.nuxeo.apidoc.adapterContrib", "adapters")));
    }

}
