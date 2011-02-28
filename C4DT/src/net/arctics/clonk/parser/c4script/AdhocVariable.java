package net.arctics.clonk.parser.c4script;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.ast.ExprElm;

/**
 * A variable that has been implicitly declared by assigning to it, hence making it ad-hoc.
 * @author madeen
 *
 */
public class AdhocVariable extends Variable {
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	/**
	 * Location of an assignment to an ad-hoc variable.<br>
	 * References to declaration and expression are weak ones to not hold on to obsolete objects.
	 * @author madeen
	 *
	 */
	public static class AssignmentLocation implements Serializable {
		private static final long serialVersionUID = 1L;
		/** File the assignment is contained in. */
		public String fileName;
		/** Declaration the assignment is contained in. */
		public Declaration declaration;
		/** Assignment expression. */
		public ExprElm expression;
		public AssignmentLocation(IFile file, Declaration declaration, ExprElm expression) {
			super();
			this.fileName = file.getFullPath().toPortableString().intern();
			this.declaration = declaration;
			this.expression = expression;
		}
	}
	
	private List<AssignmentLocation> assignmentLocations = new LinkedList<AssignmentLocation>();	 
	
	/**
	 * Add an assignment location.
	 * @param file The file the assignment is contained in
	 * @param declaration The declaration
	 * @param assignmentExpression The assignment expression itself.
	 */
	public void addAssignmentLocation(IFile file, Declaration declaration, ExprElm assignmentExpression) {
		assignmentLocations.add(new AssignmentLocation(file, declaration, assignmentExpression));
	}
	
	/**
	 * Return assignment locations that are still deemed valid (declaration still exists, assignment expression still exists)
	 * @return An iterable to iterate over assignment locations that are still valid.
	 */
	public Iterable<AssignmentLocation> assignmentLocations() {
		return assignmentLocations;
	}
	
	public AdhocVariable(ClonkIndex index, String declarationName, Scope scope) {
		super(declarationName, scope);
		this.parentDeclaration = index;
	}
}