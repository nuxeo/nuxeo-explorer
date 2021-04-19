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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.nuxeo.apidoc.export.api.ExporterDescriptor;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Json exporter for contribution stats.
 *
 * @since 22.0.0
 */
public class CSVContributionStatsExporter extends AbstractJsonContributionStatsExporter {

    public CSVContributionStatsExporter(ExporterDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public void export(OutputStream out, DistributionSnapshot distribution, SnapshotFilter filter,
            Map<String, String> properties) {
        try (OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer,
                        CSVFormat.RFC4180.withHeader('\ufeff' + "Extension Id", "Target Extension Point Id",
                                "Target Extension Point Present", "Number Of Contributions", "Code Type",
                                "From Studio"))) {
            List<ContributionStat> stats = computeStats(distribution, filter, properties);
            stats.forEach(s -> {
                try {
                    csvPrinter.printRecord(s.getExtensionId(), s.getTargetExtensionPointId(),
                            s.isTargetExtensionPointPresent(), s.getNumberOfContributions(), s.getCodeType(),
                            s.isFromStudio());
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
            });
            csvPrinter.flush();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

}
