<?php
/*
 * Created on 18.09.2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - PHPeclipse - PHP - Code Templates
 */
 
  /**
  * Plugin management
  * Administrative plugin
  */
 class adm_plugin_viewer extends plugin {
 	public $plugin_modules = array("list","install","create_instance","dummy");
 }
 
 class adm_plugin_viewer_create_instance extends module {
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
 		
 		if (!$input->node) $input->node = "form";
		
		switch($input->node) {
			case "form":
				return $tpl->form($input->pluginName);
			case "make":
				arco::$engine->make_plugin_instance($input->pluginName,$input->instanceName);
		 		$template = "<div>Instance '{$input->instanceName}' created.</div>";
		 		arco::$engine->redirect_delay(arco::$engine->currentSite, 2, $template);
				//return $lang->success;
		}
 	}
 }
 
 class adm_plugin_viewer_install extends module {
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
 		
 	 	arco::$engine->install_plugin($input->plugin);
 		$template = "<div>plugin '{$input->plugin}' installed.</div>";
 		arco::$engine->redirect_delay(arco::$engine->currentSite,2,$template);
 	}
 }
 
  class adm_plugin_viewer_dummy extends module {
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 	 	$this->init();
 		$input = arco::$engine->get_input_for($this);
 		
 	 	return "";
 	}
 }
 
 class adm_plugin_viewer_list extends module {
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
 		
 		$template = "";
 		
		// --------------------------------
		// Show available plugins
		// --------------------------------
		$plugins = arco::$engine->list_plugins();
		$plugins_installed = array_keys(arco::$engine->plugins);
		$plugin_instances = arco::$engine->plugins;
		
		$template .= $tpl->head();
		
		$dummymodule = $this->getParentPage()->modules['content'][0];
		
		foreach($plugins as $plugin) {
			if (in_array($plugin,$plugins_installed)) {
				$installed = "yes";
				$instances = implode(", ",$plugin_instances[$plugin]['instances'])." ";
				$instances .= arco::$view->main->hyperlink(ARL::in()->nxt($dummymodule)->shift_to("create_instance")->ap("pluginName",$plugin)->url,$lang->create);
			}
			else {
				$installed = arco::$view->main->hyperlink(ARL::in()->nxt($dummymodule)->shift_to("install")->ap("plugin",$plugin)->url,$lang->install);
				$instances = "not installed";
			}
			$template .= $tpl->row($plugin,$installed,$instances);
		}
		
		$template .= $tpl->foot();
		
		return $template;
 	}
 }
?>
