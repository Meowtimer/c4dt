<?php
/*
 * Created a long time ago
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
if (!defined("IN_ARCO")) die("suchst du was?");

/**
 * This special exception is catched before all other ones.
 * This exception should be displayed with the normal header und footer.
 * It is mainly for error messages like filling out a form uncompletely.
 * It is only displayed in the "frame" of the module.
 */
class UserException extends ArcoException {
	public function __construct($message) {
		
		$message = arco::$lang->main->error.":\n".$message;
		
		parent::__construct($message,4);
	}
}
 
class ArcoException extends Exception {
	public function __construct($message,$code = 0) {
		
		if ($code != 4) {
//			$message .= "\n";
//			$message .= "Error occured in file '".$this->GetFile()."' in line ".$this->GetLine().".\n";
		}
		logger::logit($message,$this->GetTrace(),$this->GetTraceAsString());
		parent::__construct(nl2br($message),$code);
	}
}

class PluginMissingLibException extends ArcoException {
	public function __construct($missing_libs,$code = 0) {
		
		$message = "Plugin initialization failed! The following libs are not installed:";
		
		foreach($missing_libs as $lib) $message .= "\n".$lib;
		
		parent::__construct($message,$code);
	}
}

class PluginException extends ArcoException {
	public function __construct($plugin,$message) {
		
		$message = "An error occured in plugin '$plugin':\n".$message;
		
		parent::__construct($message,0);
	}
}

class PluginNotInstalledException extends ArcoException {
	public function __construct($plugin,$code = 0) {
		
		$message = "Plugin initialization failed! Plugin '$plugin' is not yet plugged in.";
		
		parent::__construct($message,$code);
	}
}

class LibraryException extends ArcoException {
	public function __construct($libname,$message) {
		
		$message = "Library '$libname' couldn't be loaded. Reason: ".$message;
		
		parent::__construct($message,0);
	}
}

class HackedException extends ArcoException {
	public function __construct() {
		echo '<html><body><div style="text-align: center;"><img src="http://ugly.plzdiekthxbye.net/medium/m103.gif" border="0" alt="geh sterben" title="fu"></body></html>';
		exit;
	}
}

class SecurityException extends Exception {
	public function __construct($message) {
		logger::logSec($message);
		$message = "SECURITY EXCEPTION THROWN!\n\nMessage: \n".$message;
	
		parent::__construct($message,0);
	}
}

class UnknownLibFunctionException extends ArcoException {
	public function __construct($funcname) {
		
		$message = "Unknown library function '$funcname'.";
		
		parent::__construct($message,0);
	}
}

class AVMRuntimeException extends ArcoException {
	public function __construct($msg, $errfile = false, $errline = false) {
		$message = "Arco Virtual Machine runtime error occured:\n";
		$message .= $msg;
		
		parent::__construct($message,0);
	}
}

class FileNotFoundException extends ArcoException {
	private $missingFile;
	public function __construct($file) {
		$message = "File '$file' not found.\n";
		$this->missingFile = $file;
		parent::__construct($message,0);
	}
	public function getMissingFile() {
		return $this->missingFile;
	}
 }

class IOException extends ArcoException {
	public function __construct($message) {
		parent::__construct($message);
	}
}

class NotFoundException extends ArcoException {
	private $searchItem;
	public function __construct($message, $searchItem) {
		$this->searchItem = $searchItem;
		parent::__construct($message,0);
	}
	public function getSearchItem() {
		return $this->searchItem;
	}
}

class ViewException extends ArcoException {
	public function __construct($message) {
		$message = "An error occured in the view-system:\n".$message;
		parent::__construct($message,0);
	}
}

?>