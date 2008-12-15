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
 class adm_dbcopy extends plugin {
 	public $plugin_modules = array("show");
 }
 
 class adm_dbcopy_show extends module {
 	public function init() {
 		
 	}
 	
 	public $connection;
 	
 	public function make_html() {
 	 	$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$tpl = arco::$view->get_templates_for($this);
// 		$lang = arco::$lang->get_lang_for($this);
// 		$db = arco::$db->get_driver_for($this);
 		$input = arco::$engine->get_input_for($this);
		
 		if ($input->action) {
 			switch($input->action) {
 				case "pd":
 					$template = $this->copy(ARCOSQL_PRODUCT_DATABASE,ARCOSQL_DEVELOPMENT_DATABASE);
 					break;
 				case "dp":
 					
 					break;
 			}
 		}
 		else {
 			$pdlink = $this->link("action=pd");
 			$dplink = $this->link("action=dp");
 			$template = $tpl->content(ARCOSQL_PRODUCT_DATABASE,ARCOSQL_DEVELOPMENT_DATABASE,$pdlink,$dplink);
 		}
 		
 		return $template;
 	}
	
 	private function dropTables($tables) {
 		arco::$db->query("DROP TABLE ".implode(",",$tables));
 	}
 	
	private function copy($fromdbname,$todbname) {
		$tpl = "";
		
		// --------------------------------
		// Drop all tables in destination database
		// --------------------------------
		$result = arco::$db->query("SHOW TABLES FROM $todbname", true); // raw query
		while($result->next) {
			$tables[] = $todbname.$result->last["Tables_in_".$todbname];
			
		}
		$this->dropTables($tables);
		
		// --------------------------------
		// Get all tablenames of source database
		// --------------------------------
		$result = arco::$db->query("SHOW TABLES FROM $fromdbname", true); // raw query
		while($result->next) {
			$tables[] = $result->last["Tables_in_".$fromdbname];
		}
		
		// --------------------------------
		// Dump data and import data
		// --------------------------------
		foreach($tables as $table) {
			$queryString = "";
			
			// get scheme from source database
			$result = arco::$db->query("SHOW CREATE TABLE $fromdbname.$table", true);
			$result->next;
			$queryString = $result->last['Create Table']."\n";
			$queryString = str_replace("`$table`","`$todbname`.`$table`",$queryString);
			// create table by scheme in destination database
			arco::$db->query($queryString);
			$error = mysql_error();
			// check if table exists in destination database
			$result = arco::$db->query("SHOW TABLES FROM $todbname LIKE '$table'");
			if ($result->num == 0) {
				throw new ArcoException("Import failed: Could not create table '$table'.\n".$error);
			}
			// copy table rows from source to destination database
			$result = arco::$db->query("SELECT * FROM $fromdbname.$table", true); // raw query
			while($result->next) {
				$queryString = $this->createInsertQuery($result->last,$todbname.$table);
				arco::$db->query($queryString);
			}
		}
		
		return $tpl;
	}
	
	private function createInsertQuery($array,$tablename) {
		$columns = array();
		$values = array();
		foreach($array as $col => $value) {
			$columns[] = "`".$col."`";
			$values[] = "'".mysql_real_escape_string($value)."'";
		}
		
		$columns = implode(",",$columns);
		$values = implode(",",$values);
		
		$qry = "INSERT INTO $tablename ($columns) VALUES ($values)\n";
		return $qry;
	}
 	
 }
?>
