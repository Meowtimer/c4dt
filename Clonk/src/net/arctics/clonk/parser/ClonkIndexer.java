package net.arctics.clonk.parser;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import net.arctics.clonk.resource.c4group.C4Entry;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.resource.c4group.InvalidDataException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class ClonkIndexer {

	private Map<C4ID, C4Object> objects = new HashMap<C4ID, C4Object>();
	private File clonkDir;
	private boolean clonkDirIndexed = false;
	
	private static final Pattern commentSearch = Pattern.compile("/\\*.*\\*/",Pattern.DOTALL);
	private static final Pattern singleLineCommentSearch = Pattern.compile("//.*");
	private static final Pattern directiveSearch = Pattern.compile("#((?:strict)|(?:include)|(?:appendto))\\s+([\\w\\d_]+)",Pattern.CASE_INSENSITIVE);
	private static final Pattern functionSearch = Pattern.compile("(?:((?:public)|(?:protected)|(?:private)|(?:global))\\s+)?func\\s*([\\w\\d_]+)\\s*\\(([\\w\\d_\\s,]+)\\)",Pattern.CASE_INSENSITIVE);
	private static final Pattern oldFunctionSearch = Pattern.compile("(?:((?:public)|(?:protected)|(?:private)|(?:global))\\s+)?([\\w\\d_]+):",Pattern.CASE_INSENSITIVE); // find old function declarations (property like | basic like)
	private static final Pattern parameterSearch = Pattern.compile("\\s*((?:any)|(?:int)|(?:id)|(?:string)|(?:bool)|(?:array)|(?:object))?\\s+([\\w\\d_]+)\\s*",Pattern.CASE_INSENSITIVE);
	private static final Pattern variableSearch = Pattern.compile("((?:local)|(?:static))\\s+([\\w\\d_])\\s*(?:(?:,)|(?:;)|(?:=))",Pattern.CASE_INSENSITIVE); 
	
	public ClonkIndexer() {
//		objects.put(C4ID.GLOBAL, new C4Object(C4ID.GLOBAL,"<Global>",true));
	}
	
	public void indexClonkDirectory(File dir) {
		clonkDir = dir;
		File[] files = clonkDir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String fileName = pathname.getName();
				if (fileName.endsWith(".c4d") || fileName.endsWith(".c4s") || fileName.endsWith(".c4g")) return true;
//				if (pathname.isDirectory()) return true; // TODO: support directorys
				return false;
			}
		});
		for(File file : files) {
			try {
				C4Group group = C4Group.OpenFile(file);
				indexGroup(group, new Path("/"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (InvalidDataException e) {
				e.printStackTrace();
			} catch (CompilerException e) {
				e.printStackTrace();
			}	
		}
		clonkDirIndexed = true;
	}
	
	/**
	 * Indexes a project folder
	 * @param folder
	 * @param path
	 */
	public void indexFolder(IFolder folder, IPath path, boolean recursively) {
		IFile defCore = null, script = null;
		C4Object parent = null;
		C4ID id = null;
		try {
			IResource[] members = folder.members();
			for(IResource resource : members) {
				if (folder.getName().startsWith("c4d.")) {
					if (resource instanceof IFile) {
						IFile file = ((IFile)resource);
						if (file.getName().equals("DefCore.txt")) {
							defCore = file;
						}
						else if (file.getName().equals("Script.c")) {
							script = file;
						}
					}
				}
				else if (folder.getName().startsWith("c4g.")) {
					if (parent == null) {
						if (!objects.containsKey(folder.getName())) {
							objects.put(C4ID.getSpecialID(folder.getFullPath().toString()),
									new C4Object(C4ID.getSpecialID(folder.getFullPath().toString()),folder.getFullPath().toString().substring(folder.getProject().getName().length() + 2),path.segmentCount() == 0 || path.segment(0).startsWith("c4d.") || path.segment(0).startsWith("c4g.")));
						}
						parent = objects.get(C4ID.getSpecialID(folder.getFullPath().toString()));
					}
					if (resource instanceof IFile) {
						if (resource.getName().endsWith(".c")) {
							script = (IFile)resource;
							// TODO implement powerful parser
							InputStream stream = ((IFile)resource).getContents();
							indexScript(resource.getFullPath(), parent, getStringFromStream(stream));
							stream.close();
						}
					}
				}
				if (resource instanceof IFolder) {
					if (recursively) indexFolder((IFolder)resource, resource.getFullPath(), true); // recursive pre-call -> sub items are indexed before parent
				}
			}
			if (script != null && defCore != null) {
				InputStream stream = defCore.getContents();
				byte[] defCoreBytes = new byte[2048];
				int read = stream.read(defCoreBytes);
				boolean searchId = false;
				// fast DefCore searcher
				for(int i = 0;i < read;i++) {
					char c = (char)defCoreBytes[i];
					if (!searchId && c == '[') {
						if (new String(defCoreBytes,i,9).equalsIgnoreCase("[DefCore]")) {
							i += 9;
							searchId = true;
							c = (char)defCoreBytes[i];
						}
					}
					if (searchId && (c == '\n' || c == '\r')) {
						while(c == '\n' || c == '\r') { // skip new line characters
							c = (char)defCoreBytes[++i];
						}
						if (c == 'i' && (char)defCoreBytes[i+1] == 'd' && (char)defCoreBytes[i+2] == '=') {
							i += 3;
							id = C4ID.getID(new String(defCoreBytes,i,4));
							break;
						}
					}
				}
				stream.close();
				if (id != null) { // if this DefCore.txt has an id or is global c4g
					
					if (!objects.containsKey(id)) {
						parent = new C4Object(id, folder.getFullPath().toString().substring(folder.getProject().getName().length() + 2),path.segmentCount() == 0 || path.segment(0).startsWith("c4d.") || path.segment(0).startsWith("c4g."));
						objects.put(id, parent);
					}
					else {
						// TODO create marker to report error (c4id declared twice)
						return;
//						throw new CompilerException("Object with ID " + id + " is declared twice! (" + objects.get(id).getName() + " and " + group.getName() + ")");
					}
					
//					System.out.println("Indexed object " + parent.getName());
					InputStream scriptStream = script.getContents();
					if (scriptStream.available() == 0) 
						throw new InvalidDataException("Script file empty");
					char[] bytes = new char[scriptStream.available()];
					new InputStreamReader(scriptStream).read(bytes,0,scriptStream.available());
					String contents = new String(bytes);
					scriptStream.close();
					indexScript(path, parent, contents);
				}
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidDataException e) {
			e.printStackTrace();
		}
	}
	
	protected void indexGroup(C4Group group, IPath path) throws InvalidDataException, CompilerException {
		group.open(false);
		List<C4GroupItem> items = group.getChildEntries();
		C4Entry defCore = null, script = null;
		C4Object parent = null;
		C4ID id = null;
		// get important files
		for(int i = 0; i < items.size();i++) {
			if (group.getName().endsWith(".c4d")) {
				if (items.get(i) instanceof C4Entry) {
					if (items.get(i).getName().equals("DefCore.txt")) {
						defCore = (C4Entry)items.get(i);
					}
					if (items.get(i).getName().equals("Script.c")) {
						script = (C4Entry)items.get(i);
					}
				}
			}
			else if (group.getName().endsWith(".c4g")) {
				if (parent == null) {
					if (!objects.containsKey(group.getName())) {
						objects.put(C4ID.getSpecialID(group.getName()),new C4Object(C4ID.getSpecialID(group.getName()),group.getName(),path.segmentCount() == 0 || path.segment(0).startsWith("c4d.") || path.segment(0).startsWith("c4g.")));
					}
					parent = objects.get(C4ID.getSpecialID(group.getName()));
				}
				if (items.get(i) instanceof C4Entry) {
					if (items.get(i).getName().endsWith(".c")) {
						script = (C4Entry)items.get(i);
						if (!script.isCompleted()) script.open(false);
						byte[] bytes = script.getContents();
						String contents = new String(bytes);
						indexScript(path.append(group.getName()), parent, contents);
					}
				}
			}
			if (items.get(i) instanceof C4Group){
				indexGroup((C4Group)items.get(i), path.append(group.getName())); // recursive pre-call -> sub items are indexed before parent
			}
		}
		if (defCore != null && script != null) {
			// index this item
			if (!defCore.isCompleted()) defCore.open(false);
			byte[] defCoreBytes = defCore.getContents();
			boolean searchId = false;
			// fast DefCore searcher
			for(int i = 0; i < defCoreBytes.length;i++) {
				char c = (char)defCoreBytes[i];
				if (!searchId && c == '[') {
					if (new String(defCoreBytes,i,9).equalsIgnoreCase("[DefCore]")) {
						i += 9;
						searchId = true;
						c = (char)defCoreBytes[i];
					}
				}
				if (searchId && (c == '\n' || c == '\r')) {
					while(c == '\n' || c == '\r') { // skip new line characters
						c = (char)defCoreBytes[++i];
					}
					if (c == 'i' && (char)defCoreBytes[i+1] == 'd' && (char)defCoreBytes[i+2] == '=') {
						i += 3;
						id = C4ID.getID(new String(defCoreBytes,i,4));
						break;
					}
				}
			}
			if (id != null) { // if this DefCore.txt has an id or is global c4g
				
				if (!objects.containsKey(id)) {
					parent = new C4Object(id, path.toString().substring(1) + (path.hasTrailingSeparator() ? "" : "/") + group.getName(),path.segmentCount() == 0 || path.segment(0).startsWith("c4d.") || path.segment(0).startsWith("c4g."));
					objects.put(id, parent);
				}
				else {
					// TODO create marker to report error (c4id declared twice)
					return;
//					throw new CompilerException("Object with ID " + id + " is declared twice! (" + objects.get(id).getName() + " and " + group.getName() + ")");
				}
				
//				System.out.println("Indexed object " + parent.getName());
				
				if (!script.isCompleted()) script.open(false);
				byte[] bytes = script.getContents();
				String contents = new String(bytes);
				
				indexScript(path, parent, contents);
			}
		}
	}
	
	protected void indexScript(IPath path, C4Object parent, String contents) {
		List<Comment> comments = new LinkedList<Comment>();
		// find multi line comments /* */
		Matcher m = commentSearch.matcher(contents);
		while(m.find()) {
			comments.add(new Comment(m.start(), m.end()));
		}
		// find single line comments //
		m = singleLineCommentSearch.matcher(contents);
		while(m.find()) {
			comments.add(new Comment(m.start(), m.end()));
		}
		
		// find functions
		m = functionSearch.matcher(contents);
		while(m.find()) {
			if (isCommented(comments, m)) continue;
			if (isObjectCallback(m.group(2))) continue; // TODO: notice object callback overload
			
			C4Function func = new C4Function(m.group(2), parent,m.group(1));
			Matcher pm = parameterSearch.matcher(m.group(3));
			while(pm.find()) {
				C4Variable var = new C4Variable(pm.group(2), C4VariableScope.VAR_VAR);
				if (pm.group(1) != null) var.setType(C4Type.makeType(pm.group(1)));
				func.getParameter().add(var);
			}
//			if (func.getVisibility() == C4FunctionScope.FUNC_GLOBAL)
//				System.out.println(func.getName() + " in " + parent.getName());
			// TODO: function return type recognition
			parent.getDefinedFunctions().add(func);
		}
		
		m = oldFunctionSearch.matcher(contents);
		while(m.find()) {
			if (isCommented(comments, m)) continue;
			if (isObjectCallback(m.group(2))) continue;  // TODO: notice object callback overload
			
			if ("case".equalsIgnoreCase(m.group(2))) continue;
			
			C4Function func = new C4Function(m.group(2), parent, m.group(1));
			// TODO: parameter recognition in old function declarations?
			parent.getDefinedFunctions().add(func);
		}
		
		// find variables
		m = variableSearch.matcher(contents);
		while(m.find()) {
			if (isCommented(comments, m)) continue;
			
			C4Variable var = new C4Variable(m.group(2), m.group(1));
			// TODO: extended variable type recognition
			parent.getDefinedVariables().add(var);
		}
		
		// find directives
		m = directiveSearch.matcher(contents);
		while(m.find()) {
			if (isCommented(comments, m)) continue;
			
			C4Directive directive = new C4Directive(m.group(1),m.group(2));
			parent.getDefinedDirectives().add(directive);
		}
	}
	
	private boolean isCommented(List<Comment> comments, Matcher m) {
		for(Comment comment : comments) { // strip out comments
			if (m.start() > comment.begin && m.start() < comment.end) return true;
		}
		return false;
	}
	
	public boolean isObjectCallback(String function) {
		if (function.startsWith("Context")) return true;
		for(int i = 0; i < BuiltInDefinitions.OBJECT_CALLBACKS.length;i++) {
			if (BuiltInDefinitions.OBJECT_CALLBACKS[i].equalsIgnoreCase(function)) return true;
		}
		return false;
	}

	private String getStringFromStream(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream);
		StringBuilder buf = new StringBuilder();
		char[] buffer = new char[1024];
		int read = 0;
		while((read = reader.read(buffer)) > 0) {
			buf.append(buffer,0,read);
		}
		return buf.toString();
	}
	
	/**
	 * @return the clonkDirCompiled
	 */
	public boolean isClonkDirIndexed() {
		return clonkDirIndexed;
	}

	/**
	 * @return the objects
	 */
	public Map<C4ID, C4Object> getObjects() {
		return objects;
	}

}
