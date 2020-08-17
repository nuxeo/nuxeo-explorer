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
package org.nuxeo.apidoc.browse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.JsonPrettyPrinter;
import org.nuxeo.apidoc.snapshot.PersistSnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.snapshot.TargetExtensionPointSnapshotFilter;
import org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerService;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.PrettyPrinter;

@WebObject(type = ApiBrowser.TYPE)
public class ApiBrowser extends DefaultObject {

    public static final String TYPE = "apibrowser";

    protected String distributionId;

    protected boolean embeddedMode = false;

    protected SnapshotManager getSnapshotManager() {
        return Framework.getService(SnapshotManager.class);
    }

    protected ArtifactSearcher getSearcher() {
        return Framework.getService(ArtifactSearcher.class);
    }

    @Override
    protected void initialize(Object... args) {
        distributionId = (String) args[0];
        if (args.length > 1) {
            Boolean embed = (Boolean) args[1];
            embeddedMode = embed != null && embed;
        }
        getSnapshotManager().initWebContext(getContext().getRequest());
    }

    @GET
    @Produces("text/html")
    public Object doGet() {
        if (embeddedMode) {
            DistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
            Map<String, Integer> stats = new HashMap<>();
            stats.put("bundles", snap.getBundleIds().size());
            stats.put("jComponents", snap.getJavaComponentIds().size());
            stats.put("xComponents", snap.getXmlComponentIds().size());
            stats.put("services", snap.getServiceIds().size());
            stats.put("xps", snap.getExtensionPointIds().size());
            stats.put("contribs", snap.getComponentIds().size());
            stats.put("operations", snap.getOperations().size());
            stats.put("packages", snap.getPackages().size());
            return getView("indexSimple").arg(Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID))
                                         .arg("bundleIds", snap.getBundleIds())
                                         .arg("stats", stats);
        } else {
            return getView("index").arg(Distribution.DIST_ID, ctx.getProperty(Distribution.DIST_ID));
        }
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_BUNDLEGROUPS)
    public Object getMavenGroups() {
        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(
                getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()));
        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        return getView(ApiBrowserConstants.LIST_BUNDLEGROUPS).arg("tree", tree)
                                                             .arg(Distribution.DIST_ID,
                                                                     ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_BUNDLES)
    public Object getBundles() {
        DistributionSnapshot snapshot = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        List<String> bundleIds = snapshot.getBundleIds();
        List<ArtifactLabel> bundles = new ArrayList<>();
        for (String bid : bundleIds) {
            bundles.add(new ArtifactLabel(bid, bid, null));
        }
        return getView(ApiBrowserConstants.LIST_BUNDLES).arg("bundles", bundles)
                                                        .arg(Distribution.DIST_ID,
                                                                ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_COMPONENTS)
    public Object getComponents() {
        DistributionSnapshot snapshot = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        List<String> javaComponentIds = snapshot.getJavaComponentIds();
        List<ArtifactLabel> javaLabels = javaComponentIds.stream()
                                                         .map(ArtifactLabel::createLabelFromComponent)
                                                         .sorted()
                                                         .collect(Collectors.toList());
        javaLabels.stream().forEach(l -> l.setOrder(snapshot.getComponent(l.getId()).getRegistrationOrder()));

        List<String> xmlComponentIds = snapshot.getXmlComponentIds();
        List<ArtifactLabel> xmlLabels = xmlComponentIds.stream()
                                                       .map(ArtifactLabel::createLabelFromComponent)
                                                       .sorted()
                                                       .collect(Collectors.toList());
        xmlLabels.stream().forEach(l -> l.setOrder(snapshot.getComponent(l.getId()).getRegistrationOrder()));

        return getView(ApiBrowserConstants.LIST_COMPONENTS).arg("javaComponents", javaLabels)
                                                           .arg("xmlComponents", xmlLabels)
                                                           .arg(Distribution.DIST_ID,
                                                                   ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_SERVICES)
    public Object getServices() {
        List<String> serviceIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession())
                                                      .getServiceIds();

        List<ArtifactLabel> serviceLabels = new ArrayList<>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        Collections.sort(serviceLabels);

        return getView(ApiBrowserConstants.LIST_SERVICES).arg("services", serviceLabels)
                                                         .arg(Distribution.DIST_ID,
                                                                 ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_CONTRIBUTIONS)
    public Object getContributions() {
        DistributionSnapshot snapshot = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        return getView(ApiBrowserConstants.LIST_CONTRIBUTIONS).arg("contributions", snapshot.getContributions())
                                                              .arg("isLive", snapshot.isLive())
                                                              .arg(Distribution.DIST_ID,
                                                                      ctx.getProperty(Distribution.DIST_ID));
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_EXTENSIONPOINTS)
    public Object getExtensionPoints() {
        List<String> epIds = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession())
                                                 .getExtensionPointIds();

        List<ArtifactLabel> labels = epIds.stream()
                                          .map(ArtifactLabel::createLabelFromExtensionPoint)
                                          .collect(Collectors.toList());

        Collections.sort(labels);
        return getView(ApiBrowserConstants.LIST_EXTENSIONPOINTS).arg("eps", labels)
                                                                .arg(Distribution.DIST_ID,
                                                                        ctx.getProperty(Distribution.DIST_ID));
    }

    /**
     * XXX Not used?
     */
    @POST
    @Produces("text/html")
    @Path("filterComponents")
    public Object filterComponents(@FormParam("fulltext") String fulltext) {
        if (StringUtils.isBlank(fulltext)) {
            return getComponents();
        }
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ComponentInfo.TYPE_NAME, fulltext);

        List<ArtifactLabel> xmlLabels = new ArrayList<>();
        List<ArtifactLabel> javaLabels = new ArrayList<>();

        for (NuxeoArtifact item : artifacts) {
            ComponentInfo ci = (ComponentInfo) item;
            if (ci.isXmlPureComponent()) {
                xmlLabels.add(ArtifactLabel.createLabelFromComponent(ci.getId()));
            } else {
                javaLabels.add(ArtifactLabel.createLabelFromComponent(ci.getId()));
            }
        }
        return getView(ApiBrowserConstants.LIST_COMPONENTS).arg("javaComponents", javaLabels)
                                                           .arg("xmlComponents", xmlLabels)
                                                           .arg(Distribution.DIST_ID,
                                                                   ctx.getProperty(Distribution.DIST_ID))
                                                           .arg("searchFilter", sanitize(fulltext));
    }

    /**
     * XXX Not used?
     */
    @POST
    @Produces("text/html")
    @Path("filterBundles")
    public Object filterBundles(@FormParam("fulltext") String fulltext) {
        if (StringUtils.isBlank(fulltext)) {
            return getBundles();
        }
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                BundleInfo.TYPE_NAME, fulltext);
        List<String> bundleIds = new ArrayList<>();
        for (NuxeoArtifact item : artifacts) {
            bundleIds.add(item.getId());
        }
        return getView(ApiBrowserConstants.LIST_BUNDLES).arg("bundleIds", bundleIds)
                                                        .arg(Distribution.DIST_ID,
                                                                ctx.getProperty(Distribution.DIST_ID))
                                                        .arg("searchFilter", sanitize(fulltext));
    }

    /**
     * XXX Not used?
     */
    @POST
    @Produces("text/html")
    @Path("filterServices")
    public Object filterServices(@FormParam("fulltext") String fulltext) {
        if (StringUtils.isBlank(fulltext)) {
            return getServices();
        }
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ServiceInfo.TYPE_NAME, fulltext);
        List<String> serviceIds = new ArrayList<>();
        for (NuxeoArtifact item : artifacts) {
            serviceIds.add(item.getId());
        }
        List<ArtifactLabel> serviceLabels = new ArrayList<>();

        for (String id : serviceIds) {
            serviceLabels.add(ArtifactLabel.createLabelFromService(id));
        }
        return getView(ApiBrowserConstants.LIST_SERVICES).arg("services", serviceLabels)
                                                         .arg(Distribution.DIST_ID,
                                                                 ctx.getProperty(Distribution.DIST_ID))
                                                         .arg("searchFilter", sanitize(fulltext));
    }

    @POST
    @Produces("text/html")
    @Path("filterExtensionPoints")
    public Object filterExtensionPoints(@FormParam("fulltext") String fulltext) {
        if (StringUtils.isBlank(fulltext)) {
            return getExtensionPoints();
        }
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ExtensionPointInfo.TYPE_NAME, fulltext);
        List<String> eps = artifacts.stream().map(NuxeoArtifact::getId).collect(Collectors.toList());
        List<ArtifactLabel> labels = eps.stream()
                                        .map(ArtifactLabel::createLabelFromExtensionPoint)
                                        .collect(Collectors.toList());
        return getView(ApiBrowserConstants.LIST_EXTENSIONPOINTS).arg("eps", labels)
                                                                .arg(Distribution.DIST_ID,
                                                                        ctx.getProperty(Distribution.DIST_ID))
                                                                .arg("searchFilter", sanitize(fulltext));
    }

    @POST
    @Produces("text/html")
    @Path("filterContributions")
    public Object filterContributions(@FormParam("fulltext") String fulltext) {
        if (StringUtils.isBlank(fulltext)) {
            return getContributions();
        }
        List<NuxeoArtifact> artifacts = getSearcher().filterArtifact(getContext().getCoreSession(), distributionId,
                ExtensionInfo.TYPE_NAME, fulltext);
        return getView(ApiBrowserConstants.LIST_CONTRIBUTIONS).arg("contributions", artifacts)
                                                              .arg(Distribution.DIST_ID,
                                                                      ctx.getProperty(Distribution.DIST_ID))
                                                              .arg("searchFilter", sanitize(fulltext));
    }

    /**
     * Handles navigation to plugin view.
     *
     * @since 11.1
     */
    @Path("{pluginId}")
    public Object plugin(@PathParam("pluginId") String pluginId) {
        return ctx.newObject(pluginId, distributionId, embeddedMode);
    }

    @Path(ApiBrowserConstants.VIEW_BUNDLE + "/{bundleId}")
    public Resource viewBundle(@PathParam("bundleId") String bundleId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("bundle", bundleId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(bundleId);
        }
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_COMPONENT + "/{componentId}")
    public Resource viewComponent(@PathParam("componentId") String componentId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("component", componentId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(componentId);
        }
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_OPERATION + "/{opId}")
    public Resource viewOperation(@PathParam("opId") String opId) {
        return ctx.newObject("operation", opId);
    }

    @Path(ApiBrowserConstants.VIEW_SERVICE + "/{serviceId}")
    public Resource viewService(@PathParam("serviceId") String serviceId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("service", serviceId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(serviceId);
        }
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_EXTENSIONPOINT + "/{epId}")
    public Resource viewExtensionPoint(@PathParam("epId") String epId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("extensionPoint", epId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(epId);
        }
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_CONTRIBUTION + "/{cId}")
    public Resource viewContribution(@PathParam("cId") String cId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("contribution", cId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(cId);
        }
        return wo;
    }

    @Path(ApiBrowserConstants.VIEW_BUNDLEGROUP + "/{gId}")
    public Resource viewBundleGroup(@PathParam("gId") String gId) {
        NuxeoArtifactWebObject wo = (NuxeoArtifactWebObject) ctx.newObject("bundleGroup", gId);
        NuxeoArtifact nxItem = wo.getNxArtifact();
        if (nxItem == null) {
            throw new WebResourceNotFoundException(gId);
        }
        return wo;
    }

    /** @since 11.1 */
    @Path(ApiBrowserConstants.VIEW_PACKAGE + "/{pkgId}")
    public Resource viewPackage(@PathParam("pkgId") String pkgId) {
        return ctx.newObject(PackageWO.TYPE, pkgId);
    }

    @Path("viewArtifact/{id}")
    public Object viewArtifact(@PathParam("id") String id) {
        DistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());

        BundleGroup bg = snap.getBundleGroup(id);
        if (bg != null) {
            return viewBundleGroup(id);
        }

        BundleInfo bi = snap.getBundle(id);
        if (bi != null) {
            return viewBundle(id);
        }

        ComponentInfo ci = snap.getComponent(id);
        if (ci != null) {
            return viewComponent(id);
        }

        ServiceInfo si = snap.getService(id);
        if (si != null) {
            return viewService(id);
        }

        ExtensionPointInfo epi = snap.getExtensionPoint(id);
        if (epi != null) {
            return viewExtensionPoint(id);
        }

        ExtensionInfo ei = snap.getContribution(id);
        if (ei != null) {
            return viewContribution(id);
        }

        return Response.status(404).build();
    }

    public String getLabel(String id) {
        return null;
    }

    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_OPERATIONS)
    public Object listOperations() {
        DistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        List<OperationInfo> operations = snap.getOperations();
        return getView(ApiBrowserConstants.LIST_OPERATIONS).arg("operations", operations)
                                                           .arg(Distribution.DIST_ID,
                                                                   ctx.getProperty(Distribution.DIST_ID))
                                                           .arg("hideNav", Boolean.valueOf(false));
    }

    protected String sanitize(String value) {
        return Framework.getService(HtmlSanitizerService.class).sanitizeString(value, null);
    }

    /** @since 11.1 */
    @GET
    @Produces("text/html")
    @Path(ApiBrowserConstants.LIST_PACKAGES)
    public Object listPackages() {
        DistributionSnapshot snap = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        List<PackageInfo> packages = snap.getPackages();
        return getView(ApiBrowserConstants.LIST_PACKAGES).arg("packages", packages)
                                                         .arg(Distribution.DIST_ID,
                                                                 ctx.getProperty(Distribution.DIST_ID))
                                                         .arg("hideNav", Boolean.valueOf(false));
    }

    /**
     * Returns the distribution json export.
     *
     * @since 20.0.0
     */
    @GET
    @Path(ApiBrowserConstants.JSON_ACTION)
    @Produces("application/json")
    public Object getJson(@QueryParam("bundles") List<String> bundles,
            @QueryParam("nuxeoPackages") List<String> nuxeoPackages,
            @QueryParam("javaPackagePrefixes") List<String> javaPackagePrefixes,
            @QueryParam("checkAsPrefixes") Boolean checkAsPrefixes,
            @QueryParam("includeReferences") Boolean includeReferences, @QueryParam("pretty") Boolean pretty)
            throws IOException {
        DistributionSnapshot snapshot = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        // init potential resources depending on request
        getSnapshotManager().initWebContext(getContext().getRequest());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SnapshotFilter filter = getSnapshotFilter(bundles, nuxeoPackages, javaPackagePrefixes, checkAsPrefixes,
                includeReferences);
        PrettyPrinter printer = Boolean.TRUE.equals(pretty) ? new JsonPrettyPrinter() : null;
        snapshot.writeJson(out, filter, printer);
        return out.toString();
    }

    protected SnapshotFilter getSnapshotFilter(List<String> bundlePrefixes, List<String> nuxeoPackagePrefixes,
            List<String> javaPackagePrefixes, Boolean checkAsPrefixes, Boolean includeReferences) {
        List<String> bp = getSnapshotFilterCriterion(bundlePrefixes);
        List<String> np = getSnapshotFilterCriterion(nuxeoPackagePrefixes);
        List<String> jp = getSnapshotFilterCriterion(javaPackagePrefixes);
        if (!bp.isEmpty() || !np.isEmpty() || !jp.isEmpty()) {
            PersistSnapshotFilter filter = new PersistSnapshotFilter("Json Rest Filter",
                    Boolean.TRUE.equals(checkAsPrefixes),
                    Boolean.TRUE.equals(includeReferences) ? TargetExtensionPointSnapshotFilter.class : null);
            bp.forEach(bid -> filter.addBundle(bid));
            np.forEach(pkg -> filter.addNuxeoPackage(pkg));
            jp.forEach(pkg -> filter.addPackagesPrefix(pkg));
            return filter;
        }
        return null;
    }

    protected List<String> getSnapshotFilterCriterion(List<String> queryParam) {
        var res = new ArrayList<String>();
        if (queryParam != null) {
            queryParam.stream().filter(StringUtils::isNotBlank).forEach(res::add);
        }
        return res;
    }

}
