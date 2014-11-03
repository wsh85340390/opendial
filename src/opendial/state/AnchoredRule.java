// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.state;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;
import opendial.domains.rules.RuleOutput;
import opendial.domains.rules.Rule;
import opendial.domains.rules.Rule.RuleType;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.effects.BasicEffect;

/**
 * Representation of a probabilistic rule anchored in a particular dialogue state.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class AnchoredRule {

	// logger
	public static Logger log = new Logger("AnchoredRule", Logger.Level.DEBUG);

	// the rule
	Rule rule;	

	// rule identifier
	String id;

	// dialogue state in which the rule is anchored
	DialogueState state;

	// whether the rule is relevant
	boolean relevant = false;

	// the range of possible input values for the rule
	ValueRange inputs;

	// the range of possible output (or action) values
	ValueRange outputs;

	// the set of associated parameters
	Set<String> parameters;

	// the relevant effects for the rule
	Set<Effect> effects;


	/**
	 * Anchors the rule in the dialogue state.  The construction process leads to the
	 * determination of: <ul>
	 * <li> the relevance of the rule in the given dialogue state
	 * <li> the range of possible values for the input nodes 
	 * <li> the set of parameters associated with the rule
	 * <li> the set of possible groundings for the free variables
	 * <li> the set of possible effects generated by the rule
	 * <li> the set of possible values for the output nodes
	 * </ul>
	 * 
	 * @param rule the probabilistic rule
	 * @param state the dialogue state
	 */
	public AnchoredRule (Rule rule, DialogueState state) {
		this.rule = rule;
		id = rule.getRuleId();
		this.state = state;

		// determines the input range
		inputs = new ValueRange();
		for (ChanceNode inputNode : state.getMatchingNodes(rule.getInputVariables())) {
			inputs.addValues(inputNode.getId(), inputNode.getValues());
		}

		Set<Assignment> conditions = inputs.linearise();

		// determines the set of possible effects, output values and parameters
		// (for all possible input values and groundings)
		effects = new HashSet<Effect>();
		outputs = new ValueRange();
		parameters = new HashSet<String>();

		for (Assignment input : conditions) {
			RuleOutput output = rule.getOutput(input);
			if (!output.getEffects().isEmpty()) {
				relevant = true;
			}
			for (Effect o : output.getEffects()) {
				effects.add(o);
				parameters.addAll(output.getParameter(o).getParameterIds());
				for (BasicEffect e : o.getSubEffects()) {
					String outputVar = e.getVariable()+"'";
					outputs.addValue(outputVar, e.getValue());
				}
			}
		}
		effects.add(new Effect());

		// special case for utility rules with templated action values
		if (rule.getRuleType() == RuleType.UTIL && rule.hasUnderspecifiedEffects()) {
			relevant = true;
			parameters.addAll(rule.getParameterIds());
			rule.getOutputVariables().stream().forEach(o -> outputs.addValue(o+"'", ValueFactory.none()));
		}

	}


	/**
	 * Returns true if the anchored rule is relevant (that is, it at least one matching rule
	 * case is non-empty), and false otherwise.
	 * 
	 * @return true if rule is relevant, and false otherwise.
	 */
	public boolean isRelevant() {
		return relevant;
	}


	/**
	 * Returns the rule identifier
	 * 
	 * @return rule identifier
	 */
	public String getId() {
		return id;
	}



	/**
	 * Returns the value range for the input variables
	 * 
	 * @return the input range
	 */
	public ValueRange getInputs() {
		return inputs;
	}


	/**
	 * Returns the list of input nodes for the anchored rule
	 * 
	 * @return the input nodes
	 */
	public List<ChanceNode> getInputNodes() {
		List<ChanceNode> nodes = new ArrayList<ChanceNode>();
		for (String param : inputs.getVariables()) {
			if (state.hasChanceNode(param)) {
				nodes.add(state.getChanceNode(param));
			}
		}
		return nodes;
	}

	/**
	 * Returns the output variables for the rule
	 * 
	 * @return the output variables
	 */
	public Set<String> getOutputVariables() {
		return outputs.getVariables();
	}

	/**
	 * Returns the value range for the output variables
	 * 
	 * @return the output range
	 */
	public ValueRange getOutputs() {
		return outputs;
	}


	/**
	 * Returns the set of possible effects associated with the anchored rule
	 * 
	 * @return set of possible effects
	 */
	public Set<Effect> getEffects() {
		return effects;
	}



	/**
	 * Returns the rule
	 * 
	 * @return the rule
	 */
	public Rule getRule() {
		return rule;
	}


	/**
	 * Returns the set of parameter nodes for the anchored rule
	 * 
	 * @return the set of parameter nodes
	 */
	public Collection<ChanceNode> getParameters() {
		List<ChanceNode> nodes = new ArrayList<ChanceNode>();
		for (String param : parameters) {
			if (state.hasChanceNode(param)) {
				nodes.add(state.getChanceNode(param));
			}
		}
		return nodes;
	}


	/**
	 * Returns the string representation of the anchored rule
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return rule.toString();
	}



}

