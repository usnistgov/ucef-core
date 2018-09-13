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

package org.cpswt.coa.edge;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cpswt.coa.node.COANode;

import java.util.HashSet;

/**
 * A simple class for a COA edge with probability in the COA sequence graph. This is used to connect COA elements from a probabilistic choice node.
 */
public class COAFlowWithProbabilityEdge extends COAEdge {

	@JsonProperty("probability")
	private double probability;

	COAFlowWithProbabilityEdge() {
	    super(COAEdgeType.COAFlowWithProbability);
    }

	public COAFlowWithProbabilityEdge(COANode fromNode, COANode toNode,
									  String flowID, double probability, HashSet<String> branchesFinishedCondition) {

		super(COAEdgeType.COAFlowWithProbability, fromNode, toNode, flowID, branchesFinishedCondition);

		if (probability < 0) {
			throw new IllegalArgumentException(
					"Probability of COAFlowWithProbability edge must not be negative.");
		}

		this.probability = probability;
	}

	@Override
	public String toString() {
		return getFromNode().getName() + " --to--> " + getToNode().getName() + "[with probability " + probability + "]";
	}

	public double getProbability() {
		return probability;
	}

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public void updateProbability(double probability) {
		this.probability = probability;
	}
}
