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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to setup timing probes associated with thread, probes can be
 * nested
 *
 * beginProbe("level1") beginProbe("level2") .... endProbe("level2").reportLog()
 * endProbe("level1").reportLog()
 */
public class Probe {

    private static final Logger log = LoggerFactory.getLogger(Probe.class);
    
    // current host name
    private static String hostId = "localhost";
    
    // id which is same for classloader (application or JVM id)
    private static final String jvmId = UUID.randomUUID().toString();
    
    // back ref to parent probe in the stack
    private Probe parent;
    
    // unique probe id
    private String probeId;
    
    // arbitrary marker string (say method name)
    private String marker;
    
    // begin bracket
    private Bracket beginBracket;
    
    // end bracket
    private Bracket endBracket;
    
    // concatenated parent's probeId <separator> this probeId
    private String compositeProbeId;
    
    // optional reference to the stack this probe may be part of (say when probe was created and bound to thread local)
    // need this to remove probe from stack when it ends
    private Deque<Probe> homeStack = null;
    
    private static final char STACK_ID_SEP = '\t';

    static {
        try {
            hostId = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
        }
    }
    /**
     * Thread local probe frames
     */
    private static final ThreadLocal<Deque<Probe>> threadStacks = new ThreadLocal<Deque<Probe>>() {
        @Override
        protected Deque<Probe> initialValue() {
            return new LinkedList<Probe>();
        }
    };

    public Probe(Probe parent, String marker) {
        this(parent, marker, null);
    }

    private Probe(Probe parent, String marker, Deque<Probe> homeStack) {
        if (marker == null) {
            throw new IllegalArgumentException("Marker must be specified");
        }
        this.probeId = UUID.randomUUID().toString();
        this.parent = parent;
        this.beginBracket = new Bracket();
        this.marker = marker;
        this.homeStack = homeStack;
    }

    public String getJvmId() {
        return jvmId;
    }

    public String getHostId() {
        return hostId;
    }

    public Probe getParent() {
        return parent;
    }

    public String getProbeId() {
        return probeId;
    }

    public String getCompositeProbeId() {
        if (compositeProbeId == null) {
            compositeProbeId = parent != null
                ? new StringBuilder(parent.getCompositeProbeId()).append(STACK_ID_SEP).append(probeId).toString() : probeId;
        }
        return compositeProbeId;   
    }

    public String getMarker() {
        return marker;
    }

    public boolean isOpen() {
        return endBracket == null && beginBracket != null;
    }

    public Bracket getBeginBracket() {
        return this.beginBracket;
    }

    public Bracket getEndBracket() {
        return this.endBracket;
    }

    public Probe end() {
        if (this.endBracket != null) {
            throw new IllegalStateException("Thread proble is already ended");
        }
        if (homeStack != null) {
            // remove probe from the home stack, it may not be on the top if 
            // e.g. exception happened and stack fell thought
            while (!homeStack.isEmpty()) {
                if (homeStack.pop() == this) {
                    break;
                }
            }
            // ignore situation when probe not found in the stack
        }
        this.endBracket = new Bracket();
        return this;
    }
    
    /**
     * Ends probe making sure that probe's measured duration is 0
     * This type of probe end may require if we need measure time spent
     * since the beginning of measurement or since last probe, but we are not
     * interested in the duration of the event itself
     * @return 
     */
    public Probe checkpoint() {
        end();
        endBracket.timeStamp = beginBracket.timeStamp;
        return this;
    }

    public Probe reportLog() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss,SSS");
        // output Probe={HostId\tJvmId\tProbeId\tThreadId\tThreadName\tMarker\tBeginTime\tEndTime\tElapsed\tElapsedSinceOuter\tElapsedSinceRoot\tCompositeStackId}
        log.info(new StringBuilder().append("Probe={")
                .append(getHostId())
                .append('\t').append(getJvmId())
                .append('\t').append(getProbeId())
                .append('\t').append(getBeginBracket().threadId)
                .append('\t').append(getBeginBracket().threadName)
                .append('\t').append(getMarker())
                .append('\t').append(df.format(getBeginBracket().timeStamp))
                .append('\t').append(getEndBracket() != null ? df.format(getEndBracket().timeStamp) : "")
                .append('\t').append(getElapsed())
                .append('\t').append(getElapsedSinceOuter())
                .append('\t').append(getElapsedSinceRoot())
                .append('\t').append(getCompositeProbeId())
                .append('}').toString());
        return this;
    }

    /**
     * Returns time elapsed since beginning of probe Returns 0 if probe is still
     * open
     *
     * @return
     */
    public long getElapsed() {
        return !isOpen() ? endBracket.timeStamp - beginBracket.timeStamp : 0;
    }

    /**
     * Returns time elapsed since the beginning of the next outer probe (if any)
     * if no outer probe or current probe is open, then 0
     *
     * @return
     */
    public long getElapsedSinceOuter() {
        return !isOpen() && parent != null ? endBracket.timeStamp - parent.beginBracket.timeStamp : 0;
    }

    /**
     * Returns time elapsed since beginning of the root probe, if no root probe
     * or current probe is still open, then 0
     *
     * @return
     */
    public long getElapsedSinceRoot() {
        if (isOpen()) {
            return 0;
        }
        Probe next = this;
        while (next.parent != null) {
            next = next.parent;
        }
        return endBracket.timeStamp - next.beginBracket.timeStamp;
    }
    
    /**
     * Same as nestProbe but unconditionally removes all probes which
     * might exist in the stack
     *
     * @param marker
     * @return
     */
    public static Probe beginProbe(String marker) {
        threadStacks.get().clear();
        return nestProbe(marker);
    }

    /**
     * Begins new probe in the current thread (push), if there is already
     * started probe associated with the thread, then this new probe will be
     * nested probe (very similar to nested brackets in java)
     *
     * @param marker is required string to be associated with the probe. Marker can be
     * used later to look up probe in the thread
     * @return
     */
    public static Probe nestProbe(String marker) {
        Deque<Probe> s = threadStacks.get();
        Probe p = new Probe(topProbe(), marker, s);
        s.addFirst(p);
        return p;
    }

    /**
     * Returns the inner most probe in the current thread (peek)
     * @return
     */
    public static Probe topProbe() {
        Deque<Probe> topProbe = threadStacks.get();
        return topProbe.isEmpty() ? null : topProbe.peekFirst();
    }

    /**
     * Finds probe identified by marker in the current thread, returns null of
     * not found
     *
     * @param marker
     * @return
     */
    public static Probe findProbe(String marker) {
        for (Iterator<Probe> it = threadStacks.get().iterator(); it.hasNext();) {
            Probe probe = it.next();
            if (probe.getMarker().equals(marker)) {
                return probe;
            }
        }
        return null;
    }
    
    /**
     * Returns number of probes in the current stack
     * @return 
     */
    public static int getProbeCount() {
        return threadStacks.get().size();
    }

    @Override
    public String toString() {
        return new StringBuilder("Probe={")
                .append("hostId=").append(hostId)
                .append(",jvmId=").append(jvmId)
                .append(",probeId=").append(probeId)
                .append(",parentId=").append(parent != null ? parent.probeId : "null")
                .append(",stackId=").append(compositeProbeId)
                .append(",marker=").append(marker)
                .append(",begin=").append(beginBracket)
                .append(",end=").append(endBracket)
                .append("}")
                .toString();
    }

    public static class Bracket {

        long timeStamp;
        private String threadName;
        private long threadId;

        public Bracket() {
            timeStamp = System.currentTimeMillis();
            threadName = Thread.currentThread().getName();
            threadId = Thread.currentThread().getId();
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public String getThreadName() {
            return threadName;
        }

        public long getThreadId() {
            return threadId;
        }

        @Override
        public String toString() {
            return new StringBuilder("{")
                    .append("timeStamp=").append(timeStamp)
                    .append(",threadId=").append(threadId)
                    .append(",threadName=").append(threadName)
                    .append("}")
                    .toString();
        }
    }
}
