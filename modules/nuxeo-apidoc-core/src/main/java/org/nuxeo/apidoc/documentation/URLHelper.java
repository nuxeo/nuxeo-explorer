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
package org.nuxeo.apidoc.documentation;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Helper for URL validity checks.
 *
 * @since 20.2.0
 */
public class URLHelper {

    protected static final List<Integer> REDIRECT_CODES = Arrays.asList(HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_SEE_OTHER);

    protected static final int LOOP_MAX = 3;

    public static boolean isValid(String url) {
        return isValid(url, 0);
    }

    protected static boolean isValid(String url, int loopNumber) {
        try {
            HttpURLConnection huc = (HttpURLConnection) new URL(url).openConnection();
            // do not follow redirect, as final URL is not resolved properly (despite default implementation claim), see
            // "manual" redirection below
            huc.setInstanceFollowRedirects(false);
            huc.setRequestMethod("HEAD");
            int status = huc.getResponseCode();
            if (HttpURLConnection.HTTP_OK == status) {
                return true;
            } else if (REDIRECT_CODES.contains(status) && loopNumber < LOOP_MAX) {
                // follow redirect to check for final URL validity
                return isValid(huc.getHeaderField("Location"), loopNumber + 1);
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

}
