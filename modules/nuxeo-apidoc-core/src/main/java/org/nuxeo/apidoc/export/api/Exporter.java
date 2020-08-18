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
package org.nuxeo.apidoc.export.api;

import java.io.OutputStream;
import java.util.Map;

import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;

/**
 * @since 20.0.0
 */
public interface Exporter {

    String getName();

    String getTitle();

    String getDescription();

    String getFilename();

    String getMimetype();

    Map<String, String> getProperties();

    void export(OutputStream out, DistributionSnapshot distribution, SnapshotFilter filter,
            Map<String, String> properties);

}
