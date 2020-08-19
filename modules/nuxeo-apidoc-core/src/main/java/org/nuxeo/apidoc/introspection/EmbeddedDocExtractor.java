/*
 * (C) Copyright 2012-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.introspection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerService;
import org.nuxeo.runtime.api.Framework;

public class EmbeddedDocExtractor {

    private static final Logger log = LogManager.getLogger(EmbeddedDocExtractor.class);

    public static final String DOC_PREFIX = "doc/";

    public static final String PARENT_DOC_PREFIX = "doc-parent/";

    /**
     * Hardcoded parent readme filename for tests.
     *
     * @since 11.1
     */
    protected static final String README = "ReadMe.md";

    protected static List<Extension> MD_EXTENSIONS = Arrays.asList(TablesExtension.create());

    protected static Parser MD_PARSER = Parser.builder().extensions(MD_EXTENSIONS).build();

    protected static HtmlRenderer MD_RENDERER = HtmlRenderer.builder().extensions(MD_EXTENSIONS).build();

    /**
     * Navigates hierarchy to find target file.
     *
     * @since 11.1
     */
    public static File findFile(File jarFile, String subPath) {
        File up = new File(jarFile, "..");
        File file = new File(up, subPath);
        if (!file.exists()) {
            file = new File(new File(up, ".."), subPath);
            if (!file.exists()) {
                file = null;
            }
        }
        return file;
    }

    public static void extractEmbeddedDoc(ZipFile jarFile, BundleInfoImpl bi) throws IOException {
        Enumeration<? extends ZipEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            int isReadme = isReadme(entry.getName());
            if (isReadme > 0) {
                Blob content = null;
                String name = new Path(entry.getName()).lastSegment();
                try (InputStream is = jarFile.getInputStream(entry)) {
                    content = getReadme(name, is);
                }
                if (isReadme == 1) {
                    bi.setReadme(content);
                } else {
                    bi.setParentReadme(content);
                }
            }
            if (bi.getReadme() != null && bi.getParentReadme() != null) {
                break;
            }
        }
    }

    /**
     * Mimicks extaction of readme file for tests.
     *
     * @implNote maven is in charge of copying doc files to the target server: this needs to be done here for tests to
     *           still take readmes into account in Eclipse for instance.
     * @since 11.1
     */
    public static void extractEmbeddedDoc(File dirFile, BundleInfoImpl bi) throws IOException {
        File readme = findFile(dirFile, README);
        if (readme != null) {
            try (InputStream is = new FileInputStream(readme)) {
                bi.setReadme(getReadme(README, is));
            }
        }
        File parentReadme = findFile(new File(dirFile, "../.."), README);
        if (parentReadme != null) {
            try (InputStream is = new FileInputStream(parentReadme)) {
                bi.setParentReadme(getReadme(README, is));
            }
        }
    }

    /**
     * Returns 0 if not a doc, 1 if local doc, 2 if parent doc.
     *
     * @implNote kept in compliance with maven-resources-plugin configuration on nuxeo global maven pom.
     * @since 11.1
     */
    public static int isReadme(String name) {
        if (name == null) {
            return 0;
        }
        boolean isDoc = name.startsWith(DOC_PREFIX);
        boolean isParentDoc = name.startsWith(PARENT_DOC_PREFIX);
        if (isDoc || isParentDoc) {
            Path path = new Path(name);
            String filename = path.lastSegment();
            if ("md".equals(path.getFileExtension()) && filename.length() >= 6
                    && filename.substring(0, 6).equalsIgnoreCase("readme")) {
                return isDoc ? 1 : 2;
            }
        }
        return 0;
    }

    protected static Blob getReadme(String name, InputStream is) throws IOException {
        String content = IOUtils.toString(is, StandardCharsets.UTF_8);
        return new StringBlob(content, "text/plain", StandardCharsets.UTF_8.name(), name);
    }

    /**
     * Returns Markdown ReadMe content, converted to HTML.
     *
     * @since 20.0.0
     */
    public static String getHtmlFromMarkdown(Blob readme) {
        if (readme != null) {
            try {
                return getHtmlFromMarkdown(readme.getString());
            } catch (IOException e) {
                log.error(e, e);
            }
        }
        return null;
    }

    public static String getHtmlFromMarkdown(String md) {
        if (StringUtils.isNotBlank(md)) {
            return Framework.getService(HtmlSanitizerService.class)
                            .sanitizeString(MD_RENDERER.render(MD_PARSER.parse(md)), null);
        }
        return null;
    }

}
