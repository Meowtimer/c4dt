<?php
/*
 * Created on 29.07.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */

if (!defined("IN_ARCO")) die("suchst du was?");

 /**
  * Library system
  * @author ZokRadonh
  * @package ArcoVM
  * @version alpha 0.1
  */
class libs {
	/// $libs->nl2br("bluba");
	public $libs = array();
	public $index = array(); // $index["nl2br"] == "stdformat" <-> $libs["stdformat"]->nl2br();
	public $classes = array();
	public $auto_run_objects = array();
	
	public function __call($func,$pars) {
		if (!in_array($func,array_keys($this->index))) {
			throw new UnknownLibFunctionException($func);
		}
		return call_user_func_array(array($this->libs[$this->index[$func]],$func),$pars);
	}
	
	public function __get($class) {
		if (!array_key_exists);
	}
	
	public function register_lib($library) {
		global $engine;
		if (class_exists($library)) throw new LibraryException("Library already registered.");
		require_once(LIBS.$library.".lib.php");
		if (!class_exists($library)) throw new LibraryException("Library '$library' corrupt. There is no declaration for this class in '{$library}.lib.php'");
		$methods = call_user_func(array($library,"GetMethods"));
		foreach($methods as $method) if (array_key_exists($method,$this->index)) throw new LibraryException("Library function '$method' declared twice.\nFirst declaration in library '".$this->index[$method]."'.\nSecond declaration in library '$library'.");
		foreach($methods as $method) $this->index[$method] = $library;
		$sClasses = call_user_func(array($library,"GetSuperObjects"));
		if (is_array($sClasses)) {
			foreach($sClasses as $name => $obj) {
				$engine->set_super($name,$obj);
				$this->auto_run_objects[] = $obj;
			}
		}
		$classes = call_user_func(array($library,"GetClasses"));
		foreach($classes as $class) {
			$obj = new $class;
			if (isset($obj->name)) $this->classes[$obj->name] = $obj;
			else $this->classes[$class] = $obj;
			$this->auto_run_objects[] = $obj;
		}
		
		$this->libs[$library] = new $library;
		return $this->libs[$library];
	}
	
}

abstract class library {
	
	public function GetFilePath() {
		return PATH_LIB.__CLASS__.".lib.php";
	}
	
	abstract public static function GetMethods();
	abstract public static function GetSuperObjects();
	abstract public static function GetClasses();
	
}
 
?>
