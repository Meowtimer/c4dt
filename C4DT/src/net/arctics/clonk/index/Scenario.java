package net.arctics.clonk.index;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

/**
 * A scenario. 
 * @author madeen
 *
 */
public class Scenario extends Definition {
	
	public static final String PROPLIST_NAME = "Scenario";
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private Variable scenarioPropList = createScenarioProplist();

	private Variable createScenarioProplist() {
		if (scenarioPropList == null) {
			ProplistDeclaration type = ProplistDeclaration.newAdHocDeclaration();
			type.setLocation(SourceLocation.ZERO);
			type.setParentDeclaration(this);
			scenarioPropList = new Variable(PROPLIST_NAME, type);
			scenarioPropList.setParentDeclaration(this);
			scenarioPropList.setScope(Scope.STATIC);
		}
		return scenarioPropList;
	}
	
	public Variable propList() {
		return scenarioPropList;
	}
	
	@Override
	public void postLoad(Declaration parent, Index root) {
		createScenarioProplist();
		super.postLoad(parent, root);
	}

	public Scenario(Index index, String name, IContainer container) {
		super(index, ID.get(container.getName()), name, container);
	}
	
	public static Scenario get(IContainer folder) {
		Definition obj = definitionCorrespondingToFolder(folder);
		return obj instanceof Scenario ? (Scenario)obj : null;
	}
	
	public static Scenario getAscending(IResource res) {
		if (res == null)
			return null;
		for (IContainer c = res instanceof IContainer ? (IContainer)res : res.getParent(); c != null; c = c.getParent()) {
			Scenario s = get(c);
			if (s != null)
				return s;
		}
		return null;
	}
	
	public static Scenario nearestScenario(IResource resource) {
		Scenario scenario;
		for (scenario = null; scenario == null && resource != null; resource = resource.getParent())
			if (resource instanceof IContainer)
				scenario = get((IContainer)resource);
		return scenario;
	}

}
