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

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.nuxeo.apidoc.adapters.BundleGroupDocAdapter;
import org.nuxeo.apidoc.adapters.BundleInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ComponentInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionPointInfoDocAdapter;
import org.nuxeo.apidoc.adapters.OperationInfoDocAdapter;
import org.nuxeo.apidoc.adapters.PackageInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ServiceInfoDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.apidoc.introspection.ComponentInfoImpl;
import org.nuxeo.apidoc.introspection.ExtensionInfoImpl;
import org.nuxeo.apidoc.introspection.ExtensionPointInfoImpl;
import org.nuxeo.apidoc.introspection.OperationInfoImpl;
import org.nuxeo.apidoc.introspection.PackageInfoImpl;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.introspection.ServiceInfoImpl;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.runtime.model.ComponentName;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

/**
 * Helper for Json mappers configuration.
 *
 * @since 20.0.0
 */
public class JsonMapper {

    protected static ObjectMapper createMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
              .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.addMixIn(OperationDocumentation.Param.class, OperationDocParamMixin.class);
        mapper.addMixIn(ComponentName.class, ComponentNameMixin.class);
        mapper.addMixIn(Blob.class, BlobMixin.class);
        return mapper;
    }

    protected static SimpleModule createModule(SnapshotFilter filter, SnapshotFilter refFilter) {
        if (filter == null) {
            return new SimpleModule();
        }
        return new SimpleModule() {
            private static final long serialVersionUID = 1L;

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new BeanSerializerModifier() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription desc,
                            JsonSerializer<?> serializer) {
                        if (NuxeoArtifact.class.isAssignableFrom(desc.getBeanClass())) {
                            return new JsonSnapshotSerializer((JsonSerializer<Object>) serializer, filter, refFilter);
                        }
                        return serializer;
                    }
                });
            }
        };
    }

    /**
     * Returns the default object mapper for distribution artifacts and following filter.
     * <p>
     * Follows annotations on default live implementation.
     *
     * @since 20.0.0
     */
    public static ObjectMapper basic(SnapshotFilter filter, SnapshotFilter refFilter) {
        final ObjectMapper mapper = createMapper();
        SimpleModule module = createModule(filter, refFilter);
        module.addAbstractTypeMapping(DistributionSnapshot.class, RuntimeSnapshot.class)
              .addAbstractTypeMapping(BundleInfo.class, BundleInfoImpl.class)
              .addAbstractTypeMapping(BundleGroup.class, BundleGroupImpl.class)
              .addAbstractTypeMapping(ComponentInfo.class, ComponentInfoImpl.class)
              .addAbstractTypeMapping(ExtensionPointInfo.class, ExtensionPointInfoImpl.class)
              .addAbstractTypeMapping(ExtensionInfo.class, ExtensionInfoImpl.class)
              .addAbstractTypeMapping(OperationInfo.class, OperationInfoImpl.class)
              .addAbstractTypeMapping(ServiceInfo.class, ServiceInfoImpl.class)
              .addAbstractTypeMapping(PackageInfo.class, PackageInfoImpl.class)
              .addAbstractTypeMapping(Blob.class, StringBlobReader.class);
        mapper.registerModule(module);
        return mapper;
    }

    public static abstract class OperationDocParamMixin {
        abstract @JsonProperty("isRequired") String isRequired();
    }

    public static abstract class ComponentNameMixin {
        @JsonCreator
        ComponentNameMixin(@JsonProperty("rawName") String rawName) {
        }
    }

    public static abstract class BlobMixin {
        @JsonCreator
        BlobMixin(@JsonProperty("content") String content, @JsonProperty("name") String name) {
        }

        // string blob

        @JsonProperty("content")
        abstract String getString();

        @JsonProperty("name")
        abstract String getFilename();

        @JsonIgnore
        abstract InputStream getStream();

        @JsonIgnore
        abstract byte[] getByteArray();

        @JsonIgnore
        abstract public File getFile();

        @JsonIgnore
        abstract String getDigestAlgorithm();

        @JsonIgnore
        abstract CloseableFile getCloseableFile();

        // persisted blob

        @JsonIgnore
        abstract Binary getBinary();

        @JsonIgnore
        abstract String getKey();

        @JsonIgnore
        abstract String getProviderId();

    }

    public static class StringBlobReader extends StringBlob {
        private static final long serialVersionUID = 1L;

        @JsonCreator
        StringBlobReader(@JsonProperty("content") String content, @JsonProperty("name") String name) {
            super(content, "text/plain", StandardCharsets.UTF_8.name(), name);
        }
    }

    /**
     * Returns the default object mapper for persisted distribution artifacts.
     * <p>
     * Follows annotations on repository implementation.
     *
     * @since 20.0.0
     */
    public static ObjectMapper persisted(SnapshotFilter filter, SnapshotFilter refFilter) {
        final ObjectMapper mapper = createMapper();
        SimpleModule module = createModule(filter, refFilter);
        module.addAbstractTypeMapping(DistributionSnapshot.class, RepositoryDistributionSnapshot.class)
              .addAbstractTypeMapping(BundleInfo.class, BundleInfoDocAdapter.class)
              .addAbstractTypeMapping(BundleGroup.class, BundleGroupDocAdapter.class)
              .addAbstractTypeMapping(ComponentInfo.class, ComponentInfoDocAdapter.class)
              .addAbstractTypeMapping(ExtensionPointInfo.class, ExtensionPointInfoDocAdapter.class)
              .addAbstractTypeMapping(ExtensionInfo.class, ExtensionInfoDocAdapter.class)
              .addAbstractTypeMapping(OperationInfo.class, OperationInfoDocAdapter.class)
              .addAbstractTypeMapping(ServiceInfo.class, ServiceInfoDocAdapter.class)
              .addAbstractTypeMapping(PackageInfo.class, PackageInfoDocAdapter.class);
        mapper.registerModule(module);
        return mapper;
    }

}
