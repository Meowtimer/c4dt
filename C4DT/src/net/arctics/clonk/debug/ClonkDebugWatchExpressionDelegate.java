package net.arctics.clonk.debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.debug.ClonkDebugTarget.Commands;
import net.arctics.clonk.debug.ClonkDebugTarget.ILineReceivedListener;
import net.arctics.clonk.debug.ClonkDebugTarget.LineReceivedResult;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;

public class ClonkDebugWatchExpressionDelegate extends Object implements IWatchExpressionDelegate {

	public static final class EvaluationResultListener implements ILineReceivedListener {

		private final IDebugElement context;
		private Map<String, IWatchExpressionListener> listeners = new HashMap<String, IWatchExpressionListener>();

		EvaluationResultListener(IDebugElement context) {
			this.context = context;
		}

		@Override
		public String toString() {
			return "Listener for evaluation results";
		}

		@Override
		public boolean exclusive() {
			return false;
		}

		public void add(String expression, IWatchExpressionListener listener) {
			listeners.put(expression, listener);
		}

		@Override
		public LineReceivedResult lineReceived(String line, ClonkDebugTarget target) throws IOException {
			List<String> toRemove = new LinkedList<String>();
			boolean processed = false;
			for (Entry<String, IWatchExpressionListener> entry : listeners.entrySet()) {
				final String expression = entry.getKey();
				IWatchExpressionListener listener = entry.getValue();
				String s = Commands.EVALUATIONRESULT + " " + expression + "=";
				String errorStart = "LOG ERROR: ";
				if (line.startsWith(errorStart)) {
					System.out.println("error");
					final String[] errors = new String [] {line.substring(errorStart.length())};
					final IValue value = new ClonkDebugValue(target, null);
					listener.watchEvaluationFinished(new IWatchExpressionResult() {

						@Override
						public boolean hasErrors() {
							return true;
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
							return errors;
						}
					});
					processed = true;
					break;
				}
				else if (line.startsWith(s)){
					System.out.println("result line");
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
					toRemove.add(expression);
					processed = true;
					break;
				}
			}
			for (String s : toRemove)
				listeners.remove(s);
			return processed ? LineReceivedResult.ProcessedDontRemove : LineReceivedResult.NotProcessedDontRemove;
		}

		@Override
		public boolean active() {
			return !listeners.isEmpty();
		}
	}

	@Override
	public void evaluateExpression(final String expression, final IDebugElement context, final IWatchExpressionListener listener) {
		ClonkDebugTarget target = (ClonkDebugTarget) context.getDebugTarget();
		target.getEvaluationResultsListener().add(expression, listener);
		target.send(Commands.EXEC + " " + expression);
	}

}
