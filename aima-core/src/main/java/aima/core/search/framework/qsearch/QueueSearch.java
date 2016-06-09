package aima.core.search.framework.qsearch;

import java.util.List;
import java.util.Queue;

import aima.core.agent.Action;
import aima.core.search.framework.Metrics;
import aima.core.search.framework.Node;
import aima.core.search.framework.NodeExpander;
import aima.core.search.framework.SearchUtils;
import aima.core.search.framework.problem.Problem;
import aima.core.util.CancelableThread;

/**
 * Base class for queue-based search implementations, especially for {@link TreeSearch},
 * {@link GraphSearch}, and {@link BidirectionalSearch}.
 * 
 * @author Ravi Mohan
 * @author Ciaran O'Reilly
 * @author Mike Stampone
 * @author Ruediger Lunde
 */
public abstract class QueueSearch {
	public static final String METRIC_NODES_EXPANDED = "nodesExpanded";
	public static final String METRIC_QUEUE_SIZE = "queueSize";
	public static final String METRIC_MAX_QUEUE_SIZE = "maxQueueSize";
	public static final String METRIC_PATH_COST = "pathCost";

	final protected NodeExpander nodeExpander;
	protected Queue<Node> frontier;
	protected boolean earlyGoalCheck = false;
	protected Metrics metrics = new Metrics();
	
	protected QueueSearch(NodeExpander nodeExpander) {
		this.nodeExpander = nodeExpander;
	}
	
	public NodeExpander getNodeExpander() {
		return nodeExpander;
	}
	
	/**
	 * Returns a list of actions to the goal if the goal was found, a list
	 * containing a single NoOp Action if already at the goal, or an empty list
	 * if the goal could not be found. This template method provides a base for
	 * tree and graph search implementations. It can be customized by overriding
	 * some primitive operations, especially {@link #addToFrontier(Node)},
	 * {@link #removeFromFrontier()}, and {@link #isFrontierEmpty()}.
	 * 
	 * @param problem
	 *            the search problem
	 * @param frontier
	 *            the collection of nodes that are waiting to be expanded
	 * 
	 * @return a list of actions to the goal if the goal was found, a list
	 *         containing a single NoOp Action if already at the goal, or an
	 *         empty list if the goal could not be found.
	 */
	public List<Action> search(Problem problem, Queue<Node> frontier) {
		this.frontier = frontier;
		clearInstrumentation();
		// initialize the frontier using the initial state of the problem
		Node root = nodeExpander.createRootNode(problem.getInitialState());
		if (earlyGoalCheck) {
			if (SearchUtils.isGoalState(problem, root))
				return getSolution(root);
		}
		addToFrontier(root);
		while (!isFrontierEmpty() && !CancelableThread.currIsCanceled()) {
			// choose a leaf node and remove it from the frontier
			Node nodeToExpand = removeFromFrontier();
			// Only need to check the nodeToExpand if have not already
			// checked before adding to the frontier
			if (!earlyGoalCheck) {
				// if the node contains a goal state then return the
				// corresponding solution
				if (SearchUtils.isGoalState(problem, nodeToExpand))
					return getSolution(nodeToExpand);
			}
			// expand the chosen node, adding the resulting nodes to the
			// frontier
			for (Node successor : nodeExpander.expand(nodeToExpand, problem)) {
				if (earlyGoalCheck) {
					if (SearchUtils.isGoalState(problem, successor))
						return getSolution(successor);
				}
				addToFrontier(successor);
			}
		}
		// if the frontier is empty then return failure
		return SearchUtils.failure();
	}

	/**
	 * Primitive operation which inserts the node at the tail of the frontier.
	 */
	protected abstract void addToFrontier(Node node);

	/**
	 * Primitive operation which removes and returns the node at the head of the
	 * frontier.
	 * 
	 * @return the node at the head of the frontier.
	 */
	protected abstract Node removeFromFrontier();

	/**
	 * Primitive operation which checks whether the frontier contains not yet
	 * expanded nodes.
	 */
	protected abstract boolean isFrontierEmpty();

	/**
	 * Enables optimization for FIFO queue based search, especially breadth
	 * first search.
	 * 
	 * @param state
	 */
	public void setEarlyGoalCheck(boolean state) {
		this.earlyGoalCheck = state;
	}

	
	/**
	 * Returns all the search metrics.
	 */
	public Metrics getMetrics() {
		metrics.set(METRIC_NODES_EXPANDED,nodeExpander.getNumOfExpandCalls());
		return metrics;
	}
	
	/**
	 * Sets all metrics to zero.
	 */
	public void clearInstrumentation() {
		nodeExpander.resetCounter();
		metrics.set(METRIC_NODES_EXPANDED, 0);
		metrics.set(METRIC_QUEUE_SIZE, 0);
		metrics.set(METRIC_MAX_QUEUE_SIZE, 0);
		metrics.set(METRIC_PATH_COST, 0);
	}

	protected void updateMetrics(int queueSize) {
		metrics.set(METRIC_QUEUE_SIZE, queueSize);
		int maxQSize = metrics.getInt(METRIC_MAX_QUEUE_SIZE);
		if (queueSize > maxQSize) {
			metrics.set(METRIC_MAX_QUEUE_SIZE, queueSize);
		}
	}
	
	private List<Action> getSolution(Node node) {
		metrics.set(METRIC_PATH_COST, node.getPathCost());
		return SearchUtils.getSequenceOfActions(node);
	}
}