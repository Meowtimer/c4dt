<?php
/*
 * Created on 18.09.2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - PHPeclipse - PHP - Code Templates
 */
 
  /**
  * Page management
  * Administrative plugin
  */
 class adm_page_viewer extends plugin {
 	public $plugin_modules = array("list","create","edit");
 }
 
 class adm_page_viewer_create extends module {
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 	 	$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$tpl = arco::$view->get_templates_for($this);
 		$lang = arco::$lang->get_lang_for($this);
 		$db = arco::$db->get_driver_for($this);
 		$input = arco::$engine->get_input_for($this);
		
		$locked = true;
		$template = "";
		// --------------------------------
		// Submit case
		// --------------------------------
		if ($input->submit) {
			$locked = $input->locked ? true : false;
			if (!$input->name || !$input->title) {
				arco::$engine->raise_error();
				
				$template .= arco::$view->main->errorBox("You have to fill out everything!");
				
			}
			else {
				
				$r = arco::$db->insert("pages",array('page_name' => $input->name, 'structure' => $input->structure, 'title' => $input->title, 'locked' => $locked ? 1 : 0));
				$template .= "Page '$input->name' created.<br />";
				arco::$engine->redirect_delay(ARL::last()->url,1,$template.$lang->success);
			}
		}
		
 		// --------------------------------
		// Show formular
		// --------------------------------
 		$template .= $tpl->head();
 		
 		$structures = arco::$view->get_structures();
 		$structureTpls = array();
 		foreach(array_keys($structures) as $structure) {
 			$structureTpls[] = $tpl->option_row($structure,$input->structure == $structure ? true : false);
 		}
 		$template .= $tpl->form(implode("\n",$structureTpls), $input->name, $input->title, $locked); 		
 		
 		$template .= $tpl->foot();
 		$template .= arco::$view->main->hyperlink(ARL::last(),"back");
  		return $template;
 	}
 	
 }
 
 class adm_page_viewer_edit extends module {
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 	 	$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$tpl = arco::$view->get_templates_for($this);
 		$lang = arco::$lang->get_lang_for($this);
 		$input = arco::$engine->get_input_for($this);
 		
 		// --------------------------------
		// Submit case
		// --------------------------------
		$locked = true;
		$template = "";
		// --------------------------------
		// Submit case
		// --------------------------------
		if ($input->add) {
			$locked = $input->locked ? true : false;
			if ($input->plugin && $input->instance && $input->module) {
				if (!$input->modName) {
					arco::$engine->raise_error();
					$template .= arco::$view->main->errorBox("You haven't given a module name. Module is not added, yet.");
				}
				else {
					$highest = arco::$db->select("page_modules","position_order","WHERE page_id = $input->page AND position = '$input->add' ORDER BY position_order DESC LIMIT 1")->next['position_order'];
					var_dump($highest);
					arco::$db->insert("page_modules",array('page_id' => $input->page, 'name' => $input->modName, 'plugin' => $input->plugin, 'instance' => $input->instance, 'module' => $input->module, 'position' => $input->add, 'position_order' => $highest !== NULL ? $highest + 1 : 0));
					arco::$engine->redirect_delay(ARL::last()->url,1,$lang->success);
				}
			}
			else {
				arco::$engine->raise_error();
				
				$template .= arco::$view->main->errorBox("You have to select a plugin, an instance and a module."); 
				
			}
		}
		elseif ($input->submit == "delete") {
			arco::$db->delete("page_modules","WHERE id = $input->id");
			arco::$engine->redirect_delay(ARL::last()->url,1,$lang->success);
		}
 		elseif ($input->submit) {
 			arco::$db->update("pages",array('page_name' => $input->page_name, 'title' => $input->title, 'structure' => $input->structure, 'locked' => $input->locked ? 1 : 0), "WHERE id = $input->page");
 			arco::$engine->redirect_delay(ARL::last()->url,1,$lang->success);
 		}

 		
 		// --------------------------------
		// Show formular
		// --------------------------------
 		$template .= $tpl->head();
 		//arco::$db->select("pages","*","LEFT JOIN page_modules");
 		
 		$result = arco::$db->select(new mysql_join("pages", "page_modules", array("pages.id" => "page_id")),"*","WHERE pages.id = {$input->page}");
		// --------------------------------
		// Iterate modules
		// --------------------------------
 		$modules = array();
 		while($result->next) {
 			$cStructure = $input->structure ? $input->structure : $result->structure;
 			$cPageName = $input->page_name ? $input->page_name : $result->page_name;
 			$cTitle = $input->title ? $input->title : $result->title;
 			$cLocked = $input->locked ? $input->locked : $result->locked;
 			//($name,$plugin,$instance,$module,$position,$position_order)
 			$modules[] = $tpl->module($result->id, $result->name,$result->plugin, $result->instance, $result->module, $result->position, $result->position_order);
 			$modulesByPosition[$result->position][] = $tpl->option_row($result->name, false);
 		}
 		
 		$structures = arco::$view->get_structures();
 		$structureTpls = array();
 		foreach(array_keys($structures) as $structure) {
 			$structureTpls[] = $tpl->option_row($structure,$structure == $cStructure ? true : false);
 		}
 		$template .= $tpl->form(implode("\n",$structureTpls),implode(" ",$modules), $input->page, $cLocked, $cPageName, $cTitle);
 		// --------------------------------
		// Show zones tables - deprecated
		// --------------------------------
 		//$template .= $structures[$cStructure]->zones("&nbsp;","&nbsp;","&nbsp;","&nbsp;","&nbsp;");
 		// --------------------------------
		// Generate zoned page
		// --------------------------------
// 		$previewPage = arco::$view->get_page($cPageName);
// 		$template .= $previewPage->build_page(true);
 		
 		$plugins = "";
 		$instances = "";
 		$modules = "";
 		foreach(arco::$engine->plugins as $pluginName => $pluginData) {
 			if (!count($pluginData['instances'])) continue;
 			$plugins .= $tpl->plugin_row($pluginData['id'],$pluginName)."\n";
 			$plugin = arco::$engine->load_plugin($pluginName,$pluginData['instances'][0]);
 			$instances .= $tpl->new_instance($pluginData['id'])."\n";
 			$modules .= $tpl->new_module($pluginData['id'])."\n";
 			foreach($pluginData['instances'] as $instanceId => $instanceName) {
 				$instances .= $tpl->instance_row($pluginData['id'],$instanceId,$instanceName)."\n";
 			}
  			foreach($plugin->plugin_modules as $moduleId => $moduleName) {
 				$modules .= $tpl->module_row($pluginData['id'],$moduleId,$moduleName)."\n";
 			}
 		}
 
 	 	foreach($structures[$cStructure]->positions as $positionName) {
 			$positions[] = $tpl->position($positionName);
 			$positionSelects[] = $tpl->positionSelect($positionName,is_array($modulesByPosition[$positionName]) ? implode("\n",$modulesByPosition[$positionName]): '');
 		}

 		$template .= $tpl->foot($plugins,$instances,$modules,implode("\n",$positions),implode("\n",$positionSelects),$input->modName);
 		return $template;
 	}
 }
 
 class adm_page_viewer_list extends module {
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 	 	$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$tpl = arco::$view->get_templates_for($this);
 		$lang = arco::$lang->get_lang_for($this);
 		$input = arco::$engine->get_input_for($this);
 		
 		$template = $tpl->head();
 		
		// --------------------------------
		// Show pages
		// --------------------------------
		$qry = arco::$db->select("pages","*");
		while($qry->next) {
			$bQry = arco::$db->select("page_modules","*","WHERE page_id = {$qry->id}");
			$modules = array();
			while($bQry->next) {
				$modules[] = $tpl->module($bQry->plugin,$bQry->instance,$bQry->module,$bQry->name);
			}
			$template .= $tpl->row(
				$qry->page_name,
				$qry->structure,
				$qry->title,
				intval($qry->locked) ? "yes" : "no",
				implode(" ",$modules)." ".arco::$view->main->hyperlink(
					ARL::in()->nxt($this)->shift_to("edit")->ap("page",$qry->id)->url,"edit"
					)
				);
		}
		$template .= $tpl->foot(arco::$view->main->hyperlink(
			ARL::in()->nxt($this)->shift_to("create")->url,
			"create page"
		));
		
		return $template;
 	}
 }
?>
