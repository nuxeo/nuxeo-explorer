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
package org.nuxeo.apidoc.webengine.test.sitemap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.sitemap.SitemapGenerator;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since TODO
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
public class TestSitemapGenerator {

    @RuntimeService
    @Mock
    protected SnapshotManager snapshotManager;

    protected Date getDate(int year, int month, int dayOfMonth) {
        GregorianCalendar d = new GregorianCalendar(year, month, dayOfMonth);
        d.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC.getId()));
        return d.getTime();
    }

    @Before
    public void mockDistributions() {
        RuntimeSnapshot mockLive = mock(RuntimeSnapshot.class);
        when(mockLive.getReleaseDate()).thenReturn(getDate(2020, Calendar.NOVEMBER, 26));
        when(mockLive.isLive()).thenReturn(true);
        RepositoryDistributionSnapshot mockPersisted1 = mock(RepositoryDistributionSnapshot.class);
        when(mockPersisted1.getKey()).thenReturn("Nuxeo Platform-11.3");
        when(mockPersisted1.getCreationDate()).thenReturn(getDate(2020, Calendar.OCTOBER, 7));
        RepositoryDistributionSnapshot mockPersisted2 = mock(RepositoryDistributionSnapshot.class);
        when(mockPersisted2.getKey()).thenReturn("Nuxeo Platform LTS 2019-10.10");
        when(mockPersisted2.getReleaseDate()).thenReturn(getDate(2019, Calendar.JANUARY, 20));

        List<DistributionSnapshot> dists = Stream.of(mockLive, mockPersisted1, mockPersisted2)
                                                 .collect(Collectors.toList());
        when(snapshotManager.getAvailableDistributions(any(CoreSession.class))).thenReturn(dists);
    }

    @Test
    public void testSitemap() throws IOException, JAXBException {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource("sitemap.xml");
        String ref = FileUtils.readFileToString(new File(org.nuxeo.common.utils.FileUtils.getFilePathFromUrl(fileUrl)),
                StandardCharsets.UTF_8).trim();
        StringWriter sw = new StringWriter();
        SitemapGenerator.generateXML("https://explorer.nuxeo.com/nuxeo/site/distribution", null, sw);
        assertEquals(ref, sw.toString().trim());
    }

}
