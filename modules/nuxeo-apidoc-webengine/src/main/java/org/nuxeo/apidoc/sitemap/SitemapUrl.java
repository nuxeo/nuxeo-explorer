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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Represents a sitemap URL.
 *
 * @since 20.3.0
 */
@XmlRootElement
@XmlType(propOrder = { "loc", "lastmod", "changefreq", "priority" })
public class SitemapUrl {

    @XmlElement
    protected final String loc;

    @XmlElement
    @XmlJavaTypeAdapter(DateAdapter.class)
    protected final LocalDate lastmod;

    @XmlElement
    protected final String changefreq;

    @XmlElement
    protected final String priority;

    public SitemapUrl() {
        this(null, null, null, null);
    }

    public SitemapUrl(String loc, LocalDate lastmod, String changefreq, String priority) {
        this.loc = loc;
        this.lastmod = lastmod;
        this.changefreq = changefreq;
        this.priority = priority;
    }

    public static class DateAdapter extends XmlAdapter<String, LocalDate> {
        @Override
        public LocalDate unmarshal(String v) throws Exception {
            return LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
        }

        @Override
        public String marshal(LocalDate v) throws Exception {
            return v == null ? null : v.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }
}
