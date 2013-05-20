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


import java.text.MessageFormat;

/**
 * Time interval class with nanosecond precision
 */
public class Duration {
	/** nanosecond duration of interval */
	private long duration = 0;
	
	/** full units representation */
	private long[] fullUnits = null;
	
	/**
	 * time units multipliers
	 */
	//private static final long MS_MULT = 1000000;
    private static final long MS_MULT = 1;
	private static final long SEC_MULT = MS_MULT * 1000;
	private static final long MIN_MULT = SEC_MULT * 60;
	private static final long HOUR_MULT = MIN_MULT * 60;
	private static final long DAY_MULT = HOUR_MULT * 24;

	/**
	 * @param duration
	 */
	public Duration(long duration) {
		this.duration = duration;
	}
	
	/**
	 * ctor
	 * @param start
	 * @param end
	 */
	public Duration( long start, long end ) {
		this(end - start);
	}
	
	/**
	 * Interval in nanos
	 * @return
	 */
	public long getInNanos() {
		return duration;
	}
	
	/**
	 * Interval in millis
	 * @return
	 */
	public long getInMillis() {
		return duration / MS_MULT;
	}
	
	/**
	 * Interval in seconds
	 * @return
	 */
	public long getInSeconds() {
		return duration / SEC_MULT;
	}
	
	/**
	 * interval in mins
	 * @return
	 */
	public long getInMins() {
		return duration / MIN_MULT;
	}
	
	/**
	 * Hours
	 * @return
	 */
	public long getInHours() {
		return duration / HOUR_MULT;
	}
	
	/**
	 * Days
	 * @return
	 */
	public long getInDays() {
		return duration / DAY_MULT;
	}
	
	/**
	 * Returns fixed length array (size is 5), of duration how many
	 * days, hours, minutes, seconds and milliseconds
	 * @return
	 */
	public long[] getFullUnits() {
		if (fullUnits == null) {
			fullUnits = new long[5];
			long durationNs = duration;
			fullUnits[0] = durationNs / DAY_MULT;
			durationNs %= DAY_MULT;
			fullUnits[1] = durationNs / HOUR_MULT;
			durationNs %= HOUR_MULT;
			fullUnits[2] = durationNs / MIN_MULT;
			durationNs %= MIN_MULT;
			fullUnits[3] = durationNs / SEC_MULT;
			durationNs %= SEC_MULT;
			fullUnits[4] = durationNs / MS_MULT;
		}
		return fullUnits;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		long[] fullUnits = getFullUnits();
		if (fullUnits[0] > 0) {
			result.append(fullUnits[0]); result.append(" day(s) ");
		}
		result.append(MessageFormat.format("{0,number}:{1,number,00}:{2,number,00}.{3,number}", 
				fullUnits[1], fullUnits[2], fullUnits[3], fullUnits[4]));
		return result.toString();
	}
}
