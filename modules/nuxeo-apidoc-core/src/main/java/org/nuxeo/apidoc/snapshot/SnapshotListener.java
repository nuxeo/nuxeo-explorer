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
package org.nuxeo.apidoc.snapshot;

import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.runtime.ComponentEvent;
import org.nuxeo.runtime.ComponentListener;
import org.nuxeo.runtime.model.ComponentManager.Listener;

/**
 * Listens for component events and stores information about it at runtime startup.
 * <p>
 * This allows to detect resolution/registration orders on component contributions to be contributed to the live
 * snapshot {@link RuntimeSnapshot} information.
 *
 * @since 20.0.0
 */
public class SnapshotListener implements ComponentListener, Listener {

    @Override
    public void handleEvent(ComponentEvent event) {
        // TODO
    }

}
