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

package org.nuxeo.apidoc.snapshot;

import org.nuxeo.apidoc.api.NuxeoArtifact;

/**
 * Generic interface for a snapshot {@link NuxeoArtifact} filter.
 *
 * @since 20.0.0
 */
public interface SnapshotFilter {

    public static final String REFERENCE_FILTER_NAME_SUFFIX = "-references";

    /**
     * Returns the filter name.
     * <p>
     * Can be used to create a dedicated bundle group for bundles filtering.
     */
    String getName();

    /**
     * Returns true if given artifact should be included, false if it should be filtered out.
     */
    boolean accept(NuxeoArtifact artifact);

    /**
     * Returns the filter class used to perform references retrieval.
     * <p>
     * Can return null, in which can no references should be retrieved.
     * <p>
     * Should return a class with a constructor accepting a String name and a list of previously selected Nuxeo
     * artifacts, see for instance @link {@link TargetExtensionPointSnapshotFilter}.
     */
    Class<? extends SnapshotFilter> getReferenceClass();

}
