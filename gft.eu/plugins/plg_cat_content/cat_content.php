<?php
/*
 * Created on 07.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 // TODO: publish boolean
 
 /**
  * Categorized content plugin
  */
 class cat_content extends plugin {
 }
 
 class cat_content_content extends module {
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
 		// --------------------------------
		// Show content page
		// --------------------------------
		$navigation = $this->make_nav($this->input->content);
		$template = $tpl->head($navigation);
		$res = $db->contents->select("name,content,icon","WHERE content_id = ".$this->input->content);
		$template .= $tpl->content($res->name,arco::$libs->convert_to_html($res->content),$res->iconurl);
		$template .= $tpl->foot($navigation);
		return $template;
 	}
 	
  	public function make_nav($id,$name = "") { // recursive
 		$data = $this->db->cats->select("name, id","WHERE parent_id = '$id'")->next;
 		if ($id == 0) {
 			// --------------------------------
			// Reached end of recursion
			// --------------------------------
 			$nav = $this->tpl->nav_element($this->lang->home,ARL::in()->ap_f($this,'cat',0)->url);
 			return $nav;
 		}
 		else {
 			$nav = $this->tpl->nav_element($name,ARL::in()->ap_f($this,'cat', $id)->url);
 		}
 		return $this->make_nav($data['cat_id'],$data['name']).$nav;
 	}
 }
 
 class cat_content_list extends module {
 	private $tpl;
 	private $lang;
 	private $db;
 	private $input;
 	
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 		$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$this->tpl = arco::$view->get_templates_for($this);
 		$this->lang = arco::$lang->get_lang_for($this);
 		$this->db = arco::$db->get_driver_for($this);
 		$this->input = arco::$engine->get_input_for($this); 
 		// --------------------------------
		// Show content page
		// --------------------------------
 		if ($this->input->content) { // fixed
 			logger::log("Shifter doesn't work. Shifting directly to cat_content_content module...");
 			return $this->plugin->mod_content->make_html();
 		}
 		// --------------------------------
		// Show List-page of contents and categorys in this category
		// --------------------------------
 		else {
	 		if (!$this->input->cat) $this->input->cat = 0;
	 		if (!preg_match('/^\d+$/',$this->input->cat)) $this->input->cat = 0;
	 		$navigation = $this->make_nav($this->input->cat);
	 		$template = $this->tpl->head($navigation);
	 		// --------------------------------
			// Get categorys
			// --------------------------------
	 		$res = $this->db->cats->select("*","WHERE parent_id = '".$this->input->cat."'");
	 		while($res->next) {
	 			$template .= $this->tpl->row($res->name,ARL::in()->ap_f($this,'cat',$res->id)->url,$res->icon,$res->description);
	 		}
	 		// --------------------------------
			// Get contents
			// --------------------------------
	 		$res = $this->db->contents->select("*","WHERE parent_id = '".$this->input->cat."'");
	 		while($res->next) {
	 			$template .= $this->tpl->row($res->name,ARL::in()->nxt($this)->ap('content',$res->content_id)->shift_to("content")->url,$res->icon,$res->content);
	 		}
	 		$template .= $this->tpl->foot($navigation);
	 		return $template;
 		}
 	}
 	
 	public function make_nav($id,$name = "") { // recursive
 		$data = $this->db->cats->select("name, id","WHERE parent_id = '$id'")->next;
 		if ($id == 0) {
 			// --------------------------------
			// Reached end of recursion
			// --------------------------------
 			$nav = $this->tpl->nav_element($this->lang->home,ARL::in()->ap_f($this,'cat',0)->url);
 			return $nav;
 		}
 		else {
 			$nav = $this->tpl->nav_element($name,ARL::in()->ap_f($this,'cat', $id)->url);
 		}
 		return $this->make_nav($data['cat_id'],$data['name']).$nav;
 	}
 }
?>