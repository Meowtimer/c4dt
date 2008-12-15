<?php
/*
 * Created on 30.07.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 //include("mYsql.hook.php"); // hook mysql_main
 
 include("mysql_operations.inc.php");
 include("mysql_table.inc.php");
 
 define("ARCOSQL_RAW",1);
 define("ARCOSQL_NUMBER",2);
 define("ARCOSQL_TEXT",3);
 define("ARCOSQL_LANGTEXT",4);
 define("ARCOSQL_HTML",5);
 define("ARCOSQL_ARRAY",6);
 define("ARCOSQL_URL",7);
 define("ARCOSQL_RESOURCE",8);
 define("ARCOSQL_IMAGE", 9);
 define("ARCOSQL_NULLLANG","nulllang");
 
 define("ARCOSQL_PRODUCT_DATABASE","usr_web6_2_one");
 define("ARCOSQL_DEVELOPMENT_DATABASE","usr_web6_2_two");
 
 class mysql extends library {
	
	public static $registeredTableNames = array("select","update","delete","insert","join","alter","drop","ignore");
	public static $registeredOperations = array("join");
	
	public static function GetMethods() {
		return array();
	}
	
	public static function GetClasses() {
		return array();
	}
	
	public static function GetSuperObjects() {
		$obj = new mysql_main;
		return array("db" => $obj); 
	}
 }

 class MySQLException extends ArcoException {
	public function __construct($message,$query) {
		
		$message = "MySQL Exception:\n" . $message;
		
//		$message .= "\nQuery: ".$query; // TODO: COMMENT IN PUBLIC VERSION!
		
		parent::__construct($message,0);
	}
 }

 /**
  * MySQL Interface
  * mYsql overlay
  * @version 0.7
  */
 class mysql_main {
	
	public $serverhost = "localhost";
	public $database;
	public $username = "web6_1";
	public $password = "4rc7ic5d3";
	
	public $connection;
	
	public $queryCount = 0;
	public $currentQueryId;
	public $currentQuery;
	public $lastQueryStatement;
	
	public $defaultMaxPacketSize = 1048576;
	
	public $primarys = array();
	
	public $hookdata = array('arcosql' => array('column_name' => 1, 'data_type' => 2));
	
	public function __construct($dbname = false) {
		define("SQL_PREFIX","adce_");
		ini_set("mysql.max_allowed_packet",1024 * 1024 * 8);
		
		if ($dbname === false) {
			if (file_exists("RELEASE")) {
				$this->database = ARCOSQL_PRODUCT_DATABASE;
			}
			else {
				$this->database = ARCOSQL_DEVELOPMENT_DATABASE;
			}
		}
		else {
			$this->database = $dbname;
		}
		
		$this->connection = @mysql_connect($this->serverhost,$this->username,$this->password);
		if (!$this->connection) {
			throw new MySQLException("Could not connect to MySQL Server.","connect");
		}
		if ($dbname !== true) { 
			if (!mysql_select_db($this->database,$this->connection)) {
				throw new MySQLException("Could not select database.","connect");
			}
		}
		mysql_set_charset("latin1",$this->connection);
	}
	
	public function init() {
		// --------------------------------
		// Overlay initialization
		// --------------------------------
	 	$res = $this->select("arcosql","*");
 		while($array = mysql_fetch_array($res->query_id)) { // use low level functions due to the hook of all high level methods
 			$this->hookdata[ $array['table_name'] ][ $array['column_name'] ] = $array['data_type'];
 		}
	}
	
	public function query($par, $raw = false) {
		if ($par instanceof mysql_query) {
			$query = $par;
			$sql = $par->queryStatement;
		}
		else {
			$sql = $par;
			$query = new mysql_query($raw);
			$query->queryStatement = $sql;
		}
		$this->lastQueryStatement = $sql;
		$query->queryId = mysql_query($sql, $this->connection);
		$this->queryCount++;
		if (!$query->queryId) {
			throw new MySQLException(mysql_error(),$sql);
		}
		$this->currentQuery = new mysql_result($query);
		return $this->currentQuery;
	}
	
	/**
	 * MySQL select query
	 * @param mysql_table_collection tables
	 * @param string fieldSelection
	 * @param string additional
	 * @return mysql_result results
	 */
	public function select() {
		$query = new mysql_query();
		$args = func_get_args();
		return call_user_func_array(array($query,"select"),$args);
	}
	
	/**
	 * MySQL update query
	 * @param mysql_table_collection tables
	 * @param array updateData
	 * @param string additional
	 * @return mysql_result/boolean result/success
	 */
	public function update() {
		$query = new mysql_query();
		$args = func_get_args();
		return call_user_func_array(array($query,"update"),$args);
	}
	
	/**
	 * MySQL insert query
	 * @param mysql_table_collection tables
	 * @param array insertData
	 * @return  mysql_result result (e.g. insert ID)
	 */
	public function insert() {
		$query = new mysql_query();
		$args = func_get_args();
		return call_user_func_array(array($query,"insert"),$args);
	}
	
	/**
	 * MySQL delete query
	 * @param mysql_table_collection tables
	 * @param string additional
	 * @return mysql_result results
	 */
	public function delete() {
		$query = new mysql_query();
		$args = func_get_args();
		return call_user_func_array(array($query,"delete"),$args);
	}
	
	/**
	 * Creates a plugin specific database object
	 * @param module/plugin object
	 * @return object specific database
	 */
	public function get_driver_for($object) {
		if (!$object instanceof module && !$object instanceof plugin) throw new ArcoException("Given parameter is not a plugin or a module.");
		$pluginName = $object->getName();
		if (!array_key_exists($pluginName,arco::$engine->plugins)) throw new ArcoException("Cannot get MySQL driver for unknown plugin '".$pluginName."'.");
		$tableCollection = new mysql_table_collection();
		foreach(arco::$engine->plugins[$pluginName]['plg_tables'] as $tableAlias) {
			$tableCollection->add(mysql_table::create_for($tableAlias, $object));
		}
		$sub = new mysql_sub($tableCollection);
		$sub->instanceName = $object->getInstance();
		$sub->pluginName = $pluginName;
		$sub->parentObj = $object;
		return $sub;
	}
	
	/**
	 * Converts long table strings to short ones
	 * adce_cat_content_cat_table_products_dump -> cat_table
	 * @param string longTable
	 * @return string shortTable
	 * @deprecated 0.5 - 30.12.2006
	 */
	public function get_table_root($longTable, $withPlugin = false) { // TODO: tableRoot caching
		$parts = explode("_", str_replace(SQL_PREFIX, "", $longTable));
		if (in_array(implode("_",$parts),array_keys($this->hookdata))) return implode("_",$parts);
		// --------------------------------
		// Find plugin
		// --------------------------------
		try {
//			print_r(array_keys(arco::$engine->plugins));
			$pluginLength = $this->find_table_part(array_keys(arco::$engine->plugins), $parts);
			$plugin = implode("_",array_slice($parts, 0, $pluginLength));
		}
		catch(NotFoundException $e) {
			throw new MySQLException("Unable to extract plugin information from table string '".$e->getSearchItem()."'.", $longTable);
		}
		// --------------------------------
		// Find root table
		// --------------------------------
		try {
			$parts = array_slice($parts,2);
			$tableLength = $this->find_table_part(arco::$engine->plugins[$plugin]['plg_tables'], $parts);
			$table = implode("_",array_slice($parts, 0, $tableLength));
			return ($withPlugin ? $plugin."_" : "").$table;
		}
		catch(NotFoundException $e) {
			throw new MySQLException("Unable to extract table information from table string '".$e->getSearchItem()."' for plugin '$plugin'.", "table");
		}
	}
	
	/**
	 * @deprecated 0.5 - 30.12.2006
	 */
	private function find_table_part($haystack, $array) {
		$str = "";
		for($x = count($array);$x > 0;$x--) {
			$str = implode("_", array_slice($array, 0, $x));
			if (in_array($str,$haystack)) return $x;
		}
		throw new NotFoundException("Unable to extract table information.", implode("_",$array));
	}
	
	public function make_safe($string) {
		return str_replace('\"','"',mysql_escape_string($string));
	}
	
	public function find_field($field,$tables) {
		if (!is_array($tables) and !$tables instanceof mysql_table_collection) throw new MySQLException("Given argument is not an array. (".$tables.") for field ($field)", "no query");
		foreach($tables as $table) {
			if (!is_array($this->hookdata[$table->getForHook()])) throw new ArcoException("Hookdata is not available for table '$table'.");
			if (in_array($field,array_keys($this->hookdata[$table->getForHook()]))) return $table;
		}
		return null;
	}
	
 }

 /**
  * This class provides the possibility to have several querys at the same time.
  * It is especially necessary for language columns.
  */
 class mysql_query {
	public $lastTables;
	public $queryId;
	public $queryStatement;
	public $raw;
	
	public function __construct($raw = false) {
		$this->raw = $raw;
	}
	
	public function fetch_row($query_id = FALSE) {
 		$types = array();
 		if (!$query_id) $query_id = $this->queryId;
 		if (!$query_id) throw new MySQLException("Invalid query. Cannot fetch result array!", $this->queryStatement);
 		// --------------------------------
		// Fetch
		// --------------------------------
 		$array = mysql_fetch_array($query_id, MYSQL_ASSOC);
 		if (!is_array($array)) return null;
 		if (!$this->raw) {
	 		// --------------------------------
			// Overlay it!
			// --------------------------------
			$array = $this->parse_overlay($array, $query_id);
 		}
		return $array;
	}
	
	/**
	 * Selects rows from tables
	 * @param mixed $table
	 * @param string $select
	 * @param string $additional
	 */
	public function select($table, $select, $additional = "") {
		if (strlen($additional) > 0) $additional = " ".$additional;
		if (is_array($table)) { // $db->select(array("tab1","tab2"))
			$tableCollection = new mysql_table_collection();
			foreach($table as $e) {
				$tableCollection->add(new mysql_table($e));
			}
			$this->lastTables = $tableCollection;
			$tables = $tableCollection->getAllForSQL();
		}
		elseif ($table instanceof mysql_table_collection) { /** @since 0.5 - 30.12.2006 */  // $subdb->table->select 
			$this->lastTables = $table;
			$tables = $table->getAllForSQL();
		}
		elseif ($table instanceof mysql_operation) { // $db->select(new mysql_join())
			$this->lastTables = $table->getTables();
			$tables = $table->getCommandString();
		}
		elseif(is_object($table)) {
			throw new MySQLException("What on earth is a '".get_class($table)."'?", "error occured during query generation");
		}
		else { // $db->select("globalTable",)
			$this->lastTables = new mysql_table_collection(new mysql_table($table));
			$tables = $this->lastTables->getAllForSQL();
		}
		$this->queryStatement = "SELECT $select FROM ".$tables.$additional;
		$result = arco::$db->query($this);
		return $result;
	}
	
	public function update($table, $hashtable, $additional = "") {
		if (strlen($additional) > 0) $additional = " ".$additional;
		if (!is_array($hashtable)) throw new MySQLException("Given hashtable is not an array.", "error occured during query generation");
		if (is_array($table)) { // $db->update(array("tab1","tab2"))
			$tableCollection = new mysql_table_collection();
			foreach($table as $e) {
				$tableCollection->add(new mysql_table($e));
			}
			$this->lastTables = $tableCollection;
			$tables = $tableCollection->getAllForSQL();
			$table = $tableCollection;
		}
		elseif ($table instanceof mysql_table_collection) { /** @since 0.5 - 30.12.2006 */  // $subdb->table->update 
			$this->lastTables = $table;
			$tables = $table->getAllForSQL();
		}
		elseif(is_object($table)) { // $subdb|db->update(bullshit, )
			throw new MySQLException("What on earth is a '".get_class($table)."'?", "Update-Hashtable: ".print_r($hashtable,true));
		}
		else { // $db->update("globalTable",)
			$table = new mysql_table($table);
			$this->lastTables = new mysql_table_collection($table);
			$table = $this->lastTables;
			$tables = $this->lastTables->getAllForSQL();
		}
		
		// --------------------------------
		// Overlay
		// --------------------------------
	 	$array = $this->create_update_overlay($hashtable, $table, $additional);
	 	if (!count($array)) return true;
		// --------------------------------
		// Query construction
		// --------------------------------
		$this->queryStatement = "UPDATE IGNORE ".$tables." SET ";
		$values = array();
		foreach($array as $key => $value) {
			$tmp = "`$key` = ";
			if (is_array($value)) $value = serialize($value);
			if (substr($value,0,1) != "`") // allow `order` = `order` + 1 
				$value = is_numeric($value) ? $value : arco::$libs->str_enclose($value, true);
			$tmp .= $value;
			$values[] = $tmp;
		}
		$this->queryStatement .= implode(", ",$values);
		$this->queryStatement .= $additional;
		$result = arco::$db->query($this);
		return $result;
	}
	
	/**
	 * @param string $table
	 * @param array $hashtable
	 * @param string $additional
	 */
	public function insert($table, $hashtable, $additional = "") {
		if (strlen($additional) > 0) $additional = " ".$additional;
		if (is_array($table)) { // $db->insert(array("tab1","tab2"))
			$tableCollection = new mysql_table_collection();
			foreach($table as $e) {
				$tableCollection->add(new mysql_table($e));
			}
			$this->lastTables = $tableCollection;
			$tables = $tableCollection->getAllForSQL();
			$table = $tableCollection;
		}
		elseif ($table instanceof mysql_table_collection) { /** @since 0.5 - 30.12.2006 */  // $subdb->table->insert 
			$this->lastTables = $table;
			$tables = $table->getAllForSQL();
		}
		elseif(is_object($table)) { // $subdb|db->insert(bullshit, )
			throw new MySQLException("What on earth is a '".get_class($table)."'?", "Update-Hashtable: ".print_r($hashtable,true));
		}
		else { // $db->insert("globalTable",)
			$table = new mysql_table($table);
			$this->lastTables = new mysql_table_collection($table);
			$tables = $this->lastTables->getAllForSQL();
			$table = $this->lastTables;
		}
		// --------------------------------
		// MySQL alias-insert-bug(or feature) workaround
		// --------------------------------
		$tables = preg_replace("/ AS [`\w\d_]+/","",$tables);
		// --------------------------------
		// Overlay
		// --------------------------------
	 	$array = $this->create_insert_overlay($hashtable, $table, $additional);
		// --------------------------------
		// Query construction
		// --------------------------------
		$this->queryStatement = "INSERT INTO ".$tables." ";
		$keys = array();
		$values = array();
		foreach($array as $key => $value) {
			if (is_array($value)) $value = serialize($value);
			$keys[] = "`".$key."`";
			$values[] = is_numeric($value) ? $value : arco::$libs->str_enclose($value, true);
		}
		$this->queryStatement .= "(".implode(", ",$keys).") VALUES (".implode(", ",$values).")";
		$res = arco::$db->query($this);
		$res->insertId = mysql_insert_id(arco::$db->connection);
		return $res;
	}
	
	public function delete($table, $additional = "") {
		if (strlen($additional) > 0) $additional = " ".$additional;
		
		if (is_array($table)) { // $db->select(array("tab1","tab2"))
			$tableCollection = new mysql_table_collection();
			foreach($table as $e) {
				$tableCollection->add(new mysql_table($e));
			}
			$this->lastTables = $tableCollection;
			$tables = $tableCollection->getAllForSQLDelete();
		}
		elseif ($table instanceof mysql_table_collection) { /** @since 0.5 - 30.12.2006 */  // $subdb->table->select 
			$this->lastTables = $table;
			$tables = $table->getAllForSQLDelete();
		}
		elseif ($table instanceof mysql_operation) { // $db->select(new mysql_join())
			$this->lastTables = $table->getTables();
			$tables = $table->getCommandString();
		}
		elseif(is_object($table)) {
			throw new MySQLException("What on earth is a '".get_class($table)."'?", "error occured during query generation");
		}
		else { // $db->select("globalTable",)
			$this->lastTables = new mysql_table_collection(new mysql_table($table));
			$tables = $this->lastTables->getAllForSQLDelete();
		}
		
		/** @deprecated 0.4 - 09.04.2007 */
		/*
		if (is_array($table)) {
			$tables = array();
			foreach($table as $alias => $tab) {
				$tab = SQL_PREFIX.$tab;
				if (!is_numeric($alias)) $tables[] = $tab." AS ".$alias;
				else $tables[] = $tab;
			}
			$tables = implode(", ",$tables);
		}
		else $tables = SQL_PREFIX.$table;
		*/
		$this->queryStatement = "DELETE FROM ".$tables.$additional;
		return arco::$db->query($this)->query_id;
	}
	
	private function parse_overlay($array,$query_id) {
 		foreach(array_keys($array) as $field) { // used array_keys to avoid a copy of the whole array
 			$table =  mysql_field_table($query_id,$field); // this function is really crazy.. � la "alias.. realname.. does it really matter?"
 			// --------------------------------
			// Convert $table to table root if not already done so by mysql_field_table
			// --------------------------------
 			if ($this->lastTables->$table) { // convert by cache | wenn das shice teil mirn alias gibt
 				if ($table == "data") {
// 					print_r($this->lastTables);
 				}
 				$table = $this->lastTables->$table;
 				if ($table === null) throw new Exception("dumm oder gar dumm?");
 				$table = $table->getForHook();
 			}
 			else { // wenn das shice teil mirn complete gibt
 				try {
 					//if ($table == "data") print_r($this->lastTables);
 					$table = arco::$db->get_table_root($table, true); // convert by force
 				}
 				catch (MySQLException $e) {
 					// --------------------------------
					// It is possible that mysql_field_table returns the alias 
					// so $table is already the root table
					// --------------------------------
 					if (!array_key_exists($table,arco::$db->hookdata)) throw $e;
 				}
 			}
 			//print_r($this->lastTables);
 			if (!array_key_exists($table,arco::$db->hookdata)) throw new MySQLException("Unknown mYsql table '$table'.",$this->queryStatement);
 			if (!array_key_exists($field,arco::$db->hookdata[$table])) logger::log("Unknown mYsql column '$field' in table '$table'");
 			$type = arco::$db->hookdata[ $table ][ $field ];
 			switch($type) {
 				default:
 					logger::log("Unknown datatype '$type'! Regarding as RAW.");
 				case ARCOSQL_RAW:
   				case ARCOSQL_URL:
    			case ARCOSQL_RESOURCE:
// 					$array[$field] = $array[$field];
 					break;
  				case ARCOSQL_IMAGE:
  					logger::log("Loading resource ".$array[$field].".");
 					$array[$field] = resource::load($array[$field]);
 					break;
 				case ARCOSQL_HTML:
 					$dummy = new dummy_template();
 					$dummy->template = $array[$field];
 					$array[$field] = $dummy;
 					break;
 				case ARCOSQL_NUMBER:
					$array[$field] = intval($array[$field]);
 					break;
 				case ARCOSQL_TEXT:
					$array[$field] = $array[$field];
 					break;
  				case ARCOSQL_LANGTEXT:
					$array[$field] = arco::$lang->filter($array[$field]); // the deeper sense of mYsql-overlay
 					break;
  				case ARCOSQL_ARRAY:
					$array[$field] = unserialize($array[$field]);
 					break;
 			}
 		}
 		return $array;
	}
	
	private function create_insert_overlay($array, $tables) {
 		foreach(array_keys($array) as $field) { // used array_keys to avoid a copy of the whole array
 			if (is_numeric($field)) throw new MySQLException("You have to use an associative array to insert data into a mYsql table! E.g. 'colName' => 'value'",'stopped building query');
 			$table = arco::$db->find_field($field,$tables);
 			if ($table === NULL) throw new MySQLException("Cannot match field '$field' to any table.",$this->queryStatement);
 			if (!array_key_exists($table->getForHook(),arco::$db->hookdata)) logger::log("Unknown mYsql table '".$table->getForHook()."'.");
 			if (!array_key_exists($field,arco::$db->hookdata[$table->getForHook()])) logger::log("Unknown mYsql column '$field' in table '".$table->getForHook()."'");
 			$type = arco::$db->hookdata[ $table->getForHook() ][ $field ];
 			switch($type) {
 				default:
 					logger::log("Unknown datatype '$type'! Regarding as RAW.");
 				case ARCOSQL_RAW:
   				case ARCOSQL_URL:
    			case ARCOSQL_RESOURCE:
//					$array[$field] = $array[$field];
 					break;
  				case ARCOSQL_IMAGE:
  					$array[$field] = $array[$field]->saveInDatabase();
  					break;
 				case ARCOSQL_HTML:
 					if ($array[$field] instanceof dummy_template) {
 						$array[$field] = $array[$field]->template;
 					}
 					break;
 				case ARCOSQL_NUMBER:
					$array[$field] = intval($array[$field]);
 					break;
 				case ARCOSQL_TEXT:
					$array[$field] = arco::$libs->make_safe($array[$field]);
 					break;
 				case ARCOSQL_LANGTEXT:
 					if ($array[$field] == ARCOSQL_NULLLANG) $id = 0;
 					else $id = arco::$lang->create_lang_item(arco::$engine->currentLang,$array[$field]);
 					$array[$field] = $id;
 					break;
  				case ARCOSQL_ARRAY:
					$array[$field] = serialize($array[$field]);
 					break;
 			}
 		}
 		return $array;
	}
	
	private function create_update_overlay($array, $tables, $additional) {
 		foreach(array_keys($array) as $field) { // used array_keys to avoid a copy of the whole array
 			if (is_numeric($field)) throw new MySQLException("You have to use an associative array to insert data into a mYsql table! E.g. 'colName' => 'value'",'stopped building query');
 			$table = arco::$db->find_field($field,$tables);
 			if ($table === NULL) throw new MySQLException("Cannot match field '$field' to any table.",$this->queryStatement);
 			if (!array_key_exists($table->getForHook(),arco::$db->hookdata)) logger::log("Unknown mYsql table '".$table->getForHook()."'.");
 			if (!array_key_exists($field,arco::$db->hookdata[$table->getForHook()])) logger::log("Unknown mYsql column '$field' in table '".$table->getForHook()."'.");
 			$type = arco::$db->hookdata[ $table->getForHook() ][ $field ];
 			switch($type) {
 				default:
 					logger::log("Unknown datatype '$type'! Regarding as RAW.");
 				case ARCOSQL_RAW:
   				case ARCOSQL_URL:
    			case ARCOSQL_RESOURCE:
// 					$array[$field] = $array[$field];
 					break;
  				case ARCOSQL_IMAGE:
  					// --------------------------------
					// Get resource id
					// --------------------------------
					$r = arco::$db->select(new mysql_table_collection($table), $field, $additional);
					$a = mysql_fetch_array($r->query_id);
					$id = $a[$field];
					// --------------------------------
					// Overwrite resource
					// --------------------------------
					arco::$db->update("resources",array('image' => $array[$field]->blob),"uresid = $id");
					$array[$field] = $id;
  					break;
  				case ARCOSQL_HTML:
 					if ($array[$field] instanceof dummy_template) {
 						$array[$field] = $array[$field]->template;
 					}
 					break;
 				case ARCOSQL_NUMBER:
					if (substr($array[$field],0,1) != "`") $array[$field] = intval($array[$field]);
 					break;
 				case ARCOSQL_TEXT:
					$array[$field] = arco::$libs->make_safe($array[$field]);
 					break;
  				case ARCOSQL_LANGTEXT: // 3 subquerys -> 3 querys for each update col + main query
  					// --------------------------------
					// Get lang id
					// --------------------------------
  					$r = arco::$db->select(new mysql_table_collection($table), $field, $additional);
  					$a = mysql_fetch_array($r->query_id);
  					$id = $a[$field];
  					// --------------------------------
					// Edit lang field referenced by lang id and current language
					// --------------------------------
  					$array[$field] = arco::$lang->edit_lang_item($id, arco::$engine->currentLang, $array[$field]);
 					break;
  				case ARCOSQL_ARRAY:
					$array[$field] = serialize($array[$field]);
 					break;
 			}
 		}
 		return $array;
	}
	
 }

 class mysql_sub  {
	private $connection;
	
	public $instanceName;
	public $pluginName;
	public $tables = array();
	public $parentObj;
	protected $currentTables;
	private $operation;
	
	public function __construct($tables) {
		if (!is_array($this->tables)) throw new ArcoException("Given argument to mysql_sub() is not an array!");
		$this->currentTables = new mysql_table_collection();
		$this->tables = $tables;
	}
	
	public function __get($getname) {
		if ($this->currentTables->$getname) return $this;
		if ($this->tables->$getname) 
			$this->currentTables->add(mysql_table::create_for(strtolower($getname),$this->parentObj));
		else throw new ArcoException("Unknown table '$getname'.");
		return $this;
	}
	
	/**
	 * @since 0.5 - 28.12.2006
	 */
	public function join($leftTable, $rightTable, $relations) {
		$leftTable = mysql_table::create_for($leftTable,$this->parentObj);
		$rightTable = mysql_table::create_for($rightTable,$this->parentObj);
		$this->operation = new mysql_join($leftTable, $rightTable, $relations);
		return $this;
	}
	
	/**
	 * @deprecated 0.5 - 30.12.2006
	 */
	private function form_table_for_this($table) {
		return $this->pluginName."_".$table."_".$this->instanceName;
	}
	
	public function select($select,$additional = "") {
		$res = arco::$db->select($this->operation ? $this->operation : $this->currentTables,$select,$additional);
		$this->currentTables = new mysql_table_collection();
		return $res;
	}
	
	public function update($hashtable,$additional = "") {
		$res = arco::$db->update($this->currentTables,$hashtable,$additional);
		$this->currentTables = new mysql_table_collection();
		return $res;
	}
	
	public function insert($hashtable,$additional = "") {
		$res = arco::$db->insert($this->currentTables,$hashtable,$additional);
		$this->currentTables = new mysql_table_collection();
		return $res;
	}
	
	public function delete($additional = "") {
		$res = arco::$db->delete($this->currentTables,$additional);
		$this->currentTables = new mysql_table_collection();
		return $res;
	}
 }
 
 class mysql_result {
	public $query_id;
	public $query;
//	public $result;
	private $primary;
	private $allfetched = false;
	public $last;
	public $insertId;
	
	public function __construct($query) {
		$this->query = $query;
		$this->query_id = $query->queryId;
	}
	
	public function __get($getname) {
		switch($getname) {
			case "all":
				$result = array();
				while ($array = $this->query->fetch_row($this->query_id)) {
					$result[] = $array;
				}
				$this->allfetched = true;
				return $result;
				break;
			case "next":
				if ($this->allfetched) return null;
				$this->last = $this->query->fetch_row($this->query_id);
				return $this->last;
				break;
			case "num":
				return mysql_num_rows($this->query_id);
				break;
			case "successful":
				if ($this->query_id) return true;
				else return false;
				break;
			default:
				return $this->last[$getname];
				break;
		}
	}
 }


?>
