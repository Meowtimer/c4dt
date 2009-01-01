package net.arctics.clonk.parser.defcore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.Pair;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.defcore.IniReader.IniEntry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Its planned to parse the complete file and add them to the persistent index data storage.
 * Currently, only parsing does work and it doesn't save anything.
 * @author ZokRadonh
 *
 */
public class C4DefCoreWrapper {
	
	
	private String[] builtInDefCoreOptions = new String[] { };
	
//	private static C4DefCoreParser instance;
//	private Map<IFile,C4DefCoreData> data;
	
	private IFile defCoreFile;
	private InputStream stream;
	private C4ID objectID = null;
	private String name;
	
//	private Map<String,DefCoreOption> defCoreProperties = new HashMap<String, DefCoreOption>();
//	private Map<String,DefCoreOption> physicalProperties = new HashMap<String, DefCoreOption>();
	
	private final List<DefCoreOption> defCoreOptions;
	private final List<DefCoreOption> physicalOptions;
	
	public C4DefCoreWrapper(IFile file) {
		defCoreFile = file;
		defCoreOptions = DefCoreOption.createNewDefCoreList();
		physicalOptions = DefCoreOption.createNewPhysicalList();
//		data = new HashMap<IFile, C4DefCoreData>(100);
	}
	
	public C4DefCoreWrapper(InputStream stream) {
		this.stream = stream;
		defCoreFile = null;
		defCoreOptions = DefCoreOption.createNewDefCoreList();
		physicalOptions = DefCoreOption.createNewPhysicalList();
	}
	
//	public static C4DefCoreParser getInstance() {
//		if (instance == null) instance = new C4DefCoreParser();
//		return instance;
//	}
	
//	public C4DefCoreData getDefFor(IFile file) {
//		return data.get(file);
//	}
	
	public void parse() throws CompilerException {
		try {
			if (defCoreFile != null) {
				defCoreFile.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
				stream = defCoreFile.getContents();
			}
			IniReader reader = new IniReader(stream);
			String section = null;
			while ((section = reader.nextSection()) != null) {
				if (section.equals("DefCore")) {
					IniEntry readData = null;
					while ((readData = reader.nextEntry()) != null) {
						if (readData.getKey().equalsIgnoreCase("id"))
							objectID = C4ID.getID(readData.getValue().trim());
						else if (readData.getKey().equalsIgnoreCase("Name"))
							name = readData.getValue().trim();
						
						DefCoreOption option = getDefCoreOption(readData.getKey());
						if (option == null) {
							createMarker(IMarker.SEVERITY_WARNING, readData.getStartPos(), readData.getStartPos() + readData.getKey().length(),"Unknown option '" + readData.getKey() + "'");
//							System.out.println("unknown option '" + readData[0] + "'");
						}
						else {
							try {
								option.setInput(readData.getValue());
							} catch (DefCoreParserException e) {
								createMarker(e.getSeverity(),readData.getStartPos() + readData.getKey().length() + 1,readData.getStartPos() + readData.getKey().length() + 1 + readData.getValue().length(),e.getMessage());
							}
						}
//						defCoreProperties.put(readData[0].trim(), getTypedValue(readData[1], type));
					}
				}
				else if (section.equals("Physical")) {
					IniEntry readData = null;
					while((readData = reader.nextEntry()) != null) {
//						physicalProperties.put(readData[0].trim(), readData[1].trim());
					}
				}
			}
			if (defCoreFile != null && (objectID == null || name == null)) {
				String problem = "This file does not have a [DefCore] section or does not specify ID and/or name";
				IMarker marker = defCoreFile.createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(IMarker.TRANSIENT, false);
				marker.setAttribute(IMarker.MESSAGE, problem);
			}
			else {
//				TODO warnings for external object problems?
			}
			stream.close();
		} catch (CoreException e) {
//			e.printStackTrace();
			throw new CompilerException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private IMarker createMarker(int severity, int start, int end, String message) {
		if (defCoreFile == null) return null; // TODO external object problems?
		try {
			IMarker marker = defCoreFile.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.TRANSIENT, false);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.CHAR_START, start);
			marker.setAttribute(IMarker.CHAR_END, end);
			return marker;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Searches the option for given name
	 * @param name the name of the option (e.g. "BurnTo")
	 * @return The found option or <tt>null</tt> if not found
	 */
	public DefCoreOption getDefCoreOption(String name) {
		ListIterator<DefCoreOption> it = defCoreOptions.listIterator();
		while(it.hasNext()) {
			if (it.next().getName().equalsIgnoreCase(name)) return it.previous();
		}
		return null;
	}
	
	/**
	 * Searches the option for given name
	 * @param name the name of the option (e.g. "Throw")
	 * @return The found option or <tt>null</tt> if not found
	 */
	public DefCoreOption getPhysicalOption(String name) {
		ListIterator<DefCoreOption> it = physicalOptions.listIterator();
		while(it.hasNext()) {
			if (it.next().getName().equalsIgnoreCase(name)) return it.previous();
		}
		return null;
	}
	
	private Object getTypedValue(String input, DefCoreOption option) {
		String name = option.getName();
		Class<?> preferedType = option.getClass();
		try { // try integer types
			int value = Integer.parseInt(input);
			if (preferedType == IntegerArray.class) {
				IntegerArray array = new IntegerArray(name);
				array.setIntegers(new int[] { value });
				return array;
			}
			if (preferedType == UnsignedInteger.class) {
				if (value >= 0) return new UnsignedInteger(name, value);
				else return new SignedInteger(name, value);
			}
			return new SignedInteger(name, value);
		}
		catch (NumberFormatException e) {
		}
		
		if (preferedType == C4ID.class) { // try C4ID
			if (input.length() == 4) {
				return C4ID.getID(input);
			}
		}
		
		tryComp: { // try components array
			IDArray components;
			String[] comps = input.split(";");
			components = new IDArray(name);
			for(String component : comps) {
				String[] pair = component.split("=");
				if (pair.length != 2) break tryComp;
				components.add(C4ID.getID(pair[0]),Integer.parseInt(pair[1]));
			}
			return components;
		}
		
		try { // try simple array
			String[] parts = input.split(",");
			if (parts.length > 1) {
				int[] array = new int[parts.length];
				for(int i = 0; i < parts.length;i++) {
					array[i] = Integer.parseInt(parts[i]);
				}
				IntegerArray iArray = new IntegerArray(name);
				iArray.setIntegers(array);
				return iArray;
			}
		}
		catch (NumberFormatException e) {
		}
		
		if (preferedType == C4Variable[].class) {
			String[] parts = input.split("|");
			if (parts.length > 1) {
				CategoriesArray array = new CategoriesArray(name);
				for(int i = 0; i < parts.length;i++) {
					array.add(ClonkCore.ENGINE_OBJECT.findVariable(parts[i]));
				}
				return array;
			}
			else if (preferedType == CategoriesArray.class) {
				CategoriesArray array = new CategoriesArray(name);
				array.add(ClonkCore.ENGINE_OBJECT.findVariable(parts[0]));
				return array;
			}
		}
		
		return new DefCoreString(input);
	}
	
	/**
	 * @return the objectID
	 */
	public C4ID getObjectID() {
		return objectID;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		if (name == null)
			name = "<No name supplied>";
		return name;
	}
	
//	public void update(IResource resource) throws CoreException {
//		if (resource instanceof IFile) {
//			IFile file = (IFile)resource;
//			C4DefCoreData defCore = new C4DefCoreData(file);
//			InputStream stream = file.getContents();
//			BufferedReader blub = new BufferedReader(new InputStreamReader(stream));
//			String line;
//			try {
//				while((line = blub.readLine()) != null) {
//					if (line.matches("^id=([\\w\\d_]+)$")) {
//						String[] parts = line.split("=");
//						defCore.setId(C4ID.getID(parts[1]));
//						break;
//					}
//				}
//				/*
//				 * [DefCore]
//	id=CVLA
//				 */
//				data.put(file, defCore);
//				stream.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
	
//	public boolean visit(IResource resource) throws CoreException {
//		if (resource instanceof IFile) {
//			if (!data.containsKey(resource)) {
//				update(resource);
//			}
//			return false;
//		}
//		return true;
//	}
	
//	public class C4DefCoreData {
//		private IFile parent;
//		private C4ID id;
//		
//		public C4DefCoreData(IFile parent) {
//			this.parent = parent;
//		}
//
//		/**
//		 * @return the parent
//		 */
//		public IFile getParent() {
//			return parent;
//		}
//
//		/**
//		 * @param parent the parent to set
//		 */
//		public void setParent(IFile parent) {
//			this.parent = parent;
//		}
//
//		/**
//		 * @return the id
//		 */
//		public C4ID getId() {
//			return id;
//		}
//
//		/**
//		 * @param id the id to set
//		 */
//		public void setId(C4ID id) {
//			this.id = id;
//		}
//		
//	}
	
}
