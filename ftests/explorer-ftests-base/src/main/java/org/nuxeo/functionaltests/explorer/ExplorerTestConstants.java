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
package org.nuxeo.functionaltests.explorer;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpStatus;
import org.nuxeo.http.test.ResponseHandler;

/**
 * @since 2025.0
 */
public class ExplorerTestConstants {

    public final static String MANAGER_USERNAME = "apidocmanager";

    public final static String READER_USERNAME = "jdoe";

    public final static String TEST_PASSWORD = "test";

    public static final ResponseHandler<Void> HTTP_STATUS_OK_CHECKER = response -> {
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        return null;
    };

    public static final ResponseHandler<Void> HTTP_STATUS_NO_CONTENT_CHECKER = response -> {
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
        return null;
    };

    public static final ResponseHandler<Void> HTTP_STATUS_NOT_FOUND_CHECKER = response -> {
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        return null;
    };
}
