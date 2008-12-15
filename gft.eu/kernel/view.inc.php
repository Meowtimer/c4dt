<?php
/*
 * Created on 31.07.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 if (!defined("IN_ARCO")) die("suchst du was?");

 include("template.inc.php");

 
 /**
  * Template system
  * @author ZokRadonh
  * @package ArcoVM
  * @version alpha 0.4
  */
 class view {

	/**
	 * nen uebles array
	 * array( 
	 * 	[0] => array('old' => array('plugin' => xxx,'instance' => zzz, 'module' => yyy),'new' => array('plugin' => xxx,'instance' => zzz, 'module' => yyy)))
	 *  [1] ...
	 * )
	 */
	public $modShift = array();
	public $main = null;
	
	private $head = "";
	private $foot = "";
	
	/**
	 * Loads the main template.
	 */
	public function init() {
		$this->main = $this->get_templates("main.php","tpl_main");
	}
	
	/**
	 * Returns the a sub-object with all templates for the given module
	 * @param module mod
	 * @return object of the corresponding type offering several functions(templates)
	 */
 	public function get_templates_for(module $mod) {
 		$pluginName = get_class($mod->plugin);
 		$instanceName = $mod->plugin->instance;
 		$modName = $mod->getModuleName();
 		// --------------------------------
		// Get template class
		// --------------------------------
 		$file = SKINS.arco::$engine->currentSkin."/plg_$pluginName/tpl_$modName.php";
 		if (!file_exists($file)) throw new ArcoException("Template file not found: '$file'");
 		include_once($file);
 		// --------------------------------
		// Init template object
		// --------------------------------
 		$class = "tpl_".$pluginName."_".$modName;
 		if (!class_exists($class)) throw new ArcoException("Missing template '$class'");
 		$tpl = new $class;
 		$tpl->pluginName = $mod->getName();
 		$tpl->plugin = $mod; // well, in fact thats not correct but does it matter? // it does! it has to be the module! // TODO: rename tpl->plugin to tpl->module
 		// --------------------------------
		// Search for illegal template names
		// --------------------------------
 		$methods = get_class_methods($tpl);
 		if (in_array("tpl_main",$methods)) throw new ViewException("Illegal template-name found in plugin '{$tpl->pluginName}'. The use of the word 'main' as template name is prohibited.");
 		return $tpl;
 	}
 	
 	/**
 	 * Adds custom html code to <head> section
 	 * @param string htmlcode
 	 */
 	public function add_to_head($html) {
 		$this->head .= $html;
 	}
 	
 	/**
 	 * Returns the custom html code for <head> section
 	 * @return string htmlcode
 	 */
 	public function get_head_addition() {
 		return $this->head;
 	}
 	
  	/**
 	 * Adds custom html code before foot() section
 	 * @param string htmlcode
 	 */
 	public function add_to_foot($html) {
 		$this->foot .= $html;
 	}
 	
 	/**
 	 * Returns the custom html code
 	 * @return string htmlcode
 	 */
 	public function get_foot_addition() {
 		return $this->foot;
 	}
 	
 	/**
 	 * @param string templateName
 	 * @param string className
 	 * @return object of className type offering several functions(templates)
 	 */
 	public function get_templates($templatePath,$class) {
  		// --------------------------------
		// Get template class
		// --------------------------------
 		$file = SKINS.arco::$engine->currentSkin."/$templatePath";
 		if (!file_exists($file)) throw new ArcoException("Template file not found: '$file'");
 		include_once($file);
 		// --------------------------------
		// Init template object
		// --------------------------------
 		$tpl = new $class;
 		$tpl->pluginName = null;
 		$tpl->plugin = null;
  		// --------------------------------
		// Search for illegal template names
		// --------------------------------
 		$methods = get_class_methods($tpl);
 		if (in_array("tpl_main",$methods)) throw new ViewException("Illegal template-name found in plugin '{$tpl->pluginName}'. The use of the word 'main' as template name is prohibited.");
 		return $tpl;
 	}
 	
 	/**
 	 * @param page page
 	 * @return object of specific structure class with function structure(...)
 	 */
 	public function get_structure_for(page $page) {
 		$structfile = STRUCTURES.$page->structureName.".php";
 		if (!file_exists($structfile)) throw new ViewException("Couldn't find structure file '$structfile' for page '".$page->pageName."'.");
 		include_once($structfile);
 		$class = "struct_".$page->structureName;
 		$structure = new $class;
 		$this->verify_structure($structure);
 		$structure->init();
 		return $structure;
 	}
 	
 	/**
 	 * Creates a page-object
 	 * @param string pageName
 	 * @return new page-object that already loaded all plugins and modules according to this page
 	 */
 	public function get_page($pageName) {
 		$result = arco::$db->select("pages","*","WHERE page_name = '$pageName'");
 		if ($result->num < 1) throw new ViewException("Page '$pageName' does not exist.");
 		$pageData = $result->next;
 		$page = new page($pageData);
 		return $page;
 	}
 	
 	/**
 	 * Fetches all available skins
 	 * @return array of skins
 	 */
 	public function get_skins() {
 		$dir = opendir(SKINS);
 		$skins = array();
 		while($file = readdir($dir)) {
 			if ($file == "." || $file == ".." || !is_dir($file)) continue;
 			$skins[] = $file;
 		}
 		return $skins;
 	}
 	
 	public function verify_structure($oStructure) {
		if (!$oStructure instanceof structure) throw new ViewException("Incompatible structure file '".get_class($oStructure)."'. Structure classes have to inherit 'structure'.");
 		return true;
 	}
 	
 	public function generate_dynamic_tpl_function($tplName,$template) {
		
 	}
 	
 	public function generate_tpl_class() {
 		
 	}
 	
 	/**
 	 * Reads out all templates in database and caches it in directory skins
 	 */
 	public function rebuild_skin() {
 		$r = arco::$db->select("arcosql","*","WHERE data_type = ".ARCOSQL_HTML);
 		while($r->next) {
 			foreach(arco::$engine->plugins as $pluginName => $data) {
 				foreach($data['plg_tables'] as $table) {
 					if ($pluginName."_".$table == $r->table_name) {
 						$instances = $data['instances'];
 						foreach($instances as $instance) {
 							$sub = arco::$db->select(new mysql_table_collection(new mysql_table($pluginName,$table,$instance)),$r->column_name);
 							
 						}
 					}
 				}
 			}
 		}
 	}
 	
 	/**
 	 * Fetches all available structures
 	 * @return array of positions with structure name as key
 	 */
 	public function get_structures() {
 		$dir = opendir(STRUCTURES);
 		$structures = array();
 		while($file = readdir($dir)) {
 			if ($file == "." || $file == ".." || is_dir(STRUCTURES.$file)) continue;
 			$structureName = str_replace(".php","",$file);
 			$className = "struct_".$structureName;
 			include_once(STRUCTURES.$file);
 			if (!class_exists($className)) {
 				logger::log("Structure '$structureName' has no or an incorrect class.");
 				continue;
 			}
 			$tempObj = new $className;
 			$this->verify_structure($tempObj);
			$tempObj->init();
 			$structures[$structureName] = $tempObj;
 		}
 		return $structures;
 	}
 	
 }
 
 /**
  * A simple resource
  */
 abstract class resource {
 	public $uResId = null;
 	protected $path;
 	public $attributes = array();
 	static protected $mimes = array(
			'simpleImage' => array("image/jpeg","image/gif","image/png"),
 			'wmvVideo' => array("video/x-ms-wmv"),
 			'flvVideo' => array("video/x-flv"));
 	public $blob;
 	public $cachedBlob;
 	protected $cachedOptions;
 	public $mime;
 	public $cacheTime;
 	
 	abstract public function CanCache();
 	abstract public function CanWaterProof();
 	
 	public function __construct($blob) {
 		$this->blob = $blob;
 	}
 	
 	/**
 	 * @internal protected mYsql function
 	 */
 	public static function load($uResId) {
 		$data = arco::$db->select("resources","*","WHERE uresid = $uResId")->next;
 		$handler = resource::create($data['image'],$data['mime']);
 		$handler->cachedBlob = $data['cache'];
 		$handler->cachedOptions = $data['cache_hash'];
 		$handler->cacheTime = $data['last_modified'];
 		$handler->path = ARL::resource($uResId)->url;
 		$handler->uResId = $uResId;
 		return $handler;
 	}
 	
 	public static function create($blob, $mime) {
 		foreach(self::$mimes as $resourceHandler => $exts) {
 			foreach($exts as $ext) {
 				if ($mime == $ext) {
 					$handler = new $resourceHandler($blob);
 					$handler->mime = $mime;
 					return $handler;
 				}
 			}
 		}
 		throw new ArcoException("There is no known handler for filetype '$mime'.");
 	}
 	
 	public static function import($path) { 		
 		$mime = arco::$libs->detect_mime($path);
 		if (!$mime) throw new ArcoException("Unknown file type ('$path').");
 		foreach(self::$mimes as $resourceHandler => $exts) {
 			foreach($exts as $ext) {
 				if ($mime == $ext) {
 					$handler = new $resourceHandler(file_get_contents($path));
 					$handler->mime = $mime;
 					return $handler;
 				}
 			}
 		}
 		throw new ArcoException("Unknown file extension('$mime'). Unable to create resource handler.");
 		return new simpleData($path);
 	}
 	
 	public function getRelativePath() {
 		return SKINS.arco::$engine->currentSkin."/".$this->path;
 	}
 	
 	public function getAbsolutePath() {
 		return arco::$engine->domain.$this->getRelativePath();
 	}
 	
 	/**
 	 * @internal private mYsql function
 	 */
 	public function saveInDatabase() {
 		$qry = arco::$db->insert("resources",array('mime' => $this->mime,'image' => arco::$db->make_safe($this->blob)));
 		return $qry->insertId;
 	}
 	
 	abstract public function make_html($attributes = false);
 	abstract public function getOptionsHash();
 	/**
 	 * @param array configuration
 	 */
 	abstract public function ensureOptions($config);
 	
 }
 
 class simpleData extends resource {
 	public function CanCache() { return false; }
 	public function CanWaterProof() { return false; }
 	public function make_html($attributes = false) {
 		return arco::$view->main->download($this->path,null);
 	}
 	
 	public function getOptionsHash() {
 		return md5("");
 	}
 	
 	public function ensureOptions($config) {
 		throw new ArcoException("Not implemented yet");
 	}
 }
 
 class simpleImage extends resource {
 	protected $sizeX;
 	protected $siteY;
 	public function CanCache() { return true; }
 	public function CanWaterProof() { return true; }
 	
 	public function make_html($attributes = false) {
 		if ($attributes === false) $attributes = $this->attributes;
 		return arco::$view->main->image($this->path,$attributes);
 	}
 	
 	public function getOptionsHash() {
 		return md5($this->sizeX.$this->sizeY."5");
 	}
 	
 	public function ensureOptions($config) {
 		$this->sizeX = $config['size_x'];
 		$this->sizeY = $config['size_y'];
 		if ($this->getOptionsHash() != $this->cachedOptions) {
 			// --------------------------------
			// Refresh cache
			// --------------------------------
			logger::log("Refreshing cache of resource '$this->uResId'.");
			$im = image::CreateByBlob($this->blob, $this->mime);
			
			// --------------------------------
			// Resize if necessary
			// --------------------------------
			if ($im->width > $this->sizeX || $im->height > $this->sizeY)
				$im->ResizeProportional($this->sizeX,$this->sizeY);
			
			// --------------------------------
			// Paint waterproof
			// --------------------------------
			$simPics = array(147,150);	
			if (in_array($this->uResId,$simPics)) $im->PaintWaterProof("dodger.ttf","SIMULATION");
			else $im->PaintWaterProof("dodger.ttf");
			
			// --------------------------------
			// Generate binary
			// --------------------------------
			$this->cachedBlob = $im->GetBlob();
			
	 		// --------------------------------
			// avoid server's getaway 
			// --------------------------------
	 		ini_set("max_allowed_packet",1024 * 1024 * 8);
			if ($this->uResId) arco::$db->update("resources",array('cache' => arco::$db->make_safe($this->cachedBlob), 'cache_hash' => $this->getOptionsHash(), 'last_modified' => time()),"WHERE uresid = $this->uResId");
			ini_restore("max_allowed_packet");
 		}
 	}
 }
 
 class wmvVideo extends resource {
 	public function CanCache() { return false; }
 	public function CanWaterProof() { return false; }
 	public function make_html($attributes = false) {
 		return arco::$view->main->wmvVideo(arco::$engine->domain.arco::$engine->path.INDEXFILE.$this->path,null);
 	}
 	public function getOptionsHash() {
 		return md5("not implemented yet");
 	}
 	public function ensureOptions($config) {
 		return true;
 		throw new ArcoException("Not implemented yet");
 	}
 	
 }
 
 class flvVideo extends resource {
 	public function CanCache() { return false; }
 	public function CanWaterProof() { return false; }
 	public function make_html($attributes = false) {
 		return arco::$view->main->flvVideo("../../../".INDEXFILE.$this->path,null);
 	}
 	public function getOptionsHash() {
 		return md5("not implemented yet");
 	}
 	public function ensureOptions($config) {
 		return true;
 		throw new ArcoException("Not implemented yet");
 	}
 }
 
?>
