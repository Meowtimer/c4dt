<?php
/*
 * Created on 30.12.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
  /**
  * Simple content plugin
  */
 class content extends plugin {
 	public $plugin_modules = array("show", "edit");
 }
 
 class content_show extends module {
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 		$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$db = arco::$db->get_driver_for($this);
 		
 		$r = $db->data->select("tpl", "WHERE data_id = 'main_tpl'");
 		if (!$r->num) {
 			$db->data->insert(array('data_id' => "main_tpl",'data_content' => ARCOSQL_NULLLANG,'tpl' => ""));
 			$r = $db->data->select("tpl", "WHERE data_id = 'main_tpl'");
 		}
 		$tpl = $r->next['tpl'];
 		$dynamic_tpl =& $tpl->template;
 		$r = $db->data->select("*");
 		while($r->next) {
 			if ($r->data_id == "main_tpl") continue;
 			$dynamic_tpl = str_replace("{".$r->data_id."}",$r->data_content,$dynamic_tpl);
 		}
 		$dynamic_tpl = str_replace(array("{instanceName}","{pluginName}"),array($this->getInstance(),$this->getName()),$dynamic_tpl);
// 		$dynamic_tpl = str_replace(array("�","�","�","�","�","�","�","�","�"),array("&szlig;","&auml;","&ouml;","&uuml;","&Auml;","&Ouml;","&Uuml;","&sect;","&reg;"),$dynamic_tpl);
 		//$dynamic_tpl = $tpl->parse_template($dynamic_tpl);
 		return $tpl->tpl();
 		
 	}
 	
 }
 
 class content_edit extends module {
 	public function init() {
 		/*if (arco::$user->can_do("is_root")) {
 			throw new UserException("User is allowed to do this.");
 		}
 		else {
 			throw new UserException("User is NOT allowed to do this.");
 		}*/
 	}
 	
 	public function make_html() {
 		$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$db = arco::$db->get_driver_for($this);
 		$tpl = arco::$view->get_templates_for($this);
 		$input = arco::$engine->get_input_for($this);
 		$lang = arco::$lang->get_lang_for($this);
 		
 		if ($input->submit) {
 			if (!is_array($input->field_name)) $input->field_name = array();
 			foreach($input->field_name as $k => $name) {
 				if (!strlen($name)) {
 					$db->data->delete("WHERE id = $k");
 					continue;
 				}
// 				$input->field_content[$k] = iconv(arco::$engine->outputCharset,arco::$engine->charset,$input->field_content[$k]);
 				$db->data->update(array('data_id' => $input->field_name[$k], 'data_content' => $input->field_content[$k]),"WHERE id = $k");
 			}
 			$db->data->update(array('tpl' => $input->main_tpl),"WHERE data_id = 'main_tpl'");
 			if (is_array($input->new_field_name)) 
	 			foreach($input->new_field_name as $k => $name) {
//	 				$input->new_field_content[$k] = iconv(arco::$engine->outputCharset,arco::$engine->charset,$input->new_field_content[$k]); // TODO check if this is correct
	 				$db->data->insert(array('data_id' => $name,'data_content' => $input->new_field_content[$k]));
	 			}
 			arco::$engine->redirect_delay(ARL::last()->url,2,$lang->success);
 		}

 		// --------------------------------
		// Show edit form
		// --------------------------------
 		$pos = array();
		$r = $db->data->select("*");
		while($r->next) {
			if ($r->data_id == "main_tpl") $dynamic_tpl = arco::$libs->prepare_for_textarea($r->tpl->template);
			else $pos[] = $tpl->row($r->data_id,arco::$libs->prepare_for_textarea($r->data_content),$r->id);
		}
 		$html = $tpl->form($dynamic_tpl,implode("",$pos));
 		return $html;
 	
 	}
 }
?>
