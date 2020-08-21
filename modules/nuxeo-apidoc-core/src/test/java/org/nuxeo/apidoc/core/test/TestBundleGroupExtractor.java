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
package org.nuxeo.apidoc.core.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupExtractor;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;

/**
 * @since 11.1
 */
public class TestBundleGroupExtractor {

    protected static class MockBundle {

        protected String id;

        protected String groupId;

        protected String artifactId;

        public MockBundle(String id, String groupId, String artifactId) {
            super();
            this.id = id;
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        public String getId() {
            return id;
        }

        public BundleInfo createBundle() {
            BundleInfoImpl bundle = new BundleInfoImpl(id);
            bundle.setGroupId(groupId);
            bundle.setArtifactId(artifactId);
            return bundle;
        }

    }

    protected Map<String, BundleInfo> createMockBundles() {
        List<MockBundle> values = Arrays.asList(new MockBundle("org.nuxeo.common", "org.nuxeo.common", "nuxeo-common"),
                // connect
                new MockBundle("org.nuxeo.connect.client", "org.nuxeo.connect", "nuxeo-connect-client"),
                new MockBundle("org.nuxeo.connect.client.wrapper", "org.nuxeo.ecm.platform",
                        "nuxeo-connect-client-wrapper"),
                new MockBundle("org.nuxeo.connect.update", "org.nuxeo.ecm.platform", "nuxeo-connect-update"),
                // runtime
                new MockBundle("org.nuxeo.connect.standalone", "org.nuxeo.runtime", "nuxeo-connect-standalone"),
                new MockBundle("org.nuxeo.launcher.commons", "org.nuxeo.runtime", "nuxeo-launcher-commons"),
                new MockBundle("org.nuxeo.osgi", "org.nuxeo.runtime", "nuxeo-runtime-osgi"),
                new MockBundle("org.nuxeo.runtime", "org.nuxeo.runtime", "nuxeo-runtime"),
                new MockBundle("org.nuxeo.runtime.cluster", "org.nuxeo.runtime", "nuxeo-runtime-cluster"),
                // core
                new MockBundle("org.nuxeo.ecm.core", "org.nuxeo.ecm.core", "nuxeo-core"),
                new MockBundle("org.nuxeo.ecm.core.api", "org.nuxeo.ecm.core", "nuxeo-core-api"),
                new MockBundle("org.nuxeo.ecm.core.bulk", "org.nuxeo.ecm.core", "nuxeo-core-bulk"),
                new MockBundle("org.nuxeo.ecm.core.io", "org.nuxeo.ecm.core", "nuxeo-core-io"),
                new MockBundle("org.nuxeo.ecm.platform.el", "org.nuxeo.ecm.core", "nuxeo-core-el"),
                // platform misc
                new MockBundle("org.nuxeo.ecm.permissions", "org.nuxeo.ecm.platform", "nuxeo-permissions"),
                new MockBundle("org.nuxeo.ecm.platform", "org.nuxeo.ecm.platform", "nuxeo-platform"),
                new MockBundle("org.nuxeo.ecm.platform.api", "org.nuxeo.ecm.platform", "nuxeo-platform-api"),
                new MockBundle("org.nuxeo.ecm.platform.restapi.io", "org.nuxeo.ecm.platform", "nuxeo-rest-api-io"),
                new MockBundle("org.nuxeo.ecm.platform.restapi.server", "org.nuxeo.ecm.platform",
                        "nuxeo-rest-api-server"),
                new MockBundle("org.nuxeo.mail", "org.nuxeo.ecm.platform", "nuxeo-mail"),
                // directories
                new MockBundle("org.nuxeo.ecm.directory", "org.nuxeo.ecm.platform", "nuxeo-platform-directory-core"),
                new MockBundle("org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.platform", "nuxeo-platform-directory-api"),
                new MockBundle("org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.platform", "nuxeo-platform-directory-sql"),
                new MockBundle("org.nuxeo.ecm.directory.types.contrib", "org.nuxeo.ecm.platform",
                        "nuxeo-platform-directory-types-contrib"),
                // relations
                new MockBundle("org.nuxeo.ecm.relations", "org.nuxeo.ecm.platform", "nuxeo-platform-relations-core"),
                new MockBundle("org.nuxeo.ecm.relations.api", "org.nuxeo.ecm.platform", "nuxeo-platform-relations-api"),
                new MockBundle("org.nuxeo.ecm.relations.core.listener", "org.nuxeo.ecm.platform",
                        "nuxeo-platform-relations-core-listener"),
                new MockBundle("org.nuxeo.ecm.relations.io", "org.nuxeo.ecm.platform", "nuxeo-platform-relations-io"),
                // apidoc
                new MockBundle("org.nuxeo.apidoc.core", "org.nuxeo.ecm.platform", "nuxeo-apidoc-core"),
                new MockBundle("org.nuxeo.apidoc.repo", "org.nuxeo.ecm.platform", "nuxeo-apidoc-repo"),
                new MockBundle("org.nuxeo.apidoc.webengine", "org.nuxeo.ecm.platform", "nuxeo-apidoc-webengine"));
        return values.stream().collect(Collectors.toMap(MockBundle::getId, MockBundle::createBundle));
    }

    @Test
    public void test() {
        BundleGroupExtractor bge = new BundleGroupExtractor(createMockBundles(), "11.1-SNAPSHOT");
        List<BundleGroup> roots = bge.getRoots();

        assertEquals(5, roots.size());
        assertEquals(
                Arrays.asList("grp:org.nuxeo.common", "grp:org.nuxeo.connect", "grp:org.nuxeo.ecm.core",
                        "grp:org.nuxeo.ecm.platform", "grp:org.nuxeo.runtime"),
                roots.stream().map(BundleGroup::getId).collect(Collectors.toList()));
        Iterator<BundleGroup> rootsIt = roots.iterator();
        BundleGroup common = rootsIt.next();
        assertEquals("grp:org.nuxeo.common", common.getId());
        assertEquals(Arrays.asList(), common.getSubGroups());
        assertEquals(Arrays.asList(), common.getParentIds());
        BundleGroup connect = rootsIt.next();
        assertEquals("grp:org.nuxeo.connect", connect.getId());
        assertEquals(Arrays.asList("grp:org.nuxeo.connect.client"),
                connect.getSubGroups().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        assertEquals(Arrays.asList(), connect.getParentIds());
        BundleGroup core = rootsIt.next();
        assertEquals("grp:org.nuxeo.ecm.core", core.getId());
        assertEquals(Arrays.asList(), core.getSubGroups());
        assertEquals(Arrays.asList(), core.getParentIds());
        BundleGroup platform = rootsIt.next();
        assertEquals("grp:org.nuxeo.ecm.platform", platform.getId());
        assertEquals(
                Arrays.asList("grp:org.nuxeo.apidoc", "grp:org.nuxeo.ecm.directory",
                        "grp:org.nuxeo.ecm.platform.restapi", "grp:org.nuxeo.ecm.relations"),
                platform.getSubGroups().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        assertEquals(Arrays.asList(), platform.getParentIds());
        BundleGroup runtime = rootsIt.next();
        assertEquals("grp:org.nuxeo.runtime", runtime.getId());
        assertEquals(Arrays.asList(), runtime.getSubGroups());
        assertEquals(Arrays.asList(), runtime.getParentIds());

        Map<String, BundleGroup> groups = bge.getGroups();
        assertEquals(10, groups.size());
        assertEquals(
                Arrays.asList("grp:org.nuxeo.ecm.core", "grp:org.nuxeo.common", "grp:org.nuxeo.apidoc",
                        "grp:org.nuxeo.ecm.directory", "grp:org.nuxeo.ecm.platform.restapi",
                        "grp:org.nuxeo.ecm.relations", "grp:org.nuxeo.ecm.platform", "grp:org.nuxeo.runtime",
                        "grp:org.nuxeo.connect.client", "grp:org.nuxeo.connect"),
                groups.values().stream().map(BundleGroup::getId).collect(Collectors.toList()));
        assertEquals(Arrays.asList("org.nuxeo.common"), groups.get("grp:org.nuxeo.common").getBundleIds());
        assertEquals(Arrays.asList("org.nuxeo.connect.update"), groups.get("grp:org.nuxeo.connect").getBundleIds());
        assertEquals(Arrays.asList("org.nuxeo.connect.client", "org.nuxeo.connect.client.wrapper"),
                groups.get("grp:org.nuxeo.connect.client").getBundleIds());
        assertEquals(
                Arrays.asList("org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api", "org.nuxeo.ecm.core.bulk",
                        "org.nuxeo.ecm.core.io", "org.nuxeo.ecm.platform.el"),
                groups.get("grp:org.nuxeo.ecm.core").getBundleIds());
        assertEquals(Arrays.asList("org.nuxeo.ecm.permissions", "org.nuxeo.ecm.platform", "org.nuxeo.ecm.platform.api",
                "org.nuxeo.mail"), groups.get("grp:org.nuxeo.ecm.platform").getBundleIds());
        assertEquals(
                Arrays.asList("org.nuxeo.connect.standalone", "org.nuxeo.launcher.commons", "org.nuxeo.osgi",
                        "org.nuxeo.runtime", "org.nuxeo.runtime.cluster"),
                groups.get("grp:org.nuxeo.runtime").getBundleIds());
        // directory
        assertEquals(
                Arrays.asList("org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory.sql",
                        "org.nuxeo.ecm.directory.types.contrib"),
                groups.get("grp:org.nuxeo.ecm.directory").getBundleIds());
        assertEquals(Arrays.asList("grp:org.nuxeo.ecm.platform"),
                groups.get("grp:org.nuxeo.ecm.directory").getParentIds());
        assertEquals(Arrays.asList(), groups.get("grp:org.nuxeo.ecm.directory").getSubGroups());
        // relations
        assertEquals(
                Arrays.asList("org.nuxeo.ecm.relations", "org.nuxeo.ecm.relations.api",
                        "org.nuxeo.ecm.relations.core.listener", "org.nuxeo.ecm.relations.io"),
                groups.get("grp:org.nuxeo.ecm.relations").getBundleIds());
        assertEquals(Arrays.asList("grp:org.nuxeo.ecm.platform"),
                groups.get("grp:org.nuxeo.ecm.relations").getParentIds());
        assertEquals(Arrays.asList(), groups.get("grp:org.nuxeo.ecm.relations").getSubGroups());
        // apidoc
        assertEquals(Arrays.asList("org.nuxeo.apidoc.core", "org.nuxeo.apidoc.repo", "org.nuxeo.apidoc.webengine"),
                groups.get("grp:org.nuxeo.apidoc").getBundleIds());
        assertEquals(Arrays.asList("grp:org.nuxeo.ecm.platform"), groups.get("grp:org.nuxeo.apidoc").getParentIds());
        assertEquals(Arrays.asList(), groups.get("grp:org.nuxeo.apidoc").getSubGroups());
        // restapi
        assertEquals(Arrays.asList("org.nuxeo.ecm.platform.restapi.io", "org.nuxeo.ecm.platform.restapi.server"),
                groups.get("grp:org.nuxeo.ecm.platform.restapi").getBundleIds());
        assertEquals(Arrays.asList("grp:org.nuxeo.ecm.platform"),
                groups.get("grp:org.nuxeo.ecm.platform.restapi").getParentIds());
        assertEquals(Arrays.asList(), groups.get("grp:org.nuxeo.ecm.platform.restapi").getSubGroups());
    }

}
