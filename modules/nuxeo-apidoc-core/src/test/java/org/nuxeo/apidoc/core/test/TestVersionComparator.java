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
 *     Arnaud Kervern <akervern@nuxeo.com>
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.nuxeo.apidoc.snapshot.VersionComparator;;

/**
 * @since 20.0.0
 */
public class TestVersionComparator {

    @Test
    public void testCompare() {
        VersionComparator vc = new VersionComparator();
        assertEquals(0, vc.compare(null, null));
        assertEquals(-1, vc.compare(null, "foo"));
        assertEquals(1, vc.compare("foo", null));
        assertEquals(1, vc.compare("11.2", "11.2.0"));
        assertEquals(1, vc.compare("11.2", "11.2.1"));
        assertEquals(-1, vc.compare("11.2.10", "11.2.11"));
        assertEquals(1, vc.compare("11.2.10", "11.2.3"));
        assertEquals(-1, vc.compare("11.2-RC1", "11.2"));
        assertEquals(-1, vc.compare("11.2-SNAPSHOT", "11.2"));
        assertEquals(-20, vc.compare("11.2-RC1", "11.2-foo"));
        assertEquals(1, vc.compare("11.2-RC10", "11.2-RC2"));
        assertEquals(1, vc.compare("11.2-RC1", "11.2.1-RC2"));
    }

    @Test
    public void testList() {
        List<String> versions = new ArrayList<>(Arrays.asList("11.3", "11.2-RC2", "11.2-RC10", "11.2-foo", "11.2-bar",
                "11.2-RC1", "11.10", "11.2.0", "11.2.10", "11.2.49", "11.2", "11.2.1", "11.2.3-SNAPSHOT", "11.2.53",
                "11.2.3", "11.2.1-SNAPSHOT", "11.2.3-I20200929"));
        Collections.sort(versions, new VersionComparator());
        assertEquals(Arrays.asList("11.2.0", "11.2.1-SNAPSHOT", "11.2.1", "11.2.3-I20200929", "11.2.3-SNAPSHOT", "11.2.3",
                "11.2.10", "11.2.49", "11.2.53", "11.2-RC1", "11.2-RC2", "11.2-RC10", "11.2-bar", "11.2-foo", "11.2",
                "11.3", "11.10"), versions);
    }

    @Test
    public void testVersionMatches() {
        assertFalse(VersionComparator.isVersion(null));
        assertFalse(VersionComparator.isVersion(" "));
        assertFalse(VersionComparator.isVersion("foo"));
        assertTrue(VersionComparator.isVersion("1"));
        assertTrue(VersionComparator.isVersion("1.0"));
        assertTrue(VersionComparator.isVersion("1.1.3"));
        assertTrue(VersionComparator.isVersion("1.1-BLA1"));
        assertTrue(VersionComparator.isVersion("1.1-BLA"));
        assertTrue(VersionComparator.isVersion("1.1-SNAPSHOT"));
        assertTrue(VersionComparator.isVersion("100.10.12-DS"));
    }

}
