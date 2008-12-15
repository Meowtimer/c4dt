<?php
/*
 * Created on 01.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
if (!defined("IN_ARCO")) die("suchst du was?");
 
 /**
  * Page system
  * @author ZokRadonh
  * @package ArcoVM
  * @version alpha 0.4
  */
 class page {
 	public $pageName;
 	public $pageLongName;
 	public $structure;
 	public $structureName;
 	public $locked;
 	public $modules = array();
 	public $title;
 	public $postParser;
 	
 	public function __construct($pageData) {
 		$this->pageName = $pageData['page_name'];
 		$this->title = $pageData['title'];
 		$this->structureName = $pageData['structure'];
 		$this->locked = $pageData['locked'];
 		$this->pageLongName = $pageData['page_long_name'];
 		$this->structure = arco::$view->get_structure_for($this);
 		$result = arco::$db->select("page_modules","*","WHERE page_id = '".$pageData['id']."'");
 		while($result->next) {
 			// --------------------------------
			// Plugin instance cached?
			// --------------------------------
 			if ( array_key_exists( $result->plugin."_".$result->instance, arco::$engine->pluginInstances ) ) $plugin = arco::$engine->pluginInstances[$result->plugin."_".$result->instance];
 			// --------------------------------
			// No, create a new object
			// --------------------------------
 			else $plugin = arco::$engine->load_plugin($result->plugin,$result->instance);
 			$module = $plugin->{"mod_".$result->module};
 			$module->parentPage = $this;
 			$this->modules[$result->position][intval($result->position_order)] = $module;
 		}
 	}
 	
 	/**
 	 * combine all the shit and print out a nice html page
 	 */
 	public function build_page($zones = false) {
 		//arco::$user->update_for($this); // session update // TODO: other session update
 		$tpl = arco::$view->main;
 		$params = array();
 		$params[] = $zones ? "" : $tpl->head($this->title,"");
 		$params[] = $zones ? "" : $tpl->foot(arco::$engine->currentLang,"");
 		$iPos = 1;
 		foreach($this->structure->positions as $pos) {
 			$iPos++;
 			if (is_array($this->modules[$pos])) {
 				$params[$iPos] = "";
 				ksort($this->modules[$pos]); // allow positionOrdering
	 			foreach($this->modules[$pos] as $module) {
	 				if (substr($module->getName(),0,3) == "adm" && !arco::$user->can_do("is_root")) {
	 					//$params[$iPos] = "Permission denied.";
	 					//continue;
	 				}
	 				// --------------------------------
					// Shifting
					// --------------------------------
					$modShift = arco::$view->modShift;
	 				if ($modShift && !$zones) {
	 					while ($module->isShifted()) {
							$shift = $module->getShift();
 							// --------------------------------
							// Try to load module instance from "cache"
							// --------------------------------
 							$module = arco::$engine->pluginInstance[
 									arco::$engine->generate_common_ident_string_p($shift['plugin'],$shift['instance'],true)
 								]->{"mod_".$shift['module']};
 							if (!$module) {
 								// --------------------------------
								// Load module instance from scratch (and cache it automatically) - mostly the case
								// --------------------------------
								$module = arco::$engine->load_plugin(
										$shift['plugin'],$shift['instance']
									)->{"mod_".$shift['module']};
 							}
 							logger::log("Shifted to module '".$shift['module']."'.");
	 					}
	 				}
	 				// --------------------------------
					// Execute module
					// --------------------------------
	 				try {
	 					$params[$iPos] .= ($zones ? '<div style="display: none; position: absolute;" struct="'.$pos.'"></div>&nbsp;' : $module->make_html());
	 				}
	 				catch (UserException $e) {
	 					$params[$iPos] .= $e->getMessage(); //TODO: plugin user exception templates
	 					//TODO: plugin specific exception template
	 				}
	 				catch (RedirectBreakthrough $e) {
	 					$params[$iPos] .= '<meta http-equiv="refresh" content="'.$e->delay.';URL='.$e->link.'" />' . $e->content;
	 				}
	 				// --------------------------------
					// Post parsing
					// --------------------------------
					if ($this->postParser) {
	 					if (arco::$user->sessionVars->isIEClient) $params[$iPos] = call_user_func($this->postParser,$params[$iPos]);
					}
	 			}
 			}
 			else {
 				$params[$iPos] = $zones ? '<div style="display: none; position: absolute;" struct="'.$pos.'"></div>&nbsp;' : "";
 				logger::log("There are no modules attached to position '$pos'.");
 			}
 		}
 		// --------------------------------
		// Add custom head/foot code
		// --------------------------------
 		if (strlen(arco::$view->get_head_addition()) > 0) {
 			$params[0] = $tpl->head($this->title,arco::$view->get_head_addition());
 		}
 		if (strlen(arco::$view->get_foot_addition()) > 0) {
 			$params[1] = $tpl->foot(arco::$engine->currentLang == "en" ? "de" : "en",arco::$view->get_foot_addition());
 		}
 		// --------------------------------
		// Integrate in structure
		// --------------------------------
 		logger::log("Built page '".$this->pageName."'.");
 		$html = call_user_func_array(array($this->structure,$zones ? "structure" : "structure"),$params);
 		// --------------------------------
 		// Convert to output charset (mostly UTF-8)
 		// --------------------------------
 		//$html = iconv(arco::$engine->charset, arco::$engine->outputCharset, $html);
// 		header("Content-Type: text/html; charset=" + arco::$engine->outputCharset);
 		return $html;
 	}
 	
 	public function __toString() {
 		return "Object(page:$this->pageName)";
 	}
 }
 
class RedirectBreakthrough extends Exception {
	public $link;
	public $delay;
	public $content;
	public function __construct($link,$delay,$content = NULL) {
		$this->link = $link;
		$this->delay = $delay;
		$this->content = $content;
		parent::__construct("redirect");
	}
}
 
?>
