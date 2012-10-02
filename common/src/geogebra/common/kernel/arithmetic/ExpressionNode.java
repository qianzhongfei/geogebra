/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * ExpressionNode.java
 *
 * binary tree node for ExpressionValues (NumberValues, VectorValues)
 *
 * Created on 03. Oktober 2001, 09:37
 */

package geogebra.common.kernel.arithmetic;

import geogebra.common.kernel.Kernel;
import geogebra.common.kernel.StringTemplate;
import geogebra.common.kernel.arithmetic.Traversing.Replacer;
import geogebra.common.kernel.arithmetic3D.Vector3DValue;
import geogebra.common.kernel.geos.CasEvaluableFunction;
import geogebra.common.kernel.geos.GeoDummyVariable;
import geogebra.common.kernel.geos.GeoElement;
import geogebra.common.kernel.geos.GeoFunction;
import geogebra.common.kernel.geos.GeoFunctionNVar;
import geogebra.common.kernel.geos.GeoLine;
import geogebra.common.kernel.geos.GeoNumeric;
import geogebra.common.kernel.geos.GeoPolygon;
import geogebra.common.kernel.geos.GeoSegment;
import geogebra.common.kernel.geos.GeoVec2D;
import geogebra.common.main.App;
import geogebra.common.main.MyError;
import geogebra.common.plugin.Operation;
import geogebra.common.util.StringUtil;
import geogebra.common.util.Unicode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tree node for expressions like "3*a - b/5"
 * 
 * @author Markus
 */
public class ExpressionNode extends ValidExpression implements
		ExpressionNodeConstants, ReplaceChildrenByValues {

	private App app;
	private Kernel kernel;
	private ExpressionValue left, right;
	private Operation operation = Operation.NO_OPERATION;
	private boolean forceVector = false, forcePoint = false,
			forceFunction = false;
	/** true if this holds text and the text is in LaTeX format */
	public boolean holdsLaTeXtext = false;

	/** for leaf mode */
	public boolean leaf = false;

	/**
	 * Creates dummy expression node
	 */
	public ExpressionNode() {
	}

	/**
	 * Creates new ExpressionNode
	 * 
	 * @param kernel
	 *            kernel
	 * @param left
	 *            left subexpression
	 * @param operation
	 *            operation
	 * @param right
	 *            right subexpression
	 * */
	public ExpressionNode(Kernel kernel, ExpressionValue left,
			Operation operation, ExpressionValue right) {
		this.kernel = kernel;
		app = kernel.getApplication();

		this.operation = operation;
		setLeft(left);
		if (right != null) {
			setRight(right);
		} else { // set dummy value
			setRight(new MyDouble(kernel, Double.NaN));
		}
	}

	/**
	 * for only one leaf (for wrapping ExpressionValues as ValidExpression)
	 * 
	 * @param kernel
	 *            Kernel
	 * @param leaf
	 *            value to be wrapped
	 */
	public ExpressionNode(Kernel kernel, ExpressionValue leaf) {
		this.kernel = kernel;
		app = kernel.getApplication();

		setLeft(leaf);
		this.leaf = true;
	}

	/**
	 * copy constructor: NO deep copy of subtrees is done here! this is needed
	 * for translation of functions
	 * 
	 * @param node
	 *            Node to copy
	 */
	public ExpressionNode(ExpressionNode node) {
		kernel = node.kernel;
		app = node.app;

		leaf = node.leaf;
		operation = node.operation;
		setLeft(node.left);
		setRight(node.right);
	}

	/**
	 * 
	 * wraps a double in an ExpressionNode
	 * 
	 * @param kernel2
	 *            kernel
	 * @param d
	 *            double
	 */
	public ExpressionNode(Kernel kernel2, double d) {
		this(kernel2, new MyDouble(kernel2, d));
	}

	public Kernel getKernel() {
		return kernel;
	}

	/**
	 * @return current operation
	 */
	final public Operation getOperation() {
		return operation;
	}

	/**
	 * @param op
	 *            new operation
	 */
	public void setOperation(Operation op) {
		operation = op;
	}

	/**
	 * @param flag
	 *            true if holds LaTeX text
	 */
	public void setHoldsLaTeXtext(boolean flag) {
		holdsLaTeXtext = flag;
	}

	/**
	 * @return left subexpression
	 */
	final public ExpressionValue getLeft() {
		return left;
	}

	/**
	 * @param l
	 *            left subexpression
	 */
	final public void setLeft(ExpressionValue l) {
		left = l;
		left.setInTree(true); // needed fot list operations eg k=2 then k {1,2}
	}

	/**
	 * Result is never null; for leaves, left is packed in ExpressionNode in
	 * result.
	 * 
	 * @return left subtree
	 */
	public ExpressionNode getLeftTree() {
		if (left.isExpressionNode()) {
			return (ExpressionNode) left;
		}
		return new ExpressionNode(kernel, left);
	}

	/**
	 * @return right subexpression
	 */
	final public ExpressionValue getRight() {
		return right;
	}

	/**
	 * @param r
	 *            new right subexpression
	 */
	final public void setRight(ExpressionValue r) {
		right = r;
		if (right != null) {
			right.setInTree(true); // needed for list operations eg k=2 then k
									// {1,2}
		}
		leaf = operation == Operation.NO_OPERATION; // right is a dummy MyDouble
													// by
		// default
	}

	/**
	 * @return right subtree
	 */
	public ExpressionNode getRightTree() {
		if (right == null) {
			return null;
		}

		if (right.isExpressionNode()) {
			return (ExpressionNode) right;
		}
		return new ExpressionNode(kernel, right);
	}

	public ExpressionValue deepCopy(Kernel kernel1) {
		return getCopy(kernel1);
	}

	/**
	 * copy the whole tree structure except leafs
	 * 
	 * @param kernel1
	 *            kernel
	 * @return copy of this node
	 * */
	public ExpressionNode getCopy(Kernel kernel1) {
		// Application.debug("getCopy() input: " + this);
		ExpressionNode newNode = null;
		ExpressionValue lev = null, rev = null;

		if (left != null) {
			lev = copy(left, kernel1);
		}
		if (right != null) {
			rev = copy(right, kernel1);
		}

		if (lev != null) {
			newNode = new ExpressionNode(kernel1, lev, operation, rev);
			newNode.leaf = leaf;
		} else {
			// something went wrong
			return null;
		}

		// set member vars that are not set by constructors
		newNode.forceVector = forceVector;
		newNode.forcePoint = forcePoint;
		newNode.forceFunction = forceFunction;
		// Application.debug("getCopy() output: " + newNode);
		return newNode;
	}

	/**
	 * deep copy except for GeoElements
	 * 
	 * @param ev
	 *            value to copy
	 * @param kernel
	 *            kernel
	 * @return copy of value
	 */
	public static ExpressionValue copy(ExpressionValue ev, Kernel kernel) {
		if (ev == null) {
			return null;
		}

		ExpressionValue ret = null;
		// Application.debug("copy ExpressionValue input: " + ev);
		if (ev.isExpressionNode()) {
			ExpressionNode en = (ExpressionNode) ev;
			ret = en.getCopy(kernel);
		} else if (ev instanceof MyList) {
			MyList en = (MyList) ev;
			ret = en.getCopy(kernel);
		}
		// deep copy
		else if (ev.isPolynomialInstance() || ev.isConstant()
				|| (ev instanceof Command)) {
			ret = ev.deepCopy(kernel);
		} else {
			ret = ev;
		}
		// Application.debug("copy ExpressionValue output: " + ev);
		return ret;
	}

	/**
	 * Replaces all ExpressionNodes in tree that are leafs (=wrappers) by their
	 * leaf objects (of type ExpressionValue).
	 */
	final public void simplifyLeafs() {
		if (left.isExpressionNode()) {
			ExpressionNode node = (ExpressionNode) left;
			if (node.leaf) {
				left = node.left;
				simplifyLeafs();
			}
		}

		if (right != null) {
			if (right.isExpressionNode()) {
				ExpressionNode node = (ExpressionNode) right;
				if (node.leaf) {
					right = node.left;
					simplifyLeafs();
				}
			}
		}
	}

	/**
	 * Replaces all Command objects in tree by their evaluated GeoElement
	 * objects.
	 */
	final private void simplifyAndEvalCommands() {
		// don't evaluate any commands for the CAS here
		if (kernel.isResolveUnkownVarsAsDummyGeos()) {
			return;
		}

		if (left.isExpressionNode()) {
			((ExpressionNode) left).simplifyAndEvalCommands();
		} else if (left instanceof Command) {
			left = ((Command) left).evaluate(StringTemplate.defaultTemplate);
		}

		if (right != null) {
			if (right.isExpressionNode()) {
				((ExpressionNode) right).simplifyAndEvalCommands();
			} else if (right instanceof Command) {
				right = ((Command) right)
						.evaluate(StringTemplate.defaultTemplate);
			}
		}
	}

	/**
	 * Replaces all constant parts in tree by their values
	 */
	final public void simplifyConstantIntegers() {
		if (left.isExpressionNode()) {
			ExpressionNode node = (ExpressionNode) left;
			if (left.isConstant()) {
				ExpressionValue eval = node
						.evaluate(StringTemplate.defaultTemplate);
				if (eval.isNumberValue()) {
					// we only simplify numbers that have integer values
					if (Kernel.isInteger(((NumberValue) eval).getDouble())) {
						left = eval;
					}
				} else {
					left = eval;
				}
			} else {
				node.simplifyConstantIntegers();
			}
		}

		if ((right != null) && right.isExpressionNode()) {
			ExpressionNode node = (ExpressionNode) right;
			if (right.isConstant()) {
				ExpressionValue eval = node
						.evaluate(StringTemplate.defaultTemplate);
				if (eval.isNumberValue()) {
					// we only simplify numbers that have integer values
					if (Kernel.isInteger(((NumberValue) eval).getDouble())) {
						right = eval;
					}
				} else {
					right = eval;
				}
			} else {
				node.simplifyConstantIntegers();
			}
		}
	}

	/*
	 * Markus: no longer needed as we have added rules in MathPiper to support
	 * this notation directly Expands equation expressions like (3*x + 2 = 5) /
	 * 2 to (3*x + 2)/2 = 5/2.
	 */
	// final public ExpressionValue expandEquationExpressions() {
	// if (leaf) return this;
	//
	// if (left.isExpressionNode()) {
	// left = ((ExpressionNode) left).expandEquationExpressions();
	// }
	// if (right.isExpressionNode()) {
	// right = ((ExpressionNode) right).expandEquationExpressions();
	// }
	//
	// switch (operation) {
	// case PLUS:
	// case MINUS:
	// case MULTIPLY:
	// case DIVIDE:
	// // equ <operation> val
	// if (left instanceof Equation) {
	// ((Equation) left).applyOperation(operation, right, false);
	// leaf = true;
	// right = null;
	// }
	// // val <operation> equ
	// else if (right instanceof Equation) {
	// ((Equation) right).applyOperation(operation, left, true);
	// left = right;
	// right = null;
	// leaf = true;
	// }
	// break;
	// }
	//
	// return this;
	// }

	// used for 3D
	/*
	 * protected ExpressionValue evaluate(ExpressionValue v){ return
	 * v.evaluate(); }
	 */

	/**
	 * Evaluates this expression
	 * 
	 * @param tpl
	 *            template (needed for possible string concatenation)
	 * @return value
	 */
	@Override
	public ExpressionValue evaluate(StringTemplate tpl) {
		return kernel.getExpressionNodeEvaluator().evaluate(this, tpl);
	}

	/*
	 * public ExpressionValue evaluate(boolean cache) { return
	 * kernel.getExpressionNodeEvaluator().evaluate(this); }
	 */

	/**
	 * look for Variable objects in the tree and replace them by their resolved
	 * GeoElement
	 */
	public final void resolveVariables(boolean forEquation) {
		doResolveVariables(forEquation);
		simplifyAndEvalCommands();
		simplifyLeafs();

		// left instanceof NumberValue needed rather than left.isNumberValue()
		// as left can be an
		// ExpressionNode, eg Normal[0,1,x]
		switch (operation) {
		case POWER: // eg e^x
			if ((left instanceof NumberValue)
					&& (((NumberValue) left).getDouble() == Math.E)) {
				GeoElement geo = kernel.lookupLabel("e");
				if ((geo != null) && geo.needsReplacingInExpressionNode()) {

					// replace e^x with exp(x)
					// if e was autocreated
					operation = Operation.EXP;
					left = right;
					kernel.getConstruction().removeLabel(geo);
				}
			}
			break;
		case MULTIPLY: // eg 1 * e or e * 1
		case DIVIDE: // eg 1 / e or e / 1
		case PLUS: // eg 1 + e or e + 1
		case MINUS: // eg 1 - e or e - 1
			if ((left instanceof NumberValue)
					&& (((NumberValue) left).getDouble() == Math.E)) {
				GeoElement geo = kernel.lookupLabel("e");
				if ((geo != null) && geo.needsReplacingInExpressionNode()) {

					// replace 'e' with exp(1)
					// if e was autocreated
					left = new ExpressionNode(kernel,
							new MyDouble(kernel, 1.0), Operation.EXP, null);
					kernel.getConstruction().removeLabel(geo);
				}
			} else if ((right instanceof NumberValue)
					&& (((NumberValue) right).getDouble() == Math.E)) {
				GeoElement geo = kernel.lookupLabel("e");
				if ((geo != null) && geo.needsReplacingInExpressionNode()) {

					// replace 'e' with exp(1)
					// if e was autocreated
					right = new ExpressionNode(kernel,
							new MyDouble(kernel, 1.0), Operation.EXP, null);
					kernel.getConstruction().removeLabel(geo);
				}
			}
			break;
		}
	}

	private void doResolveVariables(boolean forEquation) {
		// resolve left wing
		if (left.isVariable()) {
			left = ((Variable) left).resolveAsExpressionValue(forEquation);
		} else {
			left.resolveVariables(forEquation);
		}

		// resolve right wing
		if (right != null) {
			if (right.isVariable()) {
				right = ((Variable) right).resolveAsExpressionValue(forEquation);
			} else {
				right.resolveVariables(forEquation);
			}
		}
	}

	/**
	 * look for GeoFunction objects in the tree and replace them by FUNCTION
	 * ExpressionNodes. This makes operations like f + g possible by changing
	 * this to f(x) + g(x)
	 * 
	 * public void wrapGeoFunctionsAsExpressionNode() { Polynomial polyX = new
	 * Polynomial(kernel, "x");
	 * 
	 * // left wing if (left.isExpressionNode()) {
	 * ((ExpressionNode)left).wrapGeoFunctionsAsExpressionNode(); } else if
	 * (left instanceof GeoFunction) { left = new ExpressionNode(kernel, left,
	 * ExpressionNode.FUNCTION, polyX); }
	 * 
	 * // resolve right wing if (right != null) { if (right.isExpressionNode())
	 * { ((ExpressionNode)right).wrapGeoFunctionsAsExpressionNode(); } else if
	 * (right instanceof GeoFunction) { right = new ExpressionNode(kernel,
	 * right, ExpressionNode.FUNCTION, polyX); } } }
	 */

	/**
	 * @return true if there is at least one Polynomial in the tree
	 */
	public boolean includesPolynomial() {
		return getPolynomialVars().size() > 0;
	}

	/**
	 * Returns all polynomial variables (x, y, and/or z) in this tree as a list.
	 * 
	 * @return list with all variables as Strings
	 */
	public TreeSet<String> getPolynomialVars() {
		TreeSet<String> vars = new TreeSet<String>();
		getPolynomialVars(vars);
		return vars;
	}

	/**
	 * Adds all polynomial variables (x, y, and/or z) in this tree to vars.
	 * 
	 * @param vars
	 *            the set to add all variables as Strings
	 */
	private void getPolynomialVars(Set<String> vars) {
		if (left.isExpressionNode()) {
			((ExpressionNode) left).getPolynomialVars(vars);
		} else if (left.isPolynomialInstance()) {
			vars.add(left.toString(StringTemplate.defaultTemplate));
		}

		if (right != null) {
			if (right.isExpressionNode()) {
				((ExpressionNode) right).getPolynomialVars(vars);
			} else if (right.isPolynomialInstance()) {
				vars.add(right.toString(StringTemplate.defaultTemplate));
				// changed to avoid deepcopy in MyList.getMyList() eg
				// Sequence[f(Element[GP,k]),k,1,NP]
				// } else if (right.isListValue()){ //to get polynomial vars in
				// GeoFunctionNVar
				// MyList list=((ListValue)right).getMyList();
			} else if (right instanceof MyList) { // to get polynomial
													// vars in
													// GeoFunctionNVar
				MyList list = (MyList) right;
				for (int i = 0; i < list.size(); i++) {
					ExpressionValue elem = list.getListElement(i);
					if (elem.isExpressionNode()) {
						((ExpressionNode) elem).getPolynomialVars(vars);
					} else if (elem.isPolynomialInstance()) {
						vars.add(elem.toString(StringTemplate.defaultTemplate));
					}
				}
			}
		}
	}

	/**
	 * Returns whether this ExpressionNode should evaluate to a GeoVector. This
	 * method returns true when all GeoElements in this tree are GeoVectors and
	 * there are no other constanct VectorValues (i.e. constant points)
	 * 
	 * @return true if this should evaluate to GeoVector
	 */
	public boolean shouldEvaluateToGeoVector() {
		boolean evalToVector = false;

		if (left.isExpressionNode()) {
			evalToVector = (((ExpressionNode) left).shouldEvaluateToGeoVector());
		} else if (left.isGeoElement()) {
			GeoElement geo = (GeoElement) left;
			evalToVector = geo.isGeoVector() || geo.isNumberValue();
		} else if (left.isNumberValue()) {
			evalToVector = true;
		}

		if ((right != null) && evalToVector) {
			if (right.isExpressionNode()) {
				evalToVector = ((ExpressionNode) right)
						.shouldEvaluateToGeoVector();
			} else if (right.isGeoElement()) {
				GeoElement geo = (GeoElement) right;
				evalToVector = geo.isGeoVector() || geo.isNumberValue();
			} else if (right.isNumberValue()) {
				evalToVector = true;
			}
		}

		return evalToVector;
	}

	/**
	 * Returns true if this tree includes a division by val
	 * 
	 * @param val
	 *            possible divisor
	 * @return true iff contains division by val
	 */
	final public boolean includesDivisionBy(ExpressionValue val) {
		if (operation == Operation.DIVIDE) {
			if (right.contains(val)) {
				return true;
			}

			if (left.isExpressionNode()
					&& ((ExpressionNode) left).includesDivisionBy(val)) {
				return true;
			}
		} else {
			if (left.isExpressionNode()
					&& ((ExpressionNode) left).includesDivisionBy(val)) {
				return true;
			}

			if ((right != null) && right.isExpressionNode()
					&& ((ExpressionNode) right).includesDivisionBy(val)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Replaces all Variable objects with the given varName in tree by the given
	 * FunctionVariable object.
	 * 
	 * Only works if the varName is inserted without CAS prefix
	 * 
	 * @param varName
	 *            variable name
	 * @param fVar
	 *            replacement variable
	 * 
	 * @return number of replacements done
	 */
	public final int replaceVariables(String varName, FunctionVariable fVar) {
		int replacements = 0;

		// left tree
		if (left.isExpressionNode()) {
			replacements += ((ExpressionNode) left).replaceVariables(varName,
					fVar);
		} else if (left instanceof MyList) {
			replacements += ((MyList) left).replaceVariables(varName, fVar);
		} else if (left instanceof Variable) {
			if (varName.equals(((Variable) left)
					.getName(StringTemplate.defaultTemplate))) {
				left = fVar;
				replacements++;
			}
		}
		if (left instanceof GeoDummyVariable) {
			if (varName.equals(((GeoDummyVariable) left)
					.toString(StringTemplate.defaultTemplate))) {
				left = fVar;
				replacements++;
			}
		} else if (left instanceof FunctionVariable) {
			if (varName.equals(((FunctionVariable) left)
					.toString(StringTemplate.defaultTemplate))) {
				left = fVar;
				replacements++;
			}
		}

		// right tree
		if (right != null) {
			if (right.isExpressionNode()) {
				replacements += ((ExpressionNode) right).replaceVariables(
						varName, fVar);
			} else if (right instanceof MyList) {
				replacements += ((MyList) right)
						.replaceVariables(varName, fVar);
			} else if (right instanceof Variable) {
				if (varName.equals(((Variable) right)
						.getName(StringTemplate.defaultTemplate))) {
					right = fVar;
					replacements++;
				}
			} else if (right instanceof GeoDummyVariable) {
				if (varName.equals(((GeoDummyVariable) right)
						.toString(StringTemplate.defaultTemplate))) {
					right = fVar;
					replacements++;
				}
			} else if (right instanceof FunctionVariable) {
				if (varName.equals(((FunctionVariable) right)
						.toString(StringTemplate.defaultTemplate))) {
					right = fVar;
					replacements++;
				}
			}
		}

		return replacements;
	}

	/**
	 * Replaces all Polynomials in tree by function variable
	 * 
	 * @param x
	 *            replacement variable
	 * 
	 * @return number of replacements done
	 */
	protected int replacePolynomials(FunctionVariable x) {
		int replacements = 0;

		// left tree
		if (left.isExpressionNode()) {
			replacements += ((ExpressionNode) left).replacePolynomials(x);
		} else if (left instanceof MyList) {
			replacements += ((MyList) left).replacePolynomials(x);
		} else if (left.isPolynomialInstance()
				&& x.toString(StringTemplate.defaultTemplate).equals(
						left.toString(StringTemplate.defaultTemplate))) {
			left = x;
			replacements++;
		}

		// right tree
		if (right != null) {
			if (right.isExpressionNode()) {
				replacements += ((ExpressionNode) right).replacePolynomials(x);
			} else if (right instanceof MyList) {
				replacements += ((MyList) right).replacePolynomials(x);
			} else if (right.isPolynomialInstance()
					&& x.toString(StringTemplate.defaultTemplate).equals(
							right.toString(StringTemplate.defaultTemplate))) {
				right = x;
				replacements++;
			}
		}

		return replacements;
	}

	/**
	 * Replaces all XCOORD, YCOORD, ZCOORD nodes by mutliplication nodes, e.g.
	 * x(x+1) becomes x*(x+1). The given function variables for "x", "y", "z"
	 * are used in this process.
	 * 
	 * @param xVar
	 *            variable x
	 * @param yVar
	 *            variable y
	 * @param zVar
	 *            variable z
	 * @param undecided
	 *            list for subexpressions where it's not clear whether they can
	 *            be used for multiplication directly or not
	 * 
	 * @return number of replacements done
	 */
	protected int replaceXYZnodes(FunctionVariable xVar, FunctionVariable yVar,
			FunctionVariable zVar, ArrayList<ExpressionNode> undecided) {
		if ((xVar == null) && ((yVar == null) & (zVar == null))) {
			return 0;
		}

		// left tree
		if (left.isExpressionNode()) {
			((ExpressionNode) left)
					.replaceXYZnodes(xVar, yVar, zVar, undecided);
		}
		// right tree
		if ((right != null) && right.isExpressionNode()) {
			((ExpressionNode) right).replaceXYZnodes(xVar, yVar, zVar,
					undecided);
		}

		switch (operation) {
		case XCOORD:
			if (xVar != null) {
				undecided.add(this);
				operation = Operation.MULTIPLY_OR_FUNCTION;
				right = left;
				left = xVar;
			}
			break;

		case YCOORD:
			if (yVar != null) {
				undecided.add(this);
				operation = Operation.MULTIPLY_OR_FUNCTION;
				right = left;
				left = yVar;
			}
			break;

		case ZCOORD:
			if (zVar != null) {
				undecided.add(this);
				operation = Operation.MULTIPLY_OR_FUNCTION;
				right = left;
				left = zVar;
			}
			break;
		case POWER:
			if (left.isExpressionNode()
					&& ((ExpressionNode) left).operation == Operation.MULTIPLY_OR_FUNCTION) {
				right = new ExpressionNode(kernel,
						((ExpressionNode) left).getRight(), Operation.POWER,
						right);
				left = ((ExpressionNode) left).getLeft();
				operation = Operation.MULTIPLY;
			}
			break;
		case FACTORIAL:
			if (left.isExpressionNode()
					&& ((ExpressionNode) left).operation == Operation.MULTIPLY_OR_FUNCTION) {
				right = new ExpressionNode(kernel,
						((ExpressionNode) left).getRight(),
						Operation.FACTORIAL, null);
				left = ((ExpressionNode) left).getLeft();
				operation = Operation.MULTIPLY;
			}
		case SQRT_SHORT:
			if (left.isExpressionNode()
					&& ((ExpressionNode) left).operation == Operation.MULTIPLY_OR_FUNCTION) {
				right = ((ExpressionNode) left).getRight();
				left = new ExpressionNode(kernel,
						((ExpressionNode) left).getLeft(), Operation.SQRT, null);
				operation = Operation.MULTIPLY;
			}
		}

		return undecided.size();
	}

	@Override
	public ExpressionValue traverse(Traversing t) {
		ExpressionValue ev = t.process(this);
		left = left.traverse(t);
		if (right != null)
			right = right.traverse(t);
		return ev;
	}

	public void replaceChildrenByValues(GeoElement geo) {
		// left tree
		if (left.isGeoElement()) {
			GeoElement treeGeo = (GeoElement) left;
			if ((left == geo) || treeGeo.isChildOf(geo)) {
				left = treeGeo.copyInternal(treeGeo.getConstruction());
			}
		} else if (left instanceof ReplaceChildrenByValues) {
			((ReplaceChildrenByValues) left).replaceChildrenByValues(geo);
		}

		// right tree
		if (right != null) {
			if (right.isGeoElement()) {
				GeoElement treeGeo = (GeoElement) right;
				if ((right == geo) || treeGeo.isChildOf(geo)) {
					right = treeGeo.copyInternal(treeGeo.getConstruction());
				}
			} else if (right instanceof ReplaceChildrenByValues) {
				((ReplaceChildrenByValues) right).replaceChildrenByValues(geo);
			}
		}
	}

	/**
	 * Returns true when the given object is found in this expression tree.
	 */
	final public boolean contains(ExpressionValue ev) {
		if (leaf) {
			return left.contains(ev);
		}
		return left.contains(ev) || right.contains(ev);
	}

	/**
	 * @return true when function variable is found in this expression tree.
	 */
	final public boolean containsFunctionVariable() {
		if ((left instanceof FunctionVariable)
				|| (right instanceof FunctionVariable)) {
			return true;
		}

		if ((left instanceof ExpressionNode)
				&& ((ExpressionNode) left).containsFunctionVariable()) {
			return true;
		}
		if ((right instanceof ExpressionNode)
				&& ((ExpressionNode) right).containsFunctionVariable()) {
			return true;
		}

		return false;
	}

	/**
	 * @return true if contains CAS evaluable function
	 */
	final public boolean containsCasEvaluableFunction() {
		if ((left instanceof CasEvaluableFunction)
				|| (right instanceof CasEvaluableFunction)) {
			return true;
		}

		if ((left instanceof ExpressionNode)
				&& ((ExpressionNode) left).containsCasEvaluableFunction()) {
			return true;
		}
		if ((right instanceof ExpressionNode)
				&& ((ExpressionNode) right).containsCasEvaluableFunction()) {
			return true;
		}

		return false;
	}

	/**
	 * @return true when the FunctionNVar is found in this expression tree.
	 */
	final public boolean containsGeoFunctionNVar() {
		if ((left instanceof GeoFunctionNVar)
				|| (right instanceof GeoFunctionNVar)) {
			return true;
		}

		if ((left instanceof ExpressionNode)
				&& ((ExpressionNode) left).containsGeoFunctionNVar()) {
			return true;
		}
		if ((right instanceof ExpressionNode)
				&& ((ExpressionNode) right).containsGeoFunctionNVar()) {
			return true;
		}

		return false;
	}

	/**
	 * transfers every non-polynomial in this tree to a polynomial. This is
	 * needed to enable polynomial simplification by evaluate()
	 * 
	 * @param equ
	 *            equation
	 */
	protected final void makePolynomialTree(Equation equ) {

		if (operation == Operation.FUNCTION_NVAR) {
			if ((left instanceof FunctionalNVar) && (right instanceof MyList)) {
				MyList list = ((MyList) right);
				FunctionNVar func = ((FunctionalNVar) left).getFunction();
				ExpressionNode expr = func.getExpression().getCopy(kernel);
				if (func.getFunctionVariables().length == list.size()) {
					for (int i = 0; i < list.size(); i++) {
						ExpressionValue ev = list.getListElement(i);
						if (ev instanceof ExpressionNode) {
							ExpressionNode en = ((ExpressionNode) ev)
									.getCopy(kernel);
							if (!equ.isFunctionDependent()) {
								equ.setFunctionDependent(en
										.includesPolynomial());
							}
							// we may only make polynomial trees after replacement
							// en.makePolynomialTree(equ);
							ev = en;
						} else if (list.getListElement(i)
								.isPolynomialInstance()) {
							equ.setFunctionDependent(true);
						}
						expr = expr.replace(
								func.getFunctionVariables()[i], ev).wrap();
					}
				} else {
					throw new MyError(app,
							new String[] { "IllegalArgumentNumber" });
				}
				expr.makePolynomialTree(equ);
				left = expr.left;
				right = expr.right;
				operation = expr.getOperation();
			}
		} else if (operation == Operation.FUNCTION) {
			if (left instanceof GeoFunction) {
				Function func = ((Functional) left).getFunction();
				ExpressionNode expr = func.getExpression().getCopy(kernel);
				if (right instanceof ExpressionNode) {
					if (!equ.isFunctionDependent()) {
						equ.setFunctionDependent(((ExpressionNode) right)
								.includesPolynomial());
					}
					// we may only make polynomial trees after replacement
					// ((ExpressionNode) right).makePolynomialTree(equ);
				} else if (right.isPolynomialInstance()) {
					equ.setFunctionDependent(true);
				}
				expr = expr.replace(func.getFunctionVariable(), right).wrap();
				expr.makePolynomialTree(equ);
				left = expr.left;
				right = expr.right;
				operation = expr.getOperation();
			}
		}
		if (!polynomialOperation(operation)) {
			left = new Polynomial(kernel, new Term( new ExpressionNode(
					kernel, left, operation, right), ""));
			leaf = true;
			operation = Operation.NO_OPERATION;
			right = null;
			return;
		}
		
		// transfer left subtree
		if (left.isExpressionNode()) {
			((ExpressionNode) left).makePolynomialTree(equ);
		} else if (!(left.isPolynomialInstance())) {
			left = new Polynomial(kernel, new Term( left, ""));
		}

		// transfer right subtree
		if (right != null) {
			if (right.isExpressionNode()) {
				((ExpressionNode) right).makePolynomialTree(equ);
			} else if (right instanceof MyList) {
				MyList list = (MyList) right;
				for (int i = 0; i < list.size(); i++) {
					ExpressionValue ev = list.getListElement(i);
					if (ev instanceof ExpressionNode) {
						((ExpressionNode) ev).makePolynomialTree(equ);
					}
				}
			} else if (!(right.isPolynomialInstance())) {
				right = new Polynomial(kernel, new Term( right, ""));
			}
		}
	}

	private static boolean polynomialOperation(Operation operation2) {
		switch (operation2) {
		case NO_OPERATION:
		case PLUS:
		case MINUS:
		case MULTIPLY:
		case MULTIPLY_OR_FUNCTION:
		case DIVIDE:
		case POWER:
		case FUNCTION:
		case FUNCTION_NVAR:
			return true;
		}
		return false;
	}

	/**
	 * returns true, if there are no variable objects in the subtree
	 */
	final public boolean isConstant() {
		if (isLeaf()) {
			return left.isConstant();
		}
		return left.isConstant() && right.isConstant();
	}

	/**
	 * returns true, if no variable is a point (GeoPoint)
	 */
	final public boolean isVectorValue() {
		if (forcePoint) {
			return false;
		}
		if (forceVector) {
			return true;
		}

		return shouldEvaluateToGeoVector();
	}

	/**
	 * Force this to evaluate to vector
	 */
	public void setForceVector() {
		// this expression should be considered as a vector, not a point
		forceVector = true;
	}

	/**
	 * @return true if forced to evaluate to vector
	 */
	final public boolean isForcedVector() {
		return forceVector;
	}

	/**
	 * 
	 */
	public void setForcePoint() {
		// this expression should be considered as a point, not a vector
		forcePoint = true;
	}

	/**
	 * @return true iff forced to evaluate to point
	 */
	final public boolean isForcedPoint() {
		return forcePoint;
	}

	/**
	 * Force to evaluate to function
	 */
	public void setForceFunction() {
		// this expression should be considered as a point, not a vector
		forceFunction = true;
	}

	/**
	 * @return true iff forced to be a function
	 */
	final public boolean isForcedFunction() {
		return forceFunction;
	}

	/**
	 * Returns whether this tree has any operations
	 * 
	 * @return true iff this tree has any operations
	 */
	final public boolean hasOperations() {
		if (leaf) {
			if (left.isExpressionNode()) {
				((ExpressionNode) left).hasOperations();
			} else {
				return false;
			}
		}

		return (right != null);
	}

	/**
	 * Returns all GeoElement objects in the subtree
	 */
	final public HashSet<GeoElement> getVariables() {
		if (leaf) {
			return left.getVariables();
		}

		HashSet<GeoElement> leftVars = left.getVariables();
		HashSet<GeoElement> rightVars = right.getVariables();
		if (leftVars == null) {
			return rightVars;
		} else if (rightVars == null) {
			return leftVars;
		} else {
			leftVars.addAll(rightVars);
			return leftVars;
		}
	}

	/**
	 * @return GeoElement variables
	 */
	final public GeoElement[] getGeoElementVariables() {
		HashSet<GeoElement> varset = getVariables();
		if (varset == null) {
			return null;
		}
		Iterator<GeoElement> i = varset.iterator();
		GeoElement[] ret = new GeoElement[varset.size()];
		int j = 0;
		while (i.hasNext()) {
			ret[j++] = i.next();
		}
		return ret;
	}

	final public boolean isLeaf() {
		return leaf; // || operation == NO_OPERATION;
	}

	/**
	 * @return true if this is leaf containing only GeoElement
	 */
	final public boolean isSingleGeoElement() {
		return leaf && left.isGeoElement();
	}

	/**
	 * @return left subexpression as GeoElement
	 */
	final public GeoElement getSingleGeoElement() {
		return (GeoElement) left;
	}

	/**
	 * @return true if this is leaf containing only Variable
	 */
	public boolean isSingleVariable() {
		return (isLeaf() && (left instanceof Variable));
	}

	/**
	 * @return true if this is leaf containing only imaginary unit
	 */
	public boolean isImaginaryUnit() {
		return (isLeaf() && (left instanceof GeoVec2D) && ((GeoVec2D) left)
				.isImaginaryUnit());
	}

	/**
	 * Returns a string representation of this node that can be used with the
	 * given CAS, e.g. "*" and "^" are always printed.
	 * 
	 * @param symbolic
	 *            true for variable names, false for values of variables
	 * @param tpl
	 *            String template
	 * @return string representation of this node that can be used with given
	 *         CAS
	 */
	final public String getCASstring(StringTemplate tpl, boolean symbolic) {
		String ret = printCASstring(symbolic, tpl);
		return ret;
	}

	/**
	 * @return true iff this node contains MyStringBuffer as subnode
	 */
	public boolean containsMyStringBuffer() {

		if ((left instanceof MyStringBuffer)
				|| (right instanceof MyStringBuffer)) {
			return true;
		}

		boolean ret = false;

		if (left.isExpressionNode()) {
			ret = ret || ((ExpressionNode) left).containsMyStringBuffer();
		}
		if ((right != null) && right.isExpressionNode()) {
			ret = ret || ((ExpressionNode) right).containsMyStringBuffer();
		}

		return ret;

	}

	/*
	 * splits a string up for editing with the Text Tool adapted from
	 * printCASstring()
	 */

	private String printCASstring(boolean symbolic, StringTemplate tpl) {
		String ret = null;

		try {

			if (leaf) { // leaf is GeoElement or not
				/*
				 * if (symbolic) { if (left.isGeoElement()) ret = ((GeoElement)
				 * left).getLabel(); else if (left.isExpressionNode()) ret =
				 * ((ExpressionNode)left).printJSCLString(symbolic); else ret =
				 * left.toString(); } else { ret = left.toValueString(); }
				 */

				if (symbolic && left.isGeoElement()) {
					ret = ((GeoElement) left).getLabel(tpl);
				} else if (left.isExpressionNode()) {
					ret = ((ExpressionNode) left).printCASstring(symbolic, tpl);
				} else {
					ret = symbolic ? left.toString(tpl) : left
							.toValueString(tpl);
				}
			}

			// STANDARD case: no leaf
			else {
				// expression node
				
				// first we need to group the factors of all possible numerators
				numerGroup();
				
				String leftStr = null, rightStr = null;
				if (symbolic && left.isGeoElement()) {
					leftStr = ((GeoElement) left).getLabel(tpl);
				} else if (left.isExpressionNode()) {
					leftStr = ((ExpressionNode) left).printCASstring(symbolic,
							tpl);
				} else {
					leftStr = symbolic ? left.toString(tpl) : left
							.toValueString(tpl);
				}

				if (right != null) {
					if (symbolic && right.isGeoElement()) {
						rightStr = ((GeoElement) right).getLabel(tpl);
					} else if (right.isExpressionNode()) {
						rightStr = ((ExpressionNode) right).printCASstring(
								symbolic, tpl);
					} else {
						rightStr = symbolic ? right.toString(tpl) : right
								.toValueString(tpl);
					}
				}
				ret = operationToString(leftStr, rightStr, !symbolic, tpl);
			}
		} finally {
			// do nothing
		}

		return ret;
	}

	private ExpressionNode numerGroup() {
		
		ExpressionNode ret = null;
		
		if (isLeaf()) {
			return this; //no change
		}
		
		if (operation == Operation.MULTIPLY) {

			if (!right.isExpressionNode())
				return this; //no change
			
			ExpressionNode r = (ExpressionNode)right;
			
			if (r == null || r.isLeaf())
				return this; //no change
		
			if (r.operation == Operation.MULTIPLY) {
				right = r.numerGroup(); //now right must be an ExpressionNode.
				r = (ExpressionNode)right;
			}
			
			if (r.operation == Operation.DIVIDE) {
				
				ExpressionNode newL;
				
				newL = new ExpressionNode(kernel, this.left, Operation.MULTIPLY, r.left);
				
				this.setOperation(Operation.DIVIDE);
				setLeft(newL);
				setRight(r.right);
				ret = r;
			}	
			
		} else {
			if (left!=null && left.isExpressionNode())
				((ExpressionNode)left).numerGroup();
			if (right!=null && right.isExpressionNode())
				((ExpressionNode)right).numerGroup();
			ret = this;
		}
			
		return ret;
	}

	/**
	 * @return a representation of all classes present in the tree
	 */
	final public String getTreeClass() {
		return getTreeClass("");
	}

	/**
	 * Decide whether the current expression value must be expanded
	 * for OpenGeoProver. This code helps both the Wu and the Area method.
	 * @param ev left or right side of the expression
	 * @return if expansion is required
	 */
	final private boolean expandForOGP(ExpressionValue ev) {
		// The following types of operations and GeoElements are supported.
		// See also the OGP code for the available (parsable) expressions.
		if (operation.equals(Operation.EQUAL_BOOLEAN) && ev instanceof GeoSegment)
			return false; // don't expand "AreEqual[Segment[X,Y],Segment[Z,W]]" format expressions
		return ((operation.equals(Operation.EQUAL_BOOLEAN) || operation.equals(Operation.DIVIDE)
				|| operation.equals(Operation.MULTIPLY) || operation.equals(Operation.MINUS)
				|| operation.equals(Operation.PLUS) || operation.equals(Operation.POWER))
		&& (ev instanceof GeoSegment || ev instanceof GeoPolygon || ev instanceof GeoNumeric));
	}
	
	/**
	 * @param prefix
	 * @return a representation of all classes present in the tree
	 */
	final private String getTreeClass(String prefix) {

		String ret = "";

		ret += "-\n";

		if (left != null) {
			ret += prefix + "  \\l:";
			if (left instanceof ExpressionNode) {
				ret += ((ExpressionNode) left).getTreeClass(prefix + "   ");
			} else {
				ret += left.getClass();
			}
			ret += "\n";
		}

		if (right != null) {
			ret += prefix + "  \\r:";
			if (right instanceof ExpressionNode) {
				ret += ((ExpressionNode) right).getTreeClass(prefix + "   ");
			} else {
				ret += right.getClass();
			}
			ret += "\n";
		}

		return ret;

	}

	/**
	 * Returns a string representation of this node.
	 */
	@Override
	final public String toString(StringTemplate tpl) {
		if (leaf) { // leaf is GeoElement or not
			if (left.isGeoElement()) {
				return ((GeoElement) left).getLabel(tpl);
			}
			return left.toString(tpl);
		}

		// expression node
		String leftStr = null, rightStr = null;
		if (left.isGeoElement()) {
			if (tpl.getStringType().equals(StringType.OGP)
					&& expandForOGP(left)) {
				leftStr = ((GeoElement) left).getCommandDescription(tpl);
			} else
				leftStr = ((GeoElement) left).getLabel(tpl);
		} else {
			leftStr = left.toString(tpl);
		}

		if (right != null) {
			if (right.isGeoElement()) {
				if (tpl.getStringType().equals(StringType.OGP)
						&&  expandForOGP(right)) {
					rightStr = ((GeoElement) right).getCommandDescription(tpl);
				} else
					rightStr = ((GeoElement) right).getLabel(tpl);
			} else {
				rightStr = right.toString(tpl);
			}
		}
		return operationToString(leftStr, rightStr, false, tpl);
	}

	/** like toString() but with current values of variables */
	@Override
	final public String toValueString(StringTemplate tpl) {
		if (isLeaf()) { // leaf is GeoElement or not
			if (left != null) {
				return left.toValueString(tpl);
			}
		}

		// expression node
		String leftStr = left.toValueString(tpl);

		String rightStr = null;
		if (right != null) {
			rightStr = right.toValueString(tpl);
		}

		return operationToString(leftStr, rightStr, true, tpl);
	}

	final public String toOutputValueString(StringTemplate tpl) {
		if (isLeaf()) { // leaf is GeoElement or not
			if (left != null) {
				return left.toOutputValueString(tpl);
			}
		}

		// expression node
		String leftStr = left.toOutputValueString(tpl);

		String rightStr = null;
		if (right != null) {
			rightStr = right.toOutputValueString(tpl);
		}

		return operationToString(leftStr, rightStr, true, tpl);
	}

	/**
	 * Returns a string representation of this node in LaTeX syntax. Note: the
	 * resulting string may contain special unicode characters like greek
	 * characters or special signs for integer exponents. These sould be handled
	 * afterwards!
	 * 
	 * @param symbolic
	 *            true for variable names, false for values of variables
	 */
	final public String toLaTeXString(boolean symbolic, StringTemplate tpl) {
		String ret;

		if (isLeaf()) { // leaf is GeoElement or not
			if (left != null) {
				ret = left.toLaTeXString(symbolic, tpl);

				return checkMathML(ret, tpl);
			}
		}

		// expression node
		String leftStr = left.toLaTeXString(symbolic, tpl);
		String rightStr = null;
		if (right != null) {
			rightStr = right.toLaTeXString(symbolic, tpl);
			if (((operation == Operation.FUNCTION_NVAR) || (operation == Operation.ELEMENT_OF))
					&& (right instanceof MyList)) {
				// 1 character will be taken from the left and right
				// of rightStr in operationToString, but more
				// is necessary in case of LaTeX, we do that here
				// " \\{ " is put by MyList 5 - 1(escape) -1(operationToString)
				rightStr = rightStr.substring(3, rightStr.length() - 3);
			}
		}

		// build latex string
		ret = operationToString(leftStr, rightStr, !symbolic, tpl);

		return checkMathML(ret, tpl);
	}

	/**
	 * make sure string wrapped in MathML if necessary eg <ci>x</ci>
	 */
	private static String checkMathML(String str, StringTemplate tpl) {
		if (tpl.hasType(StringType.MATHML) && str.charAt(0) != '<') {
			return "<ci>" + str + "</ci>";
		}

		return str;
	}

	/**
	 * Returns a string representation of this node. Note: STRING_TYPE is used
	 * for LaTeX, MathPiper, Jasymca conform output, valueForm is used by
	 * toValueString(), forLaTeX is used for LaTeX output
	 * 
	 */
	final private String operationToString(String leftStr, String rightStr,
			boolean valueForm, StringTemplate tpl) {

		ExpressionValue leftEval;
		StringBuilder sb = new StringBuilder();

		StringType STRING_TYPE = tpl.getStringType();

		switch (operation) {
		case NOT:

			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<not/>", leftStr, null);
			}else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				sb.append("mynot(");
				sb.append(leftStr);
				sb.append(')');
			} else {

				switch (STRING_TYPE) {
				case MATHML:

					break;
				case LATEX:
					sb.append("\\neg ");
					break;
					
				case LIBRE_OFFICE:
					sb.append("neg ");
					break;
					
				case MATH_PIPER:
					sb.append("Not ");
					break;

				default:
					sb.append(strNOT);
				}
				if (left.isLeaf()) {
					sb.append(leftStr);
				} else {
					sb.append(leftBracket(STRING_TYPE));
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
				}
			}
			break;

		case OR:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<or/>", leftStr, rightStr);
			} else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				appendOp(sb,"sor", leftStr, rightStr);
			}else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				sb.append(' ');

				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\vee");
					break;				
				case LIBRE_OFFICE:
					sb.append("or");
					break;
				
				case MATH_PIPER:
					sb.append("Or");
					break;

				case MPREDUCE:
					sb.append("or ");
					break;

				default:
					sb.append(strOR);
				}

				sb.append(' ');
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case AND:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<and/>", leftStr, rightStr);
			}else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				appendOp(sb,"sand", leftStr, rightStr);
			} else {
				append(sb, leftStr, left, operation, STRING_TYPE);

				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\wedge");
					break;

				case LIBRE_OFFICE:
					sb.append("and");
					break;
					
				case MATH_PIPER:
					sb.append("And");
					break;

				case MPREDUCE:
					sb.append("and ");
					break;

				default:
					sb.append(strAND);
				}
				sb.append(' ');

				append(sb, rightStr, right, operation, STRING_TYPE);
			}
			break;
		case IMPLICATION:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<implies/>", leftStr, rightStr);
			}else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				appendOp(sb,"simplies", leftStr, rightStr);
			} else {
				if (STRING_TYPE != StringType.MPREDUCE)
					append(sb, leftStr, left, operation, STRING_TYPE);

				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\to");
					break;

				case LIBRE_OFFICE:
					sb.append("toward"); //don't know if it is correct TAM 5/28/2012
					break;

				default:
					sb.append(strIMPLIES);
				}
				sb.append(' ');

				if (STRING_TYPE != StringType.MPREDUCE)
				append(sb, rightStr, right, operation, STRING_TYPE);
			}
			break;

		case EQUAL_BOOLEAN:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<eq/>", leftStr, rightStr);
			} else if (STRING_TYPE.equals(StringType.OGP)) {
				sb.append("AreEqual[" + leftStr + "," + rightStr + "]");
			} else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				appendOp(sb,"sequal", leftStr, rightStr);
			}else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\stackrel{\\small ?}{=}");
					break;
				case LIBRE_OFFICE:
				case MATH_PIPER:
				case JASYMCA:
				case MPREDUCE:
					sb.append("=");
					break;

				default:
					sb.append(strEQUAL_BOOLEAN);
				}
				sb.append(' ');
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case NOT_EQUAL:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<neq/>", leftStr, rightStr);
			}else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				appendOp(sb,"sunequal", leftStr, rightStr);
			} else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\neq");
					break;
				case LIBRE_OFFICE:
					sb.append("<>");
					break;
				case MATH_PIPER:
					sb.append("!=");
					break;

				default:
					sb.append(strNOT_EQUAL);
				}
				sb.append(' ');
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case IS_ELEMENT_OF:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<in/>", leftStr, rightStr);
			} else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\in");
					break;
				case LIBRE_OFFICE:
					sb.append(" in ");
					break;
				default:
					sb.append(strIS_ELEMENT_OF);
				}
				sb.append(' ');
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case IS_SUBSET_OF:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<subset/>", leftStr, rightStr);
			} else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\subseteq");
					break;
				case LIBRE_OFFICE:
					sb.append(" subseteq ");
					break;
				default:
					sb.append(strIS_SUBSET_OF);
				}
				sb.append(' ');
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case IS_SUBSET_OF_STRICT:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<prsubset/>", leftStr, rightStr);
			} else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\subset");
					break;
				case LIBRE_OFFICE:
					sb.append(" subset ");
					break;
				default:
					sb.append(strIS_SUBSET_OF_STRICT);
				}
				sb.append(' ');
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case SET_DIFFERENCE:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<setdiff/>", leftStr, rightStr);
			} else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\setminus");
					break;
				case LIBRE_OFFICE:
					sb.append(" setminus ");
					break;
				default:
					sb.append(strSET_DIFFERENCE);
				}
				sb.append(' ');
				if(right.isExpressionNode() && right.wrap().getOperation()==Operation.SET_DIFFERENCE){
					sb.append(leftBracket(STRING_TYPE));
					sb.append(rightStr);
					sb.append(rightBracket(STRING_TYPE));
				}else{
					append(sb, rightStr, right, operation, STRING_TYPE);
				}
				// sb.append(rightStr);
			}
			break;

		case LESS:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<lt/>", leftStr, rightStr);
			} else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				appendOp(sb,"sless", leftStr, rightStr);
			}else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				if (STRING_TYPE.equals(StringType.LATEX)
						&& kernel.isInsertLineBreaks()) {
					sb.append(" \\-< ");
				} else {
					sb.append(" < ");
				}
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case GREATER:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<gt/>", leftStr, rightStr);
			}else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				appendOp(sb,"sgreater", leftStr, rightStr);
			} else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				if (STRING_TYPE.equals(StringType.LATEX)
						&& kernel.isInsertLineBreaks()) {
					sb.append(" \\->");
				} else {
					sb.append(" > ");
				}
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case LESS_EQUAL:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<leq/>", leftStr, rightStr);
			} else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				appendOp(sb,"slessequal", leftStr, rightStr);
			}else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\leq");
					break;
				case LIBRE_OFFICE:
				case MATH_PIPER:
				case MPREDUCE:
					sb.append("<=");
					break;

				default:
					sb.append(strLESS_EQUAL);
				}
				sb.append(' ');
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case GREATER_EQUAL:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<qeq/>", leftStr, rightStr);
			} else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				appendOp(sb,"sgreaterequal", leftStr, rightStr);
			} else  {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\geq");
					break;
				case LIBRE_OFFICE:
				case MATH_PIPER:
					sb.append(">=");
					break;

				default:
					sb.append(strGREATER_EQUAL);
				}
				sb.append(' ');
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case PARALLEL:
			if (STRING_TYPE.equals(StringType.OGP)) {
				sb.append("AreParallel[" + leftStr + "," + rightStr + "]");
				break;
			}
			append(sb, leftStr, left, operation, STRING_TYPE);
			// sb.append(leftStr);
			sb.append(' ');
			switch (STRING_TYPE) {
			case LATEX:
				if (kernel.isInsertLineBreaks()) {
					sb.append("\\-");
				}
				sb.append("\\parallel");
				break;
			case LIBRE_OFFICE:
				sb.append(" parallel ");
				break;
			default:
				sb.append(strPARALLEL);
			}
			sb.append(' ');
			append(sb, rightStr, right, operation, STRING_TYPE);
			// sb.append(rightStr);
			break;

		case PERPENDICULAR:
			if (STRING_TYPE.equals(StringType.OGP)) {
				sb.append("ArePerpendicular[" + leftStr + "," + rightStr + "]");
				break;
			}
			append(sb, leftStr, left, operation, STRING_TYPE);
			// sb.append(leftStr);
			sb.append(' ');
			switch (STRING_TYPE) {
			case LATEX:
				if (kernel.isInsertLineBreaks()) {
					sb.append("\\-");
				}
				sb.append("\\perp");
				break;
			case LIBRE_OFFICE:
				sb.append(" ortho ");
				break;
			default:
				sb.append(strPERPENDICULAR);
			}
			sb.append(' ');
			append(sb, rightStr, right, operation, STRING_TYPE);
			// sb.append(rightStr);
			break;

		case VECTORPRODUCT:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<vectorproduct/>", leftStr, rightStr);
			} else if (STRING_TYPE.equals(StringType.MPREDUCE)) {
				sb.append("mycross(");
				sb.append(leftStr);
				sb.append(',');
				sb.append(rightStr);
				sb.append(')');

			} else {
				append(sb, leftStr, left, operation, STRING_TYPE);
				// sb.append(leftStr);
				sb.append(' ');
				switch (STRING_TYPE) {
				case LATEX:
					if (kernel.isInsertLineBreaks()) {
						sb.append("\\-");
					}
					sb.append("\\times");
					break;
				case LIBRE_OFFICE:
					sb.append(" cdot ");
					break;
				default:
					sb.append(strVECTORPRODUCT);
				}
				sb.append(' ');
				append(sb, rightStr, right, operation, STRING_TYPE);
				// sb.append(rightStr);
			}
			break;

		case PLUS:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<plus/>", leftStr, rightStr);
				break;
			case JASYMCA:
			case MATH_PIPER:
			case MAXIMA:
				sb.append('(');
				sb.append(leftStr);
				sb.append(") + (");
				sb.append(rightStr);
				sb.append(')');
				break;

			case MPREDUCE:
				sb.append("addition(");
				sb.append(leftStr);
				sb.append(',');
				sb.append(rightStr);
				sb.append(')');
				break;

			default:
				// check for 0
				if (valueForm) {
					if (isEqualString(left, 0, !valueForm)) {
						append(sb, rightStr, right, operation, STRING_TYPE);
						break;
					} else if (isEqualString(right, 0, !valueForm)) {
						append(sb, leftStr, left, operation, STRING_TYPE);
						break;
					}
				}

				if (left instanceof Equation) {
					sb.append(leftBracket(STRING_TYPE));
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
				} else {
					sb.append(leftStr);
				}

				// we need parantheses around right text
				// if right is not a leaf expression or
				// it is a leaf GeoElement without a label (i.e. it is
				// calculated somehow)
				if (left.isTextValue()
						&& (!right.isLeaf() || (right.isGeoElement() && !((GeoElement) right)
								.isLabelSet()))) {
					if (STRING_TYPE.equals(StringType.LATEX)
							&& kernel.isInsertLineBreaks()) {
						sb.append(" \\-+ ");
					} else {
						sb.append(" + ");
					}
					sb.append(leftBracket(STRING_TYPE));
					sb.append(rightStr);
					sb.append(rightBracket(STRING_TYPE));
				} else {
					if (rightStr.charAt(0) == '-') { // convert + - to -
						if (STRING_TYPE.equals(StringType.LATEX)
								&& kernel.isInsertLineBreaks()) {
							sb.append(" \\-- ");
						} else {
							sb.append(" - ");
						}
						sb.append(rightStr.substring(1));
					} else if (rightStr
							.startsWith(Unicode.RightToLeftUnaryMinusSign)) { // Arabic
																				// convert
																				// +
																				// -
																				// to
																				// -
						if (STRING_TYPE.equals(StringType.LATEX)
								&& kernel.isInsertLineBreaks()) {
							sb.append(" \\-- ");
						} else {
							sb.append(" - ");
						}
						sb.append(rightStr.substring(3));
					} else {
						if (STRING_TYPE.equals(StringType.LATEX)
								&& kernel.isInsertLineBreaks()) {
							sb.append(" \\-+ ");
						} else {
							sb.append(" + ");
						}
						sb.append(rightStr);
					}
				}
				break;
			}
			break;

		case MINUS:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<minus/>", leftStr, rightStr);
				break;
			case JASYMCA:
			case MATH_PIPER:
			case MAXIMA:
				sb.append('(');
				sb.append(leftStr);
				sb.append(") - (");
				sb.append(rightStr);
				sb.append(')');
				break;

			case MPREDUCE:
				sb.append("subtraction(");
				sb.append(leftStr);
				sb.append(',');
				sb.append(rightStr);
				sb.append(')');
				break;

			default:
				if (left instanceof Equation) {
					sb.append(leftBracket(STRING_TYPE));
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
				} else {
					sb.append(leftStr);
				}

				// check for 0 at right
				if (valueForm
						&& rightStr
								.equals(App.unicodeZero + "")) {
					break;
				}

				if (right.isLeaf()
						|| (opID(right) >= Operation.MULTIPLY.ordinal())) { // not
																			// +,
																			// -

					if (rightStr.charAt(0) == '-') { // convert - - to +
						if (STRING_TYPE.equals(StringType.LATEX)
								&& kernel.isInsertLineBreaks()) {
							sb.append(" \\-+ ");
						} else {
							sb.append(" + ");
						}
						sb.append(rightStr.substring(1));
					} else if (rightStr
							.startsWith(Unicode.RightToLeftUnaryMinusSign)) { // Arabic
																				// convert
																				// -
																				// -
																				// to
																				// +
						if (STRING_TYPE.equals(StringType.LATEX)
								&& kernel.isInsertLineBreaks()) {
							sb.append(" \\-+ ");
						} else {
							sb.append(" + ");
						}
						sb.append(rightStr.substring(3));
					} else {
						if (STRING_TYPE.equals(StringType.LATEX)
								&& kernel.isInsertLineBreaks()) {
							sb.append(" \\-- ");
						} else {
							sb.append(" - ");
						}
						sb.append(rightStr);
					}
				} else {
					// fix for changing height in Algebra View plus / minus
					if (STRING_TYPE.equals(StringType.LATEX)
							&& kernel.isInsertLineBreaks()) {
						sb.append(" \\-- ");
					} else {
						sb.append(" - ");
					}
					sb.append(leftBracket(STRING_TYPE));
					sb.append(rightStr);
					sb.append(rightBracket(STRING_TYPE));
				}
				break;
			}
			break;

		case MULTIPLY:
			switch (STRING_TYPE) {

			case MATHML:
				mathml(sb, "<times/>", leftStr, rightStr);
				break;
			default:
				// check for 1 at left
				if (isEqualString(left, 1, !valueForm)) {
					append(sb, rightStr, right, operation, STRING_TYPE);
					break;
				}
				// check for 1 at right
				else if (isEqualString(right, 1, !valueForm)) {
					append(sb, leftStr, left, operation, STRING_TYPE);
					break;
				}

				// removed 0 handling due to problems with functions,
				// e.g 0 * x + 1 becomes 0 + 1 and no longer is a function
				// // check for 0 at left
				// else if (valueForm && isEqualString(left, 0, !valueForm)) {
				// sb.append("0");
				// break;
				// }
				// // check for 0 at right
				// else if (valueForm && isEqualString(right, 0, !valueForm)) {
				// sb.append("0");
				// break;
				// }

				// check for degree sign or 1degree or degree1 (eg for Arabic)
				else if (((rightStr.length() == 2) && (((rightStr.charAt(0) == Unicode.degreeChar) && (rightStr
						.charAt(1) == (App.unicodeZero + 1))) || ((rightStr
						.charAt(0) == Unicode.degreeChar) && (rightStr
						.charAt(1) == (App.unicodeZero + 1)))))
						|| rightStr.equals(Unicode.degree)) {

					boolean rtl = app.isRightToLeftDigits(tpl);

					if (rtl) {
						sb.append(Unicode.degree);
					}

					if (!left.isLeaf()) {
						sb.append('('); // needed for eg (a+b)\u00b0
					}
					sb.append(leftStr);
					if (!left.isLeaf()) {
						sb.append(')'); // needed for eg (a+b)\u00b0
					}

					if (!rtl) {
						sb.append(Unicode.degree);
					}

					break;
				}

			case JASYMCA:
			case MATH_PIPER:
			case MAXIMA:
			case LATEX:
			case LIBRE_OFFICE:

				boolean nounary = true;

				// left wing
				if (left.isLeaf()
						|| (opID(left) >= Operation.MULTIPLY.ordinal())) { // not
																			// +,
																			// -
					if (isEqualString(left, -1, !valueForm)) { // unary minus
						nounary = false;
						sb.append('-');
					} else {
						if (leftStr
								.startsWith(Unicode.RightToLeftUnaryMinusSign)) {
							// brackets needed for eg Arabic digits
							sb.append(Unicode.RightToLeftMark);
							sb.append(leftBracket(STRING_TYPE));
							sb.append(leftStr);
							sb.append(rightBracket(STRING_TYPE));
							sb.append(Unicode.RightToLeftMark);
						} else {
							sb.append(leftStr);
						}
					}
				} else {
					sb.append(leftBracket(STRING_TYPE));
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
				}

				// right wing
				int opIDright = opID(right);
				if (right.isLeaf()
						|| (opIDright >= Operation.MULTIPLY.ordinal())) { // not
																			// +,
																			// -
					boolean showMultiplicationSign = false;
					boolean multiplicationSpaceNeeded = true;
					if (nounary) {
						switch (STRING_TYPE) {
						case PGF:
						case PSTRICKS:
						case GEOGEBRA_XML:
						case JASYMCA:
						case MATH_PIPER:
						case MAXIMA:
							showMultiplicationSign = true;
							break;
							
						case LIBRE_OFFICE:
						case LATEX:
							// check if we need a multiplication sign, see #414
							// digit-digit, e.g. 3 * 5
							// digit-fraction, e.g. 3 * \frac{5}{2}
							char lastLeft = leftStr
									.charAt(leftStr.length() - 1);
							char firstRight = rightStr.charAt(0);
							showMultiplicationSign =
							// left is digit or ends with }, e.g. exponent,
							// fraction
							(StringUtil.isDigit(lastLeft) || (lastLeft == '}'))
									&&
									// right is digit or fraction
									(StringUtil.isDigit(firstRight) || rightStr
											.startsWith("\\frac"));
							break;

						default: // GeoGebra syntax
							char firstLeft = leftStr.charAt(0);
							lastLeft = leftStr.charAt(leftStr.length() - 1);
							firstRight = rightStr.charAt(0);
							// check if we need a multiplication sign, see #414
							// digit-digit, e.g. 3 * 5
							showMultiplicationSign = Character
									.isDigit(lastLeft)
									&& (StringUtil.isDigit(firstRight)
									// 3*E23AB can't be written 3E23AB
									|| (rightStr.charAt(0) == 'E'));
							// check if we need a multiplication space:
							multiplicationSpaceNeeded = showMultiplicationSign;
							if (!multiplicationSpaceNeeded) {
								// check if we need a multiplication space:
								// it's needed except for number * character,
								// e.g. 23x
								// need to check start and end for eg A1 * A2
								boolean leftIsNumber = left.isLeaf()
										&& (StringUtil.isDigit(firstLeft) || (firstLeft == '-'))
										&& StringUtil.isDigit(lastLeft);

								// check if we need a multiplication space:
								// all cases except number * character, e.g. 3x
								multiplicationSpaceNeeded = showMultiplicationSign
										|| !(leftIsNumber && !Character
												.isDigit(firstRight));
							}
						}

						if (STRING_TYPE.equals(StringType.LATEX)
								&& kernel.isInsertLineBreaks()) {
							sb.append("\\-");
						}

						if (showMultiplicationSign) {
							sb.append(multiplicationSign(STRING_TYPE));
						} else if (multiplicationSpaceNeeded) {
							// space instead of multiplication sign
							sb.append(multiplicationSpace(STRING_TYPE));
						}
					}

					boolean rtlMinus;
					// show parentheses around these cases
					if (((rtlMinus = rightStr
							.startsWith(Unicode.RightToLeftUnaryMinusSign)) || (rightStr
							.charAt(0) == '-')) // 2 (-5) or -(-5)
							|| (!nounary && !right.isLeaf() && (opIDright <= Operation.DIVIDE
									.ordinal() // -(x * a) or -(x / a)
							))
							|| (showMultiplicationSign && STRING_TYPE
									.equals(StringType.GEOGEBRA))) // 3 (5)
					{
						if (rtlMinus) {
							sb.append(Unicode.RightToLeftMark);
						}
						sb.append(leftBracket(STRING_TYPE));
						sb.append(rightStr);
						sb.append(rightBracket(STRING_TYPE));
						if (rtlMinus) {
							sb.append(Unicode.RightToLeftMark);
						}
					} else {
						// -1.0 * 5 becomes "-5"
						sb.append(rightStr);
					}
				} else { // right is + or - tree
					if (nounary) {
						switch (STRING_TYPE) {
						case PGF:
						case PSTRICKS:
						case GEOGEBRA_XML:
						case JASYMCA:
						case MATH_PIPER:
						case MAXIMA:
							sb.append(multiplicationSign(STRING_TYPE));
							break;

						default:
							// space instead of multiplication sign
							sb.append(multiplicationSpace(STRING_TYPE));
						}
					}
					sb.append(leftBracket(STRING_TYPE));
					sb.append(rightStr);
					sb.append(rightBracket(STRING_TYPE));
				}
				break;

			case MPREDUCE:

				if (isEqualString(left, -1, !valueForm)) {
					sb.append("-(");
					sb.append(rightStr);
					sb.append(')');
				} else {
					sb.append("multiplication(");
					sb.append(leftStr);
					sb.append(",");
					sb.append(rightStr);
					sb.append(")");
					break;
				}
			}
			break;

		case DIVIDE:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<divide/>", leftStr, rightStr);
				break;
			case LATEX:
				if ((leftStr.charAt(0) == '-')
						&& (left.isLeaf() || (left instanceof ExpressionNode && isMultiplyOrDivide((ExpressionNode) left)))) {
					sb.append("-\\frac{");
					sb.append(leftStr.substring(1));
					sb.append("}{");
					sb.append(rightStr);
					sb.append("}");
				} else {

					sb.append("\\frac{");
					sb.append(leftStr);
					sb.append("}{");
					sb.append(rightStr);
					sb.append("}");
				}
				break;
			case LIBRE_OFFICE:
				sb.append("{ ");
				sb.append(leftStr);
				sb.append(" } over { ");
				sb.append(rightStr);
				sb.append(" }");
				break;
				
			case JASYMCA:
			case MATH_PIPER:
			case MAXIMA:
			case MPREDUCE:
				sb.append("mydivision(");
				sb.append(leftStr);
				sb.append(",");
				sb.append(rightStr);
				sb.append(')');
				break;

			default:
				// check for 1 in denominator
				if (isEqualString(right, 1, !valueForm)) {
					sb.append(leftStr);
					break;
				}

				// left wing
				// put parantheses around +, -, *
				append(sb, leftStr, left, Operation.DIVIDE, STRING_TYPE);
				sb.append(" / ");

				// right wing
				append(sb, rightStr, right, Operation.POWER, STRING_TYPE); // not
																			// +,
																			// -,
																			// *,
				// /
			}
			break;

		case POWER:
			/*
			 * support for sin^2(x) for display, too slow and hacky if
			 * (STRING_TYPE.equals(StringType.GEOGEBRA &&
			 * leftStr.startsWith("sin(")) { //&& rightStr.equals("2")) { int
			 * index; try { index = Integer.parseInt(rightStr); } catch
			 * (NumberFormatException nfe) { index = Integer.MAX_VALUE; }
			 * 
			 * if (index > 0 && index != Integer.MAX_VALUE) { sb.append("sin");
			 * sb.append(Unicode.numberToIndex(index));
			 * sb.append(leftStr.substring(3)); // everying except the "sin"
			 * break; }
			 * 
			 * }//
			 */

			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<power/>", leftStr, rightStr);
			} else {

				// everything else

				boolean finished = false;

				// support for sin^2(x) for LaTeX, eg FormulaText[]
				if (STRING_TYPE.equals(StringType.LATEX)
						&& left.isExpressionNode()) {
					switch (((ExpressionNode) left).getOperation()) {
					// #1592
					case SIN:
					case COS:
					case TAN:
					case SEC:
					case CSC:
					case COT:
					case SINH:
					case COSH:
					case TANH:
					case SECH:
					case CSCH:
					case COTH:
						int index;
						try {
							index = Integer.parseInt(rightStr);
						} catch (NumberFormatException nfe) {
							index = Integer.MAX_VALUE;
						}

						if ((index > 0) && (index != Integer.MAX_VALUE)) {
							int spaceIndex = leftStr.indexOf(' ');
							sb.append(leftStr.substring(0, spaceIndex));
							sb.append(" ^{");
							sb.append(rightStr);
							sb.append("}");
							sb.append(leftStr.substring(spaceIndex + 1)); // everything
																			// except
																			// the
																			// "\\sin "

							finished = true;

							break;
						}

					default:
						// fall through
					}

					if (finished) {
						break;
					}

				}

				switch (STRING_TYPE) {
				case MPREDUCE:
					sb.append("mypower(");
					sb.append(leftStr);
					sb.append(",");
					sb.append(rightStr);
					sb.append(')');
					break;
				case LATEX:

					// checks if the basis is leaf and if so
					// omits the brackets
					if (left.isLeaf() && (leftStr.charAt(0) != '-')) {
						sb.append(leftStr);
						break;
					}
					// else fall through
				
				case JASYMCA:
				case MATH_PIPER:
				case MAXIMA:
				
					
				case LIBRE_OFFICE:
				default:

					/*
					 * removed Michael Borcherds 2009-02-08 doesn't work eg m=1
					 * g(x) = (x - 1)^m (x - 3)
					 * 
					 * 
					 * // check for 1 in exponent if (isEqualString(right, 1,
					 * !valueForm)) { sb.append(leftStr); break; } //
					 */

					// left wing
					if ((leftStr.charAt(0) != '-')
							&& // no unary
							(left.isLeaf() || ((opID(left) > Operation.POWER
									.ordinal()) && (opID(left) != Operation.EXP
									.ordinal())))) { // not +, -, *, /, ^,
						// e^x
						sb.append(leftStr);
					} else {
						sb.append(leftBracket(STRING_TYPE));
						sb.append(leftStr);
						sb.append(rightBracket(STRING_TYPE));
					}
					break;
				}

				// right wing
				switch (STRING_TYPE) {
				case LATEX:
				case LIBRE_OFFICE:
					sb.append('^');

					// add brackets for eg a^b^c -> a^(b^c)
					boolean addParentheses = (right.isExpressionNode() && ((ExpressionNode) right)
							.getOperation().equals(Operation.POWER));

					sb.append('{');
					if (addParentheses) {
						sb.append(leftBracket(STRING_TYPE));
					}
					sb.append(rightStr);
					if (addParentheses) {
						sb.append(rightBracket(STRING_TYPE));
					}
					sb.append('}');
					break;

				case MPREDUCE:
					break;
				case JASYMCA:
				case GEOGEBRA_XML:
				case MATH_PIPER:
				case MAXIMA:
				
					sb.append('^');
					sb.append('(');
					sb.append(rightStr);
					sb.append(')');
					break;

				default:
					if (right.isLeaf()
							|| ((opID(right) > Operation.POWER.ordinal()) && (opID(right) != Operation.EXP
									.ordinal()))) { // not
						// +,
						// -,
						// *,
						// /,
						// ^,
						// e^x
						// Michael Borcherds 2008-05-14
						// display powers over 9 as unicode superscript
						try {
							int i = Integer.parseInt(rightStr);
							String index = "";
							if (i < 0) {
								sb.append('\u207B'); // superscript minus sign
								i = -i;
							}

							if (i == 0) {
								sb.append('\u2070'); // zero
							} else {
								while (i > 0) {
									switch (i % 10) {
									case 0:
										index = "\u2070" + index;
										break;
									case 1:
										index = "\u00b9" + index;
										break;
									case 2:
										index = "\u00b2" + index;
										break;
									case 3:
										index = "\u00b3" + index;
										break;
									case 4:
										index = "\u2074" + index;
										break;
									case 5:
										index = "\u2075" + index;
										break;
									case 6:
										index = "\u2076" + index;
										break;
									case 7:
										index = "\u2077" + index;
										break;
									case 8:
										index = "\u2078" + index;
										break;
									case 9:
										index = "\u2079" + index;
										break;

									}
									i = i / 10;
								}
							}

							sb.append(index);
						} catch (Exception e) {
							sb.append('^');
							sb.append(rightStr);
						}

						/*
						 * 
						 * if (rightStr.length() == 1) { switch
						 * (rightStr.charAt(0)) {
						 * 
						 * case '0': sb.append('\u2070'); break; case '1':
						 * sb.append('\u00b9'); break; case '2':
						 * sb.append('\u00b2'); break; case '3':
						 * sb.append('\u00b3'); break; case '4':
						 * sb.append('\u2074'); break; case '5':
						 * sb.append('\u2075'); break; case '6':
						 * sb.append('\u2076'); break; case '7':
						 * sb.append('\u2077'); break; case '8':
						 * sb.append('\u2078'); break; case '9':
						 * sb.append('\u2079'); break; default: sb.append('^');
						 * sb.append(rightStr); } } else { sb.append('^');
						 * sb.append(rightStr); }
						 */
					} else {
						sb.append('^');
						sb.append(leftBracket(STRING_TYPE));
						sb.append(rightStr);
						sb.append(rightBracket(STRING_TYPE));
					}
				}
			}
			break;

		case FACTORIAL:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<factorial/>", leftStr, null);
				break;
			case LIBRE_OFFICE:
				sb.append("fact {");
				if ((leftStr.charAt(0) != '-') && // no unary
						left.isLeaf()) { 
					sb.append(leftStr);
				} else {
					sb.append('(');
					sb.append(leftStr);
					sb.append(')');
				}
				sb.append(" }");
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "factorial");
				sb.append(leftStr);
				sb.append(")");
				break;

			default:
				if (((leftStr.charAt(0) != '-') && // no unary
						left.isLeaf())
						|| (opID(left) > Operation.POWER.ordinal())) { // not +,
																		// -, *,
																		// /, ^
					sb.append(leftStr);
				} else {
					sb.append(leftBracket(STRING_TYPE));
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
				}
				sb.append('!');
				break;
			}
			break;

		case COS:
			trig(leftStr,sb,"<cos/>","\\cos","Cos(","COS(","cos","cos","cos",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),true);
			break;

		case SIN:
			trig(leftStr,sb,"<sin/>","\\sin","Sin(","SIN(","sin","sin","sin",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),true);
			break;

		case TAN:
			trig(leftStr,sb,"<tan/>","\\tan","Tan(","TAN(","tan","tan","tan",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),true);
			break;

		case CSC:
			trig(leftStr,sb,"<csc/>","\\csc","Csc(","CSC(","csc","csc","csc",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),true);
			break;

		case SEC:
			trig(leftStr,sb,"<sec/>","\\sec","Sec(","SEC(","sec","sec","sec",
						STRING_TYPE,tpl.isPrintLocalizedCommandNames(),true);
			break;

		case COT:
			trig(leftStr,sb,"<cot/>","\\cot","Cot(","COT(","cot","cot","cot",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),true);
			break;

		case CSCH:
			trig(leftStr,sb,"<csch/>","\\csch","Csch(","CSCH(","csch","csch","func csch",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
			break;

		case SECH:
			trig(leftStr,sb,"<sech/>","\\sech","Sech(","SECH(","sech","sech","func sech",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
			break;

		case COTH:
			trig(leftStr,sb,"<coth/>","\\coth","Coth(","COTH(","coth","coth","coth",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
			break;

		case ARCCOS:
			trig(leftStr,sb,"<arccos/>","\\operatorname{acos}","ArcCos(","ACOS(","acos","acos","arccos",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
			break;

		case ARCSIN:
			trig(leftStr,sb,"<arcsin/>","\\operatorname{asin}","ArcSin(","ASIN(","asin","asin","arcsin",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
			break;

		case ARCTAN:
			trig(leftStr,sb,"<arctan/>","\\operatorname{atan}","ArcTan(","ATAN(","atan","atan","arctan",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
			break;

		case ARCTAN2:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<atan/>", leftStr, rightStr);
			} else {
				switch (STRING_TYPE) {
				case LATEX:
					sb.append("atan2 \\left( ");
					break;
				case LIBRE_OFFICE:
					sb.append("func atan2 left( ");
					break;
				case MATH_PIPER:
					sb.append("ArcTan2(");
					break;

				case PSTRICKS:
					sb.append("ATAN2(");
					break;

				case MPREDUCE:
					sb.append("myatan2(");
					break;

				default:
					sb.append("atan2(");
				}
				sb.append(leftStr);
				sb.append(',');
				sb.append(rightStr);
				sb.append(rightBracket(STRING_TYPE));
			}
			break;

		case COSH:
			trig(leftStr,sb,"<cosh/>","\\cosh","Cosh(","COSH(","cosh","cosh","cosh",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
			break;

		case SINH:
			trig(leftStr,sb,"<sinh/>","\\sinh","Sinh(","SINH(","sinh","sinh","sinh",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
			break;

		case TANH:
			trig(leftStr,sb,"<tanh/>","\\tanh","Tanh(","TANH(","tanh","tanh","tanh",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
		break;

		case ACOSH:
			trig(leftStr,sb,"<arccosh/>","\\operatorname{acosh}","ArcCosh(","ACOSH(","acosh","acosh","arcosh",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
			break;
		case ASINH:
			trig(leftStr,sb,"<arcsinh/>","\\operatorname{asinh}","ArcSinh(","ASINH(","asinh","asinh","arsinh",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);			
			break;

		case ATANH:
			trig(leftStr,sb,"<arctanh/>","\\operatorname{atanh}","ArcTanh(","ATANH(","atanh","atanh","artanh",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
		break;
		case REAL:
			trig(leftStr,sb,"<real/>","\\operatorname{real}","","","myreal","real","real",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
		break;
		case IMAGINARY:
			trig(leftStr,sb,"<imaginary/>","\\operatorname{imaginary}","","","imaginary","imaginary","imaginary",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
		break;
		case FRACTIONAL_PART:
			trig(leftStr,sb,"<todo/>","\\operatorname{fractionalPart}","","","fractionalPart","fractionalPart","fractionalPart",
					STRING_TYPE,tpl.isPrintLocalizedCommandNames(),false);
		break;
		case ZETA:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\zeta\\left( ");
				break;
				
			case LIBRE_OFFICE:
				sb.append("func zeta left (");
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "zeta");
				break;	
			default:
				sb.append("zeta(");
			}
			sb.append(leftStr);
			sb.append(rightBracket(STRING_TYPE));
			break;
		case CI:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\operatorname{Ci} \\left( ");
				break;

				
			case LIBRE_OFFICE:
				sb.append("func Ci left (");
				break;
				
			case MPREDUCE:
				appendReduceFunction(sb, "ci");
				break;
			default:
				sb.append("cosIntegral(");
			}
			sb.append(leftStr);
			sb.append(rightBracket(STRING_TYPE));
			break;
		case SI:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\operatorname{Si} \\left( ");
				break;
				
			case LIBRE_OFFICE:
				sb.append("func Si left (");
				break;
				
			case MPREDUCE:
				appendReduceFunction(sb, "si");
				break;

			default:
				sb.append("sinIntegral(");
			}
			sb.append(leftStr);
			sb.append(rightBracket(STRING_TYPE));
			break;
		case EI:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\operatorname{Ei} \\left( ");
				break;
			case LIBRE_OFFICE:
				sb.append("func Ei left (");
				break;
				
			case MPREDUCE:
				appendReduceFunction(sb, "ei");
				break;

			default:
				sb.append("expIntegral(");
			}
			sb.append(leftStr);
			sb.append(rightBracket(STRING_TYPE));
			break;
		case ARBCONST:
			sb.append("arbconst(");
			sb.append(leftStr);
			sb.append(")");
			break;
		case ARBINT:
			sb.append("arbint(");
			sb.append(leftStr);
			sb.append(")");
			break;
		case ARBCOMPLEX:

			sb.append("arbcomplex(");
			sb.append(leftStr);
			sb.append(")");

			break;
		case EXP:
			// Application.debug("EXP");
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<exp/>", leftStr, null);
				break;
			case LIBRE_OFFICE:
				sb.append("func ");
			case LATEX:

				// add brackets for eg e^b^c -> e^(b^c)
				boolean addParentheses = (left.isExpressionNode() && ((ExpressionNode) left)
						.getOperation().equals(Operation.POWER));

				sb.append("e^{");
				if (addParentheses) {
					sb.append(leftBracket(STRING_TYPE));
				}
				sb.append(leftStr);
				if (addParentheses) {
					sb.append(rightBracket(STRING_TYPE));
				}
				sb.append('}');
				break;

			case MATH_PIPER:
				sb.append("Exp(");
				sb.append(leftStr);
				sb.append(')');
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "exp");
				sb.append(leftStr);
				sb.append(')');
				break;

			case JASYMCA:
			case GEOGEBRA_XML:
			case MAXIMA:
				sb.append("exp(");
				sb.append(leftStr);
				sb.append(')');
				break;

			case PSTRICKS:
				sb.append("EXP(");
				sb.append(leftStr);
				sb.append(')');
				break;

			default:
				sb.append(Unicode.EULER_STRING);
				if (left.isLeaf()) {
					sb.append("^");
					sb.append(leftStr);
				} else {
					sb.append("^(");
					sb.append(leftStr);
					sb.append(')');
				}
				break;
			}
			break;

		case LOG:
			if (STRING_TYPE.equals(StringType.MATHML)) {
				mathml(sb, "<ln/>", leftStr, null);
			} else {
				switch (STRING_TYPE) {
				case LATEX:
					sb.append("\\ln \\left( ");
					break;
				case LIBRE_OFFICE:
					sb.append("ln left ( ");
					break;
				case MATH_PIPER:
					sb.append("Ln(");
					break;
				case MPREDUCE:
					appendReduceFunction(sb, "log");
					break;
				case MAXIMA:
				case JASYMCA:
				case GEOGEBRA_XML:
					sb.append("log(");
					break;

				case PSTRICKS:
				case PGF:
				default:
					sb.append("ln(");
					break;
				}
				sb.append(leftStr);
				sb.append(rightBracket(STRING_TYPE));
			}
			break;

		case LOGB:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<log/>", "<logbase>", leftStr, "</logbase>", "",
						rightStr, "");
				break;
			case LATEX:
				sb.append("\\log_{");
				sb.append(leftStr);
				sb.append('}');
				sb.append(leftBracket(STRING_TYPE));
				sb.append(rightStr);
				sb.append(rightBracket(STRING_TYPE));
				break;
			case LIBRE_OFFICE:
				sb.append("log_{");
				sb.append(leftStr);
				sb.append('}');
				sb.append(leftBracket(STRING_TYPE));
				sb.append(rightStr);
				sb.append(rightBracket(STRING_TYPE));
				break;
			case MAXIMA:
			case MATH_PIPER:
				// user defined function
				sb.append("logB(");
				sb.append(leftStr);
				sb.append(',');
				sb.append(rightStr);
				sb.append(')');
				break;

			case MPREDUCE:
				sb.append("logb(");
				sb.append(rightStr);
				sb.append(',');
				sb.append(leftStr);
				sb.append(')');
				break;

			case PSTRICKS:
			case PGF:
				// ln(x)/ln(b)
				sb.append("ln(");
				sb.append(rightStr);
				sb.append(")/ln(");
				sb.append(leftStr);
				sb.append(')');
				break;

			default:
				sb.append("log(");
				sb.append(leftStr);
				sb.append(", ");
				sb.append(rightStr);
				sb.append(')');
				break;

			}
			break;

		case POLYGAMMA:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\psi_{");
				sb.append(leftStr);
				sb.append('}');
				sb.append(leftBracket(STRING_TYPE));
				sb.append(rightStr);
				sb.append(rightBracket(STRING_TYPE));
				break;


			case MAXIMA:
			case MATH_PIPER:
			case MPREDUCE:
			default:
				sb.append("polygamma(");
				sb.append(leftStr);
				if (STRING_TYPE.equals(StringType.LIBRE_OFFICE))
					sb.append("\",\"");
				else
					sb.append(", ");
				sb.append(rightStr);
				sb.append(')');
				break;

			}
			break;

		case ERF:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\erf");
				sb.append(leftBracket(STRING_TYPE));
				sb.append(leftStr);
				sb.append(rightBracket(STRING_TYPE));
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "erf");
				sb.append(leftStr);
				sb.append(')');
				break;
				
			case LIBRE_OFFICE:
				sb.append("func ");
			case MAXIMA:
			case MATH_PIPER:
			default:
				sb.append("erf(");
				sb.append(leftStr);
				sb.append(')');
				break;

			}
			break;

		case PSI:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\digamma");
				sb.append(leftBracket(STRING_TYPE));
				sb.append(leftStr);
				sb.append(rightBracket(STRING_TYPE));
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "psi");
				sb.append(leftStr);
				sb.append(')');
				break;
				
			case LIBRE_OFFICE:
				sb.append("func ");
			case MAXIMA:
			case MATH_PIPER:
			default:
				sb.append("psi(");
				sb.append(leftStr);
				sb.append(')');
				break;

			}
			break;

		case LOG10:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<log/>", leftStr, null);
				break;
			case LATEX:
				sb.append("\\log_{10} \\left(");
				sb.append(leftStr);
				sb.append("\\right)");
				break;
			case LIBRE_OFFICE:
				sb.append("log_10 (");
				sb.append(leftStr);
				sb.append(")");
				break;
			case PSTRICKS:
				sb.append("log(");
				sb.append(leftStr);
				sb.append(')');
				break;

			case MAXIMA:
			case MATH_PIPER:
			case PGF:
				sb.append("log10("); // user-defined function in Maxima
				sb.append(leftStr);
				sb.append(')');
				break;

			case MPREDUCE:
				if (left instanceof ListValue) {
					sb.append("applyfunction2(logb,");
				} else {
					sb.append("logb(");
				}
				sb.append(leftStr);
				sb.append(",10)");
				break;

			default:
				sb.append("lg(");
				sb.append(leftStr);
				sb.append(')');
				break;
			}
			break;

		case LOG2:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\log_{2} \\left(");
				sb.append(leftStr);
				sb.append("\\right)");
				break;
			case LIBRE_OFFICE:
				sb.append("log_2 (");
				sb.append(leftStr);
				sb.append(")");
				break;
			case MAXIMA:
			case MATH_PIPER:
				sb.append("log2("); // user-defined function in Maxima
				sb.append(leftStr);
				sb.append(')');
				break;

			case MPREDUCE:
				if (left instanceof ListValue) {
					sb.append("applyfunction2(logb,");
				} else {
					sb.append("logb(");
				}
				sb.append(leftStr);
				sb.append(",2)");
				break;

			default:
				sb.append("ld(");
				sb.append(leftStr);
				sb.append(')');
				break;
			}
			break;
		case NROOT:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<root/>", leftStr, null);
				break;
			case LATEX:
				sb.append("\\sqrt[");
				sb.append(rightStr);		
				sb.append("]{");
				sb.append(leftStr);
				sb.append('}');
				break;
			case LIBRE_OFFICE:
				sb.append("nroot{");
				sb.append(rightStr);
				sb.append("},{");
				sb.append(leftStr);
				sb.append('}');
				break;
			
			case GEOGEBRA:
				sb.append(app.getFunction("nroot"));
				sb.append("(");
				sb.append(leftStr);
				sb.append(',');
				sb.append(rightStr);
				sb.append(')');
				break;
			default: //MAXIMA, MPREDUCE, PSTRICKS, ...
				sb.append("(");
				sb.append(leftStr);
				sb.append(")^(1/(");
				sb.append(rightStr);
				sb.append("))");
				break;	
			}
			break;
	
		case SQRT_SHORT:
		case SQRT:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<root/>", leftStr, null);
				break;
			case LATEX:
				sb.append("\\sqrt{");
				sb.append(leftStr);
				sb.append('}');
				break;
			case LIBRE_OFFICE:
				sb.append("sqrt{");
				sb.append(leftStr);
				sb.append('}');
				break;
			case MATH_PIPER:
				sb.append("Sqrt(");
				sb.append(leftStr);
				sb.append(')');
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "sqrt");
				sb.append(leftStr);
				sb.append(')');
				break;
			default:
				sb.append("sqrt(");
				sb.append(leftStr);
				sb.append(')');
			}
			break;

		case CBRT:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<root/>", "<degree>", "3", "</degree>", "",
						leftStr, "");
				break;
			case LATEX:
				sb.append("\\sqrt[3]{");
				sb.append(leftStr);
				sb.append('}');
				break;
			case LIBRE_OFFICE:
				sb.append("nroot{3}{");
				sb.append(leftStr);
				sb.append('}');
				break;
			case MATH_PIPER:
			case MPREDUCE:
				if (left instanceof ListValue) {
					sb.append("applyfunction2(**,");
					sb.append(leftStr);
					sb.append(",1/3)");
				} else {
					sb.append("(");
					sb.append(leftStr);
					sb.append(")^(1/3)");
				}
				break;

			default:
				sb.append("cbrt(");
				sb.append(leftStr);
				sb.append(')');
			}
			break;

		case ABS:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<abs/>", leftStr, null);
				break;
			case LATEX:
				sb.append("\\left|");
				sb.append(leftStr);
				sb.append("\\right|");
				break;
			case LIBRE_OFFICE:
				sb.append("abs{");
				sb.append(leftStr);
				sb.append('}');
				break;
			case MATH_PIPER:
				sb.append("Abs(");
				sb.append(leftStr);
				sb.append(')');
				break;

			case MPREDUCE:
				appendReduceFunction(sb, "myabs");
				sb.append(leftStr);
				sb.append(')');
				break;

			default:
				sb.append("abs(");
				sb.append(leftStr);
				sb.append(')');
			}
			break;

		case SGN:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\mathrm{sgn}(");
				break;

			case MATH_PIPER:
				sb.append("Sign(");
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "sign");
				break;
			case JASYMCA:
			case MAXIMA:
				sb.append("sign(");
				break;

			case LIBRE_OFFICE:
				sb.append("func ");
			default:
				sb.append("sgn(");
			}
			sb.append(leftStr);
			sb.append(')');
			break;

		case CONJUGATE:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<conjugate/>", leftStr, null);
				break;
			case LATEX:
				sb.append("\\overline{");
				sb.append(leftStr);
				sb.append("}");
				break;
			case LIBRE_OFFICE:
				sb.append("overline{");
				sb.append(leftStr);
				sb.append("}");
				break;
			case MATH_PIPER:
				sb.append("Conjugate(");
				sb.append(leftStr);
				sb.append(")");
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "conjugate");
				sb.append(leftStr);
				sb.append(')');
				break;
			default:
				sb.append("conjugate(");
				sb.append(leftStr);
				sb.append(')');
			}
			break;

		case ARG:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<arg/>", leftStr, null);
				break;
			case LATEX:
				sb.append("\\arg \\left( ");
				sb.append(leftStr);
				sb.append("\\right)");
				break;
			case MATH_PIPER:
				sb.append("Arg(");
				sb.append(leftStr);
				sb.append(")");
				break;
			case MAXIMA:
				sb.append("carg(");
				sb.append(leftStr);
				sb.append(')');
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "myarg");
				sb.append(leftStr);
				sb.append(')');
				break;
			case LIBRE_OFFICE:
				sb.append("func ");
			default:
				sb.append("arg(");
				sb.append(leftStr);
				sb.append(')');
			}
			break;

		case FLOOR:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<floor/>", leftStr, null);
				break;
			case LATEX:
				sb.append("\\left\\lfloor ");
				sb.append(leftStr);
				sb.append("\\right\\rfloor ");
				break;
			case LIBRE_OFFICE:
				sb.append(" left lfloor ");
				sb.append(leftStr);
				sb.append(" right rfloor");
				break;
			case MATH_PIPER:
				sb.append("Floor(");
				sb.append(leftStr);
				sb.append(')');
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "floor");
				sb.append(leftStr);
				sb.append(')');
				break;
			default:
				sb.append("floor(");
				sb.append(leftStr);
				sb.append(')');
			}
			break;

		case CEIL:
			switch (STRING_TYPE) {
			case MATHML:
				mathml(sb, "<ceiling/>", leftStr, null);
				break;
			case LATEX:
				sb.append("\\left\\lceil ");
				sb.append(leftStr);
				sb.append("\\right\\rceil ");
				break;
			case LIBRE_OFFICE:
				sb.append("left lceil ");
				sb.append(leftStr);
				sb.append(" right rceil");
				break;
			case MATH_PIPER:
				sb.append("Ceil(");
				sb.append(leftStr);
				sb.append(')');
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "ceiling");
				sb.append(leftStr);
				sb.append(')');
				break;
			case MAXIMA:
			case PSTRICKS:
				sb.append("ceiling(");
				sb.append(leftStr);
				sb.append(')');
				break;

			default:
				sb.append("ceil(");
				sb.append(leftStr);
				sb.append(')');
			}
			break;

		case ROUND:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\mathrm{round} \\left( ");
				break;

			case LIBRE_OFFICE:
				sb.append("func round left (");
				
			case MATH_PIPER:
				sb.append("Round(");
				break;

			case MPREDUCE:
				appendReduceFunction(sb, "myround");
				break;

			default:
				sb.append("round(");
			}
			sb.append(leftStr);
			sb.append(rightBracket(STRING_TYPE));
			break;

		case GAMMA:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\Gamma \\left( ");
				break;
			case LIBRE_OFFICE:
				sb.append("%GAMMA left (");
				break;
			case MATH_PIPER:
				sb.append("Gamma(");
				break;
			case MPREDUCE:
				appendReduceFunction(sb, "gamma");
				break;
				
			default:
				sb.append("gamma(");
			}
			sb.append(leftStr);
			sb.append(rightBracket(STRING_TYPE));
			break;

		case GAMMA_INCOMPLETE:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\gamma \\left( ");
				break;
			case LIBRE_OFFICE:
				sb.append("%GAMMA left (");
				break;
			case MAXIMA:
				sb.append("gamma_incomplete(");
				break;

			case MPREDUCE:
				appendReduceFunction(sb, "gamma2");
				break;


			default:
				sb.append("gamma(");
			}
			sb.append(leftStr);
			if (STRING_TYPE.equals(StringType.LIBRE_OFFICE))
				sb.append("\",\"");
			else
				sb.append(", ");
			sb.append(rightStr);
			sb.append(rightBracket(STRING_TYPE));
			break;

		case GAMMA_INCOMPLETE_REGULARIZED:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("P \\left( ");
				break;

			case LIBRE_OFFICE:
				sb.append("func gammaRegularized left (");
				
			case MAXIMA:
				sb.append("gamma_incomplete_regularized(");
				break;


			default:
				sb.append("gammaRegularized(");
			}
			sb.append(leftStr);
			if (STRING_TYPE.equals(StringType.LIBRE_OFFICE))
				sb.append("\",\"");
			else
				sb.append(", ");
			sb.append(rightStr);
			sb.append(rightBracket(STRING_TYPE));
			break;

		case BETA:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\Beta \\left( ");
				break;
			case LIBRE_OFFICE:
				sb.append("%BETA left(");
				break;
			case MATH_PIPER:
				sb.append("Beta(");
				break;

			default:
				sb.append("beta(");
			}
			sb.append(leftStr);
			if (STRING_TYPE.equals(StringType.LIBRE_OFFICE))
				sb.append("\",\"");
			else
				sb.append(", ");
			sb.append(rightStr);
			sb.append(rightBracket(STRING_TYPE));
			break;

		case BETA_INCOMPLETE:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("\\Beta \\left( ");
				break;
			case LIBRE_OFFICE:
				sb.append("%BETA left(");
				break;	

			case MAXIMA:
				sb.append("beta_incomplete(");
				break;

			default:
				sb.append("beta(");
			}
			sb.append(leftStr);
			if (STRING_TYPE.equals(StringType.LIBRE_OFFICE))
				sb.append("\",\"");
			else
				sb.append(", ");
			sb.append(rightStr);
			sb.append(rightBracket(STRING_TYPE));
			break;

		case BETA_INCOMPLETE_REGULARIZED:
			switch (STRING_TYPE) {
			case LATEX:
				sb.append("I \\left( ");
				break;

			case LIBRE_OFFICE:
				sb.append("func betaRegularized left (");
				
			case MAXIMA:
				sb.append("beta_incomplete_regularized(");
				break;

			case MPREDUCE:
				sb.append("ibeta(");
				break;

			default:
				sb.append("betaRegularized(");
			}
			sb.append(leftStr);
			if (STRING_TYPE.equals(StringType.LIBRE_OFFICE))
				sb.append("\",\"");
			else
				sb.append(", ");
			sb.append(rightStr);
			sb.append(rightBracket(STRING_TYPE));
			break;

		case RANDOM:
			if (valueForm) {
				sb.append(leftStr);
			} else {
				switch (STRING_TYPE) {
				case MPREDUCE:
					sb.append("myrandom()");
					break;
				case LIBRE_OFFICE:
					sb.append("func ");
				default:
					sb.append("random()");
				}
			}
			break;

		case XCOORD:
			if (valueForm && (leftEval = left.evaluate(tpl)).isVectorValue()) {
				sb.append(kernel.format(((VectorValue) leftEval).getVector()
						.getX(), tpl));
			} else if (valueForm
					&& (leftEval = left.evaluate(tpl)).isVector3DValue()) {
				sb.append(kernel.format(
						((Vector3DValue) leftEval).getPointAsDouble()[0], tpl));
			} else if (valueForm
					&& ((leftEval = left.evaluate(tpl)) instanceof GeoLine)) {
				sb.append(kernel.format(((GeoLine) leftEval).getX(), tpl));
			} else {
				switch (STRING_TYPE) {
				case LATEX:
					sb.append("\\mathrm{x} \\left( ");
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
					break;
				case LIBRE_OFFICE:
					sb.append("func x left (");
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
				case MATH_PIPER:
				case MAXIMA:
					// we need to protect x(A) as a constant in the CAS
					// see http://www.geogebra.org/trac/ticket/662
					// see http://www.geogebra.org/trac/ticket/922
					sb.append("xcoord(");
					sb.append(leftStr);
					sb.append(')');
					break;
				case MPREDUCE:
					appendReduceFunction(sb, "xcoord");
					sb.append(leftStr);
					sb.append(')');
					break;
				default:
					sb.append("x(");
					sb.append(leftStr);
					sb.append(')');
				}
			}
			break;

		case YCOORD:
			if (valueForm && (leftEval = left.evaluate(tpl)).isVectorValue()) {
				sb.append(kernel.format(((VectorValue) leftEval).getVector()
						.getY(), tpl));
			} else if (valueForm
					&& (leftEval = left.evaluate(tpl)).isVector3DValue()) {
				sb.append(kernel.format(
						((Vector3DValue) leftEval).getPointAsDouble()[1], tpl));
			} else if (valueForm
					&& ((leftEval = left.evaluate(tpl)) instanceof GeoLine)) {
				sb.append(kernel.format(((GeoLine) leftEval).getY(), tpl));
			} else {
				switch (STRING_TYPE) {
				case LATEX:
					sb.append("\\mathrm{y} \\left( ");
					sb.append(leftStr);
					sb.append("\\right)");
					break;
				case LIBRE_OFFICE:
					sb.append("func y left (");
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
				case MATH_PIPER:
				case MAXIMA:
					// we need to protect x(A) as a constant in the CAS
					// see http://www.geogebra.org/trac/ticket/662
					// see http://www.geogebra.org/trac/ticket/922
					sb.append("ycoord(");
					sb.append(leftStr);
					sb.append(')');
					break;
				case MPREDUCE:
					appendReduceFunction(sb, "ycoord");
					sb.append(leftStr);
					sb.append(')');
					break;
				default:
					sb.append("y(");
					sb.append(leftStr);
					sb.append(')');
				}
			}
			break;

		case ZCOORD:
			if (valueForm && (leftEval = left.evaluate(tpl)).isVector3DValue()) {
				sb.append(kernel.format(
						((Vector3DValue) leftEval).getPointAsDouble()[2], tpl));
			} else if (valueForm
					&& ((leftEval = left.evaluate(tpl)) instanceof GeoLine)) {
				sb.append(kernel.format(((GeoLine) leftEval).getZ(), tpl));
			} else {
				switch (STRING_TYPE) {
				case LATEX:
					sb.append("\\mathrm{z} \\left( ");
					sb.append(leftStr);
					sb.append("\\right)");
					break;
				case LIBRE_OFFICE:
					sb.append("func z left (");
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
				case MATH_PIPER:
				case MAXIMA:
					// we need to protect x(A) as a constant in the CAS
					// see http://www.geogebra.org/trac/ticket/662
					// see http://www.geogebra.org/trac/ticket/922
					sb.append("zcoord(");
					sb.append(leftStr);
					sb.append(')');
					break;
				case MPREDUCE:
					appendReduceFunction(sb, "zcoord");
					sb.append(leftStr);
					sb.append(')');
					break;
				default:
					sb.append("z(");
					sb.append(leftStr);
					sb.append(')');
				}
			}
			break;

		case FUNCTION:
			if (STRING_TYPE == StringType.MPREDUCE
					&& right instanceof ListValue) {
				sb.append("applyfunction(" + leftStr + "," + rightStr + ")");
				break;
			}
			// GeoFunction and GeoFunctionConditional should not be expanded
			if (left instanceof GeoFunction) {
				GeoFunction geo = (GeoFunction) left;
				if (geo.isLabelSet()) {
					if (STRING_TYPE.equals(StringType.LIBRE_OFFICE))
							sb.append("func ");
					sb.append(geo.getLabel(tpl));
					sb.append(leftBracket(STRING_TYPE));
					sb.append(rightStr);
					sb.append(rightBracket(STRING_TYPE));
				} else {
					// inline function: replace function var by right side
					FunctionVariable var = geo.getFunction()
							.getFunctionVariable();
					String oldVarStr = var.toString(tpl);
					var.setVarString(rightStr);
					if (STRING_TYPE.equals(StringType.LIBRE_OFFICE))
						sb.append("func ");
					sb.append(geo.getLabel(tpl));
					var.setVarString(oldVarStr);
				}
			} else if (valueForm && left.isExpressionNode()) {
				ExpressionNode en = (ExpressionNode) left;
				// left could contain $ nodes to wrap a GeoElement
				// e.g. A1(x) = x^2 and B1(x) = $A$1(x)
				// value form of B1 is x^2 and NOT x^2(x)
				switch (en.operation) {
				case $VAR_ROW:
				case $VAR_COL:
				case $VAR_ROW_COL:
					sb.append(leftBracket(STRING_TYPE));
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
					break;

				default:
					sb.append(leftStr);
					sb.append(leftBracket(STRING_TYPE));
					sb.append(rightStr);
					sb.append(rightBracket(STRING_TYPE));
					break;
				}
			} else {
				// standard case if we get here
				sb.append(leftStr);
				sb.append(leftBracket(STRING_TYPE));
				sb.append(rightStr);
				sb.append(rightBracket(STRING_TYPE));
			}
			break;

		// TODO: put back into case FUNCTION_NVAR:, see #1115
		case ELEMENT_OF:
			sb.append(app.getCommand("Element"));
			sb.append('[');
			if (left.isGeoElement()) {
				sb.append(((GeoElement) left).getLabel(tpl));
			} else {
				sb.append(leftStr);
			}
			sb.append(", ");
			sb.append(((MyList) right).getListElement(0).toString(tpl));
			sb.append(']');
			break;

		case FUNCTION_NVAR:
			if (valueForm) {
				// TODO: avoid replacing of expressions in operationToString
				if ((left instanceof FunctionalNVar)
						&& (right instanceof MyList)) {
					FunctionNVar func = ((FunctionalNVar) left).getFunction();
					ExpressionNode en = func.getExpression().getCopy(kernel);
					for (int i = 0; (i < func.getVarNumber())
							&& (i < ((MyList) right).size()); i++) {
						en.replace(func.getFunctionVariables()[i],
								((MyList) right).getListElement(i));
					}
					// add brackets, see
					// http://www.geogebra.org/trac/ticket/1446
					if (!STRING_TYPE.equals(StringType.LATEX)) {
						sb.append(leftBracket(STRING_TYPE));
					}
					sb.append(en.toValueString(tpl));
					if (!STRING_TYPE.equals(StringType.LATEX)) {
						sb.append(rightBracket(STRING_TYPE));
					}
				} else {
					sb.append(leftBracket(STRING_TYPE));
					sb.append(leftStr);
					sb.append(rightBracket(STRING_TYPE));
				}
			} else {
				// multivariate functions
				if (left.isGeoElement()) {
					sb.append(((GeoElement) left).getLabel(tpl));
				} else {
					sb.append(leftStr);
				}
				sb.append(leftBracket(STRING_TYPE));
				// rightStr is a list of arguments, e.g. {2, 3}
				// drop the curly braces { and }
				// or list( and ) in case of mpreduce
				if (STRING_TYPE.equals(StringType.MPREDUCE)) {
					sb.append(rightStr.substring(22, rightStr.length() - 2));
				} else {
					sb.append(rightStr.substring(1, rightStr.length() - 1));
				}
				sb.append(rightBracket(STRING_TYPE));
			}
			break;

		case VEC_FUNCTION:
			// GeoCurveCartesian should not be expanded
			if (left.isGeoElement()
					&& ((GeoElement) left).isGeoCurveCartesian()) {
				sb.append(((GeoElement) left).getLabel(tpl));
			} else {
				sb.append(leftStr);
			}
			sb.append(leftBracket(STRING_TYPE));
			sb.append(rightStr);
			sb.append(rightBracket(STRING_TYPE));
			break;

		case DERIVATIVE: // e.g. f''
			// labeled GeoElements should not be expanded
			if (left.isGeoElement() && ((GeoElement) left).isLabelSet()) {
				sb.append(((GeoElement) left).getLabel(tpl));
			} else {
				sb.append(leftStr);
			}

			if (right.isNumberValue()) {
				int order = (int) Math.round(((MyDouble) right).getDouble());
				for (; order > 0; order--) {
					sb.append('\'');
				}
			} else {
				sb.append(right);
			}
			break;

		case $VAR_ROW: // e.g. A$1
			if (valueForm) {
				// GeoElement value
				sb.append(leftStr);
			} else {
				// $ for row
				GeoElement geo = (GeoElement) left;
				if (geo.getSpreadsheetCoords() != null) {
					sb.append(geo.getSpreadsheetLabelWithDollars(false, true));
				} else {
					sb.append(leftStr);
				}
			}
			break;

		case $VAR_COL: // e.g. $A1
			if (valueForm) {
				// GeoElement value
				sb.append(leftStr);
			} else {
				// $ for row
				GeoElement geo = (GeoElement) left;
				if (geo.getSpreadsheetCoords() != null) {
					sb.append(geo.getSpreadsheetLabelWithDollars(true, false));
				} else {
					sb.append(leftStr);
				}
			}
			break;

		case $VAR_ROW_COL: // e.g. $A$1
			if (valueForm) {
				// GeoElement value
				sb.append(leftStr);
			} else {
				// $ for row
				GeoElement geo = (GeoElement) left;
				if (geo.getSpreadsheetCoords() != null) {
					sb.append(geo.getSpreadsheetLabelWithDollars(true, true));
				} else {
					sb.append(leftStr);
				}
			}
			break;

		case FREEHAND:
			// need to output eg freehand(ggbtmpvarx) so that Derivative fails
			// rather than giving zero
			sb.append("freehand(");
			sb.append(leftStr);
			sb.append(')');
			break;
		case INTEGRAL:
			if (STRING_TYPE == StringType.LATEX) {
				sb.append("\\int ");
				sb.append(leftStr);
				sb.append("d");
				sb.append(rightStr);
			} else if (STRING_TYPE == StringType.LIBRE_OFFICE) {
				sb.append("int ");
				sb.append(leftStr);
				sb.append(" d");
				sb.append(rightStr);
			} else {
				if (STRING_TYPE == StringType.MPREDUCE) {
					sb.append("int(");
				} else {
					sb.append("gGbInTeGrAl(");
				}
				sb.append(leftStr);
				sb.append(',');
				sb.append(rightStr);
				sb.append(")");
				// AbstractApplication.debug(sb);
			}
			break;
		case SUM:
			if (STRING_TYPE == StringType.LATEX) {
				sb.append("\\sum_{");
				sb.append(((MyNumberPair)left).y.toString(tpl));
				sb.append("=");
				sb.append(((MyNumberPair)right).x.toString(tpl));
				sb.append("}^{");
				sb.append(((MyNumberPair)right).y.toString(tpl));
				sb.append("}");
				sb.append(((MyNumberPair)left).x.toString(tpl));
			} else if (STRING_TYPE == StringType.LIBRE_OFFICE) {
				sb.append("sum from{");
				sb.append(((MyNumberPair)left).y.toString(tpl));
				sb.append("=");
				sb.append(((MyNumberPair)right).x.toString(tpl));
				sb.append("} to{");
				sb.append(((MyNumberPair)right).y.toString(tpl));
				sb.append("}");
				sb.append(((MyNumberPair)left).x.toString(tpl));
			} else {
				if (STRING_TYPE == StringType.MPREDUCE) {
					sb.append("sum(");
				} else {
					sb.append("gGbSuM(");
				}
				sb.append(leftStr);
				sb.append(',');
				sb.append(rightStr);
				sb.append(")");
				// AbstractApplication.debug(sb);
			}
			break;	
		case SUBSTITUTION:
			if (STRING_TYPE == StringType.LATEX) {
				sb.append("\\left.");
				sb.append(rightStr);
				sb.append("\\right\\mid_{");
				sb.append(leftStr);
				sb.append("}");
			} else if (STRING_TYPE == StringType.LIBRE_OFFICE) {
				sb.append("left none");
				sb.append(rightStr);
				sb.append("right rline_{");
				sb.append(leftStr);
				sb.append("}");	
			} else {
				if (STRING_TYPE == StringType.MPREDUCE) {
					sb.append("sub(");
				} else {
					sb.append("gGbSuBsTiTuTiOn(");
				}
				sb.append(leftStr);
				sb.append(',');
				sb.append(rightStr);
				sb.append(")");
			}
			break;
			
			
			
		default:
			sb.append("unhandled operation " + operation);
		}
		return sb.toString();
	}

	private static void appendOp(StringBuilder sb, String string, String leftStr,
			String rightStr) {
		sb.append(string);
		sb.append('(');
		sb.append(leftStr);
		sb.append(',');
		sb.append(rightStr);
		sb.append(')');
		
	}

	private void trig(String leftStr, StringBuilder sb, String mathml, String latex,
			String mathPiper, String psTricks, String mpReduce, String key,
			String libreOffice,
			StringType STRING_TYPE, boolean localized,boolean needDegrees) {
		if (STRING_TYPE.equals(StringType.MATHML)) {
			mathml(sb, mathml, leftStr, null);
		} else {
			switch (STRING_TYPE) {
			case LATEX:
				if (app.isHTML5Applet()) {
					sb.append("\\" + app.getFunction(key));
				} else if(localized) {
					sb.append("\\operatorname{");
					sb.append(app.getFunction(key));
					sb.append("}");
				}else{
					sb.append(latex);
				}
				sb.append(" \\left( ");
				break;
			case LIBRE_OFFICE:
				if(!libreOffice.equals(app.getFunction(key))){
					sb.append("func ");
					
				}
				sb.append(app.getFunction(key));
				sb.append(" left( ");
				break;
			case MATH_PIPER:
				sb.append(mathPiper);
				break;

			case PSTRICKS:
				sb.append(psTricks);
				break;
			case MPREDUCE:
				appendReduceFunction(sb, mpReduce);
				break;
			default:
				if(localized){
					sb.append(app.getFunction(key));
				}
				else{	
					sb.append(key);
				}
				sb.append("(");
			}
			if (needDegrees && STRING_TYPE.equals(StringType.PGF)) {
				sb.append("(" + leftStr + ") 180/pi");
			} else {
				sb.append(leftStr);
			}
			sb.append(rightBracket(STRING_TYPE));
		}
		
	}
	
	private void appendReduceFunction(StringBuilder sb, String string) {
		App.debug(left.getClass());
		if (left instanceof ListValue) {
			sb.append("applyfunction(");
			sb.append(string);
			sb.append(",");
		} else {
			sb.append(string);
			sb.append('(');
		}

	}

	private static boolean isMultiplyOrDivide(ExpressionNode exp) {
		return exp.getOperation().equals(Operation.MULTIPLY)
				|| exp.getOperation().equals(Operation.DIVIDE);
	}

	private static void mathml(StringBuilder sb, String op, String leftStr,
			String rightStr) {
		mathml(sb, op, "", leftStr, "", "", rightStr, "");
	}

	private static void mathml(StringBuilder sb, String op, String preL,
			String leftStr, String postL, String preR, String rightStr,
			String postR) {
		sb.append("<apply>");
		sb.append(op);
		sb.append(preL);

		if (leftStr.startsWith("<apply>")) {
			sb.append(leftStr);
		} else if (StringUtil.isNumber(leftStr)) {
			sb.append("<cn>");
			sb.append(leftStr);
			sb.append("</cn>");
		} else {
			sb.append("<ci>");
			sb.append(leftStr);
			sb.append("</ci>");
		}

		sb.append(postL);
		sb.append(preR);

		if (rightStr != null) {
			if (rightStr.startsWith("<apply>")) {
				sb.append(rightStr);
			} else if (StringUtil.isNumber(rightStr)) {
				sb.append("<cn>");
				sb.append(rightStr);
				sb.append("</cn>");
			} else {
				sb.append("<ci>");
				sb.append(rightStr);
				sb.append("</ci>");
			}
		}

		sb.append(postR);

		sb.append("</apply>");

	}

	/**
	 * return operation number for expression nodes and -1 for other expression
	 * values
	 * 
	 * @param ev
	 *            expression value
	 * @return operation number
	 */
	static public int opID(ExpressionValue ev) {
		if (ev.isExpressionNode()) {
			Operation op = ((ExpressionNode) ev).operation;
			// input (x>y)==(x+y>3) must be kept
			if (op.equals(Operation.GREATER) || op.equals(Operation.LESS)
					|| op.equals(Operation.LESS_EQUAL)
					|| op.equals(Operation.GREATER_EQUAL)) {
				return Operation.NOT_EQUAL.ordinal() - 1;
			}
			return op.ordinal();
		}
		return -1;
	}

	public boolean isNumberValue() {
		return evaluate(StringTemplate.defaultTemplate).isNumberValue();
	}

	public boolean isBooleanValue() {
		return evaluate(StringTemplate.defaultTemplate).isBooleanValue();
	}

	public boolean isListValue() {
		return evaluate(StringTemplate.defaultTemplate).isListValue();
	}

	public boolean isPolynomialInstance() {
		// return evaluate().isPolynomial();
		return false;
	}

	public boolean isTextValue() {
		// should be efficient as it is used in operationToString()
		if (leaf) {
			return left.isTextValue();
		}
		return (operation.equals(Operation.PLUS) && (left.isTextValue() || right
				.isTextValue()));
	}

	final public boolean isExpressionNode() {
		return true;
	}

	public boolean isVector3DValue() {
		return false;
	}

	/**
	 * Returns true iff ev1 and ev2 are equal
	 * 
	 * @param ev1
	 *            first value to compare
	 * @param ev2
	 *            second value to compare
	 * @return true iff ev1 and ev2 are equal
	 */
	public static boolean isEqual(ExpressionValue ev1, ExpressionValue ev2) {
		if (ev1.isNumberValue() && ev2.isNumberValue()) {
			return Kernel.isEqual(((NumberValue) ev1).getDouble(),
					((NumberValue) ev2).getDouble(), Kernel.EPSILON);
		} else if (ev1.isTextValue() && ev2.isTextValue()) {
			return ((TextValue) ev1).toValueString(
					StringTemplate.defaultTemplate).equals(
					((TextValue) ev2)
							.toValueString(StringTemplate.defaultTemplate));
		} else if (ev1.isVectorValue() && ev2.isVectorValue()) {
			return ((VectorValue) ev1).getVector().isEqual(
					((VectorValue) ev2).getVector());
		} else if (ev1.isBooleanValue() && ev2.isBooleanValue()) {
			return ((BooleanValue) ev1).getMyBoolean().getBoolean() == ((BooleanValue) ev2)
					.getMyBoolean().getBoolean();
		} else if (ev1.isGeoElement() && ev2.isGeoElement()) {
			return ((GeoElement) ev1).isEqual(((GeoElement) ev2));
		} else if (ev1 instanceof Functional && ev2 instanceof Functional) {
			return ((Functional) ev1).getGeoFunction().isEqual(
					((Functional) ev2).getGeoFunction());
		}

		return false;
	}

	/**
	 * Returns whether the given expression will give the same String output as
	 * val.
	 * 
	 * @param symbolic
	 *            whether we should use the value (true) or the label (false) of
	 *            ev when it is a GeoElement
	 * @param val
	 *            numeric value
	 * @param ev
	 *            expression value to compare with val
	 * @return true iff output of ev and val are the same
	 */
	final public static boolean isEqualString(ExpressionValue ev, double val,
			boolean symbolic) {
		if (ev.isLeaf() && (ev instanceof NumberValue)) {
			// function variables need to be kept
			if (ev instanceof FunctionVariable) {
				return false;
			} else if (ev instanceof MySpecialDouble) {
				// special doubles like pi, degree, rad need to be kept
				return false;
			}

			// check if ev is a labeled GeoElement
			if (symbolic) {
				if (ev.isGeoElement()) {
					// labeled GeoElement
					GeoElement geo = (GeoElement) ev;
					if (geo.isLabelSet() || geo.isLocalVariable()
							|| !geo.isIndependent()) {
						return false;
					}
				}
			}

			NumberValue nv = (NumberValue) ev;
			return nv.getDouble() == val;
		}
		return false;
	}

	@Override
	public boolean isTopLevelCommand() {
		return isLeaf() && (left instanceof Command);
	}

	@Override
	public Command getTopLevelCommand() {
		if (isTopLevelCommand()) {
			return (Command) left;
		}
		return null;
	}

	private static String leftBracket(StringType type) {
		//return (type.equals(StringType.LATEX)) ? " \\left( " : "(";
		
		if (type.equals(StringType.LATEX))
			return " \\left( ";
		else if (type.equals(StringType.LIBRE_OFFICE))
			return " left ( ";
		else 
			return "(";
	}

	private static String rightBracket(StringType type) {
		//return (type.equals(StringType.LATEX)) ? " \\right) " : ")";
		
		if (type.equals(StringType.LATEX))
			return " \\right)";
		else if (type.equals(StringType.LIBRE_OFFICE))
			return " right )";
		else 
			return ")";
	}

	private static String multiplicationSign(StringType type) {
		switch (type) {
		case LATEX:
			return " \\cdot ";
		
		case LIBRE_OFFICE:
			return " cdot ";
			
		case GEOGEBRA:
			return " "; // space for multiplication

		default:
			return " * ";
		}
	}

	private static String multiplicationSpace(StringType type) {
		// wide space for multiplicatoin space in LaTeX
		return (type.equals(StringType.LATEX)) ? " \\; " : " ";
	}

	/**
	 * If the expression is linear in fv, returns the corresponding coefficient.
	 * Otherwise returns null.
	 * 
	 * @param fv
	 *            variable whose coefficient we want
	 * @return coefficient or null
	 */
	public Double getCoefficient(FunctionVariable fv) {
		if (this.isLeaf()) {
			if (this.toString(StringTemplate.defaultTemplate).equals(
					fv.toString(StringTemplate.defaultTemplate))) {
				return 1.0;
			}

			return 0.0;

		}

		Double lc = getLeftTree() == null ? null : getLeftTree()
				.getCoefficient(fv);
		Double rc = getRightTree() == null ? null : getRightTree()
				.getCoefficient(fv);
		if ((lc == null) || (rc == null)) {
			return null;
		}
		switch (this.operation) {
		case PLUS:
			return lc + rc;

		case MINUS:
			return lc - rc;

		case MULTIPLY:
			if (!getRightTree().containsFunctionVariable()) {
				return lc * getRightTree().evaluateNum().getDouble();
			} else if (!getLeftTree().containsFunctionVariable()) {
				return rc * getLeftTree().evaluateNum().getDouble();
			}
			break;

		case DIVIDE:
			if (!getRightTree().containsFunctionVariable()) {
				return lc / getRightTree().evaluateNum().getDouble();
			}
			break;
		}

		if ((left.contains(fv) || right.contains(fv))) {
			return null;
		}

		return 0.0;

	}

	/*
	 * appends a string to sb, brackets are put around it if the order of
	 * operation dictates
	 */
	private static void append(StringBuilder sb, String str,
			ExpressionValue ev, Operation op, StringType STRING_TYPE) {
		if (ev.isLeaf() || (opID(ev) >= op.ordinal()) && 
				(!chainedBooleanOp(op) || !chainedBooleanOp(ev.wrap().getOperation()))) {
			sb.append(str);
		} else {
			sb.append(leftBracket(STRING_TYPE));
			sb.append(str);
			sb.append(rightBracket(STRING_TYPE));
		}

	}

	private static boolean chainedBooleanOp(Operation op) {
		switch(op){
		case EQUAL_BOOLEAN:
		case NOT_EQUAL:
		case IS_SUBSET_OF:
		case IS_SUBSET_OF_STRICT:
		case LESS:
		case LESS_EQUAL:
		case GREATER:
		case GREATER_EQUAL:
		case PERPENDICULAR:
		case PARALLEL:	
			return true;	
		}
		return false;
	}

	@Override
	public String toRealString(StringTemplate tpl) {
		if (leaf) { // leaf is GeoElement or not
			if (left.isGeoElement()) {
				return ((GeoElement) left).getRealLabel(tpl);
			}
			return left.toRealString(tpl);
		}

		// expression node
		String leftStr = null, rightStr = null;
		if (left.isGeoElement()) {
			leftStr = ((GeoElement) left).getRealLabel(tpl);
		} else {
			leftStr = left.toRealString(tpl);
		}

		if (right != null) {
			if (right.isGeoElement()) {
				rightStr = ((GeoElement) right).getRealLabel(tpl);
			} else {
				rightStr = right.toRealString(tpl);
			}
		}
		return operationToString(leftStr, rightStr, false, tpl);
	}

	private static boolean isConstantDouble(ExpressionValue ev, double v) {
		if (ev instanceof ExpressionNode)
			return isConstantDouble(((ExpressionNode) ev).getLeft(), v);
		return ev instanceof MyDouble && ev.isConstant()
				&& Kernel.isEqual(v, ((MyDouble) ev).getDouble());
	}

	/**
	 * @param v2
	 *            value to add
	 * @return result of addition
	 */
	public ExpressionNode plus(ExpressionValue v2) {
		if (isConstantDouble(v2, 0))
			return this;
		if (this.isLeaf() && isConstantDouble(left, 0))
			return new ExpressionNode(kernel, v2);
		return new ExpressionNode(kernel, this, Operation.PLUS, v2);
	}

	/**
	 * @param v2
	 *            value to compare
	 * @return result this < v2
	 */
	public ExpressionNode lessThan(ExpressionValue v2) {
		return new ExpressionNode(kernel, this, Operation.LESS, v2);
	}

	/**
	 * @param d
	 *            value to compare
	 * @return result this < d
	 */
	public ExpressionNode lessThan(double d) {
		return new ExpressionNode(kernel, this, Operation.LESS, new MyDouble(
				kernel, d));
	}
	
	/**
	 * @param d
	 *            value to compare
	 * @return result this <= d
	 */
	public ExpressionNode lessThanEqual(double d) {
		return new ExpressionNode(kernel, this, Operation.LESS_EQUAL, new MyDouble(
				kernel, d));
	}

	/**
	 * @return result of erf(this)
	 */
	public ExpressionNode erf() {
		return new ExpressionNode(kernel, this, Operation.ERF, null);
	}

	/**
	 * @return result of abs(this)
	 */
	public ExpressionNode abs() {
		return new ExpressionNode(kernel, this, Operation.ABS, null);
	}

	/**
	 * @return result of this!
	 */
	public ExpressionNode factorial() {
		return new ExpressionNode(kernel, this, Operation.FACTORIAL, null);
	}

	/**
	 * @return result of ln(this)
	 */
	public ExpressionNode ln() {
		return new ExpressionNode(kernel, this, Operation.LOG, null);
	}

	/**
	 * @return result of gamma(this)
	 */
	public ExpressionNode gamma() {
		return new ExpressionNode(kernel, this, Operation.GAMMA, null);
	}

	/**
	 * @param v2
	 *            input
	 * @return result of gamma(v2, this)
	 */
	public ExpressionNode gammaIncompleteReverseArgs(ExpressionValue v2) {
		return new ExpressionNode(kernel, v2, Operation.GAMMA_INCOMPLETE, this);
	}

	/**
	 * @param v2
	 *            input
	 * @return result of gamma(this, v2)
	 */
	public ExpressionNode gammaIncomplete(ExpressionValue v2) {
		return new ExpressionNode(kernel, this, Operation.GAMMA_INCOMPLETE, v2);
	}

	/**
	 * @param v2
	 *            input
	 * @return result of beta(this, v2)
	 */
	public ExpressionNode beta(ExpressionValue v2) {
		return new ExpressionNode(kernel, this, Operation.BETA, v2);
	}

	/**
	 * @return result of exp(this)
	 */
	public ExpressionNode exp() {
		return new ExpressionNode(kernel, this, Operation.EXP, null);
	}

	/**
	 * @return result of 1/this
	 */
	public ExpressionNode reciprocate() {
		return new ExpressionNode(kernel, this, Operation.POWER, new MyDouble(
				kernel, -1));
	}

	/**
	 * @return result of sqrt(this)
	 */
	public ExpressionNode sqrt() {
		return new ExpressionNode(kernel, this, Operation.SQRT, null);
	}

	/**
	 * @return result of sqrt(this)
	 */
	public ExpressionNode sgn() {
		return new ExpressionNode(kernel, this, Operation.SGN, null);
	}

	/**
	 * @return result of atan(this)
	 */
	public ExpressionNode atan() {
		return new ExpressionNode(kernel, this, Operation.ARCTAN, null);
	}

	/**
	 * @return result of this * -1
	 */
	public ExpressionNode reverseSign() {
		return new ExpressionNode(kernel, new MyDouble(kernel, -1.0),
				Operation.MULTIPLY, this);
	}

	/**
	 * @return result of this * -1
	 */
	public ExpressionNode reverseSign2() {
		return new ExpressionNode(kernel, new MyDouble(kernel, 0.0),
				Operation.MINUS, this);
	}

	/**
	 * @param v1
	 *            input
	 * @param v2
	 *            input
	 * @return result of betaRegularized(this, v1, v2)
	 */
	public ExpressionNode betaRegularized(ExpressionValue v1, ExpressionValue v2) {
		return new ExpressionNode(kernel, new MyNumberPair(kernel, this, v1),
				Operation.BETA_INCOMPLETE_REGULARIZED, v2);
	}

	/**
	 * @return result of this^2
	 */
	public ExpressionNode square() {
		return new ExpressionNode(kernel, this, Operation.POWER, new MyDouble(
				kernel, 2.0));
	}

	/**
	 * @param v2
	 *            value to subtract
	 * @return result of subtract
	 */
	public ExpressionNode subtract(ExpressionValue v2) {
		if (isConstantDouble(v2, 0))
			return this;
		if (this.isLeaf() && isConstantDouble(left, 0))
			return new ExpressionNode(kernel, v2);
		return new ExpressionNode(kernel, this, Operation.MINUS, v2);
	}

	/**
	 * @param d
	 *            value to add
	 * @return result of add
	 */
	public ExpressionNode plus(double d) {
		if (d == 0) {
			return this;
		}
		return new ExpressionNode(kernel, this, Operation.PLUS, new MyDouble(
				kernel, d));
	}

	/**
	 * @param d
	 *            value to multiply
	 * @return result of multiply
	 */
	public ExpressionNode multiply(double d) {
		if (Kernel.isZero(d)) {
			return new ExpressionNode(kernel, 0);
		} else if (Kernel.isEqual(1,  d)) {
			return this;
		}
		return new ExpressionNode(kernel, this, Operation.MULTIPLY,
				new MyDouble(kernel, d));
	}

	/**
	 * @param d
	 *            value to multiply
	 * @return result of multiply
	 */
	public ExpressionNode power(double d) {
		if (Kernel.isZero(d)) {
			return new ExpressionNode(kernel, 1);
		} else if (Kernel.isEqual(1,  d)) {
			return this;
		}
		return new ExpressionNode(kernel, this, Operation.POWER,
				new MyDouble(kernel, d));
	}

	/**
	 * @param d
	 *            value to subtract
	 * @return this - d
	 */
	public ExpressionNode subtract(double d) {
		if (d == 0) {
			return this;
		}
		return new ExpressionNode(kernel, this, Operation.MINUS, new MyDouble(
				kernel, d));
	}

	/**
	 * @param d
	 *            value to subtract
	 * @return d - this
	 */
	public ExpressionNode subtractR(double d) {
		if (d == 0) {
			return this;
		}
		return new ExpressionNode(kernel, new MyDouble(kernel, d),
				Operation.MINUS, this);
	}

	/**
	 * @param v2
	 *            coefficient
	 * @return result of multiplication
	 */
	public ExpressionNode multiply(ExpressionValue v2) {
		if (isConstantDouble(v2, 0))
			return new ExpressionNode(kernel, v2);
		if (isConstantDouble(v2, 1))
			return this;
		return new ExpressionNode(kernel, v2, Operation.MULTIPLY, this);
	}

	/**
	 * @param v2
	 *            exponent
	 * @return resulting power
	 */
	public ExpressionNode power(ExpressionValue v2) {
		if(isConstantDouble(v2,0))
			return new ExpressionNode(kernel,new MyDouble(kernel,1));
		if (isConstantDouble(v2, 1))
			return this;
		return new ExpressionNode(kernel, this, Operation.POWER, v2);
	}

	/**
	 * @param d
	 *            exponent
	 * @return d ^ this
	 */
	public ExpressionNode powerR(double d) {
		return new ExpressionNode(kernel, new MyDouble(kernel, d),
				Operation.POWER, this);
	}

	/**
	 * @param v2
	 *            divisor
	 * @return result of division
	 */
	public ExpressionNode divide(ExpressionValue v2) {
		return new ExpressionNode(kernel, this, Operation.DIVIDE, v2);
	}

	/**
	 * @param d
	 *            divisor
	 * @return result of division
	 */
	public ExpressionNode divide(double d) {
		return new ExpressionNode(kernel, this, Operation.DIVIDE, new MyDouble(
				kernel, d));
	}

	/**
	 * @param v2
	 *            clause
	 * @return result of conjuction
	 */
	public ExpressionNode and(ExpressionValue v2) {
		return new ExpressionNode(kernel, this, Operation.AND, v2);
	}

	/**
	 * @return negation of this expression (optimizes negation of >,<,=>,<=)
	 */
	public ExpressionNode negation() {
		switch (this.operation) {
		case GREATER:
			return new ExpressionNode(kernel, left, Operation.LESS_EQUAL, right);
		case GREATER_EQUAL:
			return new ExpressionNode(kernel, left, Operation.LESS, right);
		case LESS:
			return new ExpressionNode(kernel, left, Operation.GREATER_EQUAL,
					right);
		case LESS_EQUAL:
			return new ExpressionNode(kernel, left, Operation.GREATER, right);
		case EQUAL_BOOLEAN:
			return new ExpressionNode(kernel, left, Operation.NOT_EQUAL, right);
		case NOT_EQUAL:
			return new ExpressionNode(kernel, left, Operation.EQUAL_BOOLEAN,
					right);
		default:
			return new ExpressionNode(kernel, this, Operation.NOT, null);
		}
	}

	/**
	 * @param toRoot
	 *            true to replace powers by roots
	 * @return this node with replaced powers / roots
	 */
	public boolean replacePowersRoots(boolean toRoot) {
		boolean didReplacement = false;

		if (toRoot && getOperation() == Operation.POWER
				&& getRight().isExpressionNode()) {
			boolean hit = false;
			ExpressionNode rightLeaf = (ExpressionNode) getRight();

			// replaces 1 DIVIDE 2 by SQRT 2, and same for CBRT
			if ((rightLeaf.getOperation() == Operation.DIVIDE)) {
				if (rightLeaf.getRight()
						.toString(StringTemplate.defaultTemplate).equals("2")) {
					setOperation(Operation.SQRT);
					hit = true;
				} else if (rightLeaf.getRight()
						.toString(StringTemplate.defaultTemplate).equals("3")) {
					setOperation(Operation.CBRT);
					hit = true;
				}
				else if (!rightLeaf.getRight().unwrap().isExpressionNode() && rightLeaf.getRight().isNumberValue() &&
						Kernel.isInteger(((NumberValue)rightLeaf.getRight()).getDouble())) {
					App.debug(((NumberValue)rightLeaf.getRight()).getDouble());
					setOperation(Operation.NROOT);
					setRight(new MyDouble(kernel,((NumberValue)rightLeaf.getRight()).getDouble()));
					hit = true;
				}
				if (hit) {
					didReplacement = true;
					if (rightLeaf.getLeft()
							.toString(StringTemplate.defaultTemplate)
							.equals("1")) {
						if(operation!=Operation.NROOT)
							setRight(new MyDouble(kernel, Double.NaN));
					} else { // to parse x^(c/2) to sqrt(x^c)
						double c = rightLeaf.getLeft().evaluateNum().getDouble();
						if(c<0){
							
							setRight(new ExpressionNode(kernel,getLeft().wrap().power(-c),
									getOperation(),getRight() 
									));
							setOperation(Operation.DIVIDE);
							setLeft(new MyDouble(kernel,1.0));
								
						}
						else 	
						setLeft(new ExpressionNode(kernel, getLeft(),
								Operation.POWER, rightLeaf.getLeft()));
					}
				}

			}
		} else if (!toRoot) {
			boolean hit = false;
			// replaces SQRT 2 by 1 DIVIDE 2, and same for CBRT
			ExpressionNode power = null;
			if (getOperation() == Operation.SQRT) {
				power = new ExpressionNode(kernel, new MyDouble(kernel, 1),
						Operation.DIVIDE, new MyDouble(kernel, 2));
				hit = true;
			}
			else if (getOperation() == Operation.CBRT) {
				power = new ExpressionNode(kernel, new MyDouble(kernel, 1),
						Operation.DIVIDE, new MyDouble(kernel, 3));
				hit = true;
			}
			else if (getOperation() == Operation.NROOT) {
				power = new ExpressionNode(kernel, new MyDouble(kernel, 1),
						Operation.DIVIDE, right);
				hit = true;
			}
			if (hit) {
				didReplacement = true;
				setOperation(Operation.POWER);
				setRight(power);
			}
		}

		return didReplacement;
	}

	/**
	 * Replaces one object with another
	 * 
	 * @param oldObj
	 *            object to be replaced
	 * @param newObj
	 *            replacement
	 * @return this node with replaced objects
	 */
	public ExpressionValue replace(ExpressionValue oldObj,
			ExpressionValue newObj) {
		return traverse(Replacer.getReplacer(oldObj, newObj));
	}

	/**
	 * @param fv
	 *            parameter of function, eg 'x' in f(x)=x^2
	 * @return new GeoFunction
	 */
	public GeoFunction buildFunction(FunctionVariable fv) {
		Function tempFun = new Function(this, fv);
		tempFun.initFunction();
		return new GeoFunction(kernel.getConstruction(), tempFun);
	}
	
	@Override
	public ExpressionValue unwrap(){
		if(isLeaf())
			return getLeft();
		return this;
	}
	
	@Override
	public ExpressionNode wrap(){
		return this;
	}
}
