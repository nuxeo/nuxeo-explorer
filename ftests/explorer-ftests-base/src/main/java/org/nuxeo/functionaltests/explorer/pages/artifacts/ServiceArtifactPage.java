/*
 * (C) Copyright 2020-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.functionaltests.explorer.pages.artifacts;

import static org.junit.Assert.assertEquals;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class ServiceArtifactPage extends ArtifactPage<ServiceArtifactPage.Builder> {

    protected final String implementation;

    public ServiceArtifactPage(Builder builder, Source html) {
        super(builder, html);
        implementation = this.findElementsWithNameAndClass(HTMLElementName.SPAN, "javadoc")
                             .map(TEXT_EXTRACTOR)
                             .findFirst()
                             .orElse(null);
    }

    public static Builder builder(String serviceAndComponentName) {
        return builder(serviceAndComponentName, serviceAndComponentName);
    }

    public static Builder builder(String serviceName, String componentName) {
        return new Builder(serviceName, componentName);
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertEquals(builder.expectedImplementation, implementation);
    }

    // @Override
    // public void checkSelectedTab() {
    // DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
    // header.checkSelectedTab(header.services);
    // }

    public static class Builder extends ArtifactPage.Builder<Builder> {

        protected String expectedImplementation;

        public Builder(String expectedServiceName, String expectedComponentName) {
            super("Service " + expectedServiceName);
            contentInfoDescription("In component " + expectedComponentName);
            expectedImplementation = expectedServiceName;
        }

        public ServiceArtifactPage build(Source html) {
            return new ServiceArtifactPage(this, html);
        }
    }
}
