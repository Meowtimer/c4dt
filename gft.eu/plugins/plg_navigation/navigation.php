<?php
/*
 * Created on 08.09.2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - PHPeclipse - PHP - Code Templates
 */
 
 /**
  * Navigation menu plugin
  */
 class navigation extends plugin {
 	public $plugin_modules = array("menu","edit");
 }
 
 class navigation_edit extends module {
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
		
		$template = $tpl->head();
		
		// --------------------------------
		// Edit
		// --------------------------------
		if (is_array($input->link)) {
			foreach($input->text as $id => $text) {
				$db->items->update(array('text' => $text, 'link' => $input->link[$id], 'order' => $input->order[$id]),"WHERE id = $id");
			}
			$template .= $tpl->msg("Updated.");
		}
		
		// --------------------------------
		// Read out database
		// --------------------------------
		$r = $db->items->select("*");
		
		while($r->next) {
			$template .= $tpl->row($r->id, $r->text, $r->link, $r->parent,$r->order,$r->short_name);
		}
		
		$template .= $tpl->foot();
		
		return $template;
 	}
 }
 
 class navigation_menu extends module {
 	private $tpl;
 	private $menus = array();
 	private $depth = -1;
 	private $template;
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 		$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$this->tpl = arco::$view->get_templates_for($this);
 		$lang = arco::$lang->get_lang_for($this);
 		$this->db = arco::$db->get_driver_for($this);
 		$input = arco::$engine->get_input_for($this);	
		
		arco::$view->add_to_head('<script type="text/javascript" src="'.SKINS.arco::$engine->currentSkin.'/'.$this->getLowLevelName().'/menu.js"></script>');
		
		$this->template = $this->tpl->head();
		
		$this->make_menu(0);
		
		$this->template .= $this->tpl->foot();
		
		$noscript = $this->tpl->noscript();
		
//		$template = '<div id="menuanchor"></div>';
//		$template .= '<script type="text/javascript">/* <![CDATA[ */ initNav(); /* ]]> */ </script>';
		
		
		
//		$template = $this->tpl->javascript_init();
//		$this->menus[0] = "";
//		$this->make_menu(0);
//		
//		$template .= implode("",$this->menus);
//		
//		$template .= $this->tpl->javascript_execute();
		arco::$view->add_to_foot($this->template);
		return $noscript;
 	}
 	
  	/**
 	 * Recursive menu generator
 	 * Menus are identified by their parentId
 	 */
 	private function make_menu($parentId) {
 		$this->depth++;
		$r = $this->db->items->select("*","WHERE `parent` = $parentId ORDER BY `order`");
		if ($r->num == 0) {
			$this->depth--;
			return "";
		}
		if ($this->depth > 0) $this->template .= $this->tpl->js_head_menu($parentId,$this->depth);
		//$this->menus[$parentId] .= $this->tpl->injection($parentId);
		$todo = array();
		while($r->next) {
			$langs = explode("|",$r->displayinlang);
			if (!in_array(arco::$engine->currentLang,$langs)) continue;
			$r->template->template = str_replace(
				array('{ID}','{TEXT}', '{LINK}', '{SHORT_NAME}', '{ORDER}', '{PARENT}'),
				array($r->id, $r->text, $r->link, $r->text, $r->order, $r->parent),
				$r->template->template);
//			$this->menus[$parentId] .= "  ".$r->template->tpl();
//			$this->menus[0] .= "  ".$r->template->tpl();
			$this->template .= $this->tpl->js_row($parentId,$r->id,$r->template->tpl(),$r->link,$this->depth);
			$this->make_menu($r->id);
//			$todo[] = $r->id;
		}
		if ($this->depth > 0) $this->template .= $this->tpl->js_foot_menu();
//		foreach($todo as $id) $this->make_menu($id);
//		if ($this->depth == 0) $this->menus[0] .= $this->tpl->foot_first();
//		if ($this->depth == 1) $this->menus[0] .= $this->tpl->foot_second();
//		elseif ($this->depth > 1) $this->menus[0] .= $this->tpl->foot();
		$this->depth--;
 	}
 	
 	/**
 	 * Recursive menu generator
 	 * Menus are identified by their parentId
 	 */
 	private function make_menu_old($parentId) {
 		$this->depth++;
		$r = $this->db->items->select("*","WHERE `parent` = $parentId ORDER BY `order`");
		if ($r->num == 0) {
			$this->depth--;
			return "";
		}
		$this->menus[0] .= $this->tpl->section($parentId);
		if ($this->depth == 0) $this->menus[0] .= $this->tpl->head_first($this->tpl->injection($parentId));
		if ($this->depth == 1) $this->menus[0] .= $this->tpl->head_second($this->tpl->injection($parentId));
		elseif ($this->depth > 1) $this->menus[0] .= $this->tpl->head($this->tpl->injection($parentId));
		//$this->menus[$parentId] .= $this->tpl->injection($parentId);
		$todo = array();
		while($r->next) {
			$r->template->template = str_replace( // TODO: special custom template functionalities
				array('{ID}','{TEXT}', '{LINK}', '{SHORT_NAME}', '{ORDER}', '{PARENT}'),
				array($r->id, $r->text, $r->link, $r->short_name, $r->order, $r->parent),
				$r->template->template);
//			$this->menus[$parentId] .= "  ".$r->template->tpl();
			$this->menus[0] .= "  ".$r->template->tpl();
			$todo[] = $r->id;
		}
		foreach($todo as $id) $this->make_menu($id);
		if ($this->depth == 0) $this->menus[0] .= $this->tpl->foot_first();
		if ($this->depth == 1) $this->menus[0] .= $this->tpl->foot_second();
		elseif ($this->depth > 1) $this->menus[0] .= $this->tpl->foot();
		$this->depth--;
 	}
 }
?>
