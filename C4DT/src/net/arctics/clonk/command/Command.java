package net.arctics.clonk.command;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.IExpressionListener;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.Statement;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.TraversalContinuation;
import net.arctics.clonk.util.Utilities;

public class Command {
	public static final C4ScriptBase COMMAND_BASESCRIPT;
	
	public @interface CommandFunction {}
	
	public static class C4CommandScript extends C4ScriptBase {
		
		private class C4CommandFunction extends C4Function {
            private static final long serialVersionUID = 1L;
            
			private Statement[] statements;
			@Override
			public Object invoke(Object... args) {
			    for (Statement s : statements) {
			    	s.evaluate();
			    }
			    // FIXME
			    return null;
			}
		}

        private static final long serialVersionUID = 1L;
        
		private String script;
		private C4CommandFunction main;
		
		@Override
        public ClonkIndex getIndex() {
	        return ClonkCore.getDefault().getExternIndex();
        }

		@Override
        public Object getScriptFile() {
			try {
	            return new SimpleScriptStorage(getName(), script);
            } catch (UnsupportedEncodingException e) {
	            e.printStackTrace();
	            return null;
            }
        }
		
		public C4CommandScript(String name, String script) {
			super();
			setName(name);
			this.script = script;
			C4ScriptParser parser = new C4ScriptParser(script, this) {
				@Override
				protected C4Function newFunction() {
				    return new C4CommandFunction();
				}
				@Override
				public void parseCodeOfFunction(C4Function function) throws ParsingException {
				    if (function.getName().equals("Main")) {
				    	main = (C4CommandFunction)function;
				    }
				    final List<Statement> statements = new LinkedList<Statement>();
				    this.setExpressionListener(new IExpressionListener() {
						@Override
						public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
							if (expression instanceof Statement)
								statements.add((Statement)expression);
							return TraversalContinuation.Continue;
						}
					});
				    super.parseCodeOfFunction(function);
				    ((C4CommandFunction)function).statements = statements.toArray(new Statement[statements.size()]);
				    this.setExpressionListener(null);
				}
			};
			try {
	            parser.parse();
            } catch (ParsingException e) {
	            e.printStackTrace();
            }
		}
		
		@Override
		public C4ScriptBase[] getIncludes() {
		    return new C4ScriptBase[] {
		    	COMMAND_BASESCRIPT
		    };
		}
		
		public C4CommandFunction getMain() {
			return main;
		}
		
		public Object invoke(Object... args) {
			return main.invoke(args);
		}
		
	}
	
	static {
		COMMAND_BASESCRIPT = new C4ScriptBase() {
            private static final long serialVersionUID = 1L;
            
			@Override
			public ClonkIndex getIndex() {
			    return ClonkCore.getDefault().getExternIndex();
			}
			@Override
			public Object getScriptFile() {
			    try {
	                return new SimpleScriptStorage("CommandBase", "");
                } catch (UnsupportedEncodingException e) {
	                return null;
                }
			}
		};
		
		for (Method m : Command.class.getMethods()) {
			if (m.getAnnotation(CommandFunction.class) != null)
				addCommand(m);
		}
	}
	
	private static class C4CommandFunction extends C4Function {

        private static final long serialVersionUID = 1L;
        
        private final Method method;
        
        @Override
        public Object invoke(Object... args) {
        	try {
	            return method.invoke(null, Utilities.concat(this, args));
            } catch (Exception e) {
	            e.printStackTrace();
	            return null;
            }
        }
        
        public C4CommandFunction(C4ScriptBase parent, Method method) {
        	super(method.getName(), parent, C4FunctionScope.FUNC_PUBLIC);
        	this.method = method;
        }
		
	}
	
	public static void addCommand(Method method) {
		COMMAND_BASESCRIPT.addDeclaration(new C4CommandFunction(COMMAND_BASESCRIPT, method));
	}
	
	public static void Log(Object context, String message) {
		System.out.println(message);
	}
}
