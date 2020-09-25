/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.snapshot;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comparator for versions using 1 to 3 digits.
 * <p>
 * Makes sure version "11.2.3" is < "11.2.10" (so simple String ordering cannot be used).
 *
 * @since 20.0.0
 */
public class VersionComparator implements Comparator<String> {

    protected static final Pattern VERSION_REGEX = Pattern.compile(
            "^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-(\\D+))?(?:(\\d+))?$", Pattern.CASE_INSENSITIVE);

    public static boolean isVersion(String candidate) {
        return candidate != null && VERSION_REGEX.matcher(candidate).matches();
    }

    protected int compareNull(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        return -2;
    }

    protected int compareMarkers(String s1, String s2) {
        int resMarker = compareNull(s1, s2);
        if (resMarker == -2) {
            return s1.compareTo(s2);
        }
        return -resMarker;
    }

    @Override
    public int compare(String version1, String version2) {
        int checkNull = compareNull(version1, version2);
        if (checkNull != -2) {
            return checkNull;
        }

        Matcher m1 = VERSION_REGEX.matcher(version1);
        Matcher m2 = VERSION_REGEX.matcher(version2);
        if (m1.matches() && m2.matches()) {
            String marker1 = null;
            String marker2 = null;
            boolean compareMarkers = false;
            for (int i = 0; i < 5; i++) {
                String s1 = m1.group(i + 1);
                String s2 = m2.group(i + 1);
                if (i == 3) {
                    marker1 = s1;
                    marker2 = s2;
                    continue;
                }
                int checkNullItem = compareNull(s1, s2);
                if (checkNullItem == -2) {
                    int c1 = Integer.parseInt(s1);
                    int c2 = Integer.parseInt(s2);
                    int resItem = Integer.compare(c1, c2);
                    if (resItem != 0) {
                        return resItem;
                    } else if (i == 4) {
                        return compareMarkers(marker1, marker2);
                    }
                } else {
                    if (checkNullItem == 0 || i == 4) {
                        compareMarkers = true;
                    } else {
                        return -checkNullItem;
                    }
                }
            }
            if (compareMarkers) {
                return compareMarkers(marker1, marker2);
            }
        }

        return version1.compareTo(version2);
    }

}
