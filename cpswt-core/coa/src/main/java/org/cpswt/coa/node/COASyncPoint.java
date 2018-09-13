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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Synchronization point in the sequence graph.
 */
public class COASyncPoint extends COANode {

	@JsonProperty("time")
	private double syncTime = 0.0;

	@JsonProperty("minBranchesToSync")
	private int numBranchesToFinish = 0;

	private int numBranchesFinished = 0;

	COASyncPoint() {
	    super(COANodeType.SyncPoint);
    }

	public COASyncPoint(String nodeName, String uniqueID, double syncTime, int numBranchesToFinish) {
		super(nodeName, uniqueID, COANodeType.SyncPoint);

		this.syncTime = syncTime;
		this.numBranchesToFinish = numBranchesToFinish;
	}

	@Override
	public String toString() {
		return String.format("%s, SyncTime: %f, No. of branches to finish: %d",
				super.toString(), syncTime, numBranchesToFinish);
	}

	public double getSyncTime() {
		return syncTime;
	}

    public void setSyncTime(double syncTime) {
        this.syncTime = syncTime;
    }

    public int getNumBranchesToFinish() {
		return numBranchesToFinish;
	}

    public void setNumBranchesToFinish(int numBranchesToFinish) {
        this.numBranchesToFinish = numBranchesToFinish;
    }

    public void incrementBranchesFinished() {
		numBranchesFinished++;
	}

	public int getNumBranchesFinished() {
		return numBranchesFinished;
	}

	public boolean getIsRequiredNumOfBranchesFinished() {
		return numBranchesFinished >= numBranchesToFinish;
	}
}
