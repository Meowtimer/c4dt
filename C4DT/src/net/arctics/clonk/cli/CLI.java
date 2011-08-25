package net.arctics.clonk.cli;

import java.util.Scanner;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.ExecutableScript;

public class CLI {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("c4dt commandline interface");
		boolean done = false;
		Scanner scanner = new Scanner(System.in);
		ClonkCore.headlessInitialize(args[0], "OpenClonk");
		while (!done) {
			String command = scanner.nextLine();
			ExecutableScript script = Command.executableScriptFromCommand(command);
			if (script != null) {
				Object result = script.getMain().invoke();
				if (result != null)
					System.out.println(result.toString());
			}
		}
	}

}
