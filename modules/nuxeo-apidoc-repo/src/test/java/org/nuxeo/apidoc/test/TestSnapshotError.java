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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Deploys components in error to check their explorer snapshot.
 *
 * @since 22.0.0
 */
public class TestSnapshotError extends AbstractApidocTest {

    @Inject
    protected SnapshotManager snapshotManager;

    protected void checkStartupMessages(Level level, String... messages) {
        assertEquals(List.of(messages), Framework.getRuntime().getMessageHandler().getMessages(level));
    }

    @Test
    @Deploy("org.nuxeo.apidoc.repo:invalid/invalid-operation.xml")
    @Deploy("org.nuxeo.apidoc.repo:invalid/invalid-operation-notfound.xml")
    @Deploy("org.nuxeo.apidoc.repo:invalid/invalid-chain.xml")
    public void testInvalidOperation() {
        DistributionSnapshot snapshot = snapshotManager.getRuntimeSnapshot();
        assertNotNull(snapshot);
        // crash on operations retrieval should be prevented by NXP-29499 now
        assertNotNull(snapshot.getOperations());
        assertTrue(snapshot.getOperations().size() > 0);
        checkStartupMessages(Level.ERROR,
                "Failed to register extension to: service:org.nuxeo.ecm.core.operation.OperationServiceComponent, "
                        + "xpoint: operations in component: service:org.nuxeo.apidoc.test.faultyOperationClass "
                        + "(java.lang.IllegalArgumentException: Invalid operation class: class org.nuxeo.ecm.automation.core.AutomationComponent. "
                        + "No @Operation annotation found on class.)",
                "Failed to register extension to: service:org.nuxeo.ecm.core.operation.OperationServiceComponent, "
                        + "xpoint: operations in component: service:org.nuxeo.apidoc.test.faultyOperationContrib "
                        + "(java.lang.IllegalArgumentException: Invalid operation class 'org.nuxeo.ecm.automation.test.helpers.NonExistingOperation': "
                        + "class not found.)",
                "Operation chain with id 'testchain' references unknown operation with id 'NonExistingOperation'");
    }

}
