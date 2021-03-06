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
package org.nuxeo.apidoc.documentation;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

public class JavaDocHelper {

    public static final String BASE_URL = "https://community.nuxeo.com/api/";

    /**
     * @since 11.1
     */
    public static final String BASE_URL_PROP_NAME = "org.nuxeo.apidoc.javadoc.url";

    /**
     * @since 11.1
     */
    public static final String DEFAULT_DIST = "nuxeo";

    protected static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    protected final String defaultPrefix;

    protected final String docVersion;

    /** @since 20.2.0 */
    protected Boolean exists;

    public JavaDocHelper(String prefix, String version) {
        defaultPrefix = prefix;
        String finalVersion = version;

        if (SnapshotManager.DISTRIBUTION_ALIAS_CURRENT.equals(version)) {
            SnapshotManager sm = Framework.getService(SnapshotManager.class);
            finalVersion = sm.getRuntimeSnapshot().getVersion();
        }

        if (finalVersion.endsWith(SNAPSHOT_SUFFIX)) {
            finalVersion = StringUtils.removeEnd(finalVersion, SNAPSHOT_SUFFIX);
        }
        docVersion = finalVersion;
    }

    protected String getUrl() {
        String baseUrl = Framework.getService(ConfigurationService.class).getString(BASE_URL_PROP_NAME, BASE_URL);
        return String.format("%s%s/%s/javadoc", baseUrl, defaultPrefix, docVersion);
    }

    /**
     * Returns true if the target Javadoc URL is valid.
     * <p>
     * The URL takes into account the version, and might not be valid in case some Javadoc versions have not been
     * published.
     *
     * @since 20.2.0
     */
    public boolean exists() {
        if (exists == null) {
            exists = URLHelper.isValid(getUrl());
        }
        return exists;
    }

    /**
     * Returns the Javadoc URL for given class.
     *
     * @since 11.1
     */
    public String getUrl(String classCanonicalName) {
        return getUrl(classCanonicalName, null);
    }

    /**
     * Returns the Javadoc URL for given class and given inner class name (can be null).
     *
     * @since 11.1
     */
    public String getUrl(String classCanonicalName, String innerClassName) {
        String base = getUrl();
        String classPart = classCanonicalName.replace(".", "/");
        if (innerClassName != null) {
            classPart = String.format("%s.%s", classPart, innerClassName);
        }
        return String.format("%s/%s.html", base, classPart);
    }

    public static JavaDocHelper getHelper(String distribName, String distribVersion) {
        return new JavaDocHelper(DEFAULT_DIST, distribVersion);
    }

}
