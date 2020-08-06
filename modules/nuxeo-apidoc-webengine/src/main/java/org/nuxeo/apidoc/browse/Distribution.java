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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

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
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.snapshot.SnapshotResolverHelper;
import org.nuxeo.apidoc.worker.ExtractXmlAttributesWorker;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
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
    public static final String JSON_ACTION = "json";

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

    /**
     * List of subviews, used for validation of distribution names and aliases.
     *
     * @since 20.0.0
     */
    protected static final List<String> SUB_DISTRIBUTION_PATH_RESERVED = List.of(VIEW_ADMIN, SAVE_ACTION,
            SAVE_EXTENDED_ACTION, JSON_ACTION, DOWNLOAD_ACTION, UPDATE_ACTION, DO_UPDATE_ACTION, DELETE_ACTION,
            UPLOAD_ACTION, UPLOAD_TMP_ACTION, UPLOAD_TMP_VALID_ACTION, REINDEX_ACTION);

    protected static final Pattern VERSION_REGEX = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-.*)?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public Object handleError(Throwable t) {
        if (t instanceof WebResourceNotFoundException || t instanceof NotFoundException) {
            return show404();
        }
        log.error(t, t);
        return Response.status(500).entity(getTemplate("views/error/error.ftl")).type("text/html").build();
    }

    /**
     * Displays a customized 404 page.
     *
     * @since 20.0.0
     */
    public Object show404() {
        return Response.status(404).entity(getTemplate("views/error/error_404.ftl")).type("text/html").build();
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
    @Produces("text/html")
    public Object doGet() {
        return getView("index").arg("hideNav", Boolean.TRUE);
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

        List<DistributionSnapshot> snaps = getSnapshotManager().listPersistentSnapshots((ctx.getCoreSession()));
        if (distributionId.matches(VERSION_REGEX.toString())) {
            String finalDistributionId = distributionId;
            return snaps.stream()
                        .filter(s -> s.getVersion().equals(finalDistributionId))
                        .findFirst()
                        .map(distribution -> ctx.newObject(RedirectResource.TYPE, finalDistributionId,
                                distribution.getKey()))
                        .orElseGet(() -> ctx.newObject(Resource404.TYPE));
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
        ctx.setProperty(DIST, getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()));
        ctx.setProperty(DIST_ID, distributionId);
        return ctx.newObject(ApiBrowser.TYPE, distributionId, embeddedMode);
    }

    public List<DistributionSnapshotDesc> getAvailableDistributions() {
        return getSnapshotManager().getAvailableDistributions(ctx.getCoreSession());
    }

    public DistributionSnapshot getRuntimeDistribution() {
        return getSnapshotManager().getRuntimeSnapshot();
    }

    public List<DistributionSnapshot> listPersistedDistributions() {
        SnapshotManager sm = getSnapshotManager();
        return sm.listPersistentSnapshots(ctx.getCoreSession()).stream().sorted((o1, o2) -> {
            Matcher m1 = VERSION_REGEX.matcher(o1.getVersion());
            Matcher m2 = VERSION_REGEX.matcher(o2.getVersion());

            if (m1.matches() && m2.matches()) {
                for (int i = 0; i < 3; i++) {
                    String s1 = m1.group(i + 1);
                    int c1 = s1 != null ? Integer.parseInt(s1) : 0;
                    String s2 = m2.group(i + 1);
                    int c2 = s2 != null ? Integer.parseInt(s2) : 0;

                    if (c1 != c2 || i == 2) {
                        return Integer.compare(c2, c1);
                    }
                }
            }
            log.info(String.format("Comparing version using String between %s - %s", o1.getVersion(), o2.getVersion()));
            return o2.getVersion().compareTo(o1.getVersion());
        }).collect(Collectors.toList());
    }

    public Map<String, DistributionSnapshot> getPersistedDistributions() {
        return getSnapshotManager().getPersistentSnapshots(ctx.getCoreSession());
    }

    public DistributionSnapshot getCurrentDistribution() {
        return (DistributionSnapshot) ctx.getProperty(DIST);
    }

    @POST
    @Path(SAVE_ACTION)
    @Produces("text/html")
    public Object doSave() throws NamingException, NotSupportedException, SystemException, RollbackException,
            HeuristicMixedException, HeuristicRollbackException, ParseException {
        return performSave(null);
    }

    @POST
    @Path(SAVE_EXTENDED_ACTION)
    @Produces("text/html")
    public Object doSaveExtended() throws NamingException, NotSupportedException, SystemException, SecurityException,
            RollbackException, HeuristicMixedException, HeuristicRollbackException {
        FormData formData = getContext().getForm();

        String distribLabel = formData.getString("name");
        String bundleList = formData.getString("bundles");
        String javaPkgList = formData.getString("javaPackages");
        String nxPkgList = formData.getString("nxPackages");
        PersistSnapshotFilter filter = new PersistSnapshotFilter(distribLabel);

        if (bundleList != null) {
            Arrays.stream(bundleList.split("\n"))
                  .filter(StringUtils::isNotBlank)
                  .forEach(bid -> filter.addBundlePrefix(bid));
        }
        if (javaPkgList != null) {
            Arrays.stream(javaPkgList.split("\n"))
                  .filter(StringUtils::isNotBlank)
                  .forEach(pkg -> filter.addPackagesPrefix(pkg));
        }
        if (nxPkgList != null) {
            Arrays.stream(nxPkgList.split("\n"))
                  .filter(StringUtils::isNotBlank)
                  .forEach(pkg -> filter.addNuxeoPackagePrefix(pkg));
        }

        return performSave(filter);
    }

    protected Map<String, Serializable> readUploadFormData(FormData formData) {
        Map<String, Serializable> properties = new HashMap<>();

        // Release date
        String released = formData.getString("released");
        if (StringUtils.isNotBlank(released)) {
            properties.put(DistributionSnapshot.PROP_RELEASED, RepositoryDistributionSnapshot.convertDate(released));
        }

        return properties;
    }

    protected Object performSave(PersistSnapshotFilter filter) throws NamingException, NotSupportedException,
            SystemException, SecurityException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        if (!canSave()) {
            return show404();
        }

        boolean startedTx = false;
        UserTransaction tx = TransactionHelper.lookupUserTransaction();
        if (tx != null && !TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            tx.begin();
            startedTx = true;
        }

        FormData formData = getContext().getForm();
        String source = formData.getString("source");
        try {
            getSnapshotManager().persistRuntimeSnapshot(getContext().getCoreSession(), formData.getString("name"),
                    readUploadFormData(formData), filter);
        } catch (NuxeoException e) {
            log.error("Error during storage", e);
            if (tx != null) {
                tx.rollback();
            }
            return getView("savedKO").arg("message", e.getMessage()).arg("source", source);
        }

        if (tx != null && startedTx) {
            tx.commit();
        }
        return getView("saved").arg("source", source);
    }

    /**
     * Returns the runtime snapshot json export.
     *
     * @since 11.1
     */
    @GET
    @Path(JSON_ACTION)
    @Produces("application/json")
    public Object getJson() throws IOException {
        if (!showRuntimeSnapshot()) {
            return show404();
        }
        // init potential resources depending on request
        getSnapshotManager().initWebContext(getContext().getRequest());
        DistributionSnapshot snap = getRuntimeDistribution();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        snap.writeJson(out, null, null);
        return out.toString();
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
            return Response.status(404).build();
        }

        File tmp = getExportTmpFile();
        tmp.createNewFile();
        OutputStream out = new FileOutputStream(tmp);
        getSnapshotManager().exportSnapshot(getContext().getCoreSession(), distribId, out);
        out.close();
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
            return getView("forms").arg("hideNav", Boolean.TRUE)
                                   .arg(ApiBrowserConstants.SUCCESS_FEEBACK_MESSAGE_VARIABLE, successFeedbackMessage)
                                   .arg(ApiBrowserConstants.ERROR_FEEBACK_MESSAGE_VARIABLE, errorFeedbackMessage);
        }
        return show404();
    }

    @POST
    @Path(UPLOAD_ACTION)
    @Produces("text/html")
    public Object uploadDistrib() throws IOException {
        if (!canImportOrExportDistributions()) {
            return show404();
        }
        FormData formData = getContext().getForm();
        Blob blob = formData.getFirstBlob();
        String source = formData.getString("source");

        try {
            getSnapshotManager().importSnapshot(getContext().getCoreSession(), blob.getStream());
        } catch (IOException | IllegalArgumentException | NuxeoException e) {
            return getView("importKO").arg("message", e.getMessage()).arg("source", source);
        }

        return getView(getRedirectViewPostUpload(source));
    }

    @POST
    @Path(UPLOAD_TMP_ACTION)
    @Produces("text/html")
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
            view = getView("importKO").arg("message", e.getMessage());
        }

        view.arg("source", formData.getString("source"));
        return view;
    }

    @POST
    @Path(UPLOAD_TMP_VALID_ACTION)
    @Produces("text/html")
    public Object uploadDistribTmpValid() {
        if (!canImportOrExportDistributions()) {
            return show404();
        }

        FormData formData = getContext().getForm();
        String name = formData.getString("name");
        String version = formData.getString("version");
        String pathSegment = formData.getString("pathSegment");
        String title = formData.getString("title");

        Template view;
        try {
            getSnapshotManager().validateImportedSnapshot(getContext().getCoreSession(), name, version, pathSegment,
                    title);
            view = getView("importDone");
        } catch (IllegalArgumentException | NuxeoException e) {
            view = getView("importKO").arg("message", e.getMessage());
        }

        view.arg("source", formData.getString("source"));
        return view;
    }

    /**
     * Displays the distribution edit form.
     *
     * @since 20.0.0
     */
    @GET
    @Path(UPDATE_ACTION + "/{distributionId}")
    @Produces("text/html")
    public Object updateDistribForm(@PathParam("distributionId") String distribId) {
        return updateDistribForm(distribId, null, null);
    }

    protected Object updateDistribForm(String distribId, Map<String, String> updateProperties,
            String errorFeedbackMessage) {
        if (!showManageDistributions()) {
            return show404();
        }
        DistributionSnapshot snap = getSnapshotManager().getSnapshot(distribId, getContext().getCoreSession());
        if (snap == null || snap.isLive() || !(snap instanceof RepositoryDistributionSnapshot)) {
            return show404();
        }
        RepositoryDistributionSnapshot repoSnap = (RepositoryDistributionSnapshot) snap;
        if (updateProperties == null) {
            updateProperties = repoSnap.getUpdateProperties();
        }
        return getView("updateForm").arg("distribId", distribId)
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
    @Produces("text/html")
    public Object updateDistrib() {
        if (!showManageDistributions()) {
            return show404();
        }
        FormData formData = getContext().getForm();
        String distribId = formData.getFormProperty("distribId");
        DistributionSnapshot snap = getSnapshotManager().getSnapshot(distribId, getContext().getCoreSession());
        if (snap == null || snap.isLive() || !(snap instanceof RepositoryDistributionSnapshot)) {
            return show404();
        }
        RepositoryDistributionSnapshot repoSnap = (RepositoryDistributionSnapshot) snap;
        Map<String, String> updateProperties = repoSnap.getUpdateProperties(formData.getFormFields());
        try {
            repoSnap.updateDocument(getContext().getCoreSession(), updateProperties, formData.getString("comment"),
                    SUB_DISTRIBUTION_PATH_RESERVED);
        } catch (DocumentValidationException e) {
            return updateDistribForm(distribId, updateProperties, e.getMessage());
        }
        // will trigger retrieval of distribution again
        return redirect(URIUtils.addParametersToURIQuery(String.format("%s/%s", getPath(), VIEW_ADMIN),
                Map.of(ApiBrowserConstants.SUCCESS_FEEBACK_MESSAGE_VARIABLE, "Update Done.")));
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
    @Produces("text/plain")
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
    @Path(DELETE_ACTION + "/{distributionId}")
    @Produces("text/html")
    public Object deleteDistrib(@PathParam("distributionId") String distribId) throws IOException {
        if (!showManageDistributions()) {
            return show404();
        }

        CoreSession session = getContext().getCoreSession();
        RepositoryDistributionSnapshot snapshot = (RepositoryDistributionSnapshot) getSnapshotManager().getSnapshot(
                distribId, session);
        session.removeDocument(snapshot.getDoc().getRef());
        session.save();

        // will trigger retrieval of distributions again
        return redirect(URIUtils.addParametersToURIQuery(String.format("%s/%s", getPath(), VIEW_ADMIN),
                Map.of(ApiBrowserConstants.SUCCESS_FEEBACK_MESSAGE_VARIABLE, "Deletion Done.")));
    }

}
