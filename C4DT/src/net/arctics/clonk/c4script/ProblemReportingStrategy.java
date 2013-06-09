package net.arctics.clonk.c4script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.parser.Markers;

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

	/**
	 * Return a local problem reporter which can be used to perform partial revisits of changed code.
	 * @param script The script the reporter is responsible for.
	 * @param fragmentOffset Fragment to be passed a non-zero value if the {@link ASTNode}s to be revisited were recently parsed from some source fragment.
	 * @param chain Local reporter previously created from this strategy. If non-null the reporters will be internally linked in some way.
	 * @return The reporter
	 */
	public abstract ProblemReporter localReporter(Script script, int fragmentOffset, ProblemReporter chain);

	public ProblemReportingStrategy initialize(Markers markers, IProgressMonitor progressMonitor, Script[] scripts) {
		this.markers = markers;
		this.progressMonitor = progressMonitor;
		return this;
	}

	public final int capabilities() {
		final Capabilities caps = getClass().getAnnotation(Capabilities.class);
		return caps != null ? caps.capabilities() : 0;
	}

	public void setArgs(String args) {}
}
