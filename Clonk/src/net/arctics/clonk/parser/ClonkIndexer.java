package net.arctics.clonk.parser;

/**
 * 
 * @deprecated 
 */
public class ClonkIndexer {
//
//	private Map<C4ID, C4Object> objects = new HashMap<C4ID, C4Object>();
//	private File clonkDir;
//	private boolean clonkDirIndexed = false;
//	
//	private static final Pattern commentSearch = Pattern.compile("/\\*.*?\\*/",Pattern.DOTALL);
//	private static final Pattern singleLineCommentSearch = Pattern.compile("//.*");
//	private static final Pattern directiveSearch = Pattern.compile("#((?:strict)|(?:include)|(?:appendto))\\s+([\\w\\d_]+)",Pattern.CASE_INSENSITIVE);
//	private static final Pattern functionSearch = Pattern.compile("(?:((?:public)|(?:protected)|(?:private)|(?:global))\\s+)?func\\s+([\\w\\d_]+)\\s*\\(([\\w\\d_\\s,]*)\\)",Pattern.CASE_INSENSITIVE);
//	private static final Pattern oldFunctionSearch = Pattern.compile("(?:((?:public)|(?:protected)|(?:private)|(?:global))\\s+)([\\w\\d_]+):[^:]",Pattern.CASE_INSENSITIVE); // find old function declarations (property like | basic like)
//	private static final Pattern parameterSearch = Pattern.compile("\\s*((?:any)|(?:int)|(?:id)|(?:string)|(?:bool)|(?:array)|(?:object)|(?:dword))?\\s*([\\w\\d_]+)\\s*",Pattern.CASE_INSENSITIVE);
//	//private static final Pattern variableSearch = Pattern.compile("((?:local)|(static const)|(?:static))\\s+([\\w\\d_, ]+)(?(2)\\s*=\\s*([^;]+))\\s*;",Pattern.CASE_INSENSITIVE); // 1=scope 3=names [5=value|"static const" only]
//<<<<<<< .mine
//	// Java: immer wieder entt�uschend: conditional patterns are not supported
//=======
//	// Java: immer wieder enttäuschend: conditional patterns are not supported
//>>>>>>> .r79
//	private static final Pattern variableSearch = Pattern.compile("((?:local)|(static const)|(?:static))\\s+([\\w\\d_, ]+)(?:\\s*=\\s*([^;]+))?\\s*;",Pattern.CASE_INSENSITIVE); // 1=scope 3=names [4=value|"static const" only]
//	
//	public ClonkIndexer() {
////		objects.put(C4ID.GLOBAL, new C4Object(C4ID.GLOBAL,"<Global>",true));
//	}
//	
//	public C4Object getObjectForScript(IFile script) {
//		for (C4Object obj : objects.values()) {
//			if (obj.getScript() != null && obj.getScript().equals(script))
//				return obj;
//		}
//		return null;
//	}
//	
//	public void indexClonkDirectory(File dir) {
//		clonkDir = dir;
//		File[] files = clonkDir.listFiles(new FileFilter() {
//			public boolean accept(File pathname) {
//				String fileName = pathname.getName();
//				if (fileName.endsWith(".c4d") || fileName.endsWith(".c4s") || fileName.endsWith(".c4g")) return true;
////				if (pathname.isDirectory()) return true; // TODO: support directories
//				return false;
//			}
//		});
//		for(File file : files) {
//			try {
//				C4Group group = C4Group.OpenFile(file);
//				indexGroup(group, new Path("/"));
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			} catch (InvalidDataException e) {
//				e.printStackTrace();
//			} catch (CompilerException e) {
//				e.printStackTrace();
//			}	
//		}
//		clonkDirIndexed = true;
//	}
//	
//
//<<<<<<< .mine
//	public static C4GroupType groupTypeFromFolderName(String name) {
//		C4GroupType result = C4Group.extensionToGroupTypeMap.get(name.substring(name.lastIndexOf(".")+1));
//		if (result != null)
//			return result;
//		return C4GroupType.OtherGroup;
//	}
//=======
//	
////	/**
////	 * Indexes a project folder
////	 * @param folder
////	 * @param path
////	 * @deprecated this method is replaced by the complete C4ScriptParser
////	 */
////	public void indexFolder(IFolder folder, IPath path, boolean recursively) {
////		IFile defCore = null, script = null;
////		C4Object parent = null;
////		C4ID id = null;
////		C4GroupType groupType = groupTypeFromFolderName(folder.getName());
////		try {
////			IResource[] members = folder.members();
////			for(IResource resource : members) {
////				if (groupType == C4GroupType.DefinitionGroup || groupType == C4GroupType.ScenarioGroup) {
////					if (resource instanceof IFile) {
////						IFile file = ((IFile)resource);
////						if (file.getName().equals("DefCore.txt")) {
////							defCore = file;
////						}
////						else if (file.getName().equals("Script.c")) {
////							script = file;
////						}
////					}
////				}
////				else if (groupType == C4GroupType.ResourceGroup) {
////					if (parent == null) {
////						parent = createObjectFromFolder(folder.getFullPath().toString(), path, null);
////					}
////					if (resource instanceof IFile) {
////						if (resource.getName().endsWith(".c")) {
////							script = (IFile)resource;
////							// TODO implement powerful parser aka insert usage of powerful parser here when it's ready for prime time
////							InputStream stream = ((IFile)resource).getContents();
////							indexScript(resource.getFullPath(), parent, getStringFromStream(stream));
////							stream.close();
////						}
////					}
////				}
////				if (resource instanceof IFolder) {
////					if (recursively) indexFolder((IFolder)resource, resource.getFullPath(), true); // recursive pre-call -> sub items are indexed before parent
////				}
////			}
////			if (script != null && defCore != null) {
////				InputStream stream = defCore.getContents();
////				byte[] defCoreBytes = new byte[2048];
////				int read = stream.read(defCoreBytes);
////				int idStartOffset = 0;
////				boolean searchId = false;
////				// fast DefCore searcher
////				for(int i = 0;i < read;i++) {
////					char c = (char)defCoreBytes[i];
////					if (!searchId && c == '[') {
////						if (new String(defCoreBytes,i,9).equalsIgnoreCase("[DefCore]")) {
////							i += 9;
////							searchId = true;
////							c = (char)defCoreBytes[i];
////						}
////					}
////					if (searchId && (c == '\n' || c == '\r')) {
////						while(c == '\n' || c == '\r') { // skip new line characters
////							c = (char)defCoreBytes[++i];
////						}
////						if (c == 'i' && (char)defCoreBytes[i+1] == 'd' && (char)defCoreBytes[i+2] == '=') {
////							i += 3;
////							idStartOffset = i;
////							id = C4ID.getID(new String(defCoreBytes,i,4));
////							break;
////						}
////					}
////				}
////				stream.close();
////				if (id != null) { // if this DefCore.txt has an id or is global c4g
////					if (!objects.containsKey(id)) {
////						parent = createObjectFromFolder(folder.getFullPath().toString(), path, id);
////						parent.setScript(script);
////					}
////					else {
////						parent = objects.get(id);
////						if (parent.getScript().equals(script)) {
////							parent.getDefinedVariables().clear();
////							parent.getDefinedFunctions().clear();
////							parent.getDefinedDirectives().clear();
////						}
////						else {
////							defCore.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
////							IMarker marker = defCore.createMarker(IMarker.PROBLEM);
////							marker.setAttribute(IMarker.TRANSIENT, true);
////							marker.setAttribute(IMarker.CHAR_START, idStartOffset);
////							marker.setAttribute(IMarker.CHAR_END, idStartOffset + 4);
////							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
////							marker.setAttribute(IMarker.MESSAGE, "This object redefines " + id.getName() + " previously defined in '" + parent.getName() + "'");
////							return;
////						}
//////						throw new CompilerException("Object with ID " + id + " is declared twice! (" + objects.get(id).getName() + " and " + group.getName() + ")");
////					}
////					
//////					System.out.println("Indexed object " + parent.getName());
////					InputStream scriptStream = script.getContents();
////					if (scriptStream.available() == 0) 
////						throw new InvalidDataException("Script file empty");
////					char[] bytes = new char[scriptStream.available()];
////					new InputStreamReader(scriptStream).read(bytes,0,scriptStream.available());
////					String contents = new String(bytes);
////					scriptStream.close();
////					indexScript(path, parent, contents);
////				}
////			}
////		}
////		catch (CoreException e) {
////			e.printStackTrace();
////		}
////		catch (IOException e) {
////			e.printStackTrace();
////		} catch (InvalidDataException e) {
////			e.printStackTrace();
////		}
////	}
//>>>>>>> .r79
//
//	private C4Object createObjectFromFolder(String folderPath, IPath path, C4ID id) {
//		if (id == null)
//			id = C4ID.getSpecialID(folderPath.toString());
//		C4Object newObject = new C4Object(
//				id,
//				folderPath
//				// TODO: folderPath.substring(folder.getProject().getName().length() + 2),
//				// das brauch keine sau:
////				path.segmentCount() == 0 || c4FilenameExtensionIs(path.segment(0), "c4d") ||  c4FilenameExtensionIs(path.segment(0), "c4g")
//				
//		); 
//		objects.put(id, newObject);
//		return newObject;
//	}
//	
//	protected void indexGroup(C4Group group, IPath path) throws InvalidDataException, CompilerException {
//		group.open(false);
//		List<C4GroupItem> items = group.getChildEntries();
//		C4Entry defCore = null, script = null;
////		C4Object parent = null;
//		C4ID id = null;
//		C4GroupType groupType = group.getGroupType();
//		// get important files
//		for(int i = 0; i < items.size();i++) {
//			if (groupType == C4GroupType.DefinitionGroup) {
//				if (items.get(i) instanceof C4Entry) {
//					if (items.get(i).getName().equals("DefCore.txt")) {
//						defCore = (C4Entry)items.get(i);
//					}
//					if (items.get(i).getName().equals("Script.c")) {
//						script = (C4Entry)items.get(i);
//					}
//				}
//			}
//			else if (groupType == C4GroupType.ResourceGroup) {
//				if (parent == null) {
//					parent = createObjectFromFolder(group.getName(), path, null);
//				}
//				if (items.get(i) instanceof C4Entry) {
//					if (items.get(i).getName().endsWith(".c")) {
//						script = (C4Entry)items.get(i);
//						if (!script.isCompleted()) script.open(false);
//						byte[] bytes = script.getContents();
//						String contents = new String(bytes);
//						indexScript(path.append(group.getName()), parent, contents);
//					}
//				}
//			}
//			if (items.get(i) instanceof C4Group){
//				indexGroup((C4Group)items.get(i), path.append(group.getName())); // recursive pre-call -> sub items are indexed before parent
//			}
//		}
//		if (defCore != null && script != null) {
//			// index this item
//			if (!defCore.isCompleted()) defCore.open(false);
//			byte[] defCoreBytes = defCore.getContents();
//			boolean searchId = false;
//			// fast DefCore searcher
//			for(int i = 0; i < defCoreBytes.length;i++) {
//				char c = (char)defCoreBytes[i];
//				if (!searchId && c == '[') {
//					if (new String(defCoreBytes,i,9).equalsIgnoreCase("[DefCore]")) {
//						i += 9;
//						searchId = true;
//						c = (char)defCoreBytes[i];
//					}
//				}
//				if (searchId && (c == '\n' || c == '\r')) {
//					while(c == '\n' || c == '\r') { // skip new line characters
//						c = (char)defCoreBytes[++i];
//					}
//					if (c == 'i' && (char)defCoreBytes[i+1] == 'd' && (char)defCoreBytes[i+2] == '=') {
//						i += 3;
//						id = C4ID.getID(new String(defCoreBytes,i,4));
//						break;
//					}
//				}
//			}
//			if (id != null) { // if this DefCore.txt has an id or is global c4g
//				
//				if (!objects.containsKey(id)) {
//					parent = createObjectFromFolder(path.toString().substring(1) + (path.hasTrailingSeparator() ? "" : "/") + group.getName(), path, id);
//				}
//				else {
//					// TODO create marker to report error (c4id declared twice)
//					return;
////					throw new CompilerException("Object with ID " + id + " is declared twice! (" + objects.get(id).getName() + " and " + group.getName() + ")");
//				}
//				
////				System.out.println("Indexed object " + parent.getName());
//				
//				if (!script.isCompleted()) script.open(false);
//				byte[] bytes = script.getContents();
//				String contents = new String(bytes);
//				
//				indexScript(path, parent, contents);
//			}
//		}
//	}
//	
//	protected void indexScript(IPath path, C4Object parent, String contents) {
//		List<Comment> comments = new LinkedList<Comment>();
//		// find multi line comments /* */
//		Matcher m = commentSearch.matcher(contents);
//		while(m.find()) {
//			comments.add(new Comment(m.start(), m.end()));
//		}
//		// find single line comments //
//		m = singleLineCommentSearch.matcher(contents);
//		while(m.find()) {
//			comments.add(new Comment(m.start(), m.end()));
//		}
//		
//		// find functions
//		m = functionSearch.matcher(contents);
//		while(m.find()) {
//			if (isCommented(comments, m)) continue;
//			
//			C4Function func = new C4Function(m.group(2), parent,m.group(1));
//			func.setCallback(isObjectCallback(m.group(2)));
//			func.setLocation(new SourceLocation(m.start(2),m.end(2)));
//			Matcher pm = parameterSearch.matcher(m.group(3));
//			while(pm.find()) {
//				C4Variable var = new C4Variable(pm.group(2), C4VariableScope.VAR_VAR);
//				if (pm.group(1) != null) var.setType(C4Type.makeType(pm.group(1)));
//				func.getParameter().add(var);
//			}
//			// TODO: function return type recognition
//			parent.addField(func);
//		}
//		
////		for (C4Function func : parent.getDefinedFunctions()) {
////			Scanner scanner = new Scanner(contents);
////			int bracketDepth = -1;
////			int start = 0;
////			int end = 0;
////			while (scanner.hasNext()) {
////				String token = scanner.next();
////				// D:<
////				if (bracketDepth == -1) {
////					if (scanner.match().end() >= func.getLocation().getEnd())
////						bracketDepth = 0;
////					else
////						continue;
////				}
////		
////				if (token.equals("{")) {
////					if (bracketDepth == 0) {
////						start = scanner.match().start();
////					}
////					bracketDepth++;
////				} else if (token.equals("}")) {
////					bracketDepth--;
////					if (bracketDepth == 0) {
////						end = scanner.match().end()+1;
////						break;
////					}
////				}
////			}
////			scanner.close();
////			func.setBody(new SourceLocation(start,end));
////		}
//		
//		m = oldFunctionSearch.matcher(contents);
//		while(m.find()) {
//			if (isCommented(comments, m)) continue;
//			
////			if ("case".equalsIgnoreCase(m.group(2))) continue; // nonsense
//			
//			C4Function func = new C4Function(m.group(2), parent, m.group(1));
//			func.setCallback(isObjectCallback(m.group(2)));
//			func.setLocation(new SourceLocation(m));
//			
//			if (parent.getScript() != null) {
//				try {
//					IMarker marker = parent.getScript().createMarker(IMarker.PROBLEM);
//					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
//
//					marker.setAttribute(IMarker.TRANSIENT, true);
//					marker.setAttribute(IMarker.CHAR_START, m.start());
//					marker.setAttribute(IMarker.CHAR_END, m.end());
//					marker.setAttribute(IMarker.MESSAGE, "This is an old function declaration. Do not use them anymore!");
//				} catch (CoreException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			// TODO: parameter recognition in old function declarations?
//			parent.addField(func);
//		}
//		
//		// find variables
//		m = variableSearch.matcher(contents);
//		while(m.find()) {
//			if (isCommented(comments, m)) continue;
//			
//			// TODO: extended variable type recognition
//			C4Variable var;
//			String vars = m.group(3);
//			if (vars.contains(",")) {
//				String[] varsArray = vars.split(",");
//				int offset = m.start(3);
//				for(String variable : varsArray) {
//					var = new C4Variable(variable.trim(), m.group(1));
//					int start = offset;
//					for (int i = 0; i < variable.length() && Character.isWhitespace(variable.charAt(i)); i++)
//						start++;
//					var.setLocation(new SourceLocation(start,start+var.getName().length()));
//					offset += variable.length()+1; // ","
//					parent.addField(var);
//				}
//			}
//			else {
//				var = new C4Variable(vars.trim(), m.group(1));
//				var.setLocation(new SourceLocation(m.start(3),m.end(3)));
//				parent.addField(var);
//			}
//		}
//		
//		// find directives
//		m = directiveSearch.matcher(contents);
//		while(m.find()) {
//			if (isCommented(comments, m)) continue;
//			
//			C4Directive directive = new C4Directive(m.group(1),m.group(2));
//			parent.getDefinedDirectives().add(directive);
//		}
//	}
//	
//	private boolean isCommented(List<Comment> comments, Matcher m) {
//		for(Comment comment : comments) { // strip out comments
//			if (m.start() > comment.begin && m.start() < comment.end) return true;
//		}
//		return false;
//	}
//	
//	public boolean isObjectCallback(String function) {
//		if (function.startsWith("Context")) return true;
//		for(int i = 0; i < BuiltInDefinitions.OBJECT_CALLBACKS.length;i++) {
//			if (BuiltInDefinitions.OBJECT_CALLBACKS[i].equalsIgnoreCase(function)) return true;
//		}
//		return false;
//	}
//
//	private String getStringFromStream(InputStream stream) throws IOException {
//		InputStreamReader reader = new InputStreamReader(stream);
//		StringBuilder buf = new StringBuilder();
//		char[] buffer = new char[1024];
//		int read = 0;
//		while((read = reader.read(buffer)) > 0) {
//			buf.append(buffer,0,read);
//		}
//		return buf.toString();
//	}
//	
//	/**
//	 * @return the clonkDirCompiled
//	 */
//	public boolean isClonkDirIndexed() {
//		return clonkDirIndexed;
//	}
//
//	/**
//	 * @return the objects
//	 */
//	public Map<C4ID, C4Object> getObjects() {
//		return objects;
//	}

}
