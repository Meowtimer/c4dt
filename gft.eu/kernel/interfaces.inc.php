<?php
/*
 * Created on 01.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 if (!defined("IN_ARCO")) die("suchst du was?");
 
 interface main_tpl {
 	
 	public function tpl_head($title, $headaddition);
 	
 }
 
 abstract class structure {
 	public $positions;
 	public function init() {
  		$reflection = new ReflectionClass(get_class($this));
 		$methods = $reflection->getMethods();
 		$found = false;
 		foreach($methods as $method) {
 			if ($method->getName() == "structure") $found = true;
 		}
 		if (!$found) throw new ViewException("Incompatible structure file '".get_class($this)."'. Method structure() is mandatory."); // TODO: correct english translation
	  	$reflection = new ReflectionMethod(get_class($this),"structure");
	 	$parameters = $reflection->getParameters();
	 	$parameterNames = array();
	 	if ($parameters[0]->getName() != "head") throw new ViewException("Incompatible structure file '".get_class($this)."'. 'head' has to be the first parameter of structure()");
	 	if ($parameters[1]->getName() != "foot") throw new ViewException("Incompatible structure file '".get_class($this)."'. 'foot' has to be the second parameter of structure()");
	 	array_shift($parameters); array_shift($parameters);
	 	foreach($parameters as $parameter) {
	 		$parameterNames[] = $parameter->getName();
	 	}
	 	$this->positions = $parameterNames;
 	}
 }
 
?>
