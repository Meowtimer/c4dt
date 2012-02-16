package net.arctics.clonk.cli;

import java.util.Scanner;

import net.arctics.clonk.Core;
import net.arctics.clonk.command.Command;
import net.arctics.clonk.command.ExecutableScript;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class CLI implements IApplication {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("c4dt commandline interface");
		boolean done = false;
		Scanner scanner = new Scanner(System.in);
		String engineConfigurationFolder;
		if (args.length > 1) 
			engineConfigurationFolder = args[0];
		else
			engineConfigurationFolder = System.getenv().get("C4DTENGINECONFIGURATIONCLI");
		Core.headlessInitialize(engineConfigurationFolder, "OpenClonk");
		while (!done) {
			String command = scanner.nextLine();
			ExecutableScript script = Command.executableScriptFromCommand(command);
			if (script != null) try {
				Object result = script.main().invoke();
				if (result != null)
					System.out.println(result.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		main(new String[0]);
		return null;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

}
