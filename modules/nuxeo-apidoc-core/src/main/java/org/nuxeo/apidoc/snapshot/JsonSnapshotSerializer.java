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
package org.nuxeo.apidoc.snapshot;

import java.io.IOException;

import org.nuxeo.apidoc.api.NuxeoArtifact;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Custom serializer to handle {@link SnapshotFilter} at serialization time.
 *
 * @since 20.0.0
 */
public class JsonSnapshotSerializer extends JsonSerializer<NuxeoArtifact> {

    protected final JsonSerializer<Object> defaultSerializer;

    protected final SnapshotFilter filter;

    public JsonSnapshotSerializer(JsonSerializer<Object> serializer, SnapshotFilter filter) {
        defaultSerializer = serializer;
        this.filter = filter;
    }

    protected boolean doFilter(Object value) {
        return (filter != null && value instanceof NuxeoArtifact && !filter.accept((NuxeoArtifact) value));
    }

    @Override
    public void serialize(NuxeoArtifact value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (doFilter(value)) {
            return;
        }
        if (value != null) {
            // still try to serialize with type, lost here for some reason
            SerializationConfig config = provider.getConfig();
            JavaType baseType = config.getTypeFactory().constructType(value.getClass());
            baseType = provider.constructType(value.getClass());
            defaultSerializer.serializeWithType(value, jgen, provider, provider.findTypeSerializer(baseType));
        } else {
            defaultSerializer.serialize(value, jgen, provider);
        }
    }

    @Override
    public void serializeWithType(NuxeoArtifact value, JsonGenerator gen, SerializerProvider serializers,
            TypeSerializer typeSer) throws IOException {
        if (doFilter(value)) {
            return;
        }
        defaultSerializer.serializeWithType(value, gen, serializers, typeSer);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, NuxeoArtifact value) {
        if (doFilter(value)) {
            return true;
        }
        return defaultSerializer.isEmpty(provider, value);
    }

}
