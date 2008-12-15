<?php
/*
 * Created on 01.01.2007
 *
 * To change the template for this generated file go to
 * Window - Preferences - PHPeclipse - PHP - Code Templates
 */
 
/**
 * Special table collection
 * @since 0.5 - 30.12.2006
 */
class mysql_table_collection implements Iterator {
	public $array = array();
	
	public function __toString() {
		return "Collection(mysql_table_collection)";
	}
	
	public function current() {
		return current($this->array);
	}
	
	public function next() {
		return next($this->array);
	}
	
	public function key() {
		return key($this->array);
	}
	
	public function rewind() {
		reset($this->array);
	}
	
	public function valid() {
		return $this->current() !== false;
	}
	
	public function __construct($param = false) {
		if ($param === null) return; // prevent exception if plugins do not have any tables
		if ($param !== false) $this->add($param);
	}
	
	public function add($param) {
		if (is_array($param)) {
			foreach($param as $par) {
				$this->_add($par);
			}
		}
		else {
			$this->_add($param);
		}
	}
	
	private function _add($oTable) {
		if (!$oTable instanceof mysql_table) throw new MySQLException("Unable to add object of type '".get_class($oTable)."' to mysql_table_collection.", print_r($this->array,true));
		$this->array[] = $oTable;
	}
	
	public function __get($getName) {
		foreach($this->array as $oTable) {
			if ($oTable->table == $getName) return $oTable;
			if ($oTable->getPluginTable() == $getName) return $oTable;
			if ($oTable->getLong() == $getName) return $oTable;
			if ($oTable->getComplete() == $getName) return $oTable;
		}
		return null;
	}
	
	public function getAllForSQLDelete() {
		$str = array();
		foreach($this->array as $oTable) {
			$str[] = "`".$oTable->getComplete()."`"; 
		}
		return implode(", ",$str);
	}
	
	public function getAllForSQL() {
		$str = array();
		foreach($this->array as $oTable) {
			$str[] = $oTable->getForSQL(); 
		}
		return implode(", ",$str);
	}
}

/**
 * Table-name class
 * Provides detailed table information
 * @since 0.5 - 30.12.2006
 */
class mysql_table {
	public $plugin;
	public $table;
	public $instance;
	public $isGlobal = false;
	
	public static function create_for($table,$object) {
		$table = new mysql_table($object->getName(),$table,$object->getInstance());
		return $table;
	}
	public function __toString() {
		return "Object(mysql_table:".$this->getLong().")";
	}
	
	public function __construct($p1, $p2 = false, $p3 = false) {
		if ($p3 && $p2) $this->isGlobal = false;
		else $this->isGlobal = true;
		if ($this->isGlobal) {
			$this->table = $p1;
		}
		else {
			$this->plugin = $p1;
			$this->table = $p2;
			$this->instance = $p3;
		}
		$this->table = strtolower($this->table);
	}
	
	public function getPluginTable() {
		return ($this->isGlobal ? "" : $this->plugin."_").$this->table;
	}
	
	public function getForHook() {
		return $this->getPluginTable();
	}
	
	public function getLong() {
		return ($this->isGlobal ? "" : $this->plugin."_").$this->table.($this->isGlobal ? "" : "_".strtolower($this->instance));
	}
	
	public function getComplete() {
		return SQL_PREFIX.$this->getLong();
	}
	
	public function getForSQL() {
		return "`".$this->getComplete()."` AS `".$this->table."`";
	}
	
	public function getShort() {
		return $this->table;
	}
	
	public function getRoot() {
		return $this->getShort();
	}
}
?>
