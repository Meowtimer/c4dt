package net.arctics.clonk.c4script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.Pair;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Abstract base class for problem reporting strategies.
 * A problem reporting strategy represents a wholesale approach on how to find problems
 * on a set of {@link Script}s provided as input.
 * @author madeen
 *
 */
public abstract class ProblemReportingStrategy implements Runnable {

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	public @interface Capabilities {
		static final int TYPING = 1, ISSUES = 2;
		int capabilities() default ISSUES|TYPING;
	}

	protected Markers markers = new Markers();
	protected IProgressMonitor progressMonitor;
	protected IASTVisitor<ProblemReporter> observer;

	/**
	 * {@link Markers} problems are reported against.
	 * @return The markers
	 */
	public Markers markers() { return markers; }

	/**
	 * Run on the input provided to {@link #initialize(Markers, IProgressMonitor, Script[])}
	 */
	@Override
	public void run() { throw new UnsupportedOperationException(); }

	public ProblemReportingStrategy initialize(Markers markers, IProgressMonitor progressMonitor, Script[] scripts) {
		return initialize(markers, progressMonitor);
	}

	private ProblemReportingStrategy initialize(Markers markers, IProgressMonitor progressMonitor) {
		this.markers = markers != null ? markers : new Markers(false);
		this.progressMonitor = progressMonitor;
		return this;
	}

	public ProblemReportingStrategy initialize(Markers markers, IProgressMonitor progressMonitor, Collection<Pair<Script, Function>> functions) {
		return initialize(markers, progressMonitor);
	}

	public final int capabilities() {
		final Capabilities caps = getClass().getAnnotation(Capabilities.class);
		return caps != null ? caps.capabilities() : 0;
	}

	public void setObserver(IASTVisitor<ProblemReporter> observer) { this.observer = observer; }

	public void setArgs(String args) {}

	public abstract ProblemReporter localReporter(Script script, int fragmentOffset);
}
