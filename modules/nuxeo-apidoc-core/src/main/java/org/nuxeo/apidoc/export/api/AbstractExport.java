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
package org.nuxeo.apidoc.export.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 20.0.0
 */
public abstract class AbstractExport implements Export {

    protected String name;

    protected String type;

    protected String title;

    protected String description;

    protected final Map<String, String> properties = new HashMap<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String getProperty(String name, String defaultValue) {
        return properties.getOrDefault(name, defaultValue);
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    @Override
    public void setProperty(String name, String value) {
        this.properties.put(name, value);
    }

    protected <T extends Export> T copy(Class<T> targetClass) throws ReflectiveOperationException {
        T export = targetClass.getConstructor().newInstance();
        export.setName(getName());
        export.setTitle(getTitle());
        export.setDescription(getDescription());
        export.setType(getType());
        export.setProperties(getProperties());
        return export;
    }

}
