<?php
/*
 * Created on 04.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 abstract class template {
 	public $pluginName;
 	public $plugin;
 	private $tpl; // template string
 	private $hasLogic = false;
 	private $parameterNames = array();
 	private $parameters = array();
 	private $currentFunction;
 	protected $dummy = false;
 	
 	public function __call($funcname,$params) {
 		$this->currentFunction = $funcname;
 		// --------------------------------
		// Convert array elements to full array member
		// --------------------------------
		$paramsNew = array();
 		foreach($params as $param) {
 			if (is_array($param)) {
 				foreach($param as $e) {
 					$paramsNew[] = $e;
 				}
 			}
 			else $paramsNew[] = $param;
 		}
 		$params = $paramsNew;
 		// --------------------------------
		// Execute template
		// --------------------------------
 		if(!method_exists($this,"tpl_".$funcname)) throw new ViewException("Unknown template '$funcname' for plugin '$this->pluginName'");
 		$this->parameterNames = $this->get_parameters("tpl_".$funcname);
 		$this->parameters = $params;
 		$this->tpl = call_user_func_array(array($this,"tpl_".$funcname),$params);
 		return $this->parse_template($this->tpl);
 	}
 	
 	private function get_parameters($func) {
 		$reflect = new ReflectionMethod(get_class($this),$func);
 		$pars = $reflect->getParameters();
 		foreach($pars as &$par) $par = $par->getName();
 		return $pars;
 	}
 	
  	public function parse_template($template) {
 		// --------------------------------
 		// Insert template vars
 		// --------------------------------
  		$template = @preg_replace_callback('/{arco\.([^}]+)}/i',array($this,"parse_element"),$template);
 		
 		// --------------------------------
		// Language specific zones
		// --------------------------------
		$regex = '/{ \? IsLang: ([a-z,]+) }  (.+?)  (?: { : IsLang }  ([^{]+) )?  { \/ IsLang }/ixs';
		$template = preg_replace_callback($regex,array($this,"parse_logic_inlang"),$template);
 		
 		if ($this->dummy) return $template;
 		// --------------------------------
		// its ok paul, make the rocket in the sky again (logic)
		// --------------------------------
		if (count($this->parameterNames)) {
			$pars = array();
			foreach($this->parameterNames as $i => $par) $pars[] = "(?:".$par.")";
			$regex = '/{ \?  ('.implode("|",$pars).')  }  (.+?)  (?: { : \1 }  ([^{]+) )?  { \/ \1 }/ixs';
			//print $regex;
			$template = preg_replace_callback($regex,array($this,"parse_logic"),$template);
		}
		
		// --------------------------------
		// Cheat Jack 2
		// --------------------------------
		$template = str_replace(array('&lebre;','&ribre;'),array("{","}"),$template);
		// --------------------------------
		// Reset tmp. variables
		// --------------------------------
		$this->currentFunction = null;
		$this->parameterNames = array();
		$this->parameters = array();
 		return $template;
 	}
 	
 	protected function parse_logic($element) {
 		$parameterName = $element[1];
 		$trueValue = $element[2];
 		$falseValue = $element[3];
 		// --------------------------------
		// Find parameter key number
		// --------------------------------
 		$parameterKey = array_search($parameterName,$this->parameterNames);
 		if ($parameterKey < 0) throw new ViewException("Parameter '$parameterName' does not exist in function '".get_class($this)."::{$this->currentFunction}'");
 		// --------------------------------
		// if parameter == true
		// --------------------------------
 		if ($this->parameters[$parameterKey]) return $trueValue;
 		else return $falseValue;
 	}
 	
  	protected function parse_logic_inlang($element) {
 		$languages = explode(",",$element[1]);
 		$trueValue = $element[2];
 		$falseValue = $element[3];
 		// --------------------------------
		// Check if active language is in range
		// --------------------------------
 		if (in_array(arco::$engine->currentLang,$languages)) return $trueValue;
 		else return $falseValue;
 	}
 	
 	private function parse_element($element) {
 		$parts = explode(".",$element[1]);
 		switch($parts[0]) {
 			case "lang":
 				if (in_array($parts[1],array_keys(arco::$engine->plugins)) || $parts[1] == "main") {
 					if (!strlen($parts[2])) {
						throw new ViewException("Template error in plugin '$this->pluginName' in template '$this->currentFunction'. Correct usage is: {arco.lang.'pluginName'.'langItem')");
 					}
 					return arco::$lang->$parts[1]->$parts[2];
 				}
 				else {
 					if ($this->dummy) throw new ViewException("Incompatible template command. A dummy template cannot access plugin specific elements. (".implode(".",$parts).")");
 					$data = arco::$lang->{$this->pluginName}->$parts[1];
 					return $data;
 				}
 				break;
 			case "name": //{arco.name.thevar -> plg_inst_thevar
 				if ($this->dummy) throw new ViewException("Incompatible template command. A dummy template cannot access plugin specific elements. (".implode(".",$parts).")");
 				return $this->pluginName."_".$this->plugin->getInstance()."_".$parts[1];
 			case "formAction":
 				if ($this->dummy) throw new ViewException("Incompatible template command. A dummy template cannot access plugin specific elements. (".implode(".",$parts).")");
 				$uri = ARL::in();
 				if (is_array(arco::$engine->input['shifter'])) {
 					foreach(arco::$engine->input['shifter'] as $shift) {
	 					$uri->full_append("shifter[]",$shift);
	 				}
 				}
 				return $uri->url;
 			case "include": //{arco.include.main.hyperlink.   arco.include.
 				if ($parts[1] == "main") {
 					return arco::$view->main->{$parts[2]}(array_slice($parts,3)); // well, php is not that bad
 				}
 				else {
 					if ($this->dummy) throw new ViewException("Incompatible template command. A dummy template cannot access plugin specific elements. (".implode(".",$parts).")");
 					// --------------------------------
					// Caching? // TODO: template caching?
					// --------------------------------
					$tpl = arco::$view->get_templates_for($this->plugin);
					return $tpl->{$parts[1]}(array_slice($parts,2));
 				}
 			case "link":
 				// new system:  Hallotext<url>tooltip
 				if (preg_match('#^(.+)<([^>]+)>(.*)$#',implode(".",array_slice($parts,1)),$match)) {
 					if (!$match[3]) $match[3] = false;
 					$link = new link($match[2]);
					return $link->html($match[1],$match[3]);
 				}
 				else return implode(".",array_slice($parts,0));
			case "url":
 				//{arco.url.next_for()->ap("action","muharg")}
 				array_shift($parts);
 				if ($parts[0] == "extern") {
 					array_shift($parts);
 					return implode(".",$parts); // return directly the url... no changes for extern links | ex() is btw senseless
 				}
 				elseif ($parts[0] == "page") {
 					$url = ARL::page($parts[1]);
 					array_shift($parts); array_shift($parts);
 					if (count($parts)) eval('$url->'.implode("->",$parts).";");
 					return $url->url;
 				}
 				$url = ARL::in();
 				foreach($parts as &$part) $part = str_replace("next_for()",'next_for($this->plugin)',$part);
 				$cmd = implode("->",$parts);
 				//foreach($parts as $part) {
 					eval('$url->'.$cmd.";");
 				//}
 				return $url->url;
 				break;
 			case "suburl":
 				if ($this->dummy) throw new ViewException("Incompatible template command. A dummy template cannot access plugin specific elements. (".implode(".",$parts).")");
 				array_shift($parts);
 				$url = ARL::in()->next_for($this->plugin);
 				if (is_array(arco::$engine->input['shifter'])) {
 					foreach(arco::$engine->input['shifter'] as $shift) {
	 					$url->full_append("shifter[]",$shift);
	 				}
 				}
 				$cmd = implode("->",$parts);
 				//foreach($parts as $part) {
 					eval('$url->'.$cmd.";");
 				//}
 				return $url->url;
// 			case "logic":
// 				$this->hasLogic = true;
// 			    return $element[0];
 			case "prefix":
 				if ($this->dummy) throw new ViewException("Incompatible template command. A dummy template cannot access plugin specific elements. (".implode(".",$parts).")");
 				return $this->plugin->getName()."_".$this->plugin->getInstance()."_";
 			case "user":
 				return arco::$user->$parts[1];
 			case "img":
 				if ($this->dummy) throw new ViewException("Incompatible template command. A dummy template cannot access plugin specific elements. (".implode(".",$parts).")");
 				$path = SKINS."plg_$this->pluginName/".implode(".",array_slice($parts,1)); // to allow dot-extensions: arco.img.pic.gif
 				if (file_exists($path)) return $path;
 			case "const":
 				return constant(strtoupper($parts[1]));
 			case "pageName":
 				return $GLOBALS['page']->pageLongName;
 			default:
 				if ((arco::$engine->{$parts[0]})) {
 					return arco::$engine->{$parts[0]};
 				}
 		}
 	}
 }
 
 class dummy_template extends template {
 	protected $dummy = true;
 	public $template;
 	public function tpl_tpl() {
 		return $this->template;
 	}
 	
 }
?>
