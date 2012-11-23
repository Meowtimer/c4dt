package net.arctics.clonk.mapcreator;

import org.eclipse.swt.graphics.ImageData;

public class GlobalFunctions {
	public static final int C4S_MaxPlayer = 4;
	
	public static boolean Inside(int num, int min, int max) {
		return num >= min && num <= max;
	}
	static int RandomCount;
	static long RandomHold;

	public static int Random(int iRange)
	{
		RandomCount++;
		if (iRange==0) return 0;
		RandomHold = RandomHold * 214013L + 2531011L;
		return (int) ((RandomHold >> 16) % iRange);
	}
	
	public static int BoundBy(int bval, int lbound, int rbound) { return bval < lbound ? lbound : bval > rbound ? rbound : bval; }
	
	public static void Fill(ImageData data, int col) {
		for (int x = 0; x < data.width; x++)
			for (int y = 0; y < data.height; y++)
				data.setPixel(x, y, col);
	}
}
