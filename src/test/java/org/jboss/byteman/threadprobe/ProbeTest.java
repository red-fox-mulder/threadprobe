/** 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * (C) 2013,
 * @authors Red F. Mulder red.fox.mulder@google.com
 */
package org.jboss.byteman.threadprobe;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author amatusev
 */
public class ProbeTest {
    
    public ProbeTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void testMultilevelBeginProbe() {
        Probe.beginProbe("root");
        Probe.nestProbe("level1");
        Probe.nestProbe("level2");
        Probe.beginProbe("new Root");
        assertTrue("Should be only one in stack", Probe.getProbeCount() == 1);
    }
    
    @Test
    public void testTopProbe() {
        Probe.beginProbe("top123");
        Probe topProbe = Probe.topProbe();
        assertNotNull("Null top probe", topProbe);
        assertTrue("Unexpected top probe", topProbe.getMarker().equals("top123"));
    }
    
    @Test
    public void testMultilevelNestedBeginEnds() {
        Probe.beginProbe("root");
        Probe.nestProbe("level1");
        Probe.nestProbe("level2");
        Probe.nestProbe("level3").end();
        Probe.topProbe().end();
        Probe.topProbe().end();
        Probe.topProbe().end();
        assertTrue("Should be empty stack", Probe.getProbeCount() == 0);
    }
    
    @Test
    public void testUnwindProbesStack() {
        Probe.beginProbe("root");
        Probe l1 = Probe.nestProbe("level1");
        Probe.nestProbe("level2");
        Probe.nestProbe("level3").end();
        l1.end();
        assertTrue("Should be 1 root in stack", Probe.getProbeCount() == 1);
        assertTrue(Probe.topProbe().getMarker().equals("root"));
    }
    
    @Test
    public void testUnwindProbesStack2() {
        Probe l1 = Probe.beginProbe("root");
        Probe.nestProbe("level1");
        Probe.nestProbe("level2");
        Probe.nestProbe("level3").end();
        l1.end();
        assertTrue("Should be 0 in stack", Probe.getProbeCount() == 0);
    }
    
    public void testParents() {
        Probe l1 = Probe.beginProbe("root");
        Probe l2 = Probe.nestProbe("level1");
        assertTrue("Parent is correct", l2.getParent() == l1);
    }
    
    public void testCheckpointTimeIsZero() {
        Probe checkpoint = Probe.beginProbe("one").checkpoint();
        assertTrue("Time should be zero", checkpoint.getElapsed() == 0);
    }
    
    public void testCompositeProbeIds() {
        Probe l1 = Probe.beginProbe("1");
        Probe l2 = Probe.nestProbe("2");
        assertTrue("should start with parent id", l2.getCompositeProbeId().startsWith(l2.getCompositeProbeId()));
    }
}