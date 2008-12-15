<?php
/*
 * Created on 01.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 if (!defined("IN_ARCO")) die("suchst du was?");
 
 class LogException extends Exception {
 	public function __construct($log) {
 		logger::logit($log,$this->GetTrace());
 	}
 }
 
 class SecLogException extends Exception {
 	public function __construct($log) {
 		logger::logsecit($log,$this->GetTrace());
 	}
 }
 
 /**
  * Log system. Freak thou exception!
  * @author ZokRadonh
  * @package ArcoVM
  * @version alpha 0.1
  */
 class logger {
 	public static $logStream;
 	public static $logSecStream;
 	
 	public static function logsecit($message,$trace,$stringtrace = FALSE) {
 		if (!logger::$logSecStream) logger::$logSecStream = fopen(ROOT."sec.log","a");
 		while(array_key_exists(0,$trace) && strstr($trace[0]['file'],"log.inc.php")) array_shift($trace);
 		while(array_key_exists(0,$trace) && ($trace[0]['class'] == "logger")) array_shift($trace);
 		while(array_key_exists(0,$trace) && strstr($trace[0]['file'],"exception.inc.php")) array_shift($trace);
 		$logmessage = "[".date("d.m.Y H:i:s")."] ";
 		if (array_key_exists(0,$trace)) {
 			if (!is_array($trace[0]['args'])) $trace[0]['args'] = array();
 			$logmessage .=
	 		$trace[0]['file'].":".
	 		$trace[0]['line']." ".
	 		$trace[0]['class'].
	 		$trace[0]['type'].
	 		$trace[0]['function']."(".
	 		implode(",",array_map(create_function('$e','if (is_object($e)) return "Object(".get_class($e).")"; else return strval($e);'),$trace[0]['args'])).
			") \n";
 		}
 		$logmessage .= "        ".str_replace("\n","\n        ",$message)."\n";
 		if ($stringtrace) $logmessage .= "              ".str_replace("\n","\n              ",$stringtrace)."\n";
 		fputs(logger::$logSecStream,$logmessage);
 	}
 	
 	public static function logit($message,$trace,$stringtrace = FALSE) {
 		if (!logger::$logStream) logger::$logStream = fopen(ROOT."log.log","a");
 		while(array_key_exists(0,$trace) && strstr($trace[0]['file'],"log.inc.php")) array_shift($trace);
 		while(array_key_exists(0,$trace) && ($trace[0]['class'] == "logger")) array_shift($trace);
 		while(array_key_exists(0,$trace) && strstr($trace[0]['file'],"exception.inc.php")) array_shift($trace);
 		$logmessage = "[".date("d.m.Y H:i:s")."] ";
 		if (array_key_exists(0,$trace)) {
 			if (!is_array($trace[0]['args'])) $trace[0]['args'] = array();
 			$logmessage .=
	 		$trace[0]['file'].":".
	 		$trace[0]['line']." ".
	 		$trace[0]['class'].
	 		$trace[0]['type'].
	 		$trace[0]['function']."(".
	 		implode(",",array_map(create_function('$e','if (is_object($e)) return "Object(".get_class($e).")"; else return strval($e);'),$trace[0]['args'])).
			") \n";
 		}
 		$logmessage .= "        ".str_replace("\n","\n        ",$message)."\n";
 		if ($stringtrace) $logmessage .= "              ".str_replace("\n","\n              ",$stringtrace)."\n";
 		fputs(logger::$logStream,$logmessage);
 	}

 	public static function log($message) {
 		try {
 			throw new LogException($message);
 		}
 		catch (LogException $e) {
 			// supi :)
 		}
 	}
 	
 	public static function logSec($message) {
 	 	try {
 			throw new SecLogException($message);
 		}
 		catch (SecLogException $e) {
 			// supi :)
 		}
 	}
 }
?>
