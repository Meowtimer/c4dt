<?php
/*
 * Created on 07.06.2007
 *
 */
 
 class gallery extends plugin  {
 	public $plugin_modules = array("switcher","list","diashow","import","shifter");
 }
 
 class gallery_list extends module {
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 		$db = $this->db;
 		$tpl = $this->tpl;
 		$input = $this->input;
 		$lang = $this->lang;
 		
 		
 	}
 }
 
 class gallery_diashow extends module {
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 		
 	}
 }
 
 class gallery_switcher extends module {
 	
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 		$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$db = arco::$db->get_driver_for($this);
 		$tpl = arco::$view->get_templates_for($this);
 		$input = arco::$engine->get_input_for($this);
 		
 		// --------------------------------
		// Retrieve gallery data
		// --------------------------------
		$r = $db->items->select("*","ORDER BY `order`");
	
	 	$currentImage = $input->currentImage ? $input->currentImage : 1;
 		$prev = $currentImage == 1 ? false : $currentImage - 1;
 		$next = $currentImage + 1;
 		if ($next > $r->num) $next = false;
 		
 		if ($next !== false) $next = ARL::in()->nxt($this)->ap("currentImage",$next)->url;
 		if ($prev !== false) $prev = ARL::in()->nxt($this)->ap("currentImage",$prev)->url;
 		
 		// --------------------------------
		// Fetch current image
		// --------------------------------
		$currentImage = arco::$db->make_safe($currentImage);
		if (!is_numeric($currentImage)) throw new HackedException();
		
 		$r = $db->items->select("picture","WHERE `order` = ".$currentImage);
 		if (!$r->num) throw new UserException("no pics available");
		$imageData = $r->next;
		
		// --------------------------------
		// Apply gallery-instance options
		// --------------------------------
		$cfg = arco::$db->select("gallery_config","size_x, size_y","WHERE instance = '".$this->getInstance()."'")->next;
		$imageData['picture']->ensureOptions($cfg); // this automatically refreshes database
		
		// --------------------------------
		// Build templates
		// --------------------------------
		$template = $tpl->head();
		$template .= $tpl->row($prev,$next,$imageData['picture']->make_html('id="gallery_switcher" style="border: 1px solid;"'),$cfg['size_x'] + 14,$cfg['size_y']);
//		$template .= $tpl->foot($input->currentImage ? false : arco::$libs->to_direct_url($next));
		$template .= $tpl->foot(false);
 		
 		return $template;
 	}
 	
 }
 
 class gallery_shifter extends module {
 	public function init() {
// 		$this->requirePerm("admin");
 	}
 	
 	public function make_html() {
 		$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$db = $this->db;
 		$tpl = $this->tpl;
 		$input = $this->input;
 		$template = "";
 		
 	 	switch($input->action) {
 			case "del":
 				if (!is_numeric($input->id)) throw new UserException("<p>Invalid ID</p>");
 				$order = $db->items->select("`order`","WHERE id = ".arco::$db->make_safe($input->id))->next['order'];
 				$db->items->delete("WHERE id = ".arco::$db->make_safe($input->id));
 				$db->items->update(array("order" => "`order` - 1"),"WHERE `order` > $order");
 				$template .= "deleted.";
 			break;
 			case "up":
 				if (!is_numeric($input->id)) throw new UserException("<p>Invalid ID</p>");
 				$old = $db->items->select("`order`","WHERE id = $input->id")->next['order'];
 				if (!$old) throw new UserException("Unable to find picture with id '$input->id'.");
 				$new = $old - 1;
 				$db->items->update(array("order" => $new),"WHERE id = ".arco::$db->make_safe($input->id));
 				$db->items->update(array("order" => $old),"WHERE `order` = $new AND `id` != ".arco::$db->make_safe($input->id));
 				$template .= "up.";
 			break;
 			case "down":
 				if (!is_numeric($input->id)) throw new UserException("<p>Invalid ID</p>");
 				$old = $db->items->select("`order`","WHERE id = ".arco::$db->make_safe($input->id))->next['order'];
 				if (!$old) throw new UserException("Unable to find picture with id '$input->id'.");
 				$new = $old + 1;
 				$db->items->update(array("order" => $new),"WHERE id = ".arco::$db->make_safe($input->id));
 				$db->items->update(array("order" => $old),"WHERE `order` = $new AND `id` != ".arco::$db->make_safe($input->id));
 				$template .= "down.";
 			break;
 			case "insert":
 				if (!$input->order) throw new UserException("input->order does not exist or is 0.");
 				$path = RESOURCES."import/".$this->getInstance()."/";
 	 	 		foreach(glob($path."*") as $filename) {
		 			if (strstr($filename,"Thumbs.db")) continue;
		 			
		 			// --------------------------------
					// Server macht einen auf Sterbenden Schwan
					// --------------------------------
		 			$resHandler = resource::import($filename);
		 			$import[] = $resHandler;
		 		}
		 		// --------------------------------
				// Push follower down
				// --------------------------------
		 		$numNewResources = count($import);
		 		if (!$numNewResources) throw new UserException("There are no resources in the import directory($path).");
		 		$db->items->update(array("order" => "`order` + $numNewResources"),"WHERE `order` > ".arco::$db->make_safe($input->order));
		 		echo arco::$db->lastQueryStatement;
		 		// => UPDATE IGNORE `adce_gallery_items_furthermods` AS `items` SET `order` = 0 WHERE `order` > 23
		 		// --------------------------------
				// Insert new resources
				// --------------------------------
				
				$x = $input->order;
		 		foreach($import as $resHandler) {
		 			$db->items->insert(array('order' => ++$x,'picture' => $resHandler));
		 		}
		 		$template .= "inserted $numNewResources resource".($numNewResources == 1 ? "" : "s");
 			break;
 		}
		
 		$template .= $tpl->head();
 		
 		$r = $db->items->select("*","ORDER BY `order`");
 		
 		while($r->next) {
 			$delLink = $this->link("action=del&id=$r->id")->html("del");
 			$upLink = $this->link("action=up&id=$r->id")->html("up");
 			$downLink = $this->link("action=down&id=$r->id")->html("down");
 			$insertLink = $this->link("action=insert&order=$r->order")->html("insert here");
 			$imageHTML = $r->picture->make_html();
 			$template .= $tpl->row($delLink,$upLink,$downLink,$insertLink,$imageHTML);
 		}
 		
 		$template .= $tpl->foot();
 		
 		return $template;
 	}
 }
 
 class gallery_import extends module {
 	
 	public function init() {
 		$this->requirePerm("admin");
 	}
 	
 	public function make_html() {
 		$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$db = arco::$db->get_driver_for($this);
 		$tpl = arco::$view->get_templates_for($this);
 		$input = arco::$engine->get_input_for($this);
 		
 		// --------------------------------
		// Input error handling
		// --------------------------------
 		if (!$input->path) throw new UserException("Invalid input. Specify ".$this->getName()."_".$this->getInstance()."_path=");
 		if (!is_dir($input->path)) throw new UserException("Invalid Input. '$input->path' is not a directory.");
 		
 		// --------------------------------
		// to avoid server's getaway 
		// --------------------------------
// 		ini_set("max_allowed_packet","8MB");
 		
// 		arco::$db->query("SET SESSION max_allowed_packet = ".(1024 * 1024 * 32));
 		
 		// --------------------------------
		// Import
		// --------------------------------
 		$x = $db->items->select("`order`","ORDER BY `order` DESC LIMIT 1")->next['order'];
 		foreach(glob($input->path."*") as $filename) {
 			if (strstr($filename,"Thumbs.db")) continue;
 			$x++;
 			
 			// --------------------------------
			// Server macht einen auf Sterbenden Schwan
			// --------------------------------
 			$resHandler = resource::import($filename);
			$db->items->insert(array('order' => $x,'picture' => $resHandler));
 		}
 		
 		// --------------------------------
		// restore settings
		// --------------------------------
// 		ini_restore("mysql.max_allowed_packet");
 	}
 	
 }
 
?>
