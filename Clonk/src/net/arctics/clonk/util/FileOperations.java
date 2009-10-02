package net.arctics.clonk.util;

import java.io.*;

// stupid little helper class for file operations i can't find anywhere in standard java
public class FileOperations {

	public static void copyDirectory(File srcPath, File dstPath) throws IOException {
		if (srcPath.isDirectory()) {
			if (!dstPath.exists()) {
				dstPath.mkdir();
			}
			String files[] = srcPath.list();
			for (int i = 0; i < files.length; i++) {
				copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
			}
		} else {
			if (!srcPath.exists()) {
				throw new FileNotFoundException(srcPath.toString());
			} else {
				InputStream in = new FileInputStream(srcPath);
				OutputStream out = new FileOutputStream(dstPath); 
				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}
		}
	}

}