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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.introspection;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.documentation.SecureXMLHelper;
import org.nuxeo.apidoc.snapshot.SnapshotListener;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.JarUtils;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The entry point to the server runtime introspection To build a description of the current running server you need to
 * create a {@link ServerInfo} object using the method {@link #build(String, String)}.
 * <p>
 * Example
 *
 * <pre>
 * ServerInfo info = ServerInfo.build();
 * </pre>
 *
 * The server name and version will be fetched from the runtime properties: {@link Environment#DISTRIBUTION_NAME} and
 * {@link Environment#DISTRIBUTION_VERSION} If you want to use another name and version just call
 * {@link #build(String, String)} instead to build your server information.
 * <p>
 * After building a <code>ServerInfo</code> object you can start browsing the bundles deployed on the server by calling
 * {@link #getBundles()} or fetch a specific bundle given its symbolic name {@link #getBundle(String)}.
 * <p>
 * Example:
 *
 * <pre>
 * ServerInfo info = ServerInfo.build();
 * BundleInfo binfo =info.getBundle("org.nuxeo.runtime");
 * System.out.println("Bundle Id: "+binfo.getBundleId());
 * System.out.println("File Name: "+binfo.getFileName());
 * System.out.println("Manifest: "+ binfo.getManifest());
 * for (ComponentInfo cinfo : binfo.getComponents()) {
 *   System.out.println("Component: "+cinfo.getName());
 *   System.out.println(cinfo.getDocumentation());
 *   // find extension points provided by this component
 *   for (ExtensionPointInfo xpi : cinfo.getExtensionPoints()) {
 *     System.out.println("Extension point: "+xpi.getName());
 *     System.out.println("Accepted contribution classes: "+Arrays.asList(xpi.getTypes()));
 *     // find contributed extensions to this extension point:
 *
 *   }
 *   // find contribution provided by this component
 *   for (ExtensionInfo xi : cinfo.getExtensions()) {
 *      System.out.println("Extension: "+xi.getId()+" to "+xi.getExtensionPoint());
 *      System.out.println(xi.getDocumentation());
 *      ...
 *   }
 * }
 * </pre>
 */
public class ServerInfo {

    private static final Logger log = LogManager.getLogger(ServerInfo.class);

    public static final String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF";

    public static final String POM_XML = "pom.xml";

    public static final String POM_PROPERTIES = "pom.properties";

    protected static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    protected static final XPathFactory xpathFactory = XPathFactory.newInstance();

    protected final String name;

    protected final String version;

    protected final Map<String, BundleInfo> bundles = new HashMap<>();

    protected final Map<String, PackageInfoImpl> packages = new HashMap<>();

    protected final List<Class<?>> allSpi = new ArrayList<>();

    public ServerInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public ServerInfo(@JsonProperty("name") String name, @JsonProperty("version") String version,
            @JsonProperty("bundles") List<BundleInfo> bundles) {
        this(name, version);
        if (bundles != null) {
            bundles.forEach(bundle -> this.bundles.put(bundle.getBundleId(), bundle));
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public List<BundleInfo> getBundles() {
        // order by name for deterministic processing
        return bundles.values().stream().sorted(Comparator.comparing(BundleInfo::getId)).collect(Collectors.toList());
    }

    public void addBundle(BundleInfo bundle) {
        bundles.put(bundle.getId(), bundle);
    }

    public void addBundle(List<BundleInfo> someBundles) {
        for (BundleInfo bundle : someBundles) {
            addBundle(bundle);
        }
    }

    public BundleInfo getBundle(String id) {
        return bundles.get(id);
    }

    /**
     * Registers package by name.
     *
     * @implNote Uses name instead of id as only one given version of the package should be installed at any time.
     * @since 11.1
     */
    public void addPackage(PackageInfoImpl pkg) {
        packages.put(pkg.getName(), pkg);
    }

    /**
     * Returns registered packages.
     *
     * @since 11.1
     */
    public List<PackageInfo> getPackages() {
        return packages.values().stream().sorted(Comparator.comparing(PackageInfo::getId)).collect(Collectors.toList());
    }

    public static ServerInfo build() {
        return build(Framework.getProperty(Environment.DISTRIBUTION_NAME, "Nuxeo"),
                Framework.getProperty(Environment.DISTRIBUTION_VERSION, "unknown"));

    }

    /**
     * Retrieves standard package info and parses contained jars to extract bundle name.
     *
     * @implNote: code partly copy/pasted from {@link DeploymentPreprocessor#processManifest} internal method;
     * @since 11.1
     */
    protected static PackageInfoImpl computePackageInfo(LocalPackage pkg, Map<String, List<LocalPackage>> pkgByBundle) {
        for (File jar : FileUtils.listFiles(pkg.getData().getRoot(), new String[] { "jar" }, true)) {
            Manifest mf = JarUtils.getManifest(jar);
            if (mf != null) {
                Attributes attrs = mf.getMainAttributes();
                String id = attrs.getValue("Bundle-SymbolicName");
                if (id == null) {
                    continue;
                }
                int p = id.indexOf(';');
                if (p > -1) { // remove properties part if any
                    id = id.substring(0, p);
                }
                pkgByBundle.computeIfAbsent(id, k -> new ArrayList<>()).add(pkg);
            }
        }
        return new PackageInfoImpl(pkg.getId(), pkg.getName(), pkg.getVersion().toString(), pkg.getTitle(),
                pkg.getType().toString(), computeDependencies(pkg.getDependencies()),
                computeDependencies(pkg.getOptionalDependencies()), computeDependencies(pkg.getConflicts()));
    }

    protected static List<String> computeDependencies(PackageDependency[] deps) {
        return Arrays.stream(deps).map(PackageDependency::toString).collect(Collectors.toList());
    }

    protected static BundleInfoImpl computeBundleInfo(Bundle bundle, Map<String, List<LocalPackage>> pkgByBundle) {
        RuntimeService runtime = Framework.getRuntime();
        BundleInfoImpl binfo = new BundleInfoImpl(bundle.getSymbolicName());
        binfo.setFileName(runtime.getBundleFile(bundle).getName());
        binfo.setLocation(bundle.getLocation());

        if (!(bundle instanceof BundleImpl)) {
            return binfo;
        }
        BundleImpl nxBundle = (BundleImpl) bundle;
        File jarFile = nxBundle.getBundleFile().getFile();
        if (jarFile == null) {
            return binfo;
        }
        try {
            if (jarFile.isDirectory()) {
                // directory: run from Eclipse in unit tests
                // .../nuxeo-runtime/nuxeo-runtime/bin
                // or sometimes
                // .../nuxeo-runtime/nuxeo-runtime/bin/main
                File manifest = new File(jarFile, META_INF_MANIFEST_MF);
                if (manifest.exists()) {
                    InputStream is = new FileInputStream(manifest);
                    String mf = IOUtils.toString(is, StandardCharsets.UTF_8);
                    binfo.setManifest(mf);
                    binfo.setRequirements(getBundleRequires(mf));
                }
                // find and parse pom.xml
                File pom = EmbeddedDocExtractor.findFile(jarFile, POM_XML);
                if (pom != null) {
                    DocumentBuilder b = documentBuilderFactory.newDocumentBuilder();
                    Document doc = b.parse(new FileInputStream(pom));
                    XPath xpath = xpathFactory.newXPath();
                    String groupId = (String) xpath.evaluate("//project/groupId", doc, XPathConstants.STRING);
                    if ("".equals(groupId)) {
                        groupId = (String) xpath.evaluate("//project/parent/groupId", doc, XPathConstants.STRING);
                    }
                    String artifactId = (String) xpath.evaluate("//project/artifactId", doc, XPathConstants.STRING);
                    if ("".equals(artifactId)) {
                        artifactId = (String) xpath.evaluate("//project/parent/artifactId", doc, XPathConstants.STRING);
                    }
                    String version = (String) xpath.evaluate("//project/version", doc, XPathConstants.STRING);
                    if ("".equals(version)) {
                        version = (String) xpath.evaluate("//project/parent/version", doc, XPathConstants.STRING);
                    }
                    binfo.setArtifactId(artifactId);
                    binfo.setGroupId(groupId);
                    binfo.setArtifactVersion(version);
                }
                // find READMEs to mimick maven behavior in eclipse tests
                EmbeddedDocExtractor.extractEmbeddedDoc(jarFile, binfo);
            } else {
                try (ZipFile zFile = new ZipFile(jarFile)) {
                    ZipEntry mfEntry = zFile.getEntry(META_INF_MANIFEST_MF);
                    if (mfEntry != null) {
                        try (InputStream mfStream = zFile.getInputStream(mfEntry)) {
                            String mf = IOUtils.toString(mfStream, StandardCharsets.UTF_8);
                            binfo.setManifest(mf);
                            binfo.setRequirements(getBundleRequires(mf));
                        }

                    }
                    Enumeration<? extends ZipEntry> entries = zFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(POM_PROPERTIES)) {
                            try (InputStream is = zFile.getInputStream(entry)) {
                                PropertyResourceBundle prb = new PropertyResourceBundle(is);
                                String groupId = prb.getString("groupId");
                                String artifactId = prb.getString("artifactId");
                                String version = prb.getString("version");
                                binfo.setArtifactId(artifactId);
                                binfo.setGroupId(groupId);
                                binfo.setArtifactVersion(version);
                            }
                            break;
                        }
                    }
                }
                try (ZipFile zFile = new ZipFile(jarFile)) {
                    EmbeddedDocExtractor.extractEmbeddedDoc(zFile, binfo);
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException | XPathException | NuxeoException e) {
            log.error(e, e);
        }

        if (pkgByBundle.containsKey(binfo.getId())) {
            List<String> packages = pkgByBundle.get(binfo.getId())
                                               .stream()
                                               .map(LocalPackage::getName)
                                               .collect(Collectors.toList());
            binfo.setPackages(packages);
        }
        return binfo;
    }

    /**
     * Retrieve bundle name from manifest.
     *
     * @implNote: code copy/pasted from {@link DeploymentPreprocessor#processManifest} internal method.
     * @since 11.1
     */
    protected static String getBundleName(String mforig) throws IOException {
        if (mforig == null) {
            return null;
        }
        Manifest mf = new Manifest(new ByteArrayInputStream(mforig.getBytes()));
        Attributes attrs = mf.getMainAttributes();
        String id = attrs.getValue("Bundle-SymbolicName");
        int p = id.indexOf(';');
        if (p > -1) { // remove properties part if any
            id = id.substring(0, p);
        }
        return id;
    }

    /**
     * Retrieve bundle requirements.
     *
     * @implNote: code copy/pasted from {@link DeploymentPreprocessor#processManifest} internal method.
     * @since 11.1
     */
    protected static List<String> getBundleRequires(String mforig) throws IOException {
        List<String> res = new ArrayList<>();
        if (mforig == null) {
            return res;
        }
        Manifest mf = new Manifest(new ByteArrayInputStream(mforig.getBytes()));
        Attributes attrs = mf.getMainAttributes();
        String requires = attrs.getValue("Nuxeo-Require");
        if (requires == null) {
            // if not specific requirement is met, fallback on 'Require-Bundle'
            requires = attrs.getValue("Require-Bundle");
        }
        if (requires != null) {
            String[] ids = StringUtils.split(requires, ',', true);
            for (int i = 0; i < ids.length; i++) {
                String rid = ids[i];
                int p = rid.indexOf(';');
                if (p > -1) { // remove properties part if any
                    ids[i] = rid.substring(0, p);
                }
                res.add(ids[i]);
            }
        }
        return res;
    }

    protected static List<Class<?>> getSPI(Class<?> klass) {
        List<Class<?>> spi = new ArrayList<>();
        for (Field field : klass.getDeclaredFields()) {
            String cName = field.getType().getCanonicalName();
            if (cName.startsWith("org.nuxeo")) {
                // remove XObjects
                Class<?> fieldClass = field.getType();
                Annotation[] annotations = fieldClass.getDeclaredAnnotations();
                if (annotations.length == 0) {
                    spi.add(fieldClass);
                }
            }
        }
        return spi;
    }

    public static ServerInfo build(String name, String version) {
        RuntimeService runtime = Framework.getRuntime();
        ServerInfo server = new ServerInfo(name, version);
        BundleInfoImpl configVirtualBundle = new BundleInfoImpl(BundleInfo.RUNTIME_CONFIG_BUNDLE);
        server.addBundle(configVirtualBundle);

        // get package link with bundles
        Map<String, List<LocalPackage>> pkgByBundle = new HashMap<>();
        PackageManager pman = Framework.getService(PackageManager.class);
        PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
        if (pman != null) {
            List<DownloadablePackage> installedPackages = pman.listInstalledPackages();
            installedPackages.stream().map(DownloadablePackage::getId).map(packId -> {
                try {
                    return pus.getPackage(packId);
                } catch (PackageException e) {
                    return null;
                }
            }).filter(Objects::nonNull).forEach(pkg -> {
                server.addPackage(computePackageInfo(pkg, pkgByBundle));
            });
        }

        Map<String, ExtensionPointInfoImpl> xpRegistry = new HashMap<>();
        List<ExtensionInfoImpl> contribRegistry = new ArrayList<>();

        SnapshotListener snapshotListener = Framework.getService(SnapshotListener.class);
        // This list is ordered by resolution order (including component requirements): we can deduce bundle range
        // ordering from it, depending on contained registrations. It does not account for unresolved registrations that
        // should be handled separately.
        Collection<ComponentName> registrations = runtime.getComponentManager().getResolvedRegistrations();
        long resolutionOrder = 0;
        for (ComponentName cname : registrations) {
            RegistrationInfo ri = runtime.getComponentManager().getRegistrationInfo(cname);
            Bundle bundle = ri.getContext().getBundle();
            BundleInfoImpl binfo = null;

            if (bundle == null) {
                binfo = configVirtualBundle;
            } else {
                String symName = bundle.getSymbolicName();
                if (symName == null) {
                    log.error("No symbolic name found for bundle " + cname);
                    continue;
                }
                // avoids duplicating/overriding the bundles
                if (server.bundles.containsKey(bundle.getSymbolicName())) {
                    binfo = (BundleInfoImpl) server.bundles.get(bundle.getSymbolicName());
                } else {
                    binfo = computeBundleInfo(bundle, pkgByBundle);
                }
            }

            ComponentInfoImpl component = new ComponentInfoImpl(binfo, cname.getName());
            component.setResolutionOrder(resolutionOrder++);
            // set additional orders from snapshot listener
            component.setDeclaredStartOrder(snapshotListener.getDeclaredStartOrder(cname.getName()));
            component.setStartOrder(snapshotListener.getStartOrder(cname.getName()));
            component.setAliases(ri.getAliases().stream().map(ComponentName::getName).collect(Collectors.toList()));

            if (ri.getExtensionPoints() != null) {
                for (ExtensionPoint xp : ri.getExtensionPoints()) {
                    ExtensionPointInfoImpl xpinfo = new ExtensionPointInfoImpl(component, xp.getName());
                    Class<?>[] ctypes = xp.getContributions();
                    String[] descriptors = new String[ctypes.length];

                    for (int i = 0; i < ctypes.length; i++) {
                        descriptors[i] = ctypes[i].getCanonicalName();
                        List<Class<?>> spi = getSPI(ctypes[i]);
                        xpinfo.addSpi(spi);
                        server.allSpi.addAll(spi);
                    }
                    xpinfo.setDescriptors(descriptors);
                    xpinfo.setDocumentation(xp.getDocumentation());
                    xpinfo.setAliases(ri.getAliases()
                                        .stream()
                                        .map(ComponentName::getName)
                                        .map(a -> ExtensionPointInfo.computeId(a, xp.getName()))
                                        .collect(Collectors.toList()));
                    xpRegistry.put(xpinfo.getId(), xpinfo);
                    xpinfo.getAliases().forEach(a -> xpRegistry.put(a, xpinfo));
                    component.addExtensionPoint(xpinfo);
                }
            }

            component.setXmlFileUrl(ri.getXmlFileUrl());

            if (ri.getProvidedServiceNames() != null) {
                for (String serviceName : ri.getProvidedServiceNames()) {
                    component.addService(serviceName, isServiceOverriden(ri, serviceName));
                }
            }

            if (ri.getExtensions() != null) {
                Map<String, AtomicLong> comps = new HashMap<>();
                for (Extension xt : ri.getExtensions()) {
                    // handle multiple contributions to the same extension point
                    String id = xt.getExtensionPoint();
                    comps.computeIfAbsent(id, k -> new AtomicLong(-1)).incrementAndGet();
                    ExtensionInfoImpl xtinfo = new ExtensionInfoImpl(component, xt.getExtensionPoint(),
                            comps.get(id).get());
                    xtinfo.setTargetComponentName(xt.getTargetComponent());
                    xtinfo.setDocumentation(xt.getDocumentation());
                    xtinfo.setXml(SecureXMLHelper.secure(xt.toXML()));
                    // set additional order from snapshot listener
                    xtinfo.setRegistrationOrder(snapshotListener.getExtensionRegistrationOrder(xtinfo.getId()));

                    contribRegistry.add(xtinfo);

                    component.addExtension(xtinfo);
                }
            }

            component.setComponentClass(ri.getImplementation());
            component.setDocumentation(ri.getDocumentation());

            ri.getRequiredComponents().forEach(req -> component.addRequirement(req.getName()));

            binfo.addComponent(component);
            server.addBundle(binfo);
        }

        // post process all bundles to:
        // - register bundles that contain no components
        // - set the bundle min and max resolution orders as held by the runtime context
        // - try to match the bundle to a package
        Bundle[] allbundles = runtime.getContext().getBundle().getBundleContext().getBundles();
        for (Bundle bundle : allbundles) {
            BundleInfo bi;
            if (!server.bundles.containsKey(bundle.getSymbolicName())) {
                bi = computeBundleInfo(bundle, pkgByBundle);
                server.addBundle(bi);
            } else {
                bi = server.bundles.get(bundle.getSymbolicName());
            }
            List<ComponentInfo> components = bi.getComponents();
            components.stream()
                      .mapToLong(ComponentInfo::getResolutionOrder)
                      .min()
                      .ifPresent(min -> bi.setMinResolutionOrder(min));
            components.stream()
                      .mapToLong(ComponentInfo::getResolutionOrder)
                      .max()
                      .ifPresent(max -> bi.setMaxResolutionOrder(max));
            if (!bi.getPackages().isEmpty()) {
                bi.getPackages().forEach(pkgName -> server.packages.get(pkgName).addBundle(bi));
            }
        }

        // associate contrib to XP
        for (ExtensionInfoImpl contrib : contribRegistry) {
            String xp = contrib.getExtensionPoint();
            ExtensionPointInfoImpl ep = xpRegistry.get(xp);
            if (ep != null) {
                ep.addExtension(contrib);
            }
        }

        return server;
    }

    protected static boolean isServiceOverriden(RegistrationInfo ri, String serviceName) {
        try {
            Class<?> typeof = Class.forName(serviceName);
            final Object adapter = ri.getComponent().getAdapter(typeof);
            final Object service = Framework.getService(typeof);
            if (adapter == service) {
                return false;
            }
            return service.getClass() != adapter.getClass();
        } catch (ClassNotFoundException cause) {
            return false;
        } catch (NullPointerException cause) {
            return false;
        }
    }

    public List<Class<?>> getAllSpi() {
        return allSpi;
    }

}
