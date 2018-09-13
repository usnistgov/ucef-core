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
 * Represents an AwaitN COA element in the sequence graph.
 */
public class COAAwaitN extends COANode {

	@JsonProperty("minBranchesToAwait")
	private int numBranchesToFinish = 0;
	private int numBranchesFinished = 0;

	COAAwaitN() {
		super(COANodeType.AwaitN);
	}

	public COAAwaitN(String nodeName, String uniqueID, int numBranchesToFinish) {
		super(nodeName, uniqueID, COANodeType.AwaitN);

		this.numBranchesToFinish = numBranchesToFinish;
	}

	@Override
	public String toString() {
		return super.toString() + ", No. of branches to finish: "
				+ numBranchesToFinish + ", No. of branches already finished: "
				+ numBranchesFinished;
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
