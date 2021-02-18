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

import static org.nuxeo.runtime.ComponentEvent.COMPONENT_STARTED;
import static org.nuxeo.runtime.ComponentEvent.EXTENSION_REGISTERED;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.runtime.ComponentEvent;
import org.nuxeo.runtime.ComponentListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentManager.Listener;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.Extension;

/**
 * Listens for component events and stores information about it at runtime startup.
 * <p>
 * This allows to detect resolution/registration orders on component contributions to be contributed to the live
 * snapshot {@link RuntimeSnapshot} information.
 * <p>
 * Events will stop being handled after the framework has started.
 *
 * @since 20.0.0
 */
public class SnapshotListener implements ComponentListener, Listener {

    // records framework startup to avoid handling later hot-reload events
    protected boolean listening = true;

    protected long startCounter = -1;

    // component name -> start order
    protected final Map<String, Long> starts = new HashMap<>();

    // component name -> declared start order
    protected final Map<String, Long> declaredStarts = new HashMap<>();

    // component extension point -> integer (to handle multiple contributions to the same extension point)
    protected final Map<String, AtomicLong> compLocalCounters = new HashMap<>();

    // extension point id -> extension registration counter
    protected final Map<String, AtomicLong> xpCounters = new HashMap<>();

    // extension id -> extension registration counter
    protected final Map<String, Long> registrations = new HashMap<>();

    @Override
    public void handleEvent(ComponentEvent event) {
        if (!listening || Framework.isBooleanPropertyTrue(SnapshotManagerComponent.PROPERTY_SITE_MODE)) {
            // no information needed for live runtime snapshot in this case
            return;
        }

        ComponentInstance component = event.registrationInfo.getComponent();
        if (component == null) {
            // should not happen for events below
            return;
        }
        String name = component.getName().getName();
        if (event.id == COMPONENT_STARTED) {
            starts.put(name, ++startCounter);
            int declaredOrder = event.registrationInfo.getApplicationStartedOrder();
            if (declaredOrder != ComponentStartOrders.DEFAULT && declaredOrder != 0) {
                // default sort order: for components that don't care
                // zero sort order: for components without implementation
                declaredStarts.put(name, Long.valueOf(declaredOrder));
            }
        } else if (event.id == EXTENSION_REGISTERED) {
            Object data = event.data;
            if (data instanceof Extension) {
                Extension xt = (Extension) data;
                String point = xt.getExtensionPoint();
                String xpTargetId = ExtensionPointInfo.computeId(xt.getTargetComponent().getName(), point);
                long counter = xpCounters.computeIfAbsent(xpTargetId, k -> new AtomicLong(-1)).incrementAndGet();
                // handle multiple contributions to the same extension point
                String xpInfoId = ExtensionInfo.computeId(name, point);
                long index = compLocalCounters.computeIfAbsent(xpInfoId, k -> new AtomicLong(-1)).incrementAndGet();
                registrations.put(ExtensionInfo.computeId(name, point, index), counter);
            }
        }
    }

    @Override
    public void afterStart(ComponentManager mgr, boolean isResume) {
        listening = false;
    }

    // API

    public boolean isListening() {
        return listening;
    }

    public Long getStartTotal() {
        return Long.valueOf(startCounter + 1);
    }

    /**
     * Returns the declared start order on component, as {@link Component#getApplicationStartedOrder()}.
     * <p>
     * Returns null if the value is the default one (0 for XML component, 1000 for Java components).
     */
    public Long getDeclaredStartOrder(String componentName) {
        return declaredStarts.get(componentName);
    }

    public Long getStartOrder(String componentName) {
        return starts.get(componentName);
    }

    public Long getExtensionPointTotal(String extensionPointId) {
        if (!xpCounters.containsKey(extensionPointId)) {
            return 0L;
        }
        return Long.valueOf(xpCounters.get(extensionPointId).get() + 1);
    }

    public Long getExtensionRegistrationOrder(String extensionId) {
        return registrations.get(extensionId);
    }

}
