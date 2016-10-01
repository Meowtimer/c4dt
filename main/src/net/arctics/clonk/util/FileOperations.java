package net.arctics.clonk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// stupid little helper class for file operations i can't find anywhere in standard java
public class FileOperations {

	public static void copyDirectory(final File srcPath, final File dstPath) throws IOException {
		if (srcPath.isDirectory()) {
			if (!dstPath.exists()) {
				dstPath.mkdir();
			}
			final String files[] = srcPath.list();
			for (int i = 0; i < files.length; i++) {
				copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
			}
		} else if (!srcPath.exists()) {
			throw new FileNotFoundException(srcPath.toString());
		} else {
			final InputStream in = new FileInputStream(srcPath);
			final OutputStream out = new FileOutputStream(dstPath);
			// Transfer bytes from in to out
			final byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}
	}

}