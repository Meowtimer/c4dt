package net.arctics.clonk.debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.debug.ClonkDebugTarget.Commands;
import net.arctics.clonk.debug.ClonkDebugTarget.ILineReceivedListener;
import net.arctics.clonk.debug.ClonkDebugTarget.LineReceivedResult;
import net.arctics.clonk.util.ICreate;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;

public class ClonkDebugWatchExpressionDelegate extends Object implements IWatchExpressionDelegate {

	public static final class EvaluationResultListener implements ILineReceivedListener {

		private Map<String, IWatchExpressionListener> listeners = new HashMap<String, IWatchExpressionListener>();

		EvaluationResultListener(IDebugElement context) {
		}

		@Override
		public String toString() {
			return "Listener for evaluation results"; //$NON-NLS-1$
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
			String toRemove = null;
			boolean processed = false;
			for (Entry<String, IWatchExpressionListener> entry : listeners.entrySet()) {
				final String expression = entry.getKey();
				IWatchExpressionListener listener = entry.getValue();
				String s = Commands.EVALUATIONRESULT + " " + expression + "="; //$NON-NLS-1$ //$NON-NLS-2$
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
					toRemove = expression;
					processed = true;
					break;
				}
			}
			if (toRemove != null)
				listeners.remove(toRemove);
			return processed
				? listeners.size() == 0
					? LineReceivedResult.ProcessedRemove
					: LineReceivedResult.ProcessedDontRemove
				: LineReceivedResult.NotProcessedDontRemove;
		}

		@Override
		public boolean active() {
			return !listeners.isEmpty();
		}
	}

	@Override
	public void evaluateExpression(final String expression, final IDebugElement context, final IWatchExpressionListener listener) {
		ClonkDebugTarget target = (ClonkDebugTarget) context.getDebugTarget();
		target.requestLineReceivedListener(new ICreate<EvaluationResultListener>() {
			@Override
			public Class<EvaluationResultListener> cls() {
				return EvaluationResultListener.class;
			}
			@Override
			public EvaluationResultListener create() {
				return new EvaluationResultListener(context);
			}
		}).add(expression, listener);
		target.send(Commands.EXEC + " " + expression); //$NON-NLS-1$
	}

}
