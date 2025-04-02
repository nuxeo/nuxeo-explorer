/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.functionaltests.explorer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.HTTP_STATUS_NO_CONTENT_CHECKER;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.HTTP_STATUS_OK_CHECKER;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.MANAGER_USERNAME;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.READER_USERNAME;
import static org.nuxeo.functionaltests.explorer.ExplorerTestConstants.TEST_PASSWORD;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.repository.SnapshotPersister;
import org.nuxeo.apidoc.security.SecurityHelper;
import org.nuxeo.common.test.ModuleUnderTest;
import org.nuxeo.ecm.core.io.impl.DWord;
import org.nuxeo.functionaltests.HtmlPageHandler;
import org.nuxeo.functionaltests.RestTestRule;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.UploadDistribTmpPage;
import org.nuxeo.http.test.HttpClientTestRule;

/**
 * @since 2025.0
 */
public class ExplorerTestRule implements TestRule {

    public static final String SAMPLE_EXPORT_DISTRIBUTION_NAME = "Nuxeo Platform Site Mode";

    public static final String SAMPLE_EXPORT_DISTRIBUTION_VERSION = "1.0.1";

    protected final TemporaryFolder folder = new TemporaryFolder(new File(ModuleUnderTest.getOutputDirectory()));

    protected final HttpClientTestRule adminHttpClient = HttpClientTestRule.builder()
                                                                           .adminCredentials()
                                                                           .accept(MediaType.TEXT_HTML)
                                                                           .build();

    protected final RestTestRule restHelper = new RestTestRule();

    protected boolean hasPersistedDistribution;

    // test rule methods part

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            public void evaluate() throws Throwable {
                starting();
                var errors = new ArrayList<Throwable>();
                try {
                    base.evaluate();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    try {
                        finished();
                    } catch (Throwable t) {
                        errors.add(t);
                    }
                }
                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    public void starting() throws Throwable {
        folder.create();
        adminHttpClient.starting();
        restHelper.starting();
    }

    public void finished() throws Throwable {
        if (hasPersistedDistribution) {
            cleanupPersistedDistributions();
        }
        restHelper.finished();
        adminHttpClient.finished();
        folder.delete();
    }

    // helper methods part

    /**
     * Since 20.0.0, the live distrib can only be seen by admins
     */
    public String persistLiveDistribution(String distributionName, PersistOption... optionsVarArg) {
        var options = Set.of(optionsVarArg);
        var adminPage = adminHttpClient.buildGetRequest(DistribAdminPage.URL)
                                       .execute(new HtmlPageHandler<>(
                                               DistribAdminPage.builder().savePresence(true)::build));
        String liveVersion = adminPage.getLiveVersion();

        boolean partial = options.contains(PersistOption.PARTIAL);
        adminHttpClient.buildPostRequest(partial ? DistribAdminPage.SAVE_EXTENDED_URL : DistribAdminPage.SAVE_URL)
                       .entity(Map.of("name", distributionName, "bundles", "org.nuxeo.apidoc"))
                       .execute(HTTP_STATUS_OK_CHECKER);
        hasPersistedDistribution = true;
        return liveVersion;
    }

    public void cleanupPersistedDistributions() {
        adminHttpClient.buildDeleteRequest("/api/v1/path" + SnapshotPersister.Root_PATH + SnapshotPersister.Root_NAME)
                       .accept(MediaType.APPLICATION_JSON)
                       .execute(HTTP_STATUS_NO_CONTENT_CHECKER);
    }

    public void importSampleExportDistribution() throws IOException {
        var sampleExportDistribution = createSampleZip();
        importDistributionFromDistribAdminPage(adminHttpClient, SAMPLE_EXPORT_DISTRIBUTION_NAME,
                SAMPLE_EXPORT_DISTRIBUTION_VERSION, sampleExportDistribution);
    }

    protected File createSampleZip() throws IOException {
        String sourceDirPath = "data/sample_export";
        File zip = new File(folder.getRoot(), "distrib-apidoc.zip");
        FileUtils.deleteQuietly(zip);
        Path p = Files.createFile(Paths.get(zip.getPath()));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            ZipEntry zipEntry = new ZipEntry(".nuxeo-archive");
            zs.putNextEntry(zipEntry);
            zs.closeEntry();
            // read paths from reference file as NuxeoArchiveReader requires a given order and extra info
            Path epath = Paths.get(sourceDirPath, "entries.txt");
            List<String> lines;
            try (InputStream stream = getReferenceStream(epath)) {
                lines = IOUtils.readLines(stream, UTF_8);
            }
            for (Iterator<String> lineIter = lines.iterator(); lineIter.hasNext();) {
                String path = lineIter.next();
                if (StringUtils.isEmpty(path)) {
                    continue;
                }
                ZipEntry entry = new ZipEntry(path);
                if (path.endsWith("/")) {
                    // directory entry case
                    entry.setExtra(new DWord(Integer.valueOf(lineIter.next())).getBytes());
                    zs.putNextEntry(entry);
                } else {
                    zs.putNextEntry(entry);
                    Path ppath = Paths.get(sourceDirPath, path);
                    try (InputStream stream = getReferenceStream(ppath)) {
                        IOUtils.copy(stream, zs);
                    }
                }
                zs.closeEntry();
            }
        }
        return zip;
    }

    public void importDistributionFromDistribAdminPage(HttpClientTestRule httpClient, String distributionName,
            String distributionVersion, File file) {
        var distribAdminPage = httpClient.buildGetRequest(DistribAdminPage.URL)
                                         .execute(new HtmlPageHandler<>(DistribAdminPage.builder()::build));
        try {
            var uploadDistribTmpPage = distribAdminPage.getUploadFragment()
                                                       .buildFormRequest(httpClient, file)
                                                       .execute(new HtmlPageHandler<>(
                                                               UploadDistribTmpPage.builder()::build));
            uploadDistribTmpPage.getUploadConfirmFragment()
                                .buildFormRequest(httpClient, distributionName, distributionVersion)
                                .execute(HTTP_STATUS_OK_CHECKER);
        } finally {
            hasPersistedDistribution = true;
        }
    }

    public void createManagerUser() {
        if (!restHelper.groupExists(SecurityHelper.DEFAULT_APIDOC_MANAGERS_GROUP)) {
            restHelper.createGroup(SecurityHelper.DEFAULT_APIDOC_MANAGERS_GROUP, "Apidoc Managers");
        }
        if (!restHelper.userExists(MANAGER_USERNAME)) {
            restHelper.createUser(MANAGER_USERNAME, TEST_PASSWORD, null, null, null, null,
                    SecurityHelper.DEFAULT_APIDOC_MANAGERS_GROUP);
        }
    }

    public void createReaderUser() {
        if (!restHelper.userExists(READER_USERNAME)) {
            restHelper.createUser(READER_USERNAME, TEST_PASSWORD);
        }
    }

    // static methods part

    public static String getArtifactURL(String distributionId, String type, String id) {
        return ExplorerHomePage.URL + '/' + distributionId + '/' + ApiBrowserConstants.getArtifactView(type) + '/' + id;
    }

    public static InputStream getReferenceStream(Path path) throws IOException {
        return getReferenceStream(path.toString());
    }

    /**
     * @since 20.0.0
     */
    public static InputStream getReferenceStream(String path) throws IOException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Reference file not found at " + path);
        }
        return stream;
    }

    /**
     * Available options when persisting distribution.
     */
    public enum PersistOption {

        PARTIAL, //
        INCLUDE_REFERENCES, //

    }
}
