package net.arctics.clonk.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

public class C4DefCoreParser implements IResourceVisitor {
	private static C4DefCoreParser instance;
	private Map<IFile,C4DefCoreData> data;
	
	private C4DefCoreParser() {
		data = new HashMap<IFile, C4DefCoreData>(100);
	}
	
	public static C4DefCoreParser getInstance() {
		if (instance == null) instance = new C4DefCoreParser();
		return instance;
	}
	
	public C4DefCoreData getDefFor(IFile file) {
		return data.get(file);
	}
	
	public boolean visit(IResource resource) throws CoreException {
		if (resource instanceof IFile) {
			if (!data.containsKey(resource)) {
				IFile file = (IFile)resource;
				C4DefCoreData defCore = new C4DefCoreData(file);
				InputStream stream = file.getContents();
				BufferedReader blub = new BufferedReader(new InputStreamReader(stream));
				String line;
				try {
					while((line = blub.readLine()) != null) {
						if (line.matches("^id=([\\w\\d_]+)$")) {
							String[] parts = line.split("=");
							defCore.setId(C4ID.getID(parts[1]));
							break;
						}
					}
					/*
					 * [DefCore]
	id=CVLA
					 */
					data.put(file, defCore);
					stream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return false;
		}
		return true;
	}
	
	public class C4DefCoreData {
		private IFile parent;
		private C4ID id;
		
		public C4DefCoreData(IFile parent) {
			this.parent = parent;
		}

		/**
		 * @return the parent
		 */
		public IFile getParent() {
			return parent;
		}

		/**
		 * @param parent the parent to set
		 */
		public void setParent(IFile parent) {
			this.parent = parent;
		}

		/**
		 * @return the id
		 */
		public C4ID getId() {
			return id;
		}

		/**
		 * @param id the id to set
		 */
		public void setId(C4ID id) {
			this.id = id;
		}
		
	}
	
}
