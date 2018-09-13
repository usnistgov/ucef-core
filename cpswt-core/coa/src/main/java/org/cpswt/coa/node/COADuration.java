/*
 * Copyright (c) 2008, Institute for Software Integrated Systems, Vanderbilt University
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE VANDERBILT UNIVERSITY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE VANDERBILT
 * UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE VANDERBILT UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE VANDERBILT UNIVERSITY HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * 
 */

package org.cpswt.coa.node;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a duration element in the sequence graph.
 */
public class COADuration extends COANode {

    @JsonProperty("time")
	protected double duration = 0.0;

	protected double endTime = -1.0;
	protected boolean isTimerOn = false;

	COADuration() {
	    super(COANodeType.Dur);
    }

    COADuration(COANodeType nodeType) {
	    super(nodeType);
    }

	public COADuration(String nodeName, String uniqueID, double duration) {
		this(nodeName, uniqueID, duration, COANodeType.Dur);
	}

	protected COADuration(String nodeName, String uniqueID, double duration, COANodeType nodeType) {
		super(nodeName, uniqueID, nodeType);
		if (duration < 0) {
			throw new IllegalArgumentException("Error! Negative duration not permitted");
		}
		this.duration = duration;
	}

	@Override
	public String toString() {
		return super.toString() + ", Duration: " + duration + ", TimerON: "
				+ isTimerOn;
	}

	public double getDuration() {
		return duration;
	}
    public void setDuration(double duration) {
        this.duration = duration;
    }

    public void startTimer(double currentTime) {
		if (currentTime < 0) {
			throw new IllegalArgumentException("Error! Negative currentTime given for: " + this);
		}
		this.endTime = currentTime + this.duration;
		this.isTimerOn = true;
	}

	public double getEndTime() {
		return endTime;
	}

	public boolean getIsTimerOn() {
		return isTimerOn;
	}
}
