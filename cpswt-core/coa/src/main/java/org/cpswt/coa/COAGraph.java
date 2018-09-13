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

package org.cpswt.coa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.cpswt.coa.edge.*;
import org.cpswt.coa.node.*;
import org.cpswt.utils.RandomSingleton;

import hla.rti.RTIambassador;
import org.cpswt.hla.InteractionRoot;

import java.lang.reflect.Method;

/**
 * This is the main class to represent the COA sequence graph.
 */
public class COAGraph {

	private static final Logger logger = LogManager.getLogger(COAGraph.class);

	private HashMap<String, COANode> _allNodes = new HashMap<String, COANode>();

	private HashSet<COANode> _rootNodes = new HashSet<COANode>();

	private HashSet<COANode> _currentRootNodes = new HashSet<COANode>();

	private HashSet<COAEdge> _allEdges = new HashSet<COAEdge>();

	private HashMap<COANode, HashSet<COAEdge>> _edgesFromNodeMap = new HashMap<COANode, HashSet<COAEdge>>();

	public COAGraph() {
	}

	public void setCurrentRootNodesAsActive() {
		// Mark COA nodes at the beginning of the graph as active from the
		// beginning
		for (COANode n : _currentRootNodes) {
			n.setActive();
		}
	}

	public void initialize(String federationName, RTIambassador rti) {
		synchronized(rti) {
			// Mark COA nodes at the beginning of the graph as active from the
			// beginning
			for (COANode n : _currentRootNodes) {
				n.setActive();
			}

			// Make sure all interaction classes are loaded and pub-sub is configured
			for (COANode node: _allNodes.values()) {
				if (COANodeType.Action == node.getNodeType()) {
					COAAction actionNode = (COAAction) node;
					loadPublishedInteractionAndConfigurePublish(actionNode.getInteractionClassName(), federationName, rti);
				} else if (COANodeType.Outcome == node.getNodeType()) {
					COAOutcome outcomeNode = (COAOutcome) node;
					loadSubscribedInteractionAndConfigureSubscribe(outcomeNode.getInteractionClassName(), federationName, rti);
					logger.trace("COAGraph: Before setting interaction class handle outcome node's handle value is: {}", outcomeNode.getInteractionClassHandle());
					outcomeNode.setInteractionClassHandle(InteractionRoot.get_handle(outcomeNode.getInteractionClassName()));
					logger.trace("COAGraph: After setting interaction class handle outcome node's handle value is: {}", outcomeNode.getInteractionClassHandle());
				}
			}
		}
	}

	public Class loadInteractionClass(String intrFullyQualifiedName, String federationName) {
		// Get class name for the fully qualified interaction name and try loading it
		logger.trace("COAGraph: Interaction class name: {}... Now trying to load interaction class", intrFullyQualifiedName);
		String intrClassName = federationName + "." + intrFullyQualifiedName.substring( intrFullyQualifiedName.lastIndexOf( '.' ) + 1 );
		Class intrClass = null;
		try {
			intrClass = Class.forName(intrClassName);
			logger.trace("COAGraph: Class loaded successfully: {}", intrClassName);
			return intrClass;
		} catch (Exception e) {
			logger.error("COAGraph: Could not load class: {}", intrClassName);
			e.printStackTrace();
		}

		return null;
	}

	public InteractionRoot createInteractionInstance(String intrFullyQualifiedName, String intrClassName) {
		logger.trace("COAGraph: Trying to create interaction using class: {}", intrClassName);
        InteractionRoot interactionRoot = InteractionRoot.create_interaction(intrFullyQualifiedName);
		logger.trace("COAGraph: Interaction created was: {}", interactionRoot);
		return interactionRoot;
	}

	public void publishOrSubscribeAnInteractionClass(Class intrClass, RTIambassador rti, boolean bPublish) {
		synchronized (rti) {
			logger.trace("COAGraph:publishOrSubscribeAnInteractionClass: Got interaction class as: {}", intrClass);
			if (intrClass == null)
				return;
			try {
				Class[] pubSubMethodArgs = new Class[1];
				pubSubMethodArgs[0] = hla.rti.RTIambassador.class;
				logger.trace("COAGraph:publishOrSubscribeAnInteractionClass: Getting Publish/Subscribe method to invoke");
				Method pubSubMethod = null;
				if (bPublish) {
					pubSubMethod = intrClass.getDeclaredMethod("publish", pubSubMethodArgs);
				} else {
					pubSubMethod = intrClass.getDeclaredMethod("subscribe", pubSubMethodArgs);
				}
				logger.trace("COAGraph:publishOrSubscribeAnInteractionClass: Invoking Publish/Subscribe method: {}", pubSubMethod);
				pubSubMethod.invoke(null, rti);
				logger.trace("COAGraph:publishOrSubscribeAnInteractionClass: Publish/Subscribe method invokation was successful");
			} catch (Exception e) {
				logger.error("COAGraph:publishOrSubscribeAnInteractionClass: Failed to invoke Publish/Subscribe method");
				e.printStackTrace();
			}
		}
	}

	public void loadPublishedInteractionAndConfigurePublish(String intrFullyQualifiedName, String federationName, RTIambassador rti) {
		synchronized (rti) {
			Class intrClass = loadInteractionClass(intrFullyQualifiedName, federationName);
			if ( intrClass != null ) {
				logger.trace("COAGraph: For COAs, PUBLISHING interaction class: {}", intrClass);
				publishOrSubscribeAnInteractionClass(intrClass, rti, true);
			}
		}
	}

	public void loadSubscribedInteractionAndConfigureSubscribe(String intrFullyQualifiedName, String federationName, RTIambassador rti) {
		synchronized(rti) {
			Class intrClass = loadInteractionClass(intrFullyQualifiedName, federationName);
			if ( intrClass != null ) {
				logger.trace("COAGraph: For COAs, SUBSCRIBING to interaction class: {}", intrClass);
				publishOrSubscribeAnInteractionClass(intrClass, rti, false);
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (COANode n : _rootNodes) {
			buffer.append(n.getSuccessorGraphString(""));
			buffer.append("\n");
		}
		return buffer.toString();
	}

	public HashMap<String, COANode> getAllCOANodes() {
		return _allNodes;
	}

	public HashSet<COANode> getRootNodes() {
		return _rootNodes;
	}

	public HashSet<COANode> getCurrentRootNodes() {
		return _currentRootNodes;
	}

	public HashSet<COAEdge> getAllCOAEdges() {
		return _allEdges;
	}

	public void addNode(COANode node) {
		if (node == null) {
			throw new RuntimeException("(" + this
					+ "): Node supplied to add is NULL");
		}
		_allNodes.put(node.getId(), node);
		_rootNodes.add(node);
		_currentRootNodes.add(node);
	}

	public COANode getNode(String nodeUniqueID) {
		if (_allNodes.isEmpty() || !_allNodes.containsKey(nodeUniqueID)) {
			throw new IllegalStateException(
					"Searched node was not found - make sure all nodes are added before being referred.");
		}
		return _allNodes.get(nodeUniqueID);
	}

	public void addEdge(COAEdge edge) {
		if (edge == null) {
			throw new RuntimeException("(" + this
					+ "): Edge supplied to add is NULL");
		}

		// fill fromNode and toNode after deserialization
		if(edge.getFromNode() == null) {
			edge.setFromNode(this.getNode(edge.getFromNodeId()));
		}
		if(edge.getToNode() == null) {
			edge.setToNode(this.getNode(edge.getToNodeId()));
		}

		COANode fromNode = edge.getFromNode();
		COANode toNode = edge.getToNode();

		if (_allNodes.isEmpty()
				|| !_allNodes.containsKey(fromNode.getId())
				|| !_allNodes.containsKey(toNode.getId())) {
			throw new RuntimeException(
					"All nodes must be added before edges are added in the graph.");
		}

		// First, check for correct use of Outcome and OutcomeFilter nodes and
		// update link of Outcome node in OutcomeFilter node
		if (toNode.getNodeType() == COANodeType.OutcomeFilter) {
			if (toNode.getPredecessors().size() > 0) {
				throw new RuntimeException("OutcomeFilter "
						+ toNode.getName()
						+ " must be preceeded only by an Outcome node!");
			}
			if (fromNode.getNodeType() != COANodeType.Outcome) {
				throw new RuntimeException(
						"OutcomeFilter "
								+ toNode.getName()
								+ " node can only be preceeded by a node of type Outcome!");
			}

			((COAOutcomeFilter) toNode).setOutcome((COAOutcome) fromNode);
		}

		_rootNodes.remove(toNode);
		_currentRootNodes.remove(toNode);
		fromNode.addSuccessor(toNode);
		toNode.addPredecessor(fromNode);

		_allEdges.add(edge);

		if (!_edgesFromNodeMap.containsKey(fromNode)) {
			HashSet<COAEdge> fromNodeEdges = new HashSet<COAEdge>();
			_edgesFromNodeMap.put(fromNode, fromNodeEdges);
		}
		_edgesFromNodeMap.get(fromNode).add(edge);
	}

	public void markNodeExecuted(COANode node, double nodeExecutedTime) {
		if (node == null) {
			throw new RuntimeException(
					"Node give to be marked as executed is NULL.");
		}
		if (nodeExecutedTime < 0) {
			throw new RuntimeException(
					"Time of node execution given as negative.");
		}
		if (_currentRootNodes.isEmpty() || !_currentRootNodes.contains(node)) {
			throw new RuntimeException(
					"Invalid node give to be marked as executed: " + node);
		}

		node.setExecuted(nodeExecutedTime);
		_currentRootNodes.remove(node);
		logger.debug("Node executed in sequence graph: {}", node);

		// If the node is a probabilistic choice node, choose one successor
		// and remove the rest, if any.
		if (COANodeType.ProbabilisticChoice == node.getNodeType()) {
			HashMap<COANode, Double> successorsWithCumuProb = new HashMap<COANode, Double>();
			HashSet<COAEdge> outEdges = _edgesFromNodeMap.get(node);
			COANode chosenSuccessor = null;

			// Normalize probabilities on the successor elements
			if (outEdges != null) {
				// Get the sum of all probabilities
				double probabilitiesSum = 0;
				for (COAEdge edge : outEdges) {
					COAFlowWithProbabilityEdge edgeWithProb = (COAFlowWithProbabilityEdge) edge;
					probabilitiesSum += edgeWithProb.getProbability();
				}
				// Divide individual probabilities by the sum, so that the total
				// of all probabilities becomes equal to 1.0
				for (COAEdge edge : outEdges) {
					COAFlowWithProbabilityEdge edgeWithProb = (COAFlowWithProbabilityEdge) edge;
					double updatedProbability = edgeWithProb.getProbability()
							/ probabilitiesSum;
					logger.debug("Normalizing probability of execution of node '{}' to {}",edgeWithProb.getToNode(), updatedProbability);
					edgeWithProb.updateProbability(updatedProbability);
				}
				// Update successors map with cumulative probabilities for
				// successor selection
				double cumulativeProbabilitySum = 0;
				for (COAEdge edge : outEdges) {
					COAFlowWithProbabilityEdge edgeWithProb = (COAFlowWithProbabilityEdge) edge;
					// Update cumulative sum using the normalized probability of
					// successors
					cumulativeProbabilitySum += edgeWithProb.getProbability();
					successorsWithCumuProb.put(edge.getToNode(),
							cumulativeProbabilitySum);
				}
			}

			// Choose
			Random rand = RandomSingleton.instance();
			double choiceProb = rand.nextDouble();
			logger.debug("ProbabilisticChoice cumulative probability chosen = {}", choiceProb);
			for (COANode n : successorsWithCumuProb.keySet()) {
				if (successorsWithCumuProb.get(n) >= choiceProb) {
					chosenSuccessor = n;
					logger.debug("\tThus, choosing successor node: {}", n);
					break;
				}
			}

			// Remove not chosen successors
			for (COANode n : successorsWithCumuProb.keySet()) {
				if (n != chosenSuccessor) {
					n.setEnabledAsChoice(false);
				}
			}
		}

		// Successor status updates and graph updates
		HashSet<COANode> successors = node.getSuccessors();
		logger.trace("COAGraph: Node {} has {} successors", node, successors.size());
		if (successors.size() > 0) {
			for (COANode succ : successors) {
				// Take care of multiple directed edges coming to the successor
				// node. Add successor to the currentRootNodes if: (A) the
				// successor is not already in currentRootNodes, and (B) the
				// successor wasn't already executed, and (C) none of its
				// predecessors are still in the currentRootNodes (except when
				// the successor is of type SyncPt or AwaitN).
				// Also, before activating make sure that the chosen successor
				// is enabled as choice.
				logger.trace("COAGraph: Checking out the successor {}", succ);
				boolean succAlreadyInCurrentRootNodes = _currentRootNodes
						.contains(succ);
				if (!succAlreadyInCurrentRootNodes
						&& COANodeStatus.Executed != succ.getNodeStatus()) {
					boolean aSuccPredecessorInCurrentRootNodes = false;
					for (COANode succPred : succ.getPredecessors()) {
						if (_currentRootNodes.contains(succPred)) {
							aSuccPredecessorInCurrentRootNodes = true;
							break; // inner for-loop
						}
					}
					if (!aSuccPredecessorInCurrentRootNodes
							|| COANodeType.SyncPoint == succ.getNodeType()
							|| COANodeType.AwaitN== succ.getNodeType()) {
						if (COANodeType.SyncPoint == node.getNodeType()) {
							// TODO: If node that executed is SyncPt, then
							// enable only those successors that have valid
							// branches finished (i.e., handle exceptions)
							if (succ.enabledAsChoice()) {
								_currentRootNodes.add(succ);
								succ.setActive();
							}
						} else {
							if (succ.enabledAsChoice()) {
								_currentRootNodes.add(succ);
								succ.setActive();
							}
						}
					}
				}

				// Do post-processing on successors, if any
				if (COANodeType.SyncPoint == succ.getNodeType()) {
					COASyncPoint nodeSyncPt = (COASyncPoint) succ;
					nodeSyncPt.incrementBranchesFinished();
				} else if (COANodeType.AwaitN == succ.getNodeType()) {
					COAAwaitN nodeAwaitN = (COAAwaitN) succ;
					nodeAwaitN.incrementBranchesFinished();
				}
			}
		}
	}
}
