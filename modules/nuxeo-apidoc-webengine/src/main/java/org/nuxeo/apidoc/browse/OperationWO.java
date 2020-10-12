/*
 * (C) Copyright 2011-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.browse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.impl.OperationChainCompiler;
import org.nuxeo.ecm.automation.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "operation")
public class OperationWO extends NuxeoArtifactWebObject {

    protected OperationInfo getTargetComponentInfo() {
        return getSnapshot().getOperation(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetComponentInfo();
    }

    protected String getSignatureInfo(OperationInfo op, boolean isInput) {
        List<String> signature = op.getSignature();
        if (signature == null || signature.isEmpty()) {
            return "void";
        }
        List<String> result = new ArrayList<>();
        for (int i = (isInput ? 0 : 1); i < signature.size(); i += 2) {
            result.add(signature.get(i));
        }
        return String.join(", ", result);
    }

    public String getInputsAsString(OperationInfo op) {
        return getSignatureInfo(op, true);
    }

    public String getOutputsAsString(OperationInfo op) {
        return getSignatureInfo(op, false);
    }

    public String getParamDefaultValue(Param param) {
        if (param.values != null && param.values.length > 0) {
            return String.join(", ", param.values);
        }
        return "";
    }

    @Produces("text/html")
    @Override
    public Object doViewDefault() {
        Template t = (Template) super.doViewDefault();
        try {
            OperationInfo opInfo = getTargetComponentInfo();
            OperationDocumentation opDoc = OperationInfo.getDocumentation(opInfo);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                JsonWriter.writeOperation(baos, opDoc, true);
                t.arg("json", baos.toString());
            }

            if (Constants.CAT_CHAIN.equals(opInfo.getCategory())) {
                // handle chains use case, where implementation type is an inner class
                DistributionSnapshot dist = getSnapshot();
                JavaDocHelper helper = JavaDocHelper.getHelper(dist.getName(), dist.getVersion());
                String javadocUrl = helper.getUrl(OperationChainCompiler.class.getCanonicalName(), "CompiledChainImpl");
                t.arg("implementationUrl", javadocUrl);
            }

        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        return t;
    }

    @Override
    public String getSearchCriterion() {
        return "'" + super.getSearchCriterion() + "' Operation";
    }

}
