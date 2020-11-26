/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.sitemap;

import static org.nuxeo.apidoc.browse.ApiBrowserConstants.LIST_BUNDLES;
import static org.nuxeo.apidoc.browse.ApiBrowserConstants.LIST_COMPONENTS;
import static org.nuxeo.apidoc.browse.ApiBrowserConstants.LIST_CONTRIBUTIONS;
import static org.nuxeo.apidoc.browse.ApiBrowserConstants.LIST_EXTENSIONPOINTS;
import static org.nuxeo.apidoc.browse.ApiBrowserConstants.LIST_OPERATIONS;
import static org.nuxeo.apidoc.browse.ApiBrowserConstants.LIST_PACKAGES;
import static org.nuxeo.apidoc.browse.ApiBrowserConstants.LIST_SERVICES;

import java.io.Writer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

/**
 * Generates a sitemap for available distributions.
 *
 * @since 20.3.0
 */
public class SitemapGenerator {

    protected static final List<String> PATHS = List.of("", LIST_BUNDLES, LIST_COMPONENTS, LIST_CONTRIBUTIONS,
            LIST_EXTENSIONPOINTS, LIST_OPERATIONS, LIST_PACKAGES, LIST_SERVICES);

    public static void generateXML(String baseUrl, CoreSession session, Writer writer) throws JAXBException {
        Sitemap map = getSiteMap(baseUrl, session);
        JAXBContext context = JAXBContext.newInstance(Sitemap.class);
        Marshaller mar = context.createMarshaller();
        mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        mar.marshal(map, writer);
    }

    protected static Sitemap getSiteMap(String baseUrl, CoreSession session) {
        List<SitemapUrl> distribUrls = Framework.getService(SnapshotManager.class)
                                                .getAvailableDistributions(session)
                                                .stream()
                                                .flatMap(s -> streamUrls(baseUrl, s))
                                                .collect(Collectors.toList());
        LocalDate lastmod = null;
        if (!distribUrls.isEmpty()) {
            lastmod = distribUrls.get(0).lastmod;
        }
        // add home page
        distribUrls.add(0, new SitemapUrl(baseUrl, lastmod, null, null));
        return new Sitemap(distribUrls);
    }

    protected static Stream<SitemapUrl> streamUrls(String baseUrl, DistributionSnapshot snapshot) {
        Date date = ObjectUtils.firstNonNull(snapshot.getReleaseDate(), snapshot.getCreationDate());
        LocalDate lDate = date != null ? LocalDate.ofInstant(date.toInstant(), ZoneOffset.UTC) : null;
        String encodedDistName = snapshot.isLive() ? SnapshotManager.DISTRIBUTION_ALIAS_CURRENT
                : URIUtils.quoteURIPathComponent(snapshot.getKey(), true);
        return PATHS.stream()
                    .map(p -> getUrl(baseUrl, encodedDistName, p))
                    .map(url -> new SitemapUrl(url, lDate, null, null));
    }

    protected static String getUrl(String baseUrl, String encodedDistName, String... fragments) {
        StringBuilder builder = new StringBuilder();
        builder.append(baseUrl);
        if (!baseUrl.endsWith("/")) {
            builder.append("/");
        }
        builder.append(encodedDistName);
        builder.append("/");
        if (fragments != null) {
            builder.append(StringUtils.join(
                    Arrays.stream(fragments).map(f -> URIUtils.quoteURIPathComponent(f, true)).toArray(), "/"));
        }
        return builder.toString();
    }

}
