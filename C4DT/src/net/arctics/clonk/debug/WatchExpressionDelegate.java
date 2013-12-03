package net.arctics.clonk.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.debug.Target.Commands;
import net.arctics.clonk.debug.Target.ILineReceivedListener;
import net.arctics.clonk.debug.Target.LineReceivedResult;
import net.arctics.clonk.util.ICreate;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;

/**
 * Delegate handling C4Script watch expressions.
 * @author madeen
 *
 */
public class WatchExpressionDelegate extends Object implements IWatchExpressionDelegate {

	/**
	 * Listener looking out for evaluation request results sent by the engine.
	 * @author madeen
	 *
	 */
	public static final class EvaluationResultListener implements ILineReceivedListener {

		/**
		 * Map mapping expression string to corresponding watch expression listener.
		 */
		private final Map<String, IWatchExpressionListener> listeners = new HashMap<String, IWatchExpressionListener>();

		EvaluationResultListener(final IDebugElement context) {}

		@Override
		public String toString() {
			return "Listener for evaluation results"; //$NON-NLS-1$
		}

		@Override
		public boolean exclusive() {
			return false;
		}

		/**
		 * Add a new watch expression listener to this meta listener.
		 * @param expression The expression for which to add the listener
		 * @param listener the watch expression listener
		 */
		public void add(final String expression, final IWatchExpressionListener listener) {
			synchronized (listeners) {
				listeners.put(expression, listener);
			}
		}

		/**
		 * Dispatch received line to the appropriate watch expression listener.
		 */
		@Override
		public LineReceivedResult lineReceived(final String line, final Target target) throws IOException {
			String toRemove = null;
			boolean processed = false;
			List<Entry<String, IWatchExpressionListener>> cpy;
			synchronized (listeners) {
				cpy = new ArrayList<>(listeners.entrySet());
			}
			for (final Entry<String, IWatchExpressionListener> entry : cpy) {
				final String expression = entry.getKey();
				final IWatchExpressionListener listener = entry.getValue();
				final String s = Commands.EVALUATIONRESULT + " " + expression + "="; //$NON-NLS-1$ //$NON-NLS-2$
				if (line.startsWith(s)){
					final Value value = new Value(target, line.substring(s.length()));
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
			boolean noneLeft;
			synchronized (listeners) {
				if (toRemove != null)
					listeners.remove(toRemove);
				noneLeft = listeners.size() == 0;
			}
			return processed
				? noneLeft
					? LineReceivedResult.ProcessedRemove
					: LineReceivedResult.ProcessedDontRemove
				: LineReceivedResult.NotProcessedDontRemove;
		}

		/**
		 * This listener is only active if any watch expression listeners have been added.
		 */
		@Override
		public boolean active() {
			synchronized (listeners) {
				return !listeners.isEmpty();
			}
		}
	}

	/**
	 * Send exec request to the running engine and put a watch expression listener in place informing the Debug system about the result.
	 *
	 */
	@Override
	public void evaluateExpression(final String expression, final IDebugElement context, final IWatchExpressionListener listener) {
		final Target target = (Target) context.getDebugTarget();
		if (target.isDisconnected())
			listener.watchEvaluationFinished(new IWatchExpressionResult() {
				@Override
				public boolean hasErrors() { return false; }
				@Override
				public IValue getValue() { return null; }
				@Override
				public String getExpressionText() { return expression; }
				@Override
				public DebugException getException() { return null; }
				@Override
				public String[] getErrorMessages() { return null; }
			});
		else {
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

}
