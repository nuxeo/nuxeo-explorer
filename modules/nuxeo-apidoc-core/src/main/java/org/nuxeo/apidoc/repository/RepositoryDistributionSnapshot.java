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
package org.nuxeo.apidoc.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.adapters.BaseNuxeoArtifactDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.JsonMapper;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class RepositoryDistributionSnapshot extends BaseNuxeoArtifactDocAdapter implements DistributionSnapshot {

    private static final Logger log = LogManager.getLogger(RepositoryDistributionSnapshot.class);

    protected JavaDocHelper jdocHelper = null;

    public static RepositoryDistributionSnapshot create(DistributionSnapshot distrib, CoreSession session,
            String containerPath, String label, Map<String, Serializable> properties, List<String> reservedKeys)
            throws DocumentValidationException {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        String name = computeDocumentName(distrib.getKey());
        if (label != null) {
            name = computeDocumentName(label);
        }
        String version = distrib.getVersion();

        // Set first properties passed by parameter to not override default
        // behavior
        if (properties != null) {
            properties.forEach(doc::setPropertyValue);
            if (properties.containsKey(PROP_VERSION)) {
                version = (String) properties.get(PROP_VERSION);
            }
        }

        doc.setPathInfo(containerPath, name);
        if (StringUtils.isBlank(label)) {
            doc.setPropertyValue(TITLE_PROPERTY_PATH, distrib.getKey());
            doc.setPropertyValue(PROP_KEY, distrib.getKey());
            doc.setPropertyValue(PROP_NAME, distrib.getName());
        } else {
            doc.setPropertyValue(TITLE_PROPERTY_PATH, label);
            doc.setPropertyValue(PROP_KEY, label + "-" + version);
            doc.setPropertyValue(PROP_NAME, label);
        }
        doc.setPropertyValue(PROP_LATEST_FT, distrib.isLatestFT());
        doc.setPropertyValue(PROP_LATEST_LTS, distrib.isLatestLTS());
        doc.setPropertyValue(PROP_VERSION, version);

        fillContextData(doc);
        return new RepositoryDistributionSnapshot(session.createDocument(doc));
    }

    public static List<DistributionSnapshot> readPersistentSnapshots(CoreSession session) {
        String query = String.format("SELECT * FROM %s where %s AND %s", TYPE_NAME, QueryHelper.NOT_DELETED,
                QueryHelper.NOT_VERSION);
        DocumentModelList docs = query(session, query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(DistributionSnapshot.class))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    /**
     * Returns a list of sorted distribution with given key, maybe including aliases.
     *
     * @since 20.0.0
     */
    public static List<DistributionSnapshot> readPersistentSnapshots(CoreSession session, String key,
            boolean includeAliases) {
        String query = String.format("SELECT * FROM %s where %s AND %s", TYPE_NAME, QueryHelper.NOT_DELETED,
                QueryHelper.NOT_VERSION);
        String escapedKey = NXQL.escapeString(key);
        if (includeAliases) {
            query += String.format(" AND (%s = %s OR %s = %s)", DistributionSnapshot.PROP_KEY, escapedKey,
                    DistributionSnapshot.PROP_ALIASES, escapedKey);
        } else {
            query += String.format(" AND %s = %s", DistributionSnapshot.PROP_KEY, escapedKey);
        }
        query += " ORDER BY " + NXQL.ECM_UUID;
        DocumentModelList docs = query(session, query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(DistributionSnapshot.class))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    public RepositoryDistributionSnapshot(DocumentModel doc) {
        super(doc);
    }

    protected <T> List<T> getChildren(Class<T> adapter, String docType, String sort) {
        String query = QueryHelper.select(docType, doc, sort);
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.stream().map(doc -> doc.getAdapter(adapter)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected <T> T getChild(Class<T> adapter, String docType, String idField, String id) {
        String query = QueryHelper.select(docType, doc, idField, id);
        DocumentModelList docs = query(getCoreSession(), query);
        if (docs.isEmpty()) {
            log.debug(String.format("Unable to find %s with id '%s'", docType, id));
            return null;
        } else if (docs.size() == 1) {
            return docs.get(0).getAdapter(adapter);
        } else {
            log.error(String.format("Multiple match for %s with id '%s'", docType, id));
            return docs.get(0).getAdapter(adapter);
        }
    }

    @Override
    public BundleInfo getBundle(String id) {
        return getChild(BundleInfo.class, BundleInfo.TYPE_NAME, BundleInfo.PROP_BUNDLE_ID, id);
    }

    @Override
    public BundleGroup getBundleGroup(String groupId) {
        if (groupId == null) {
            return null;
        }
        if (groupId.startsWith(BundleGroup.PREFIX)) {
            return getChild(BundleGroup.class, BundleGroup.TYPE_NAME, BundleGroup.PROP_KEY, groupId);
        } else {
            return getChild(BundleGroup.class, BundleGroup.TYPE_NAME, BundleGroup.PROP_GROUP_NAME, groupId);
        }
    }

    protected DocumentModel getBundleContainer() {
        DocumentRef ref = new PathRef(doc.getPathAsString(), SnapshotPersister.Bundle_Root_NAME);
        if (getCoreSession().exists(ref)) {
            return getCoreSession().getDocument(ref);
        } else {
            // for compatibility with the previous persistence model
            return doc;
        }
    }

    @Override
    public List<BundleGroup> getBundleGroups() {
        String query = QueryHelper.select(BundleGroup.TYPE_NAME, doc, NXQL.ECM_PARENTID, getBundleContainer().getId());
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(BundleGroup.class))
                   .filter(Objects::nonNull)
                   .sorted(Comparator.comparing(BundleGroup::getId))
                   .collect(Collectors.toList());
    }

    @Override
    public List<BundleInfo> getBundles() {
        return getChildren(BundleInfo.class, BundleInfo.TYPE_NAME, NXQL.ECM_POS);
    }

    @Override
    public List<String> getBundleIds() {
        return queryAndFetchIds(getCoreSession(), BundleInfo.PROP_BUNDLE_ID, BundleInfo.TYPE_NAME, doc,
                BundleInfo.PROP_BUNDLE_ID);
    }

    @Override
    public ComponentInfo getComponent(String id) {
        ComponentInfo c = getChild(ComponentInfo.class, ComponentInfo.TYPE_NAME, ComponentInfo.PROP_COMPONENT_ID, id);
        if (c == null) {
            // try with an alias
            c = getChild(ComponentInfo.class, ComponentInfo.TYPE_NAME, ComponentInfo.PROP_ALIASES, id);
        }
        return c;
    }

    @Override
    public List<String> getComponentIds() {
        return queryAndFetchIds(getCoreSession(), ComponentInfo.PROP_COMPONENT_ID, ComponentInfo.TYPE_NAME, doc,
                ComponentInfo.PROP_COMPONENT_ID);
    }

    @Override
    public List<ComponentInfo> getComponents() {
        return getChildren(ComponentInfo.class, ComponentInfo.TYPE_NAME, ComponentInfo.PROP_COMPONENT_ID);
    }

    @Override
    public ExtensionInfo getContribution(String id) {
        return getChild(ExtensionInfo.class, ExtensionInfo.TYPE_NAME, ExtensionInfo.PROP_CONTRIB_ID, id);
    }

    @Override
    public List<String> getContributionIds() {
        return queryAndFetchIds(getCoreSession(), ExtensionInfo.PROP_CONTRIB_ID, ExtensionInfo.TYPE_NAME, doc,
                ExtensionInfo.PROP_CONTRIB_ID);
    }

    @Override
    public List<ExtensionInfo> getContributions() {
        return getChildren(ExtensionInfo.class, ExtensionInfo.TYPE_NAME, NXQL.ECM_POS);
    }

    @Override
    public ExtensionPointInfo getExtensionPoint(String id) {
        ExtensionPointInfo xp = getChild(ExtensionPointInfo.class, ExtensionPointInfo.TYPE_NAME,
                ExtensionPointInfo.PROP_EP_ID, id);
        if (xp == null) {
            // try with an alias
            xp = getChild(ExtensionPointInfo.class, ExtensionPointInfo.TYPE_NAME, ExtensionPointInfo.PROP_EP_ALIASES,
                    id);
        }
        return xp;
    }

    @Override
    public List<String> getExtensionPointIds() {
        return queryAndFetchIds(getCoreSession(), ExtensionPointInfo.PROP_EP_ID, ExtensionPointInfo.TYPE_NAME, doc,
                ExtensionPointInfo.PROP_EP_ID);
    }

    public List<String> getBundleGroupIds() {
        return queryAndFetchIds(getCoreSession(), BundleGroup.PROP_KEY, BundleGroup.TYPE_NAME, doc,
                BundleGroup.PROP_KEY);
    }

    @Override
    public List<String> getServiceIds() {
        return queryAndFetchIds(getCoreSession(), ServiceInfo.PROP_CLASS_NAME, ServiceInfo.TYPE_NAME, doc,
                ServiceInfo.PROP_CLASS_NAME);
    }

    @Override
    public String getName() {
        return safeGet(PROP_NAME, "!unknown!");
    }

    @Override
    public String getVersion() {
        return safeGet(PROP_VERSION, "!unknown!");
    }

    @Override
    public String getKey() {
        return safeGet(PROP_KEY, "!unknown!");
    }

    @Override
    public String getId() {
        return getKey();
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public ServiceInfo getService(String id) {
        // Select only not overridden ticket and old imported NXService without overridden value
        String query = String.format("%s AND (%s = 0 OR %s is NULL)",
                QueryHelper.select(ServiceInfo.TYPE_NAME, getDoc(), ServiceInfo.PROP_CLASS_NAME, id),
                ServiceInfo.PROP_OVERRIDEN, ServiceInfo.PROP_OVERRIDEN);
        DocumentModelList docs = query(getCoreSession(), query);
        if (docs.size() == 0) {
            return null;
        }
        if (docs.size() > 1) {
            throw new AssertionError("Multiple services found for " + id);
        }
        return docs.get(0).getAdapter(ServiceInfo.class);
    }

    protected List<String> getComponentIds(boolean isXML) {
        String query = String.format("%s AND %s = %s ORDER BY %s", QueryHelper.select(ComponentInfo.TYPE_NAME, doc),
                ComponentInfo.PROP_IS_XML, isXML ? 1 : 0, ComponentInfo.PROP_COMPONENT_NAME);
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(ComponentInfo.class))
                   .filter(Objects::nonNull)
                   .map(NuxeoArtifact::getId)
                   .collect(Collectors.toList());
    }

    @Override
    public Date getCreationDate() {
        Calendar cal = safeGet(PROP_CREATED);
        return cal == null ? null : cal.getTime();
    }

    @Override
    public Date getReleaseDate() {
        Calendar cal = safeGet(PROP_RELEASED);
        return cal == null ? getCreationDate() : cal.getTime();
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public OperationInfo getOperation(String id) {
        if (id.startsWith(OperationInfo.ARTIFACT_PREFIX)) {
            id = id.substring(OperationInfo.ARTIFACT_PREFIX.length());
        }
        String query = String.format("%s OR %s = %s",
                QueryHelper.select(OperationInfo.TYPE_NAME, getDoc(), OperationInfo.PROP_NAME, id),
                OperationInfo.PROP_ALIASES, NXQL.escapeString(id));
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.isEmpty() ? null : docs.get(0).getAdapter(OperationInfo.class);
    }

    @Override
    public List<OperationInfo> getOperations() {
        String query = QueryHelper.select(OperationInfo.TYPE_NAME, getDoc(), NXQL.ECM_POS);
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(OperationInfo.class))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    @Override
    public PackageInfo getPackage(String name) {
        String query = QueryHelper.select(PackageInfo.TYPE_NAME, getDoc(), PackageInfo.PROP_PACKAGE_NAME, name);
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.isEmpty() ? null : docs.get(0).getAdapter(PackageInfo.class);
    }

    @Override
    public List<PackageInfo> getPackages() {
        String query = QueryHelper.select(PackageInfo.TYPE_NAME, getDoc(), PackageInfo.PROP_PACKAGE_ID);
        DocumentModelList docs = query(getCoreSession(), query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(PackageInfo.class))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    public JavaDocHelper getJavaDocHelper() {
        if (jdocHelper == null) {
            jdocHelper = JavaDocHelper.getHelper(getName(), getVersion());
        }
        return jdocHelper;
    }

    @Override
    public void cleanPreviousArtifacts() {
        String query = QueryHelper.select("Document", getDoc());
        DocumentModelList docs = query(getCoreSession(), query);
        getCoreSession().removeDocuments(docs.stream().map(doc -> doc.getRef()).toArray(size -> new DocumentRef[size]));
    }

    @Override
    public boolean isLatestFT() {
        return safeGet(PROP_LATEST_FT);
    }

    @Override
    public boolean isLatestLTS() {
        return safeGet(PROP_LATEST_LTS);
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = safeGet(PROP_ALIASES);
        if (isLatestLTS() && !aliases.contains(SnapshotManager.DISTRIBUTION_ALIAS_LATEST_LTS)) {
            aliases.add(SnapshotManager.DISTRIBUTION_ALIAS_LATEST_LTS);
        }
        if (isLatestFT() && !aliases.contains(SnapshotManager.DISTRIBUTION_ALIAS_LATEST_FT)) {
            aliases.add(SnapshotManager.DISTRIBUTION_ALIAS_LATEST_FT);
        }
        return aliases;
    }

    @Override
    public boolean isHidden() {
        return Boolean.TRUE.equals(safeGet(PROP_HIDE));
    }

    protected ObjectMapper getJsonMapper(boolean read, SnapshotFilter filter) {
        ObjectMapper mapper;
        if (read) {
            mapper = JsonMapper.basic(null, null);
        } else {
            SnapshotFilter refFilter = null;
            if (filter != null && filter.getReferenceClass() != null) {
                // resolve bundle selection to get reference filter
                var selectedBundles = new ArrayList<NuxeoArtifact>();
                getBundles().stream().filter(filter::accept).forEach(selectedBundles::add);
                try {
                    Constructor<? extends SnapshotFilter> constructor = filter.getReferenceClass()
                                                                              .getConstructor(String.class, List.class);
                    refFilter = constructor.newInstance(filter.getName() + SnapshotFilter.REFERENCE_FILTER_NAME_SUFFIX,
                            selectedBundles);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
            mapper = JsonMapper.persisted(filter, refFilter);
        }
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (Plugin<?> plugin : Framework.getService(SnapshotManager.class).getPlugins()) {
            mapper = plugin.enrishJsonMapper(mapper);
        }
        return mapper;
    }

    @Override
    public void writeJson(OutputStream out, SnapshotFilter filter, PrettyPrinter printer) {
        ObjectWriter writer = getJsonMapper(false, filter).writerFor(DistributionSnapshot.class)
                                                          .withoutRootName()
                                                          .with(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
                                                          .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        if (printer != null) {
            writer = writer.with(printer);
        }
        try {
            writer.writeValue(out, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DistributionSnapshot readJson(InputStream in) {
        ObjectReader reader = getJsonMapper(true, null).readerFor(DistributionSnapshot.class)
                                                       .without(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                                                       .with(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        try {
            return reader.readValue(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, PluginSnapshot<?>> getPluginSnapshots() {
        return Framework.getService(SnapshotManager.class)
                        .getPlugins()
                        .stream()
                        .collect(Collectors.toMap(Plugin::getId, p -> p.getRepositorySnapshot(getDoc())));
    }

    /**
     * Returns a key/value map of properties for update.
     *
     * @since 20.0.0
     */
    public Map<String, Serializable> getUpdateProperties() {
        Map<String, Serializable> props = new HashMap<>();
        List.of(TITLE_PROPERTY_PATH, PROP_NAME, PROP_VERSION, PROP_KEY).forEach(p -> props.put(p, safeGet(p)));
        if (StringUtils.isBlank((String) props.get(TITLE_PROPERTY_PATH))
                && StringUtils.isNotBlank((String) props.get(PROP_NAME))) {
            props.put(TITLE_PROPERTY_PATH, props.get(PROP_NAME));
        }
        List.of(PROP_LATEST_LTS, PROP_LATEST_FT, PROP_HIDE)
            .forEach(p -> props.put(p, String.valueOf(doc.getPropertyValue(p))));
        Date releaseDate = getReleaseDate();
        props.put(PROP_RELEASED, releaseDate == null ? null : new SimpleDateFormat("yyyy-MM-dd").format(releaseDate));
        List<String> aliases = safeGet(PROP_ALIASES);
        props.put(PROP_ALIASES, String.join("\n", aliases));
        return props;
    }

    /**
     * Returns a key/value map of properties for update from request properties.
     *
     * @since 20.0.0
     */
    public static Map<String, Serializable> getUpdateProperties(Map<String, String[]> formFields) {
        Map<String, Serializable> props = new HashMap<>();
        if (formFields != null) {
            Stream.of(TITLE_PROPERTY_PATH, PROP_NAME, PROP_VERSION, PROP_KEY, PROP_RELEASED, PROP_ALIASES)
                  .filter(formFields::containsKey)
                  .forEach(p -> props.put(p, formFields.get(p)[0]));
            if (StringUtils.isBlank((String) props.get(TITLE_PROPERTY_PATH))
                    && StringUtils.isNotBlank((String) props.get(PROP_NAME))) {
                props.put(TITLE_PROPERTY_PATH, props.get(PROP_NAME));
            }
            // checkboxes will fill the request (with value "on"), only when checked
            List.of(PROP_LATEST_LTS, PROP_LATEST_FT, PROP_HIDE)
                .forEach(p -> props.put(p, Boolean.toString(formFields.containsKey(p))));
        }
        return props;
    }

    /**
     * Updates the distribution document metadata.
     *
     * @since 20.0.0
     */
    public DocumentModel updateDocument(CoreSession session, Map<String, Serializable> updateProperties, String comment,
            List<String> reservedKeys) throws DocumentValidationException {
        final DocumentModel doc = getDoc();
        if (updateProperties == null) {
            return doc;
        }
        // validations
        if (Stream.of(TITLE_PROPERTY_PATH, PROP_NAME, PROP_VERSION, PROP_KEY)
                  .anyMatch(p -> updateProperties.containsKey(p)
                          && StringUtils.isBlank((String) updateProperties.get(p)))) {
            throw new DocumentValidationException("Please fill all required fields.");
        }
        if (updateProperties.containsKey(PROP_KEY)) {
            validateKeyOrAlias((String) updateProperties.get(PROP_KEY), reservedKeys);
        }
        List<String> aliases = null;
        if (updateProperties.containsKey(PROP_ALIASES)) {
            aliases = Arrays.stream(((String) updateProperties.get(PROP_ALIASES)).split("\n"))
                            .map(String::trim)
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.toList());
            aliases.forEach(alias -> validateKeyOrAlias(alias, reservedKeys));
        }
        // updates
        Stream.of(TITLE_PROPERTY_PATH, PROP_NAME, PROP_VERSION, PROP_KEY, PROP_LATEST_LTS, PROP_LATEST_FT, PROP_HIDE)
              .filter(updateProperties::containsKey)
              .forEach(p -> doc.setPropertyValue(p, updateProperties.get(p)));
        List.of(PROP_LATEST_LTS, PROP_LATEST_FT, PROP_HIDE)
            .forEach(p -> doc.setPropertyValue(p, updateProperties.get(p)));
        if (updateProperties.containsKey(PROP_RELEASED)) {
            doc.setPropertyValue(DistributionSnapshot.PROP_RELEASED,
                    convertDate((String) updateProperties.get(PROP_RELEASED)));
        }
        if (aliases != null) {
            doc.setPropertyValue(PROP_ALIASES, (Serializable) aliases);
        }
        if (!StringUtils.isBlank(comment)) {
            doc.putContextData("comment", comment);
        }
        fillContextData(doc);
        DocumentModel updatedDoc = session.saveDocument(doc);
        session.save();
        return updatedDoc;
    }

    protected void validateKeyOrAlias(String keyOrAlias, List<String> reservedKeys) throws DocumentValidationException {
        var forbidden = new ArrayList<>(List.of(
                // reserved for live distrib
                SnapshotManager.DISTRIBUTION_ALIAS_CURRENT, SnapshotManager.DISTRIBUTION_ALIAS_ADM,
                // added automatically
                SnapshotManager.DISTRIBUTION_ALIAS_LATEST_FT, SnapshotManager.DISTRIBUTION_ALIAS_LATEST_LTS));
        if (reservedKeys != null) {
            forbidden.addAll(reservedKeys);
        }
        if (forbidden.contains(keyOrAlias)
                || (keyOrAlias != null && keyOrAlias.startsWith(SnapshotManager.CUSTOM_VIEW_PREFIX))) {
            throw new DocumentValidationException(
                    String.format("Distribution key or alias is reserved: '%s'", keyOrAlias));
        }
    }

    /**
     * Converts string date from request to a date that can be stored.
     *
     * @since 20.0.0
     */
    public static final Date convertDate(String date) {
        if (StringUtils.isNotBlank(date)) {
            LocalDate ldate = LocalDate.parse(date);
            Instant instant = ldate.atStartOfDay().atZone(ZoneOffset.UTC).toInstant();
            return Date.from(instant);
        }
        return null;
    }

}
