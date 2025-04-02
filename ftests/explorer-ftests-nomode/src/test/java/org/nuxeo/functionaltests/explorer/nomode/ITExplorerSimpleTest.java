/*
 * (C) Copyright 2014-2020 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.nomode;

import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.HTTP_STATUS_NOT_FOUND_CHECKER;

import java.util.function.UnaryOperator;

import jakarta.ws.rs.core.MediaType;

import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.HtmlPageHandler;
import org.nuxeo.functionaltests.explorer.ExplorerTestRule;
import org.nuxeo.functionaltests.explorer.pages.LiveSimplePage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ArtifactsPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ComponentArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ContributionArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ExtensionPointArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.OperationArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.PackageArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ServiceArtifactPage;
import org.nuxeo.http.test.HttpClientTestRule;

/**
 * Test explorer "adm" "simple" webengine pages.
 *
 * @since 11.1
 */
public class ITExplorerSimpleTest {

    @Rule
    public final HttpClientTestRule adminHttpClient = HttpClientTestRule.builder()
                                                                        .adminCredentials()
                                                                        .accept(MediaType.TEXT_HTML)
                                                                        .build();

    @Test
    public void testLiveDistributionSimplePage() {
        var distrib = adminHttpClient.buildGetRequest(LiveSimplePage.URL)
                                     .execute(new HtmlPageHandler<>(LiveSimplePage.builder()::build));
        var bundles = distrib.getBundles();
        bundles.assertContainsRow("org.nuxeo.admin.center", "/viewBundle/org.nuxeo.admin.center", null);
        bundles.assertContainsRow("org.nuxeo.apidoc.core", "/viewBundle/org.nuxeo.apidoc.core", null);

        String firstBundlePath = bundles.getFirstRow().getLink().getHrefAndStrip("");
        adminHttpClient.buildGetRequest(firstBundlePath)
                       .execute(new HtmlPageHandler<>(
                               BundleArtifactPage.builder("org.nuxeo.ecm.platform", "org.nuxeo.admin.center")
                                                 .tableOfContents("Resolution Order", "Components", "Maven Artifact",
                                                         "Manifest", "Exports", "Charts")
                                                 .mavenArtifact("org.nuxeo.ecm.platform", "nuxeo-admin-center-core")
                                                 .resolutionOrderPresence(true)::build));
        adminHttpClient.buildGetRequest(getArtifactURL(BundleInfo.TYPE_NAME, "org.nuxeo.apidoc.core"))
                       .execute(new HtmlPageHandler<>(
                               BundleArtifactPage.builder("org.nuxeo.apidoc", "org.nuxeo.apidoc.core")
                                                 .tableOfContents("Documentation", "Components", "Packages",
                                                         "Maven Artifact", "Manifest", "Exports", "Charts")
                                                 .documentationHtmlFromResource("data/apidoc_core_readmes.html")
                                                 .mavenArtifact("org.nuxeo.ecm.platform", "nuxeo-apidoc-core")
                                                 .resolutionOrderPresence(false)
                                                 .packages("platform-explorer")::build));
    }

    @Test
    public void testExtensionPoints() {
        var listExtensionPointsPage = adminHttpClient.buildGetRequest(
                LiveSimplePage.URL + '/' + ApiBrowserConstants.LIST_EXTENSIONPOINTS)
                                                     .execute(new HtmlPageHandler<>(
                                                             ArtifactsPage.builder("All Extension Points")
                                                                          .hasArtifactRow("actions",
                                                                                  "/viewExtensionPoint/org.nuxeo.ecm.platform.actions.ActionService--actions",
                                                                                  "ActionService - org.nuxeo.ecm.platform.actions.ActionService")
                                                                          .hasArtifactRow("plugins",
                                                                                  "/viewExtensionPoint/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins",
                                                                                  "SnapshotManagerComponent - org.nuxeo.apidoc.snapshot.SnapshotManagerComponent")::build));

        String pluginsPath = listExtensionPointsPage.getArtifacts()
                                                    .findRowOrThrow("plugins",
                                                            "SnapshotManagerComponent - org.nuxeo.apidoc.snapshot.SnapshotManagerComponent")
                                                    .getLink()
                                                    .getHrefAndStrip("");
        adminHttpClient.buildGetRequest(pluginsPath)
                       .execute(new HtmlPageHandler<>(
                               ExtensionPointArtifactPage.builder("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                                       "plugins")
                                                         .tableOfContents("Documentation", "Contribution Descriptors",
                                                                 "Contributions")
                                                         .documentationHtmlFromResource(
                                                                 "data/SnapshotManagerComponent_plugins_extensionPoint_documentation.html")
                                                         .descriptors(
                                                                 "Class: org.nuxeo.apidoc.plugin.PluginDescriptor")::build));
    }

    @Test
    public void testContributions() {
        var listContributionsPage = adminHttpClient.buildGetRequest(
                LiveSimplePage.URL + '/' + ApiBrowserConstants.LIST_CONTRIBUTIONS)
                                                   .execute(new HtmlPageHandler<>(
                                                           ArtifactsPage.builder("All Contributions")
                                                                        .hasArtifactRow("cluster-config--configuration",
                                                                                "/viewContribution/cluster-config--configuration",
                                                                                "configuration - org.nuxeo.runtime.cluster.ClusterService")
                                                                        .hasArtifactRow(
                                                                                "org.nuxeo.apidoc.adapterContrib--adapters",
                                                                                "/viewContribution/org.nuxeo.apidoc.adapterContrib--adapters",
                                                                                "adapters - org.nuxeo.ecm.core.api.DocumentAdapterService")::build));

        String adaptersPath = listContributionsPage.getArtifacts()
                                                   .findRowOrThrow("org.nuxeo.apidoc.adapterContrib--adapters")
                                                   .getLink()
                                                   .getHrefAndStrip("");
        adminHttpClient.buildGetRequest(adaptersPath)
                       .execute(new HtmlPageHandler<>(
                               ContributionArtifactPage.builder("org.nuxeo.apidoc.adapterContrib", "adapters")
                                                       .documentation(
                                                               "These contributions provide a mapping between live introspections "
                                                                       + "and persisted representations of a distribution.")::build));
    }

    @Test
    public void testServices() {
        var listServicesPage = adminHttpClient.buildGetRequest(
                LiveSimplePage.URL + '/' + ApiBrowserConstants.LIST_SERVICES)
                                              .execute(new HtmlPageHandler<>(
                                                      ArtifactsPage.builder("All Services")
                                                                   .hasArtifactRow("ActionManager",
                                                                           "/viewService/org.nuxeo.ecm.platform.actions.ejb.ActionManager",
                                                                           "org.nuxeo.ecm.platform.actions.ejb.ActionManager")
                                                                   .hasArtifactRow("SnapshotManager",
                                                                           "/viewService/org.nuxeo.apidoc.snapshot.SnapshotManager",
                                                                           "org.nuxeo.apidoc.snapshot.SnapshotManager")::build));

        String snapshotManagerPath = listServicesPage.getArtifacts()
                                                     .findRowOrThrow("SnapshotManager")
                                                     .getLink()
                                                     .getHrefAndStrip("");
        adminHttpClient.buildGetRequest(snapshotManagerPath)
                       .execute(new HtmlPageHandler<>(
                               ServiceArtifactPage.builder("org.nuxeo.apidoc.snapshot.SnapshotManager",
                                       "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent")
                                                  .tableOfContents("Implementation")::build));
    }

    @Test
    public void testOperations() {
        var listOperationsPage = adminHttpClient.buildGetRequest(
                LiveSimplePage.URL + '/' + ApiBrowserConstants.LIST_OPERATIONS)
                                                .execute(new HtmlPageHandler<>(
                                                        ArtifactsPage.builder("All Operations")
                                                                     .hasArtifactRow("acceptComment",
                                                                             "/viewOperation/acceptComment",
                                                                             "Chain acceptComment")
                                                                     .hasArtifactRow("Add Facet",
                                                                             "/viewOperation/Document.AddFacet",
                                                                             "Document Document.AddFacet")::build));

        String addFacetPath = listOperationsPage.getArtifacts()
                                                .findRowOrThrow("Add Facet")
                                                .getLink()
                                                .getHrefAndStrip("");
        adminHttpClient.buildGetRequest(addFacetPath)
                       .execute(new HtmlPageHandler<>(
                               OperationArtifactPage.builder("Document.AddFacet", "Add Facet",
                                       "org.nuxeo.ecm.core.automation.coreContrib")
                                                    .tableOfContents("Description", "Parameters", "Signature",
                                                            "Implementation Information", "JSON Definition")
                                                    .description("Adds the facet to the document. " //
                                                            + "WARNING: The save parameter is true by default, which means the document is saved in "
                                                            + "the database after adding the facet. It must be set to false when the operation is used "
                                                            + "in the context of an event that will fail if the document is saved (empty document created, "
                                                            + "about to create, before modification, ...).")
                                                    .additionalInfo("Document")
                                                    .hasParameterRow("facet", "", "string", "yes", " ")
                                                    .hasParameterRow("save", "", "boolean", "no", "true ")
                                                    .signature("document, documents", "document, documents")
                                                    .implementation(
                                                            "org.nuxeo.ecm.automation.core.operations.document.AddFacet")
                                                    .json("""
                                                            {
                                                              "id" : "Document.AddFacet",
                                                              "aliases" : [ "Document.AddFacet" ],
                                                              "label" : "Add Facet",
                                                              "category" : "Document",
                                                              "requires" : null,
                                                              "description" : "Adds the facet to the document. <p>WARNING: The save parameter is true by default, which means the document is saved in the database after adding the facet. It must be set to false when the operation is used in the context of an event that will fail if the document is saved (empty document created, about to create, before modification, ...).</p>",
                                                              "url" : "Document.AddFacet",
                                                              "signature" : [ "document", "document", "documents", "documents" ],
                                                              "params" : [ {
                                                                "name" : "facet",
                                                                "description" : "",
                                                                "type" : "string",
                                                                "required" : true,
                                                                "widget" : null,
                                                                "order" : 0,
                                                                "values" : [ ]
                                                              }, {
                                                                "name" : "save",
                                                                "description" : "",
                                                                "type" : "boolean",
                                                                "required" : false,
                                                                "widget" : null,
                                                                "order" : 0,
                                                                "values" : [ "true" ]
                                                              } ]
                                                            }""")::build));
    }

    @Test
    public void testComponents() {
        var listComponentsPage = adminHttpClient.buildGetRequest(
                LiveSimplePage.URL + '/' + ApiBrowserConstants.LIST_COMPONENTS)
                                                .execute(new HtmlPageHandler<>(
                                                        ArtifactsPage.builder("All Components")
                                                                     .hasArtifactRow("actions.ActionService",
                                                                             "/viewComponent/org.nuxeo.ecm.platform.actions.ActionService",
                                                                             "Java org.nuxeo.ecm.platform.actions.ActionService")
                                                                     .hasArtifactRow(
                                                                             "apidoc.snapshot.SnapshotManagerComponent",
                                                                             "/viewComponent/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                                                                             "Java org.nuxeo.apidoc.snapshot.SnapshotManagerComponent")::build));

        String snapshotManagerPath = listComponentsPage.getArtifacts()
                                                       .findRowOrThrow("apidoc.snapshot.SnapshotManagerComponent")
                                                       .getLink()
                                                       .getHrefAndStrip("");
        adminHttpClient.buildGetRequest(snapshotManagerPath)
                       .execute(new HtmlPageHandler<>(
                               ComponentArtifactPage.builder("org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                                       "org.nuxeo.apidoc.repo")
                                                    .tableOfContents("Documentation", "Resolution Order", "Start Order",
                                                            "Implementation", "Services", "Extension Points",
                                                            "Contributions", "XML Source")
                                                    .documentation(
                                                            """
                                                                    This component handles the introspection of the current live Runtime as a distribution.
                                                                    It can also persist this introspection as Nuxeo documents, to handle import and export of external distributions.""")
                                                    .startOrderPresence(true)
                                                    .implementation(
                                                            "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent")
                                                    .xmlSourcePresence(true)::build));
    }

    @Test
    public void testBundles() {
        var listBundlesPage = adminHttpClient.buildGetRequest(
                LiveSimplePage.URL + '/' + ApiBrowserConstants.LIST_BUNDLES)
                                             .execute(new HtmlPageHandler<>(
                                                     ArtifactsPage.builder("All Bundles")
                                                                  .hasArtifactRow("org.nuxeo.admin.center",
                                                                          "/viewBundle/org.nuxeo.admin.center")
                                                                  .hasArtifactRow("org.nuxeo.apidoc.core",
                                                                          "/viewBundle/org.nuxeo.apidoc.core")::build));

        String apiDocCorePath = listBundlesPage.getArtifacts()
                                               .findRowOrThrow("org.nuxeo.apidoc.core")
                                               .getLink()
                                               .getHrefAndStrip("");
        adminHttpClient.buildGetRequest(apiDocCorePath)
                       .execute(new HtmlPageHandler<>(
                               BundleArtifactPage.builder("org.nuxeo.apidoc", "org.nuxeo.apidoc.core")
                                                 .tableOfContents("Documentation", "Components", "Packages",
                                                         "Maven Artifact", "Manifest", "Exports", "Charts")
                                                 .documentationHtmlFromResource("data/apidoc_core_readmes.html")
                                                 .mavenArtifact("org.nuxeo.ecm.platform", "nuxeo-apidoc-core")
                                                 .resolutionOrderPresence(false)
                                                 .packages("platform-explorer")::build));
    }

    @Test
    public void testPackages() {
        var listPackagesPage = adminHttpClient.buildGetRequest(
                LiveSimplePage.URL + '/' + ApiBrowserConstants.LIST_PACKAGES)
                                              .execute(new HtmlPageHandler<>(
                                                      ArtifactsPage.builder("All Packages")
                                                                   .hasArtifactRow("Platform Explorer",
                                                                           "/viewPackage/platform-explorer",
                                                                           "addon platform-explorer")::build));

        String explorerPackagePath = listPackagesPage.getArtifacts()
                                                     .findRowOrThrow("Platform Explorer")
                                                     .getLink()
                                                     .getHrefAndStrip("");
        adminHttpClient.buildGetRequest(explorerPackagePath)
                       .execute(new HtmlPageHandler<>(
                               PackageArtifactPage.builder("platform-explorer", "Platform Explorer")
                                                  .tableOfContents("General Information", "Dependencies", "Bundles",
                                                          "Components", "Services", "Extension Points", "Contributions",
                                                          "Exports", "Charts")
                                                  .dependencies("nuxeo-search-client-opensearch1")
                                                  .bundles("org.nuxeo.apidoc.core", "org.nuxeo.apidoc.repo",
                                                          "org.nuxeo.apidoc.webengine")
                                                  .components("org.nuxeo.apidoc.doctypeContrib",
                                                          "org.nuxeo.apidoc.lifecycle.contrib",
                                                          "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                                                          "org.nuxeo.apidoc.adapterContrib",
                                                          "org.nuxeo.apidoc.listener.contrib",
                                                          "org.nuxeo.apidoc.schemaContrib")
                                                  .services("ArtifactSearcher", "SnapshotListener", "SnapshotManager")
                                                  .extensionPoints(
                                                          "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--exporters",
                                                          "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins")
                                                  .contributions("org.nuxeo.apidoc.adapterContrib--adapters",
                                                          "org.nuxeo.apidoc.doctypeContrib--doctype",
                                                          "org.nuxeo.apidoc.lifecycle.contrib--lifecycle",
                                                          "org.nuxeo.apidoc.lifecycle.contrib--types",
                                                          "org.nuxeo.apidoc.listener.contrib--listener",
                                                          "org.nuxeo.apidoc.schemaContrib--schema",
                                                          "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration",
                                                          "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration1",
                                                          "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration2",
                                                          "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--configuration3",
                                                          "org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--exporters")
                                                  .exports("Json Export Default Json serialization",
                                                          "Json Graph Json dependency graph",
                                                          "Json Contribution Stats Json statistics for contributions",
                                                          "CSV Contribution Stats CSV statistics for contributions")::build));
    }

    protected String getArtifactURL(String type, String id) {
        return ExplorerTestRule.getArtifactURL(SnapshotManager.DISTRIBUTION_ALIAS_ADM, type, id);
    }

    @Test
    public void testInvalidArtifactPages() {
        UnaryOperator<String> getInvalidArtifactURLByType = type -> getArtifactURL(type, "foo");
        adminHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(BundleGroup.TYPE_NAME))
                       .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        adminHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(BundleInfo.TYPE_NAME))
                       .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        adminHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ComponentInfo.TYPE_NAME))
                       .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        adminHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ExtensionInfo.TYPE_NAME))
                       .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        adminHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ExtensionPointInfo.TYPE_NAME))
                       .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        adminHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(ServiceInfo.TYPE_NAME))
                       .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        adminHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(PackageInfo.TYPE_NAME))
                       .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
        adminHttpClient.buildGetRequest(getInvalidArtifactURLByType.apply(OperationInfo.TYPE_NAME))
                       .execute(HTTP_STATUS_NOT_FOUND_CHECKER);
    }
}
