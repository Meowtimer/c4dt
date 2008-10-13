package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

/**
 * This class saves all data of a C4Object in a serializable form and is able to reconstruct the old object
 * @author ZokRadonh
 *
 */
public class C4ObjectSerializer implements Serializable {
	
	private static final long serialVersionUID = 1495127962718254364L;

	protected String id;
	protected String fullName;
	protected String path;
	private boolean projectMember;
	
	private List<C4Function> definedFunctions;
	private List<C4Variable> definedVariables;
	private List<C4Directive> definedDirectives;
	
	protected C4ObjectSerializer() {	
	}
	
	public static C4ObjectSerializer fromC4Object(C4Object obj) {
		C4ObjectSerializer serial = new C4ObjectSerializer();
		serial.id = obj.getId().getName();
		serial.fullName = obj.getName();
		serial.projectMember = obj instanceof C4ObjectIntern;
		if (serial.projectMember) {
			serial.path = ((C4ObjectIntern)obj).getObjectFolder().getProjectRelativePath().toPortableString();
		}
		else {
			serial.path = ((C4ObjectExtern)obj).getPath().toPortableString();
		}
		serial.definedDirectives = obj.definedDirectives;
		serial.definedVariables = obj.definedVariables;
		serial.definedFunctions = obj.definedFunctions;
		
		return serial;
	}
	
	/**
	 * Reconstructs the old C4Object and assigns it to the referenced IFolder
	 * @return the reconstructed C4Object or <code>null</code> if referenced IFolder does not exist
	 */
	public C4Object reconstruct() {
		C4Object obj;
		// create object
		if (projectMember) {
			// associate IFolder object
			IContainer container = ResourcesPlugin.getWorkspace().getRoot().getFolder(Path.fromPortableString(path));
			obj = new C4ObjectIntern(C4ID.getID(id),fullName, container);
		}
		else {
			obj = new C4ObjectExtern(C4ID.getID(id),fullName, Path.fromPortableString(path));
		}
		// set indexed data
		obj.definedDirectives = definedDirectives;
		obj.definedFunctions = definedFunctions;
		obj.definedVariables = definedVariables;
//		obj.setDefinedDirectives(definedDirectives);
//		obj.setDefinedFunctions(definedFunctions);
//		obj.setDefinedVariables(definedVariables);
		return obj;
	}
}

