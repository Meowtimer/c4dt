<?php
/*
 * Created on 13.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 /**
  * Arco Resource Locator: Freaked URL System. Provides "inline" access to the whole url construction class. (Includes shifting)
  * @author ZokRadonh
  * @package ArcoVM
  * @version alpha 0.6
  */
 class ARL {
 	
 	/**
 	 * Usage is fucking easy xD (short aliases are used here)
 	 * $tpl->row($somedata,ARL::in()->nxt($this)->ap("id",5)->ap("yes","iloveit")->url);
 	 * or
 	 * $tpl->row($blabla,ARL::in()->ap_f($this,"name","fritz")->url);
 	 * instead of 
 	 * $tpl->row($blub,arco::$engine->currentSite."&amp;pluginName_id=".$id."&amp;pluginName_yes=".$love);
 	 */
 	
 	public  $url = "";
 	private $objectAvailable = false;
 	private $pluginName;
 	private $instanceName;
 	private $modName;
 	private $seperator = "&amp;";
 	private $object = null;
 	private $vars = array();
 	
 	public static function in() { // ::intern alias
 		return ARL::intern();
 	}
 	
 	public static function intern() {
 		$i = new ARL;
 		$i->url .= arco::$engine->currentSite;
 		return $i;
 	}
 	
 	public static function extern ($raw = null) {
 		$i = new ARL;
 		$i->url .= "$raw";
 		return $i;
 	}
 	
 	public static function resource($uResId) {
 		$i = new ARL;
 		$i->url = "?resource=".$uResId;
 		return $i;
 	}
 	
 	public static function last() {
 		$i = new ARL;
 		$i->url .= arco::$engine->last;
 		if (Strlen(arco::$engine->last) == 0) throw new ArcoException("session vars unstable");
 		return $i;
 	}
 	
 	public static function current() {
 		$i = new ARL;
 		$i->url .= arco::$engine->self;
 		$i->url .= $_SERVER['QUERY_STRING'];
 		return $i;
 	}
 	
  	public static function page($pageName) {
 		$i = new ARL;
 		$i->url .= arco::$engine->self;
 		$i->url .= "page=".$pageName;
 		return $i;
 	}
 	
 	public function __get($getName) {
 		if ($getName == "url") {
 			$url = $this->url;
 			foreach($this->vars as $varName => $varValue) {
 				$url .= $this->seperator.$varName."=".$varValue;
 			}
 			return $url;
 		}
 		return null;
 	}
 	
 	public function __toString() {
 		return $this->url;
 	}
 	
 	public function no_html() {
 		$this->seperator = "&";
 		return $this;
 	}
 	
 	public function in_html() { // you should really never use it.. its default!
 		$this->seperator = "&amp;";
 		return $this;
 	}
 	
 	public function next_for($object) {
 		$this->pluginName = $object->getName();
 		$this->instanceName = $object->getInstance();
 		$this->modName = $object->getModuleName();
 		$this->object = $object;
 		$this->objectAvailable = true;
 		// --------------------------------
		// Hold shift if shifted
		// --------------------------------
		if ($object->isShifted()) {
			$newMod = $object->getShift();
			return $this->shift_to($newMod['module']);
		}
		//elseif ($object->wasShifted()) {
		//	$this->shift_to_for($object->getOldShift(),$object->getModuleName());
		//}
 		return $this;
 	}
 	
  	public function nxt($object) { // next_for alias
 		return $this->next_for($object);
 	}
 	
  	public function append($var, $value) {
  		if (!$this->objectAvailable) throw new ArcoException("Illegal usage of ARL. An object depending method was used without defining object environment.");
 		$this->_append($this->pluginName."_".$this->instanceName."_".$var,$value);
 		return $this;
 	}
 	
 	public function ap($var,$value) { // append alias
 		return $this->append($var,$value);
 	}
 	
  	public function append_for($object, $var, $value) { // plugin_instance_var=value
 		$this->_append($object->getName()."_".$object->getInstance()."_".$var,$value);
 		return $this;
 	}
 	
 	public function ap_f($object,$var,$value) { // append_for alias
 		return $this->append_for($object,$var,$value);
 	}
 	
  	public function ap_for($object,$var,$value) { // append_for alias
 		return $this->append_for($object,$var,$value);
 	}
 	
 	public function full_append($fullVar,$value) {
 		$this->_append($fullVar,$value);
 	}
 	
 	/**
 	 * Raw append of data.
 	 */
 	public function raw($string) {
 		$this->url .= $string;
 		return $this;
 	}
 	
  	private function _append($fullVar, $value) {
 		$this->url .= $this->seperator.$fullVar."=".$value;
		//$this->vars[$fullVar] = $value;
 	}
 	
 	// --------------------------------
	// Shifter functions:
	// --------------------------------
 	public function shift_to($modname) {
 		$this->_append("shifter[]",$this->_create_shift_string($this->pluginName,$this->instanceName,$this->modName,$modname));
 		return $this;
 	}
 	
 	public function shift_to_for($object,$modname) {
 		$this->_append("shifter[]",$this->_create_shift_string($object->getName(),$object->getInstance(),$object->getModuleName(),$modname));
 		return $this;
 	}
 	
  	private function _create_shift_string($pluginName,$instanceName,$modName,$newModName) {
 		return $pluginName.".".$instanceName.".".$modName."~".$newModName;
 	}
 	
 }
 
 /**
  * ARL suxxs
  * Usage: new link("http://www.google.de") // for not plugin-specific links
  * Usage: $this->link("action=del&id=4")   // for all links
  * $link->shift_to()...
  * $tpl->row($link);
  * $tpl->row($link->html($name,$tooltip = false))
  * {arco.link.Delete Picture<action=del&id=4>Tooltip}
  * {arco.link.Delete Picture<http://www.google.de/ig?hl=de>Tooltip}
  */
 class link {
 	private $url = "";
 	private $objectAvailable = false;
 	private $pluginName;
 	private $instanceName;
 	private $modName;
 	private $seperator = "&amp;";
 	private $object = null;
 	private $targetPageName = "";
 	
 	public function __construct($par1,$par2 = false) {
 		$obj = null;
 		if (is_object($par1)) {
	 		$obj = $par1;
	 		$str = $par2;
 		}
 		else $str = $par1;
  		if (preg_match('#^[\w]{,5}://#',$str)) { // extern URL
	 		$str = str_replace("&","&amp;",$str);
	 		$str = str_replace("&amp;amp;","&amp;",$str);
	 		//$str = preg_replace('/&(amp;)?/','&amp;',$str); // besser oder?
	 		$this->url = $str;
 		}
 		else { // intern URL
 			$this->targetPageName = arco::$engine->currentPage;
 			if ($obj) $this->next_for($obj);
 			$this->parse_query($str);
 		}
 	}
 	
 	public function page($pageName) {
// 		$this->url = str_replace("?page=".arco::$engine->currentSite,"?page=".$pageName,$this->url);
		$this->targetPageName = $pageName;
 	}
 	
  	public function html($name,$tooltip = false) {
 		return arco::$view->main->hyperlink($this->__toString(),$name,$tooltip);
 	}
 	
 	public function __toString() {
 		return "?page=".$this->targetPageName.$this->url;
 	}
 	
  	public function next_for($object) {
 		$this->pluginName = $object->getName();
 		$this->instanceName = $object->getInstance();
 		$this->modName = $object->getModuleName();
 		$this->object = $object;
 		$this->objectAvailable = true;
 		// --------------------------------
		// Hold shift if shifted
		// --------------------------------
		if ($object->isShifted()) {
			$newMod = $object->getShift();
			return $this->shift_to($newMod['module']);
		}
		elseif ($object->wasShifted()) { // TODO: Correct shifting
			$this->shift_to_for($object->getOldShift(),$object->getModuleName());
		}
 		return $this;
 	}
 	
 	private function parse_query($queryStr) {
 		$elements = explode("&",$queryStr);  // dacht er ich treff ihn nich
 		foreach($elements as $element) {
 			if (substr($element,0,4) == "amp;") $element = substr($element,4); // aber beim 2. mal zack
 			list($key,$value) = explode("=",$element); 
 			$this->_append($this->pluginName."_".$this->instanceName."_".$key,$value); // hab ich ihm ein mitgegeben
 		}
 		//$elements = array_map("explode", array_pad(array(),count($elements),"="), $elements); 
 		
 	}
 	
   	private function _append($fullVar, $value) {
 		$this->url .= $this->seperator.$fullVar."=".$value;
 	}
 	
  	// --------------------------------
	// Shifter functions:
	// --------------------------------
 	public function shift_to($modname) {
 		$this->_append("shifter[]",$this->_create_shift_string($this->pluginName,$this->instanceName,$this->modName,$modname));
 		return $this;
 	}
 	
 	public function shift_to_for($object,$modname) {
		if (is_object($object)) {
			$pluginName = $object->getName();
			$instanceName = $object->getInstance();
			$moduleName = $object->getModuleName();
		}
		elseif (is_array($object)) {
			$pluginName = $object['plugin'];
			$instanceName = $object['instance'];
			$moduleName = $object['module'];
		}
 		$this->_append("shifter[]",$this->_create_shift_string($pluginName,$instanceName,$moduleName,$modname));
 		return $this;
 	}
 	
  	protected function _create_shift_string($pluginName,$instanceName,$modName,$newModName) {
 		return $pluginName.".".$instanceName.".".$modName."~".$newModName;
 	}

 }
?>
