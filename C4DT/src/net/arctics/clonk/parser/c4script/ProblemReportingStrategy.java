package net.arctics.clonk.parser.c4script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.core.runtime.IProgressMonitor;

import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.resource.ClonkBuilder;

public abstract class ProblemReportingStrategy implements Runnable {
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	public @interface Capabilities {
		static final int TYPING = 1, ISSUES = 2;
		int capabilities() default ISSUES|TYPING;
	}
	
	protected Markers markers = new Markers();
	protected IProgressMonitor progressMonitor;
	public Markers markers() { return markers; }

	@Override
	public void run() { throw new UnsupportedOperationException(); }
	
	public abstract ProblemReportingContext localTypingContext(Script script, int fragmentOffset, ProblemReportingContext chain);
	public void initialize(Markers markers, IProgressMonitor progressMonitor, Script[] scripts) {
		this.markers = markers;
		this.progressMonitor = progressMonitor;
	}
	
	public final int capabilities() {
		Capabilities caps = getClass().getAnnotation(Capabilities.class);
		return caps != null ? caps.capabilities() : 0;
	}
}
