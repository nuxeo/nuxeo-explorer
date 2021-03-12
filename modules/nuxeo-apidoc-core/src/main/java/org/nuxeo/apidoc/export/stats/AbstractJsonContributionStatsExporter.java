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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.export.api.AbstractExporter;
import org.nuxeo.apidoc.export.api.ExporterDescriptor;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;

/**
 * Json exporter for contribution stats.
 *
 * @since 22.0.0
 */
public abstract class AbstractJsonContributionStatsExporter extends AbstractExporter {

    protected static final String STUDIO_COMPONENT_PREFIX = "studio.extensions";

    protected AbstractJsonContributionStatsExporter(ExporterDescriptor descriptor) {
        super(descriptor);
    }

    protected CodeType computeCodeType(ExtensionInfo contrib, List<String> javaTypes, List<String> javaLikeTypes,
            List<String> scriptingTypes) {
        String xp = contrib.getExtensionPoint();
        if (javaTypes.contains(xp)) {
            return CodeType.JAVA;
        }
        if (javaLikeTypes.contains(xp)) {
            return CodeType.JAVALIKE;
        }
        if (scriptingTypes.contains(xp)) {
            return CodeType.SCRIPTING;
        }
        return CodeType.XML;
    }

    protected List<String> addAliases(DistributionSnapshot distribution, List<String> xps) {
        List<String> res = new ArrayList<>();
        xps.forEach(xp -> {
            res.add(xp);
            ExtensionPointInfo xpInfo = distribution.getExtensionPoint(xp);
            if (xpInfo != null) {
                res.addAll(xpInfo.getAliases());
            }
        });
        return res;
    }

    protected List<ContributionStat> computeStats(DistributionSnapshot distribution, SnapshotFilter filter,
            Map<String, String> properties) {
        List<String> javaTypes = addAliases(distribution, CodeType.JAVA.getValues(properties, getProperties()));
        List<String> javaLikeTypes = addAliases(distribution, CodeType.JAVALIKE.getValues(properties, getProperties()));
        List<String> scriptingTypes = addAliases(distribution,
                CodeType.SCRIPTING.getValues(properties, getProperties()));
        List<String> xpIds = distribution.getExtensionPointIds();
        return distribution.getContributions()
                           .stream()
                           .sorted(Comparator.comparing(ExtensionInfo::getId))
                           .filter(contrib -> filter == null || filter.accept(contrib))
                           .map(contrib -> {
                               ContributionStat stat = new ContributionStat();
                               stat.setExtensionId(contrib.getId());
                               String xp = contrib.getExtensionPoint();
                               stat.setTargetExtensionPointId(xp);
                               stat.setTargetExtensionPointPresent(xpIds.contains(xp));
                               stat.setNumberOfContributions(Long.valueOf(contrib.getContributionItems().size()));
                               stat.setCodeType(computeCodeType(contrib, javaTypes, javaLikeTypes, scriptingTypes));
                               stat.setFromStudio(contrib.getId().startsWith(STUDIO_COMPONENT_PREFIX));
                               return stat;
                           })
                           .collect(Collectors.toList());
    }

}
