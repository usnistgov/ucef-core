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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cpswt.coa.node.COANode;

import java.util.HashSet;

/**
 * A simple class for a COA edge in the COA sequence graph.
 */
public class COAEdge {

    @JsonProperty("type")
	private COAEdgeType edgeType;

	@JsonProperty("ID")
	private String id;

	@JsonIgnore
	private HashSet<String> branchesFinishedCondition = new HashSet<String>();

	@JsonProperty("name")
    private String name;

	@JsonProperty("fromNode")
    private String fromNodeId;

	@JsonProperty("toNode")
    private String toNodeId;

	@JsonIgnore
	private COANode fromNode;
	@JsonIgnore
	private COANode toNode;

	COAEdge() {}

    COAEdge(COAEdgeType edgeType) {
	    this.edgeType = edgeType;
    }

	public COAEdge(COAEdgeType edgeType, COANode fromNode, COANode toNode,
				   String id, HashSet<String> branchesFinishedCondition) {
		if (fromNode == null || toNode == null || id == null) {
			throw new IllegalArgumentException(
					"Null parameters given while creating an edge.");
		}
		if (COAEdgeType.COAException == edgeType
				&& branchesFinishedCondition == null) {
			throw new IllegalArgumentException(
					"Branches that should have finished not specified for the COAException edge.");
		}
		this.edgeType = edgeType;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.id = id;
		if (branchesFinishedCondition != null
				&& !branchesFinishedCondition.isEmpty()) {
			this.branchesFinishedCondition.addAll(branchesFinishedCondition);
		}
	}

	@Override
	public String toString() {
	    return String.format("[%s] %s --to--> %s", name, fromNode.getName(), toNode.getName());
	}

	public COAEdgeType getEdgeType() {
		return edgeType;
	}

    public void setEdgeType(COAEdgeType edgeType) {
        this.edgeType = edgeType;
    }

    public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(String fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public String getToNodeId() {
        return toNodeId;
    }

    public void setToNodeId(String toNodeId) {
        this.toNodeId = toNodeId;
    }

    public HashSet<String> getBranchesFinishedCondition() {
		return branchesFinishedCondition;
	}

	public COANode getFromNode() {
		return fromNode;
	}

    public void setFromNode(COANode fromNode) {
        this.fromNode = fromNode;
    }

    public COANode getToNode() {
		return toNode;
	}

	public void setToNode(COANode toNode) {
        this.toNode = toNode;
    }
}
