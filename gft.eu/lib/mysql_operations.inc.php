<?php
/*
 * Created on 28.12.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 abstract class mysql_operation {
 	abstract function getTables();
 }
 
 class mysql_join extends mysql_operation {
 	public $leftTable;
 	public $rightTable;
 	public $relations = array();
	public function __construct($leftTable,$rightTable,$relations) { // new mysql_join ()
		if ($leftTable instanceof mysql_table) $this->leftTable = $leftTable;
		else $this->leftTable = new mysql_table($leftTable);
		if ($rightTable instanceof mysql_table) $this->rightTable = $rightTable;
		else $this->rightTable = new mysql_table($rightTable);
		$this->relations = $relations;
	}
	
	public function getTables() {
		return new mysql_table_collection(array($this->leftTable,$this->rightTable));
	}
	
	public function getCommandString() {
		reset($this->relations);
//		for($x = 0;$x < count($this->tables);$x++) {
			list($leftRelation,$rightRelation) = each($this->relations);
			$str = $this->leftTable->getForSQL()." LEFT JOIN " .
				$this->rightTable->getForSQL()." ON " .
				$leftRelation." = ".$rightRelation;
			return $str;
//		}
	}
 }
?>