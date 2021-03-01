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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.export.ArchiveFile;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.listener.AttributesExtractorStater;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.security.SecurityHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshotDesc;
import org.nuxeo.apidoc.snapshot.PersistSnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.snapshot.SnapshotResolverHelper;
import org.nuxeo.apidoc.snapshot.TargetExtensionPointSnapshotFilter;
import org.nuxeo.apidoc.snapshot.VersionComparator;
import org.nuxeo.apidoc.worker.ExtractXmlAttributesWorker;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.jersey.api.NotFoundException;

@Path("/distribution")
// needed for 5.4.1
@WebObject(type = Distribution.TYPE)
public class Distribution extends ModuleRoot {

    private static final Logger log = LogManager.getLogger(Distribution.class);

    /** @since 20.0.0 */
    public static final String TYPE = "distribution";

    public static final String DIST_ID = "distId";

    protected static final String DIST = "distribution";

    /** @since 20.0.0 */
    public static final String VIEW_ADMIN = "_admin";

    /** @since 20.0.0 */
    public static final String SAVE_ACTION = "save";

    /** @since 20.0.0 */
    public static final String SAVE_EXTENDED_ACTION = "saveExtended";

    /** @since 20.0.0 */
    public static final String DOWNLOAD_ACTION = "download";

    /** @since 20.0.0 */
    public static final String UPDATE_ACTION = "updateDistrib";

    /** @since 20.0.0 */
    public static final String DO_UPDATE_ACTION = "doUpdate";

    /** @since 20.0.0 */
    public static final String DELETE_ACTION = "delete";

    /** @since 20.0.0 */
    public static final String UPLOAD_ACTION = "uploadDistrib";

    /** @since 20.0.0 */
    public static final String UPLOAD_TMP_ACTION = "uploadDistribTmp";

    /** @since 20.0.0 */
    public static final String UPLOAD_TMP_VALID_ACTION = "uploadDistribTmpValid";

    /** @since 20.0.0 */
    public static final String REINDEX_ACTION = "_reindex";

    /** @since 20.0.0 */
    public static final String LOGIN_ACTION = "apidocLogin";

    /**
     * List of subviews, used for validation of distribution names and aliases.
     *
     * @since 20.0.0
     */
    protected static final List<String> SUB_DISTRIBUTION_PATH_RESERVED = Arrays.asList(VIEW_ADMIN, SAVE_ACTION,
            SAVE_EXTENDED_ACTION, DOWNLOAD_ACTION, UPDATE_ACTION, DO_UPDATE_ACTION, DELETE_ACTION, UPLOAD_ACTION,
            UPLOAD_TMP_ACTION, UPLOAD_TMP_VALID_ACTION, REINDEX_ACTION);

    /**
     * Customized error management.
     * <p>
     * Do not rely on templates to avoids error "Cannot create a CoreSession when transaction is marked rollback-only"
     * in some cases, at render time.
     */
    @Override
    public Object handleError(Throwable t) {
        if (t instanceof WebResourceNotFoundException || t instanceof NotFoundException) {
            return show404();
        }
        log.error(t, t);
        return showError();
    }

    protected Object showError() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("<html>");
        pw.println("<head><title>Server Error</title></head>");
        pw.println("<body>");
        pw.println("<h1>Server Error</h1>");
        pw.println("<p>An error occurred. Please retry or contact your administrator for details in logs.</p>");
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
        return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_HTML_TYPE).entity(sw.toString()).build();
    }

    protected Object show404() {
        return Response.status(Status.NOT_FOUND)
                       .type(MediaType.TEXT_HTML_TYPE)
                       .entity(Resource404.getPageContent())
                       .build();
    }

    protected void commitOrRollbackAndRestartTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    protected static SnapshotManager getSnapshotManager() {
        return Framework.getService(SnapshotManager.class);
    }

    public String getNavigationPoint() {
        String url = getContext().getURL();
        String point = null;
        if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_BUNDLEGROUPS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_BUNDLEGROUP)) {
            point = ApiBrowserConstants.LIST_BUNDLEGROUPS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_BUNDLES)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_BUNDLE)) {
            point = ApiBrowserConstants.LIST_BUNDLES;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_COMPONENTS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_COMPONENT)) {
            point = ApiBrowserConstants.LIST_COMPONENTS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_SERVICES)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_SERVICE)) {
            point = ApiBrowserConstants.LIST_SERVICES;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_EXTENSIONPOINTS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_EXTENSIONPOINT)) {
            point = ApiBrowserConstants.LIST_EXTENSIONPOINTS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_CONTRIBUTIONS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_CONTRIBUTION)) {
            point = ApiBrowserConstants.LIST_CONTRIBUTIONS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_OPERATIONS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_OPERATION)) {
            point = ApiBrowserConstants.LIST_OPERATIONS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_PACKAGES)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_PACKAGE)) {
            point = ApiBrowserConstants.LIST_PACKAGES;
        }
        if (point == null) {
            // check plugins
            List<Plugin<?>> plugins = getSnapshotManager().getPlugins();
            for (Plugin<?> plugin : plugins) {
                point = plugin.getView(url);
                if (point != null) {
                    break;
                }
            }
        }

        return point;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Object doGet() {
        return getView("index");
    }

    @Path(SnapshotManager.DISTRIBUTION_ALIAS_LATEST)
    public Resource getLatest() {
        return listPersistedDistributions().stream()
                                           .filter(snap -> snap.getName().toLowerCase().startsWith("nuxeo platform")
                                                   || snap.getAliases()
                                                          .contains(SnapshotManager.DISTRIBUTION_ALIAS_LATEST))
                                           .findFirst()
                                           .map(distribution -> ctx.newObject(RedirectResource.TYPE,
                                                   SnapshotManager.DISTRIBUTION_ALIAS_LATEST, distribution.getKey()))
                                           .orElseGet(() -> ctx.newObject(Resource404.TYPE));
    }

    @Path("{distributionId}")
    public Resource viewDistribution(@PathParam("distributionId") String distributionId) {
        if (StringUtils.isBlank(distributionId)) {
            return this;
        }
        if (isSiteMode() && RuntimeSnapshot.LIVE_ALIASES.contains(distributionId)) {
            return ctx.newObject(Resource404.TYPE);
        }

        // resolve version-like distribs based only on version names, taking only persistent distributions and not
        // including aliases
        List<DistributionSnapshot> snaps = getSnapshotManager().listPersistentSnapshots((ctx.getCoreSession()));
        if (VersionComparator.isVersion(distributionId)) {
            String finalDistributionId = distributionId;
            Optional<DistributionSnapshot> resolved = snaps.stream()
                                                           .filter(s -> s.getVersion().equals(finalDistributionId))
                                                           .findFirst();
            if (resolved.isPresent()) {
                return ctx.newObject(RedirectResource.TYPE, finalDistributionId, resolved.get().getKey());
            }
        }

        boolean showRuntimeSnapshot = showRuntimeSnapshot();
        String orgDistributionId = distributionId;
        Boolean embeddedMode = Boolean.FALSE;
        if (SnapshotManager.DISTRIBUTION_ALIAS_ADM.equals(distributionId)) {
            if (!showRuntimeSnapshot) {
                return ctx.newObject(Resource404.TYPE);
            }
            embeddedMode = Boolean.TRUE;
        } else {
            if (showRuntimeSnapshot) {
                snaps.add(getRuntimeDistribution());
            }
            distributionId = SnapshotResolverHelper.findBestMatch(snaps, distributionId);
        }
        if (StringUtils.isBlank(distributionId)) {
            return ctx.newObject(Resource404.TYPE);
        }
        if (!orgDistributionId.equals(distributionId)) {
            return ctx.newObject(RedirectResource.TYPE, orgDistributionId, distributionId);
        }

        ctx.setProperty(ApiBrowserConstants.EMBEDDED_MODE_MARKER, embeddedMode);
        DistributionSnapshot snapshot = getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession());
        ctx.setProperty(DIST, snapshot);
        ctx.setProperty(DIST_ID, distributionId);
        return ctx.newObject(ApiBrowser.TYPE, distributionId, embeddedMode, snapshot);
    }

    public List<DistributionSnapshotDesc> getAvailableDistributions() {
        return getSnapshotManager().getAvailableDistributions(ctx.getCoreSession());
    }

    public DistributionSnapshot getRuntimeDistribution() {
        return getSnapshotManager().getRuntimeSnapshot();
    }

    /**
     * Returns true if given key is shared by several instances.
     *
     * @since 20.0.0
     */
    public boolean isKeyDuplicated(String key) {
        return getSnapshotManager().getPersistentSnapshots(ctx.getCoreSession(), key, false).size() > 1;
    }

    public List<DistributionSnapshot> listPersistedDistributions() {
        return getSnapshotManager().listPersistentSnapshots(ctx.getCoreSession());
    }

    public DistributionSnapshot getCurrentDistribution() {
        return (DistributionSnapshot) ctx.getProperty(DIST);
    }

    @POST
    @Path(SAVE_ACTION)
    @Produces(MediaType.TEXT_HTML)
    public Object doSave() {
        return performSave(null, true);
    }

    /**
     * Performs save without redirecting to success/error views.
     *
     * @since 20.3.0
     */
    @POST
    @Path(SAVE_ACTION)
    @Produces(MediaType.TEXT_PLAIN)
    public Object doSaveRequest() {
        return performSave(null, false);
    }

    @POST
    @Path(SAVE_EXTENDED_ACTION)
    @Produces(MediaType.TEXT_HTML)
    public Object doSaveExtended() {
        return performSave(getSaveFilter(), true);
    }

    /**
     * Performs extended save without redirecting to success/error views.
     *
     * @since 20.3.0
     */
    @POST
    @Path(SAVE_EXTENDED_ACTION)
    @Produces(MediaType.TEXT_PLAIN)
    public Object doSaveExtendedRequest() {
        return performSave(getSaveFilter(), false);
    }

    protected SnapshotFilter getSaveFilter() {
        FormData formData = getContext().getForm();

        String bundleList = formData.getString("bundles");
        String ebundleList = formData.getString("excludedBundles");
        String javaPkgList = formData.getString("javaPackages");
        String ejavaPkgList = formData.getString("excludedJavaPackages");
        String nxPkgList = formData.getString("nuxeoPackages");
        String enxPkgList = formData.getString("excludedNuxeoPackages");

        if (StringUtils.isAllBlank(bundleList, javaPkgList, nxPkgList, ebundleList, ejavaPkgList, enxPkgList)) {
            // no actual filtering
            return null;
        }

        String distribLabel = formData.getString("name");
        boolean checkAsPrefixes = "on".equals(formData.getString("checkAsPrefixes"));
        boolean includeReferences = "on".equals(formData.getString("includeReferences"));

        PersistSnapshotFilter filter = new PersistSnapshotFilter(distribLabel, checkAsPrefixes,
                includeReferences ? TargetExtensionPointSnapshotFilter.class : null);

        if (bundleList != null) {
            Arrays.stream(bundleList.split("\n")).filter(StringUtils::isNotBlank).forEach(filter::addBundle);
        }
        if (ebundleList != null) {
            Arrays.stream(ebundleList.split("\n")).filter(StringUtils::isNotBlank).forEach(filter::addExcludedBundle);
        }
        if (javaPkgList != null) {
            Arrays.stream(javaPkgList.split("\n")).filter(StringUtils::isNotBlank).forEach(filter::addPackagesPrefix);
        }
        if (ejavaPkgList != null) {
            Arrays.stream(ejavaPkgList.split("\n"))
                  .filter(StringUtils::isNotBlank)
                  .forEach(filter::addExcludedPackagesPrefix);
        }
        if (nxPkgList != null) {
            Arrays.stream(nxPkgList.split("\n")).filter(StringUtils::isNotBlank).forEach(filter::addNuxeoPackage);
        }
        if (enxPkgList != null) {
            Arrays.stream(enxPkgList.split("\n"))
                  .filter(StringUtils::isNotBlank)
                  .forEach(filter::addExcludedNuxeoPackage);
        }

        return filter;
    }

    protected Map<String, Serializable> readUploadFormData(FormData formData) {
        Map<String, Serializable> properties = new HashMap<>();

        // Release date
        String released = formData.getString("released");
        if (StringUtils.isNotBlank(released)) {
            properties.put(DistributionSnapshot.PROP_RELEASED, RepositoryDistributionSnapshot.convertDate(released));
        }
        // Version
        String version = formData.getString("version");
        if (StringUtils.isNotBlank(version)) {
            properties.put(DistributionSnapshot.PROP_VERSION, version);
        }

        return properties;
    }

    protected Object performSave(SnapshotFilter filter, boolean redirect) {
        if (!canSave()) {
            return show404();
        }

        FormData formData = getContext().getForm();
        String source = formData.getString("source");
        Template view;
        boolean hasError = false;
        String errorMessage = null;
        try {
            getSnapshotManager().persistRuntimeSnapshot(getContext().getCoreSession(), formData.getString("name"),
                    readUploadFormData(formData), SUB_DISTRIBUTION_PATH_RESERVED, filter);
            hasError = false;
        } catch (NuxeoException e) {
            log.error("Error during storage", e);
            TransactionHelper.setTransactionRollbackOnly();
            hasError = false;
            errorMessage = e.getMessage();
        }

        commitOrRollbackAndRestartTransaction();
        if (redirect) {
            if (hasError) {
                view = getView("savedKO").arg("message", errorMessage).arg("source", source);
            } else {
                view = getView("saved").arg("source", source);
            }
            return view;
        } else {
            if (hasError) {
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                               .type(MediaType.TEXT_PLAIN)
                               .entity("Save failed: " + errorMessage)
                               .build();
            } else {
                return Response.status(Status.OK).type(MediaType.TEXT_PLAIN).entity("Save done.").build();
            }
        }

    }

    protected File getExportTmpFile() {
        File tmpFile = new File(Environment.getDefault().getTemp(), "export.zip");
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    @GET
    @Path(DOWNLOAD_ACTION + "/{distributionId}")
    public Response downloadDistrib(@PathParam("distributionId") String distribId) throws IOException {
        if (!canImportOrExportDistributions()) {
            return Response.status(Status.NOT_FOUND).build();
        }

        File tmp = getExportTmpFile();
        tmp.createNewFile();
        try (OutputStream out = new FileOutputStream(tmp)) {
            getSnapshotManager().exportSnapshot(getContext().getCoreSession(), distribId, out);
        }
        String fName = "nuxeo-distribution-" + distribId + ".zip";
        fName = fName.replace(" ", "_");
        ArchiveFile aFile = new ArchiveFile(tmp.getAbsolutePath());
        return Response.ok(aFile)
                       .header("Content-Disposition", "attachment;filename=" + fName)
                       .type("application/zip")
                       .build();
    }

    /**
     * Use to allow authorized users to upload distribution even in site mode
     *
     * @since 8.3
     */
    @GET
    @Path(VIEW_ADMIN)
    public Object getForms(
            @QueryParam(ApiBrowserConstants.SUCCESS_FEEBACK_MESSAGE_VARIABLE) String successFeedbackMessage,
            @QueryParam(ApiBrowserConstants.ERROR_FEEBACK_MESSAGE_VARIABLE) String errorFeedbackMessage) {
        NuxeoPrincipal principal = getContext().getPrincipal();
        if (SecurityHelper.canManageDistributions(principal)) {
            return getView("forms").arg(ApiBrowserConstants.SUCCESS_FEEBACK_MESSAGE_VARIABLE, successFeedbackMessage)
                                   .arg(ApiBrowserConstants.ERROR_FEEBACK_MESSAGE_VARIABLE, errorFeedbackMessage);
        }
        return show404();
    }

    @POST
    @Path(UPLOAD_ACTION)
    @Produces(MediaType.TEXT_HTML)
    public Object uploadDistrib() {
        if (!canImportOrExportDistributions()) {
            return show404();
        }
        FormData formData = getContext().getForm();
        Blob blob = formData.getFirstBlob();
        Map<String, Serializable> updateProperties = RepositoryDistributionSnapshot.getUpdateProperties(
                formData.getFormFields());

        try {
            getSnapshotManager().importSnapshot(getContext().getCoreSession(), blob.getStream(), updateProperties,
                    SUB_DISTRIBUTION_PATH_RESERVED);
        } catch (IOException | IllegalArgumentException | NuxeoException e) {
            log.error(e, e);
            TransactionHelper.setTransactionRollbackOnly();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .type(MediaType.TEXT_PLAIN)
                           .entity("Upload not done: " + e.getMessage())
                           .build();
        }

        commitOrRollbackAndRestartTransaction();
        return Response.status(Status.OK).type(MediaType.TEXT_PLAIN).entity("Upload done.").build();
    }

    @POST
    @Path(UPLOAD_TMP_ACTION)
    @Produces(MediaType.TEXT_HTML)
    public Object uploadDistribTmp() {
        if (!canImportOrExportDistributions()) {
            return show404();
        }
        FormData formData = getContext().getForm();
        Blob blob = formData.getFirstBlob();
        if (blob == null || blob.getLength() == 0) {
            return null;
        }
        Template view;
        try {
            DocumentModel snap = getSnapshotManager().importTmpSnapshot(getContext().getCoreSession(),
                    blob.getStream());
            if (snap == null) {
                view = getView("importKO").arg("message", "Unable to import archive.");
            } else {
                DistributionSnapshot snapObject = snap.getAdapter(DistributionSnapshot.class);
                view = getView("uploadEdit").arg("tmpSnap", snap).arg("snapObject", snapObject);
            }
        } catch (IOException | IllegalArgumentException | NuxeoException e) {
            TransactionHelper.setTransactionRollbackOnly();
            view = getView("importKO").arg("message", e.getMessage());
        }

        commitOrRollbackAndRestartTransaction();

        view.arg("source", formData.getString("source"));
        return view;
    }

    @POST
    @Path(UPLOAD_TMP_VALID_ACTION)
    @Produces(MediaType.TEXT_HTML)
    public Object uploadDistribTmpValid() {
        if (!canImportOrExportDistributions()) {
            return show404();
        }

        FormData formData = getContext().getForm();
        String distribDocId = formData.getFormProperty("distribDocId");
        Map<String, Serializable> updateProperties = RepositoryDistributionSnapshot.getUpdateProperties(
                formData.getFormFields());
        Template view;
        try {
            getSnapshotManager().validateImportedSnapshot(getContext().getCoreSession(), distribDocId, updateProperties,
                    SUB_DISTRIBUTION_PATH_RESERVED);
            view = getView("importDone");
        } catch (IllegalArgumentException | NuxeoException e) {
            view = getView("importKO").arg("message", e.getMessage());
            TransactionHelper.setTransactionRollbackOnly();
        }

        commitOrRollbackAndRestartTransaction();

        view.arg("source", formData.getString("source"));
        return view;
    }

    protected DistributionSnapshot getPersistedDistrib(String distribId, String distribDocId) {
        if (StringUtils.isNotBlank(distribDocId)) {
            try {
                return ctx.getCoreSession().getDocument(new IdRef(distribDocId)).getAdapter(DistributionSnapshot.class);
            } catch (DocumentNotFoundException e) {
                return null;
            }
        }
        List<DistributionSnapshot> snapshots = getSnapshotManager().getPersistentSnapshots(ctx.getCoreSession(),
                distribId, false);
        if (snapshots.size() == 1) {
            return snapshots.get(0);
        }
        if (snapshots.size() > 1) {
            log.warn(String.format("Multiple distributions with key '%s': cannot retrieve one for sure", distribId));
        }
        return null;
    }

    /**
     * Displays the distribution edit form.
     *
     * @since 20.0.0
     */
    @GET
    @Path(UPDATE_ACTION + "/{distribId}")
    @Produces(MediaType.TEXT_HTML)
    public Object updateDistribForm(@PathParam("distribId") String distribId,
            @QueryParam("distribDocId") String distribDocId) {
        return updateDistribForm(distribId, distribDocId, null, null);
    }

    protected Object updateDistribForm(String distribId, String distribDocId,
            Map<String, Serializable> updateProperties, String errorFeedbackMessage) {
        if (!showManageDistributions()) {
            return show404();
        }
        DistributionSnapshot snap = getPersistedDistrib(distribId, distribDocId);
        if (!(snap instanceof RepositoryDistributionSnapshot)) {
            return show404();
        }
        RepositoryDistributionSnapshot repoSnap = (RepositoryDistributionSnapshot) snap;
        if (updateProperties == null) {
            updateProperties = repoSnap.getUpdateProperties();
        }
        return getView("updateForm").arg("distribId", distribId)
                                    .arg("distribDocId", repoSnap.getDoc().getId())
                                    .arg("properties", updateProperties)
                                    .arg(ApiBrowserConstants.ERROR_FEEBACK_MESSAGE_VARIABLE, errorFeedbackMessage);
    }

    /**
     * Updates the distribution metadata.
     *
     * @since 20.0.0
     */
    @POST
    @Path(DO_UPDATE_ACTION)
    @Produces(MediaType.TEXT_HTML)
    public Object updateDistrib() {
        if (!showManageDistributions()) {
            return show404();
        }
        FormData formData = getContext().getForm();
        String distribId = formData.getFormProperty("distribId");
        String distribDocId = formData.getFormProperty("distribDocId");
        DistributionSnapshot snap = getPersistedDistrib(distribId, distribDocId);
        if (!(snap instanceof RepositoryDistributionSnapshot)) {
            return show404();
        }
        RepositoryDistributionSnapshot repoSnap = (RepositoryDistributionSnapshot) snap;
        Map<String, Serializable> updateProperties = RepositoryDistributionSnapshot.getUpdateProperties(
                formData.getFormFields());
        try {
            repoSnap.updateDocument(getContext().getCoreSession(), updateProperties, formData.getString("comment"),
                    SUB_DISTRIBUTION_PATH_RESERVED);
        } catch (IllegalArgumentException | DocumentValidationException e) {
            return updateDistribForm(distribId, repoSnap.getDoc().getId(), updateProperties, e.getMessage());
        }
        // will trigger retrieval of distribution again
        return redirect(URIUtils.addParametersToURIQuery(String.format("%s/%s", getPath(), VIEW_ADMIN),
                Collections.singletonMap(ApiBrowserConstants.SUCCESS_FEEBACK_MESSAGE_VARIABLE, "Update Done.")));
    }

    /**
     * Returns the view to redirect to depending on source upload page.
     *
     * @since 11.1
     */
    public String getRedirectViewPostUpload(String source) {
        if ("admin".equals(source)) {
            return VIEW_ADMIN;
        }
        return "";
    }

    @GET
    @Path(REINDEX_ACTION)
    @Produces(MediaType.TEXT_PLAIN)
    public Object reindex() {
        NuxeoPrincipal nxPrincipal = getContext().getPrincipal();
        if (!nxPrincipal.isAdministrator()) {
            return show404();
        }

        CoreSession coreSession = getContext().getCoreSession();
        String query = String.format(
                "SELECT ecm:uuid FROM Document WHERE ecm:primaryType in ('%s') AND ecm:isProxy = 0 AND ecm:isTrashed = 0",
                StringUtils.join(AttributesExtractorStater.DOC_TYPES, "','"));

        try (IterableQueryResult it = coreSession.queryAndFetch(query, NXQL.NXQL, QueryFilter.EMPTY);) {
            for (Map<String, Serializable> map : it) {
                String id = (String) map.get(NXQL.ECM_UUID);
                Work work = new ExtractXmlAttributesWorker(coreSession.getRepositoryName(), nxPrincipal.getName(), id);
                Framework.getService(WorkManager.class).schedule(work);
            }
        }

        return Response.ok().build();
    }

    public boolean isSiteMode() {
        return getSnapshotManager().isSiteMode();
    }

    public boolean isEmbeddedMode() {
        return Boolean.TRUE.equals(getContext().getProperty(ApiBrowserConstants.EMBEDDED_MODE_MARKER, Boolean.FALSE));
    }

    public boolean isEditor() {
        return !isSiteMode() && canImportOrExportDistributions();
    }

    protected boolean canSave() {
        return !isEmbeddedMode() && !isSiteMode()
                && SecurityHelper.canSnapshotLiveDistribution(getContext().getPrincipal());
    }

    protected boolean canImportOrExportDistributions() {
        return !isEmbeddedMode() && SecurityHelper.canManageDistributions(getContext().getPrincipal());
    }

    /**
     * Returns true if current user can manage ditributions.
     *
     * @since 20.0.0
     */
    public boolean showManageDistributions() {
        return canImportOrExportDistributions();
    }

    /**
     * Returns true if the current {@link RuntimeSnapshot} can be seen by user.
     *
     * @since 20.0.0
     */
    public boolean showRuntimeSnapshot() {
        return !isSiteMode() && SecurityHelper.canSnapshotLiveDistribution(getContext().getPrincipal());
    }

    /**
     * @since 20.0.0
     */
    public boolean isRunningFunctionalTests() {
        return !StringUtils.isBlank(Framework.getProperty(ApiBrowserConstants.PROPERTY_TESTER_NAME));
    }

    /**
     * Generates the list of plugins that should be displayed in the menu.
     */
    public List<Plugin<?>> getPluginMenu() {
        return getSnapshotManager().getPlugins()
                                   .stream()
                                   .filter(plugin -> !plugin.isHidden())
                                   .collect(Collectors.toList());
    }

    /**
     * Returns the webapp name (usually "nuxeo")
     *
     * @since 20.0.0
     */
    public String getWebappName() {
        return VirtualHostHelper.getWebAppName(getContext().getRequest());
    }

    /**
     * Deletes the corresponding distribution.
     *
     * @since 20.0.0
     */
    @GET
    @Path(DELETE_ACTION + "/{distribId}")
    @Produces(MediaType.TEXT_HTML)
    public Object deleteDistrib(@PathParam("distribId") String distribId,
            @QueryParam("distribDocId") String distribDocId) throws IOException {
        if (!showManageDistributions()) {
            return show404();
        }

        DistributionSnapshot snap = getPersistedDistrib(distribId, distribDocId);
        if (!(snap instanceof RepositoryDistributionSnapshot)) {
            return show404();
        }
        CoreSession session = getContext().getCoreSession();
        session.removeDocument(((RepositoryDistributionSnapshot) snap).getDoc().getRef());
        session.save();

        // will trigger retrieval of distributions again
        return redirect(URIUtils.addParametersToURIQuery(String.format("%s/%s", getPath(), VIEW_ADMIN),
                Collections.singletonMap(ApiBrowserConstants.SUCCESS_FEEBACK_MESSAGE_VARIABLE, "Deletion Done.")));
    }

    /**
     * Handles redirection to login page for anonymous user use case.
     * <p>
     * Invalidates current session, otherwise login prompt is not shown in some cases, see NXP-29634.
     *
     * @since 20.0.0
     */
    @GET
    @Path(LOGIN_ACTION)
    public Object handleLogin() throws URISyntaxException {
        Framework.getService(PluggableAuthenticationService.class).invalidateSession(request);
        Map<String, String> params = new HashMap<>();
        params.put(NXAuthConstants.FORCE_ANONYMOUS_LOGIN, "true");
        params.put(NXAuthConstants.REQUESTED_URL, getPath());
        URI uri = new URI(URIUtils.addParametersToURIQuery(NXAuthConstants.LOGIN_PAGE, params));
        return Response.seeOther(uri).build();
    }

}
