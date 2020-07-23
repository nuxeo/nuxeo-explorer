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
package org.nuxeo.apidoc.export.graphs.introspection;

import java.nio.charset.StandardCharsets;

import org.nuxeo.apidoc.export.api.Export;
import org.nuxeo.apidoc.export.graphs.api.GraphExport;
import org.nuxeo.apidoc.export.graphs.api.NodeFilter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Graph implementation supporting edition logics, but does not handle the content generation.
 *
 * @since 20.0.0
 */
public class ContentGraphExport extends AbstractGraphExport implements Export {

    protected String content;

    protected String contentType;

    protected String contentName;

    public ContentGraphExport() {
        this(null, null, null);
    }

    public ContentGraphExport(String content, String contentType, String contentName) {
        super();
        this.content = content;
        this.contentType = contentType;
        this.contentName = contentName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentName() {
        return contentName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    public void init(GraphExport graph) {
        update(graph.getName(), graph.getTitle(), graph.getDescription(), graph.getType());
    }

    public void update(String name, String title, String description, String type) {
        setName(name);
        setTitle(title);
        setDescription(description);
        setType(type);
    }

    public void updateContent(String content, String contentType, String contentName) {
        setContent(content);
        setContentType(contentType);
        setContentName(contentName);
    }

    public void updateContent(GraphExport graph) {
        // NOOP
    }

    @Override
    public <T extends GraphExport> T copy(Class<T> targetClass, NodeFilter nodeFilter)
            throws ReflectiveOperationException {
        T copy = super.copy(targetClass, nodeFilter);
        if (copy instanceof ContentGraphExport) {
            ((ContentGraphExport) copy).updateContent(this);
        }
        return copy;
    }

    @Override
    @JsonIgnore
    public Blob getBlob() {
        Blob blob = Blobs.createBlob(content);
        blob.setFilename(contentName);
        blob.setMimeType(contentType);
        blob.setEncoding(StandardCharsets.UTF_8.name());
        return blob;
    }

}
