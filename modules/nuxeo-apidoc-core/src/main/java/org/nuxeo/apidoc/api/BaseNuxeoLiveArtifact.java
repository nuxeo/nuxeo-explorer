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
package org.nuxeo.apidoc.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all live artifacts.
 *
 * @since 22.0.0
 */
public abstract class BaseNuxeoLiveArtifact extends BaseNuxeoArtifact {

    protected final List<String> errors = new ArrayList<>();

    protected final List<String> warnings = new ArrayList<>();

    @Override
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @Override
    public void setErrors(List<String> errors) {
        this.errors.clear();
        if (errors != null) {
            this.errors.addAll(errors);
        }
    }

    @Override
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    @Override
    public void setWarnings(List<String> warnings) {
        this.warnings.clear();
        if (warnings != null) {
            this.warnings.addAll(warnings);
        }
    }

}
