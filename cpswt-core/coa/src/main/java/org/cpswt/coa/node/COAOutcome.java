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
 * @author Himanshu Neema
 */

package org.cpswt.coa.node;

//import org.cpswt.hla.InteractionRoot;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an Outcome element in the sequence graph.
 */
public class COAOutcome extends COANode {

    @JsonProperty("interactionName")
	private String interactionClassName;

	private int interactionClassHandle = 0;
	private double awaitStartTime = -1;
	private boolean isTimerOn = false;
	private Object lastArrivedInteraction = null;

	COAOutcome() {
	    super(COANodeType.Outcome);
    }

	public COAOutcome(String nodeName, String uniqueID, String interactionClassName) {
		super(nodeName, uniqueID, COANodeType.Outcome);

		this.interactionClassName = interactionClassName;
	}

	@Override
	public String toString() {
		return super.toString() + ", Interaction: " + interactionClassName
				+ ", TimerON: " + isTimerOn + ", AwaitStartTime: "
				+ awaitStartTime;
	}

	public String getInteractionClassName() {
		return interactionClassName;
	}

    public void setInteractionClassName(String interactionClassName) {
        this.interactionClassName = interactionClassName;
    }

    public int getInteractionClassHandle() {
		return interactionClassHandle;
	}
	public double getAwaitStartTime() {
		return awaitStartTime;
	}
	public boolean getIsTimerOn() {
		return isTimerOn;
	}
	public Object getLastArrivedInteraction() {
		return lastArrivedInteraction;
	}

	public void setLastArrivedInteraction(Object interactionRoot) {
		if (interactionRoot == null) {
			throw new IllegalArgumentException(
					"Error! NULL interaction was set in the COA Outcome!");
		}

		this.lastArrivedInteraction = interactionRoot;
	}

	public void setInteractionClassHandle(int interactionClassHandle) {
		if (interactionClassHandle < 0) {
			throw new IllegalArgumentException(
					"Error! Negative values given while configuring: " + this);
		}
		this.interactionClassHandle = interactionClassHandle;
	}

	public void startTimer(double currentTime) {
		if (currentTime < 0) {
			throw new IllegalArgumentException(
					"Error! Negative values given while setting awaitStartTime for: "
							+ this);
		}
		this.awaitStartTime = currentTime;
		this.isTimerOn = true;
	}
}
