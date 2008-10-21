package net.arctics.clonk.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
//	private static C4DefCoreParser instance;
//	private Map<IFile,C4DefCoreData> data;
	
	private IFile defCoreFile;
	private InputStream stream;
	private C4ID objectID = null;
	private String name;
	private Map<String,String> defCoreProperties = new HashMap<String, String>(7);
	private Map<String,String> physicalProperties = new HashMap<String, String>(3);
	
	public C4DefCoreWrapper(IFile file) {
		defCoreFile = file;
//		data = new HashMap<IFile, C4DefCoreData>(100);
	}
	
	public C4DefCoreWrapper(InputStream stream) {
		this.stream = stream;
		defCoreFile = null;
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
			String section = reader.nextSection();
			if (section == null) {
				if (defCoreFile != null) {
					String problem = "This file does not have a [DefCore] section!";
					IMarker marker = defCoreFile.createMarker(IMarker.PROBLEM);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.TRANSIENT, false);
					marker.setAttribute(IMarker.MESSAGE, problem);
				}
				else {
//					TODO warnings for external object problems?
				}
				return;
			}
			if (section.equals("DefCore")) {
				String[] readData;
				do {
					// fetch next key=value row
					readData = reader.nextEntry();
					if (readData == null) { // EOF reached
						break;
					}
					if (readData[0].equals("id"))
						objectID = C4ID.getID(readData[1]);
					else if (readData[0].equals("Name"))
						name = readData[1];
				} while(objectID == null || name == null);
			}
			stream.close();
		} catch (CoreException e) {
//			e.printStackTrace();
			throw new CompilerException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
