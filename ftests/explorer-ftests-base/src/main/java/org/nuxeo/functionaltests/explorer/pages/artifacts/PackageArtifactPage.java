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
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.HtmlLink;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class PackageArtifactPage extends ArtifactPage<PackageArtifactPage.Builder> {

    protected final String packageId;

    protected final String packageName;

    protected final String packageVersion;

    protected final HtmlLink marketplaceLink;

    protected final List<String> dependencies;

    protected final List<String> optionalDependencies;

    protected final List<String> conflicts;

    protected final List<String> bundles;

    protected final List<String> components;

    protected final List<String> services;

    protected final List<String> extensionPoints;

    protected final List<String> contributions;

    protected final List<String> exports;

    public PackageArtifactPage(Builder builder, Source html) {
        super(builder, html);
        packageId = this.findElementWithId("packageId")
                        .map(TEXT_EXTRACTOR)
                        .orElseThrow(() -> new AssertionError("Unable to find the package id"));
        packageName = this.findElementWithId("packageName")
                          .map(TEXT_EXTRACTOR)
                          .orElseThrow(() -> new AssertionError("Unable to find the package name"));
        packageVersion = this.findElementWithId("packageVersion")
                             .map(TEXT_EXTRACTOR)
                             .orElseThrow(() -> new AssertionError("Unable to find the package version"));
        marketplaceLink = this.findElementWithId("marketplaceLink")
                              .map(HtmlLink::new)
                              .orElseThrow(() -> new AssertionError("Unable to find the marketeplace link"));
        dependencies = this.findElementWithId("dependencies")
                           .stream()
                           .flatMap(ul -> ul.getAllElements(HTMLElementName.LI).stream())
                           .map(TEXT_EXTRACTOR)
                           .toList();
        optionalDependencies = this.findElementWithId("optionalDependencies")
                                   .stream()
                                   .flatMap(ul -> ul.getAllElements(HTMLElementName.LI).stream())
                                   .map(TEXT_EXTRACTOR)
                                   .toList();
        conflicts = this.findElementWithId("conflicts")
                        .stream()
                        .flatMap(ul -> ul.getAllElements(HTMLElementName.LI).stream())
                        .map(TEXT_EXTRACTOR)
                        .toList();
        bundles = this.findElementWithId("bundles")
                      .stream()
                      .flatMap(div -> div.getAllElements(HTMLElementName.LI).stream())
                      .map(TEXT_EXTRACTOR)
                      .toList();
        components = this.findElementWithId("components")
                         .stream()
                         .flatMap(div -> div.getAllElements(HTMLElementName.LI).stream())
                         .map(TEXT_EXTRACTOR)
                         .toList();
        services = this.findElementWithId("services")
                       .stream()
                       .flatMap(div -> div.getAllElements(HTMLElementName.LI).stream())
                       .map(TEXT_EXTRACTOR)
                       .toList();
        extensionPoints = this.findElementWithId("extensionpoints")
                              .stream()
                              .flatMap(div -> div.getAllElements(HTMLElementName.LI).stream())
                              .map(TEXT_EXTRACTOR)
                              .toList();
        contributions = this.findElementWithId("contributions")
                            .stream()
                            .flatMap(div -> div.getAllElements(HTMLElementName.LI).stream())
                            .map(TEXT_EXTRACTOR)
                            .toList();
        exports = this.findElementsWithNameAndClass(HTMLElementName.UL, "exports")
                      .flatMap(ul -> ul.getAllElements(HTMLElementName.LI).stream())
                      .map(TEXT_EXTRACTOR)
                      .toList();
    }

    public static Builder builder(String packageName, String packageTitle) {
        return new Builder(packageName, packageTitle);
    }

    @Override
    public void assertElement() {
        super.assertElement();
        assertEquals(builder.expectedPackageName, packageName);
        assertFalse(StringUtils.isBlank(packageVersion));
        assertEquals(builder.expectedPackageName + '-' + packageVersion, packageId);
        var expectedMarketplaceLink = "https://connect.nuxeo.com/nuxeo/site/marketplace/package/platform-explorer?version="
                + packageVersion;
        assertEquals(expectedMarketplaceLink, marketplaceLink.getHref());
        assertEquals(expectedMarketplaceLink, marketplaceLink.getText());
        assertEquals(builder.expectedDependencies, dependencies);
        assertEquals(builder.expectedOptionalDependencies, optionalDependencies);
        assertEquals(builder.expectedConflicts, conflicts);
        assertEquals(builder.expectedBundles, bundles);
        assertEquals(builder.expectedComponents, components);
        assertEquals(builder.expectedServices, services);
        assertEquals(builder.expectedExtensionPoints, extensionPoints);
        assertEquals(builder.expectedContributions, contributions);
        assertEquals(builder.expectedExports, exports);
    }
    //
    // @Override
    // public void checkSelectedTab() {
    // DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
    // header.checkSelectedTab(header.packages);
    // }

    public static class Builder extends ArtifactPage.Builder<Builder> {

        protected final String expectedPackageName;

        protected List<String> expectedDependencies = List.of();

        protected List<String> expectedOptionalDependencies = List.of();

        protected List<String> expectedConflicts = List.of();

        protected List<String> expectedBundles = List.of();

        protected List<String> expectedComponents = List.of();

        protected List<String> expectedServices = List.of();

        protected List<String> expectedExtensionPoints = List.of();

        protected List<String> expectedContributions = List.of();

        protected List<String> expectedExports = List.of();

        public Builder(String expectedPackageName, String expectedPackageTitle) {
            super("Package " + expectedPackageTitle,
                    String.format("Package %s (%s)", expectedPackageTitle, expectedPackageName));
            this.expectedPackageName = expectedPackageName;
        }

        public Builder dependencies(String expectedDependency, String... expectedDependencies) {
            return dependencies(toList(expectedDependency, expectedDependencies));
        }

        public Builder dependencies(List<String> expectedDependencies) {
            this.expectedDependencies = List.copyOf(expectedDependencies);
            return this;
        }

        public Builder optionalDependencies(String expectedOptionalDependency, String... expectedOptionalDependencies) {
            return optionalDependencies(toList(expectedOptionalDependency, expectedOptionalDependencies));
        }

        public Builder optionalDependencies(List<String> expectedOptionalDependencies) {
            this.expectedOptionalDependencies = List.copyOf(expectedOptionalDependencies);
            return this;
        }

        public Builder conflicts(String expectedConflict, String... expectedConflicts) {
            return conflicts(toList(expectedConflict, expectedConflicts));
        }

        public Builder conflicts(List<String> expectedConflicts) {
            this.expectedConflicts = List.copyOf(expectedConflicts);
            return this;
        }

        public Builder bundles(String expectedBundle, String... expectedBundles) {
            return bundles(toList(expectedBundle, expectedBundles));
        }

        public Builder bundles(List<String> expectedBundles) {
            this.expectedBundles = List.copyOf(expectedBundles);
            return this;
        }

        public Builder components(String expectedComponent, String... expectedComponents) {
            return components(toList(expectedComponent, expectedComponents));
        }

        public Builder components(List<String> expectedComponents) {
            this.expectedComponents = List.copyOf(expectedComponents);
            return this;
        }

        public Builder services(String expectedService, String... expectedServices) {
            return services(toList(expectedService, expectedServices));
        }

        public Builder services(List<String> expectedServices) {
            this.expectedServices = List.copyOf(expectedServices);
            return this;
        }

        public Builder extensionPoints(String expectedExtensionPoint, String... expectedExtensionPoints) {
            return extensionPoints(toList(expectedExtensionPoint, expectedExtensionPoints));
        }

        public Builder extensionPoints(List<String> expectedExtensionPoints) {
            this.expectedExtensionPoints = List.copyOf(expectedExtensionPoints);
            return this;
        }

        public Builder contributions(String expectedContribution, String... expectedContributions) {
            return contributions(toList(expectedContribution, expectedContributions));
        }

        public Builder contributions(List<String> expectedContributions) {
            this.expectedContributions = List.copyOf(expectedContributions);
            return this;
        }

        public Builder exports(String expectedExport, String... expectedExports) {
            return exports(toList(expectedExport, expectedExports));
        }

        public Builder exports(List<String> expectedExports) {
            this.expectedExports = List.copyOf(expectedExports);
            return this;
        }

        public PackageArtifactPage build(Source html) {
            return new PackageArtifactPage(this, html);
        }
    }
}
