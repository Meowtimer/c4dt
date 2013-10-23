package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.Utilities.defaulting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.Pair;

import org.eclipse.core.runtime.IProgressMonitor;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Abstract base class for problem reporting strategies.
 * A problem reporting strategy represents a wholesale approach on how to find problems
 * on a set of {@link Script}s provided as input.
 * @author madeen
 *
 */
public abstract class ProblemReportingStrategy implements Runnable {

	protected static final Markers NULL_MARKERS = new Markers(false) {
		@Override
		public void enabled(boolean value) {
			if (value)
				System.out.println("Nope");
		}
	};

	/**
	 * Describes the capabilities of a {@link ProblemReportingStrategy}
	 * @author madeen
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	public @interface Capabilities {
		/**
		 * Strategy does typing.
		 */
		static final int TYPING = 1;
		/**
		 * Strategy reports issues apart from typing.
		 */
		static final int ISSUES = 2;
		/**
		 * Return a mask containing capabilities ({@link #TYPING}, {@link #ISSUES})
		 * @return Capabilities mask
		 */
		int capabilities() default ISSUES|TYPING;
	}

	protected Markers markers = new Markers();
	protected IProgressMonitor progressMonitor;
	protected IASTVisitor<ProblemReporter> observer;
	protected Index index;

	/**
	 * {@link Markers} problems are reported against.
	 * @return The markers
	 */
	public Markers markers() { return markers; }

	/**
	 * Index the strategy is operating on.
	 * @return The strategy
	 */
	public Index index() { return index; }
	/**
	 * Shortcut to {@link #index()}{@link Index#engine()}
	 * @return The engine
	 */
	public Engine engine() { return index().engine(); }

	/**
	 * Run on the input provided to {@link #initialize(Markers, IProgressMonitor, Script[])}
	 */
	@Override
	public void run() { throw new NotImplementedException(); }

	public void run2() {}

	/**
	 * Apply the results.
	 */
	public void apply() {}

	/**
	 * Initialize this strategy to run on all functions of a set of scripts.
	 * @param markers {@link Markers} to use for creating markers
	 * @param progressMonitor Progress monitor to report progress against
	 * @param scripts Set of scripts to run on.
	 * @return Return the call target
	 */
	public ProblemReportingStrategy initialize(Markers markers, IProgressMonitor progressMonitor, Script[] scripts) {
		return initialize(markers, progressMonitor);
	}

	private ProblemReportingStrategy initialize(Markers markers, IProgressMonitor progressMonitor) {
		this.markers = defaulting(markers, NULL_MARKERS);
		this.progressMonitor = progressMonitor;
		return this;
	}

	/**
	 * Initialize this strategy to run on certain functions only.
	 * @param markers {@link Markers} to use for creating markers
	 * @param progressMonitor Progress monitor to report progress against
	 * @param functions {@link Script}/{@link Function} pairs to run on
	 * @return Return the call target
	 */
	public ProblemReportingStrategy initialize(Markers markers, IProgressMonitor progressMonitor, Collection<Pair<Script, Function>> functions) {
		return initialize(markers, progressMonitor);
	}

	/**
	 * Return capabilities of this strategy.
	 * @return The capabilities
	 */
	public final int capabilities() {
		final Capabilities caps = getClass().getAnnotation(Capabilities.class);
		return caps != null ? caps.capabilities() : 0;
	}

	/**
	 * Set an observer which is notified when the strategy visits any {@link ASTNode}.
	 * @param observer The observer
	 */
	public void setObserver(IASTVisitor<ProblemReporter> observer) { this.observer = observer; }

	/**
	 * Perform initial configuration before this strategy can be further initialized using {@link #initialize(Markers, IProgressMonitor, Collection)}/{@link #initialize(Markers, IProgressMonitor, Script[])}
	 * @param index The {@link Index} to run on
	 * @param args Custom arguments string
	 * @return Return call target.
	 */
	public ProblemReportingStrategy configure(Index index, String args) {
		this.index = index;
		findProjectName();
		return this;
	}

	/**
	 * Capture markers on functions the strategy is configured to operate on.
	 */
	public void captureMarkers() {}

	public void steer(Runnable runnable) {
		runnable.run();
	}

	protected String projectName;
	protected void findProjectName() {
		String name = "UnknownProject";
		if (index != null && index.nature() != null)
			name = index.nature().getProject().getName();
		projectName = name.intern();
	}

}
