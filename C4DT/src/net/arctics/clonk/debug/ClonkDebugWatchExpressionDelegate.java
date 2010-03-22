package net.arctics.clonk.debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.Command.C4CommandScript;
import net.arctics.clonk.debug.ClonkDebugTarget.Commands;
import net.arctics.clonk.debug.ClonkDebugTarget.ILineReceiveListener;
import net.arctics.clonk.debug.ClonkDebugTarget.LineReceivedResult;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;

public class ClonkDebugWatchExpressionDelegate extends Object implements IWatchExpressionDelegate {

	private Map<String, C4CommandScript> cachedCommandScripts = new HashMap<String, C4CommandScript>();
	
	private C4CommandScript getCommandScript(String text) {
		C4CommandScript result = cachedCommandScripts.get(text);
		if (result == null) {
			result = new C4CommandScript("<Expression>", String.format(Command.COMMAND_SCRIPT_TEMPLATE, "return " + text));
			cachedCommandScripts.put(text, result);
		}
		return result;
	}

	@Override
	public void evaluateExpression(final String expression, final IDebugElement context, final IWatchExpressionListener listener) {
		/*C4CommandScript script = getCommandScript(expression);
		IVariableValueProvider variableProvider = new IVariableValueProvider() {
			@Override
			public Object getValueForVariable(String varName) {
				try {
					ClonkDebugTarget target = (ClonkDebugTarget) context.getDebugTarget();
					ClonkDebugThread thread = (ClonkDebugThread) target.getThreads()[0];
					ClonkDebugStackFrame stackFrame = thread.getStackFrames() != null && thread.getStackFrames().length > 0 ? (ClonkDebugStackFrame)thread.getStackFrames()[0] : null;
					if (stackFrame != null) {
						for (ClonkDebugVariable var : stackFrame.getVariables()) {
							if (var.getVariable().getName().equals(varName)) {
								return var.getValue().getValue();
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		final Object obj = script.invoke(variableProvider);
		final ClonkDebugValue value = obj != null ? new ClonkDebugValue((ClonkDebugTarget) context.getDebugTarget(), obj) : null;
		listener.watchEvaluationFinished(new IWatchExpressionResult() {
			
			@Override
			public boolean hasErrors() {
				return false;
			}
			
			@Override
			public IValue getValue() {
				return value;
			}
			
			@Override
			public String getExpressionText() {
				return expression;
			}
			
			@Override
			public DebugException getException() {
				return null;
			}
			
			@Override
			public String[] getErrorMessages() {
				return new String[0];
			}
		});*/
		((ClonkDebugTarget)context.getDebugTarget()).send(Commands.EXEC + " " + expression, new ILineReceiveListener() {
			@Override
			public LineReceivedResult lineReceived(String line, ClonkDebugTarget target) throws IOException {
				String s = Commands.EVALUATIONRESULT + " " + expression + "=";
				if (line.startsWith(s)){
					final ClonkDebugValue value = new ClonkDebugValue(target, line.substring(s.length()));
					listener.watchEvaluationFinished(new IWatchExpressionResult() {

						@Override
						public boolean hasErrors() {
							return false;
						}

						@Override
						public IValue getValue() {
							return value;
						}

						@Override
						public String getExpressionText() {
							return expression;
						}

						@Override
						public DebugException getException() {
							return null;
						}

						@Override
						public String[] getErrorMessages() {
							return null;
						}
					});
					return LineReceivedResult.ProcessedRemove;
				}
				return LineReceivedResult.NotProcessed;
			}
		});		
	}

}
