/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.export.stats;

/**
 * @since 22.0.0
 */
public class ContributionStat {

    protected String extensionId;

    protected String targetExtensionPointId;

    protected boolean targetExtensionPointPresent;

    protected Long numberOfContributions;

    protected CodeType codeType;

    protected boolean fromStudio;

    public String getExtensionId() {
        return extensionId;
    }

    public void setExtensionId(String extensionId) {
        this.extensionId = extensionId;
    }

    public String getTargetExtensionPointId() {
        return targetExtensionPointId;
    }

    public void setTargetExtensionPointId(String targetExtensionPointId) {
        this.targetExtensionPointId = targetExtensionPointId;
    }

    public boolean isTargetExtensionPointPresent() {
        return targetExtensionPointPresent;
    }

    public void setTargetExtensionPointPresent(boolean targetExtensionPointPresent) {
        this.targetExtensionPointPresent = targetExtensionPointPresent;
    }

    public Long getNumberOfContributions() {
        return numberOfContributions;
    }

    public void setNumberOfContributions(Long numberOfContributions) {
        this.numberOfContributions = numberOfContributions;
    }

    public CodeType getCodeType() {
        return codeType;
    }

    public void setCodeType(CodeType codeType) {
        this.codeType = codeType;
    }

    public boolean isFromStudio() {
        return fromStudio;
    }

    public void setFromStudio(boolean fromStudio) {
        this.fromStudio = fromStudio;
    }

}
