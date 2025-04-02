/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.functionaltests.explorer.pages;

import org.nuxeo.functionaltests.AbstractHtmlPage;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

/**
 * @since 2025.0
 */
public class UploadDistribTmpPage extends AbstractHtmlPage<UploadDistribTmpPage.Builder> {

    protected final UploadConfirmFragment uploadConfirmFragment;

    public UploadDistribTmpPage(Builder builder, Source html) {
        super(builder, html);
        uploadConfirmFragment = this.firstWithNameAndClassAs(HTMLElementName.FORM, "upload-confirm-fragment",
                UploadConfirmFragment::new);
    }

    public static Builder builder() {
        return new Builder();
    }

    public UploadConfirmFragment getUploadConfirmFragment() {
        return uploadConfirmFragment;
    }

    public static class Builder extends AbstractHtmlPage.Builder<Builder> {

        public Builder() {
            super("Nuxeo Platform Explorer");
        }

        public UploadDistribTmpPage build(Source html) {
            return new UploadDistribTmpPage(this, html);
        }
    }
}
