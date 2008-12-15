<?php
/*
 * Created on 31.07.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 if (!defined("IN_ARCO")) die("suchst du was?");
 
 abstract class plugin {

	public $dummy = false;
	public $instance;
	public $modules;

	protected function make_module($modname) {
		$class = get_class($this)."_".$modname;
		if (!class_exists($class)) {
			throw new ArcoException("Unknown module '$modname' for plugin '".get_class($this)."'.");
		}
		$obj = new $class;
		$obj->plugin = $this;
		return $obj;
	}
	
	public final function getParentPage() {
		return $this->parentPage;
	}
	
	public final function getName() {
		return get_class($this);
	}
	
	public final function getInstance() {
		return $this->instance;
	}

	public final function __construct($instance) {
		$this->instance = $instance;
		foreach($this->plugin_modules as $module) {
			$this->modules[$module] = $this->make_module($module);
		}
		try {
			arco::$lang->load_lang_for($this);
		}
		catch(FileNotFoundException $e) {
			logger::log("Plugin '".$this->getName()."' has no lang files.");
		}
	}
	
	public final function __get($getname) {
		if ($getname == "plugin_modules") {
			$mainClass = get_class($this);
			$classes = get_declared_classes();
			$modules = array();
			reset($classes);
			while(list(,$class) = each($classes)) {
				if (substr($class,0,strlen($mainClass)) == $mainClass && $class != $mainClass) $modules[] = substr($class,strlen($mainClass) + 1);
			}
			$this->plugin_modules = $modules;
			return $modules;
		}
		if (substr($getname,0,4) == "mod_") {
			$module_name = substr($getname,4);
			if (in_array($module_name,array_keys($this->modules))) return $this->modules[$module_name];
			if (!in_array($module_name,$this->plugin_modules)) throw new PluginException(get_class($this),"Unknown module '$module_name' for plugin '".get_class($this)."'");
			$module = $this->make_module($module_name);
			$this->modules[$module_name] = $module;
			return $module;
		}
		else {
			return NULL;
		}
	}
	
	public final function __call($getname,$parameter) {
		if (substr($getname,0,4) == "mod_") {
			$module_name = substr($getname,4);
			if (in_array($module_name,array_keys($this->modules))) return $this->modules[$module_name];
			if (!in_array($module_name,$this->plugin_modules)) throw new PluginException(get_class($this),"Unknown module '$modname' for plugin '".get_class($this)."'");
			$module = $this->make_module($module_name);
			$this->modules[$module_name] = $module;
			return $module;
		}
		else {
			throw new ArcoException("Unknown method '$getname' in plugin '".__CLASS__."'.");
		}
	}
	
	public final function __set($setname,$value) {
		if (substr($setname,0,4) == "mod_") {
			$module_name = substr($setname,4);
			$mod_class = __CLASS__."_".$module_name;
			if (class_exists($mod_class) && $value instanceof $mod_class) {
				$this->modules[$module_name] = $value;
			}
		}
		else {
			$this->$setname = $value;
		}
	}
	
}
?>
