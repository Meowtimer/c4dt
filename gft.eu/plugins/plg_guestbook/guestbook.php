<?php
/*
 * Created on 30.07.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
class guestbook extends plugin  {

	public $plugin_modules = array("inscribe","list");
	
}

class guestbook_inscribe extends module {
	
	public $plugin;
	
	public function init() {
	}
	
	public function make_html() {
		$this->init();
		$input = arco::$engine->get_input_for($this);
		
		if (!arco::$engine->input['node']) arco::$engine->input['node'] = "form";
		
		$tpl = arco::$view->get_templates_for($this);
		switch(arco::$engine->input['node']) {
			case "form":
				return $tpl->form();
			case "inscribe":
				$db = arco::$db->get_driver_for($this);
				$result = $db->insert("posts",array(
										'author' => arco::$engine->input['author'],
										'text' => arco::$engine->input['text'],
										'time' => time(),
										'ip' => $_SERVER['REMOTE_ADDR']
									));
				if ($result->successful) return arco::$lang->guestbook->success_inscribe;
				else throw new UserException(arco::$lang->guestbook->failed_inscribe);
		}

	}
	
}

class guestbook_list extends module {
	
	public $plugin;
	private $start;
	
	public function init() {
		
	}
	
	public function make_html() {
		$this->init();
		$input = arco::$engine->get_input_for($this);
		$db = arco::$db->get_driver_for($this);
		$tpl = arco::$view->get_templates_for($this);
		$this->start = intval($input->start ? $input->start : 0);
		$result = $db->posts->select("*","ORDER BY time DESC LIMIT ".$this->start.",10");
		$template = $tpl->head();
		while($result->next) {
			$result->last = arco::$libs->convert_to_html($result->last);
			$template .= $tpl->row($result->last['author'],$result->last['text'],date("d.m.Y H:i",$result->last['time']));
		}
		$template .= $tpl->foot();
		return $template;
	}
	
}
 
?>
