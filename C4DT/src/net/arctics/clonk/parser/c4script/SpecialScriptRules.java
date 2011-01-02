package net.arctics.clonk.parser.c4script;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;

/**
 * This class contains some special rules that are registered with interested parties
 * (like CallFuncians and other species dwelling in ASTs)
 * @author madeen
 *
 */
public class SpecialScriptRules {
	
	public abstract class SpecialFuncRule {
		public boolean validateArguments(CallFunc callFunc, ExprElm[] arguments, C4ScriptParser parser) {
			return false;
		}
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			return null;
		}
	}
	
	private Map<String, SpecialFuncRule> funcRules = new HashMap<String, SpecialFuncRule>();
	
	protected void putFuncRule(SpecialFuncRule rule, String... forFunctions) {
		for (String func : forFunctions) {
			funcRules.put(func, rule);
		}
	}
	
	public SpecialFuncRule getFuncRuleFor(String function) {
		return funcRules.get(function);
	}
	
	// CreateObject and similar functions that will return an object of the specified type
	protected final SpecialFuncRule objectCreationRule = new SpecialFuncRule() {
		@Override
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			if (callFunc.getParams().length >= 1) {
				IType t = callFunc.getParams()[0].getType(parser);
				if (t instanceof C4ObjectType) {
					return ((C4ObjectType)t).getType();
				}
			}
			return null;
		}
	};
	
	// GetID() returns type of calling object
	protected final SpecialFuncRule getIDRule = new SpecialFuncRule() {
		@Override
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			IType t = callFunc.getPredecessorInSequence() == null ? parser.getContainerObject() : callFunc.getPredecessorInSequence().getType(parser);
			if (t instanceof C4Object)
				return ((C4Object)t).getObjectType();
			return null;
		}
	};
	
	// // It's a criteria search (FindObjects etc) so guess return type from arguments passed to the criteria search function
	protected final SpecialFuncRule criteriaSearchRule = new SpecialFuncRule() {
		@Override
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			IType t = searchCriteriaAssumedResult(parser, callFunc, true);
			if (t != null) {
				C4Function f = (C4Function) callFunc.getDeclaration();
				if (f.getReturnType() == C4Type.ARRAY)
					return new ArrayType(t);
			}
			return t;
		}
		
		private C4Object searchCriteriaAssumedResult(C4ScriptParser parser, CallFunc callFunc, boolean topLevel) {
			C4Object result = null;
			String declarationName = callFunc.getDeclarationName();
			// parameters to FindObjects itself are also &&-ed together
			if (topLevel || declarationName.equals("Find_And")) {
				for (ExprElm parm : callFunc.getParams()) {
					if (parm instanceof CallFunc) {
						CallFunc call = (CallFunc)parm;
						C4Object t = searchCriteriaAssumedResult(parser, call, false);
						if (t != null) {
							if (result == null)
								result = t;
							else {
								if (t.includes(result))
									result = t;
							}
						}
					}
				}
			}
			else if (declarationName.equals("Find_ID")) { //$NON-NLS-1$
				if (callFunc.getParams().length > 0) {
					result = callFunc.getParams()[0].guessObjectType(parser);
				}
			}
			return result;
		};
	};

	public SpecialScriptRules() {
		putFuncRule(objectCreationRule, "CreateObject", "CreateContents");
		putFuncRule(getIDRule, "GetID");
		putFuncRule(criteriaSearchRule, "FindObjects");
	}
}
