<?php
/*
 * Created a long time ago(2005)
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */

if (!defined("IN_ARCO")) die("suchst du was?");

require("constants.inc.php"); // constants
include("exception.inc.php"); // exceptions
include("log.inc.php");       // log system
include("language.inc.php");  // language package system
include("module.inc.php");    // module interface
include("plugin.inc.php");    // plugin interface
include("arl.inc.php");       // Arco Resource Locator system
include("library.inc.php");   // library system
include("page.inc.php");      // page system
include("view.inc.php");      // The View (templates, structures, pages)
include("interfaces.inc.php");// Some interfaces
include("user.inc.php");      // user/session system

/**
 * Arco Virtual Machine
 * @package ArcoVM
 * @author ZokRadonh
 * @version alpha 0.4
 */
class engine {
	
	private $loaded_librarys = array();
	private $available_librarys = array();
	public $librarys_uptodate = false;
	private $superObjects = array();
	private $bootComplete;
	private $errorOccured = false;
	
	public $config = array();
	
	public $domain; // http://adce/
	public $path; // v2/
	
	public $charset = "ISO-8859-1"; // default charset
	public $outputCharset = "UTF-8";
	
 	public $currentSkin; // uglyCold
 	public $currentLang; // en
 	public $currentRequest; // arco_index.php?page=..........
 	public $currentSite; // ?page=welcome
 	public $currentPage; // welcome
	public $self; // arco_index.php?
	public $last; // arco_index.php?page=..................
	
	public $input = array();
	
	public $plugins = array(); // array with pluginname as key and value is the array that comes from mYsql
	public $pluginInstances = array();
	
	public function __construct() {
		$this->bootComplete = false;
		
	}
	
	/**
	 * starts the system and loads all the librarys and other configs
	 */
	public function boot() {
		global $libs, $lang, $view;
		
		date_default_timezone_set("Europe/Berlin");
		
		logger::log("++ Booting... (".$_SERVER['QUERY_STRING'].")");
		
		//error_reporting(E_ALL);
		
		$this->path = "";
		
		//set_error_handler(array($this,"AVMError"));
		
		$lang = new language();
		$libs = new libs();
		$view = new view();
		
		// --------------------------------
		// Load librarys
		// --------------------------------
		if (!file_exists(LIBS."cache.dat")) $this->list_libs();
		else $this->list_libs_from_cache();
		foreach($this->available_librarys as $lib) {
			$this->load_lib($lib);
		}
		
		// --------------------------------
		// Arco Class
		// --------------------------------
		$arco = "class arco {\n";
		foreach($this->superObjects as $name => $obj) {
			$arco .= "public static ".'$'."$name;\n";
		}
		$arco .= "public static ".'$libs'.";\n";
		$arco .= "public static ".'$engine'.";\n";
		$arco .= "public static ".'$lang'.";\n";
		$arco .= "public static ".'$view'.";\n";
		$arco .= "public static ".'$user'.";\n";
		$arco .= "}";
		
		// start arco class
		eval($arco); 
		foreach($this->superObjects as $name => $obj) {
			$GLOBALS[$name] = $obj;
			arco::${$name} = $obj;
		}
		arco::$libs = $libs;
		arco::$engine = $this;
		arco::$lang = $lang;
		arco::$view = $view;
		
		// --------------------------------
		// Initialize librarys
		// --------------------------------
		foreach($libs->auto_run_objects as $obj) {
			$obj->init();
		}
		
		// --------------------------------
		// Loading plugins
		// --------------------------------
		$result = arco::$db->select("plugins","*");
		if ($result->num < 1) logger::log("No installed plugin found.");
		else
		{
			while($result->next) {
				$this->plugins[$result->plg_name] = $result->last;
			}
		}
		// --------------------------------
		// Loading global config data
		// --------------------------------
		$result = arco::$db->select("main","*");
		while($result->next) {
			$this->config[$result->config_name] = $result->config_value;
		}
		$this->self = INDEXFILE."?";
		$this->currentRequest = INDEXFILE."?".urldecode($_SERVER['QUERY_STRING']);
		$this->domain = "http://".$_SERVER['HTTP_HOST']."/";
		
		// --------------------------------
		// Initiate session/user management
		// --------------------------------
		user::initiate();
		$this->last = arco::$user->sessionVars->lastPage;
		
		
		// --------------------------------
		// Set skin
		// --------------------------------
		if ($_REQUEST['skin']) arco::$user->sessionVars->skin = $_REQUEST['skin'];
		if (arco::$user->sessionVars->skin) $this->load_skin(arco::$user->sessionVars->skin);
		else $this->load_skin($this->config['standardSkin']);
		
		// --------------------------------
		// Set language
		// --------------------------------
		if ($_REQUEST['lang']) arco::$user->sessionVars->lang = $_REQUEST['lang'];
		if (arco::$user->sessionVars->lang) $this->load_lang(arco::$user->sessionVars->lang);
		else $this->load_lang($this->config['standardLang']);
		
		// --------------------------------
		// Load main language package
		// --------------------------------
		$lang->load_lang("main.php","main"); // Attention! $engine->load_lang != $lang->load_lang - TODO: fix duplicate load_lang
		
		// --------------------------------
		// Initialize View
		// --------------------------------
		arco::$view->init();
		
		// --------------------------------
		// Parse input
		// --------------------------------
		$this->create_input();
		
		// --------------------------------
		// Finish up
		// --------------------------------
		logger::log("Boot successful.");
		$this->bootComplete = true;
	}
	
	public function AVMError($errno, $errstr, $errfile, $errline) {
		if ($errno ^ E_NOTICE && $errno ^ E_STRICT)
			throw new AVMRuntimeException($errstr, $errfile, $errline);
//		else 
//			logger::log("NOTICE: ". $errstr);
	}
	
	public function create_input() {
		foreach($_REQUEST as $key => $value) {
			if ($key == "shifter") {
				foreach($value as $shiftstring) {
					$this->input['shifter'][] = $shiftstring;
					list($old,$new) = explode("~",$shiftstring);
					$parts = explode(".",$old);
					arco::$view->modShift[] = array(
						'old' =>
							array('plugin' => $parts[0],'instance' => $parts[1], 'module' => $parts[2]),
						'new' =>
							array('plugin' => $parts[0],'instance' => $parts[1], 'module' => $new) // extended url shifter
					);
				}
			}
			//else $this->input[$key] = arco::$libs->make_safe($value);
			else $this->input[$key] = $value;
		}
	}
	
	/**
	 * Gets the input for the given plugin/module environment
	 * @param plugin/module object
	 * @return stdClass inputs
	 */
	public function get_input_for($object) {
		// --------------------------------
		// Find the input with syntax plugin_instance_
		// --------------------------------
		reset($this->input);
		$prefix = $object->getName()."_".$object->getInstance()."_";
		$prefix_length = strlen($prefix);
		$input = new stdClass;
		while(list($key,$value) = each($this->input)) {
			if (substr($key,0,$prefix_length) == $prefix) $input->{substr($key,$prefix_length)} = $value;
		}
		return $input;
	}
	
	public function raise_error() {
		$this->errorOccured = true;
	}
	
	public function was_error() {
		return $this->errorOccured;
	}
	
	public function redirect($link) {
		header("Location: $link"); // is there a life after 'Location'?
		exit; // then kill it, suicide
	}
	
	/**
	 * @param string link
	 * @param int seconds
	 * @param string content (optional)
	 */
	public function redirect_delay($link,$seconds,$content = NULL) {
		throw new RedirectBreakthrough($link,0,$content);
	}
	
	
	/**
	 * @param string lang
	 * @return void
	 */
	public function load_lang($lang) {
		if (strstr($lang,"/") || !file_exists(LANG.$lang) || $lang == "") throw new ArcoException("Invalid language '$lang'.");
		if (file_exists(LANG.$lang."/charset")) $this->charset = file_get_contents(LANG.$lang."/charset");
		else {
			logger::log("There is no charset definied for language '$lang'.");
		}
		$this->currentLang = $lang;
	}
	
	/**
	 * @param string skin
	 * @return void
	 */
	public function load_skin($skin) {
		if (strstr($skin,"/") || !file_exists(SKINS.$skin)) throw new ArcoException("Invalid skin.");
		$this->currentSkin = $skin;
	}
	
	public function is_super($key) {
		if (array_key_exists($key,$this->superObjects)) return true;
		return false;
	}
	
	public function set_super($key,$value) {
		if ($this->bootComplete) throw new AVMRuntimeException("Unable to set superGlobal '$key'.");
		$this->superObjects[$key] = $value;
	}
	
	public function load_lib($library) {
		global $libs;
		$this->list_libs();
		if (!in_array($library,$this->available_librarys)) {
			throw new LibraryException($library,"Library unknown.");
		}
		return $libs->register_lib($library);
	}
	
	public function is_lib_registered($libname) {
		return class_exists($libname);
	}
	
	public function list_libs() {
		if ($this->librarys_uptodate) return false;
		if (!is_dir(LIBS)) throw new ArcoException("LIBS constant corrupt!");
		$dir = opendir(LIBS);
		unset($this->available_librarys);
		while($file = readdir($dir)) {
			if (is_dir(LIBS.$file)) continue; // subdirs in PATH_LIB are not supported
			if (!preg_match('/\.lib\.php$/',$file)) continue; // a lib file has to end with .lib.php
			$this->available_librarys[] = preg_replace('/^(.*)\.lib\.php$/','\1',$file);
		}
		file_put_contents(LIBS."cache.dat",implode(" ",$this->available_librarys));
		$this->librarys_uptodate = true;
		return true;
	}
	
	public function list_libs_from_cache() {
		$this->available_librarys = explode(" ",file_get_contents(LIBS."cache.dat"));
		$this->librarys_uptodate = true;
	}
	
	/**
	 * Install a newly extracted plugin
	 */
	public function install_plugin($pluginName) {
		logger::log("Trying to install plugin '$pluginName'");
		// --------------------------------
		// Verify plugin environment
		// --------------------------------
		if (!file_exists(PLUGINS."plg_".$pluginName."/config.php")) throw new PluginException($pluginName,"Unable to install this plugin. Install file 'config.php' does not exist in directory '".PLUGINS."plg_$pluginName/'");
		if (in_array($pluginName,array_keys($this->plugins))) throw new PluginException($pluginName,"This plugin is already installed!");
		// --------------------------------
		// Load plugin configuration
		// --------------------------------
		$mysql_tables = array();
		$overlay_tables = array();
		$global_tables = array();
		include(PLUGINS."plg_".$pluginName."/config.php");
		// TODO: verify plugin configuration
		// TODO: plugin specific permissions
		// TODO: plugin specific administration items
		
		// --------------------------------
		// Register mYsql overlay data
		// --------------------------------
		foreach($overlay_tables as $table => $cols) {
			if (array_key_exists($table,arco::$db->hookdata)) logger::log("mYsql overlay table '$table' already exists.");
			else {
				foreach($cols as $colName => $colType) arco::$db->insert("arcosql",array('table_name' => $pluginName."_".$table,'column_name' => $colName, 'data_type' => $colType));
			}
		}
		
		// --------------------------------
		// Create global plugin tables
		// --------------------------------
		foreach($global_tables as $tableName => $data) {
			arco::$db->query('CREATE TABLE '.SQL_PREFIX.$pluginName."_".$tableName." (\n" .
					$data."\n" .
        			' )' .
        			' TYPE = myisam');
		}
		
		// --------------------------------
		// Register plugin
		// --------------------------------
		arco::$db->insert("plugins",array('plg_name' => $pluginName,'plg_tables' => array_keys($mysql_tables),'instances' => array(),'librarys' => array()));
	}
	
	public function list_plugins() {
		if (!is_dir(PLUGINS)) throw new ArcoException("PLUGIN constant corrupt!");
		$dir = opendir(PLUGINS);
		$plugins = array();
		while($file = readdir($dir)) {
			if (!preg_match('/^plg_(.+)$/',$file,$match)) continue;
			if (!is_dir(PLUGINS.$file)) continue;
			$plugins[] = $match[1];
		}
		return $plugins;
	}
	
	/**
	 * Create an instance of a plugin
	 * @param string pluginName
	 * @param string instanceName
	 */
	public function make_plugin_instance($pluginName,$instanceName) {
		if (is_array($this->plugins[$pluginName]['instances'])) {
			if (in_array($instanceName,$this->plugins[$pluginName]['instances'])) throw new PluginException($pluginName,"Instance '$instanceName' already exists.");
		}
		$newId = $instanceName;
		$mysql_tables = array();
		$file = PLUGINS."plg_".$pluginName."/config.php";
		if (!file_exists($file)) throw new PluginException($pluginName,"Scheme-File '$file' does not exist. Unable to create instance '$instanceName' for plugin '$pluginName'");
		include($file);
		foreach($this->plugins[$pluginName]['plg_tables'] as $table) {
			arco::$db->query('CREATE TABLE '.SQL_PREFIX.$pluginName."_".$table."_".$newId." (\n" .
					$mysql_tables[$table]."\n" .
        			' )' .
        			' TYPE = myisam');
		}
		$this->plugins[$pluginName]['instances'][] = $newId;
		arco::$db->update("plugins",array('instances' => $this->plugins[$pluginName]['instances']),"WHERE plg_name = '$pluginName'");
		logger::log("Created new instance '$instanceName' for plugin '$pluginName'.");
	}
	
	/**
	 * Duplicates an instance
	 * @param string pluginName
	 * @param string instanceName
	 * @param string newInstanceName
	 */
	public function duplicate_instance($pluginName, $instanceName, $newInstanceName) {
		if (is_array($this->plugins[$pluginName]['instances'])) {
			if (in_array($newInstanceName,$this->plugins[$pluginName]['instances'])) throw new PluginException($pluginName,"Instance '$newInstanceName' already exists.");
			if (!in_array($instanceName,$this->plugins[$pluginName]['instances'])) throw new PluginException($pluginName,"Instance '$instanceName' does not exist.");
		}
		$mysql_tables = array();
		$configFile = PLUGINS."plg_".$pluginName."/config.php";
		if (!file_exists($configFile)) throw new FileNotFoundException($configFile,"Scheme-File '$configFile' does not exist. Unable to create instance '$instanceName' for plugin '$pluginName'");
		include($configFile);
		foreach($this->plugins[$pluginName]['plg_tables'] as $table) {
			arco::$db->query('CREATE TABLE '.SQL_PREFIX.$pluginName."_".$table."_".$newInstanceName." (\n" .
					$mysql_tables[$table]."\n" .
        			' )' .
        			' TYPE = myisam');
		}
		$this->plugins[$pluginName]['instances'][] = $newInstanceName;
		arco::$db->update("plugins",array('instances' => $this->plugins[$pluginName]['instances']),"WHERE plg_name = '$pluginName'");
		
	}
	
	public function duplicate_page($pageName,$newPageName) {
		$r = arco::$db->select("pages","*","WHERE page_name = '$pageName'");
		$data = $r->next;
		$oldId = $data['id'];
		unset($data['id']);
		$data['page_name'] = $newPageName;
		$i = arco::$db->insert("pages",$data);
		$r = arco::$db->select("page_modules","*","WHERE page_id = $oldId");
		while($data = $r->next) {
			unset($data['id']);
			$data['page_id'] = $i->insertId;
			if ($data['instance'] != "headline" && $data['plugin'] != "navigation") {
				$this->duplicate_instance($data['plugin'],$data['instance'],$newPageName."Inst");
				logger::log("Duplicated instance '{$data[instance]}'.");
				$data['instance'] = $newPageName."Inst";
			}
			arco::$db->insert("page_modules",$data);
		}
		logger::log("Duplicated page '$pageName'.");
	}
	
	/**
	 * Load a plugin instance.
	 * @param string pluginName
	 * @param string instanceName
	 * @return plugin object
	 */
	public function load_plugin($pluginName,$instanceName) {
		if (array_key_exists($pluginName,$this->pluginInstances)) {
			if (array_key_exists($instanceName,$this->pluginInstances[$pluginName])) {
				logger::log("Plugin '$pluginName' already loaded. Fetching object from cache...");
				return $this->pluginInstances[$pluginName][$instanceName];
			}
		}
		if (!file_exists(PLUGINS."plg_$pluginName/$pluginName.php")) throw new PluginException("Plugin '$pluginName' not found.");
		// --------------------------------
		// Load plugin file only once
		// --------------------------------
		include_once(PLUGINS."plg_$pluginName/$pluginName.php");
		$plg = new $pluginName($instanceName);
		// --------------------------------
		// Cache generated instance
		// --------------------------------
		$this->pluginInstances[$pluginName][$instanceName] = $plg;
//		logger::log("Loaded plugin '$pluginName'.");
		return $plg;
	}
	
	/**
	 * Generates a common identification string for a module/plugin
	 * @param object module
	 * @param boolean addmod
	 * @return string identstring
	 */
	public function generate_common_ident_string($object, $noMod = false) {
		return $object->getName()."_".$object->getInstance().($noMod ? '' : "_".$object->getModuleName());
	}
	
	public function generate_common_ident_string_p($plugin,$instance,$module = false) {
		return $plugin."_".$instance.($module !== false ? "_".$module : "");
	}
	
	public function optimize_lang_database() {
		$r = arco::$db->select("arcosql","*");
		$check = array();
		while($r->next) {
			if ($r->data_type == 4) {
				$check[] = array($r->table_name,$r->column_name);
			}
		}
		$referencedLangItems = array(0);
		foreach($check as $columnNfo) {
			list($table,$col) = $columnNfo;
			$r = arco::$db->select($table,$col);
			while($r->next) {
				$referencedLangItems[] = $r->$col;
			}
		}
		
		arco::$db->delete("lang","WHERE id NOT IN (".implode(",",$referencedLangItems).")");
		
	}
}

?>