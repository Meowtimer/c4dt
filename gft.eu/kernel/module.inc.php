<?php
/*
 * Created on 31.07.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
if (!defined("IN_ARCO")) die("suchst du was?");
 
abstract class module {
	
	public $plugin;
	public $parentPage;
	public $actions = array();
	protected $html;
	
	abstract public function init();
	abstract public function make_html();
	
	/**
	 * Returns the plugin name
	 *
	 * @return string Plugin name
	 */
	public final function getName() {
		return $this->plugin->getName();
	}
	
	/**
	 * Returns the module name
	 *
	 * @return string Module name
	 */
	public final function getModuleName() {
		if (!$this instanceof module) throw new ArcoException("Cannot get module name. '\$this' is a plugin.");
		$plugin = $this->plugin->getName();
		$module = substr(get_class($this),strlen($plugin)+1);
		return $module;
	}
	
	/**
	 * Returns low level plugin name (plg_plugin)
	 *
	 * @return string Low-level plugin name
	 */
	public final function getLowLevelName() {
		return "plg_".$this->getName();
	}
	
	/**
	 * Returns parental page that contains this module
	 *
	 * @return page Parent page
	 */
	public final function getParentPage() {
		return $this->parentPage;
	}
	
	/**
	 * Returns instance name
	 *
	 * @return string Instance name
	 */
	public final function getInstance() {
		return $this->plugin->getInstance();
	}
	
	/**
	 * @todo action system
	 */
	private function execute_action() {
		$input = arco::$engine->get_input_for($this);
		
	}
	
	/**
	 * Checks if shifter data is available for this instance
	 * @return boolean shifted
	 */
	public final function isShifted() {
		$modShift = arco::$view->modShift;
		for($x = 0;$x < count($modShift);$x++) {
	 		if (
				$modShift[$x]['old']['plugin'] == $this->getName() &&
				$modShift[$x]['old']['instance'] == $this->getInstance() &&
			 	$modShift[$x]['old']['module'] == $this->getModuleName()
	 		) {
	 			return true;
	 		}
		}
		return false;
	}
	
	/**
	 * Checks if this object was shifted.
	 * @return boolean shifted
	 */
	public final function wasShifted() {
		$modShift = arco::$view->modShift;
		for($x = 0;$x < count($modShift);$x++) {
	 		if (
				$modShift[$x]['new']['plugin'] == $this->getName() &&
				$modShift[$x]['new']['instance'] == $this->getInstance() &&
			 	$modShift[$x]['new']['module'] == $this->getModuleName()
	 		) {
	 			return true;
	 		}
		}
		return false;
	}
	
	/**
	 * Returns the old module that was shifted for this one
	 * @return array 'plugin' => plugin,'instance' => instance,'module' => module
	 */
	public final function getOldShift() {
		$modShift = arco::$view->modShift;
		for($x = 0;$x < count($modShift);$x++) {
	 		if (
				$modShift[$x]['new']['plugin'] == $this->getName() &&
				$modShift[$x]['new']['instance'] == $this->getInstance() &&
			 	$modShift[$x]['new']['module'] == $this->getModuleName()
	 		) {
	 			return $modShift[$x]['old'];
	 		}
		}
		throw new ArcoException("Unable to get old shift data for plugin '".$this->getName()."' & module '".$this->getModuleName()."' & instance '".$this->getInstance()."'.");
	}
	
	/**
	 * Returns the new module that is shifted for the old one
	 * @return array 'plugin' => plugin,'instance' => instance,'module' => module
	 */
	public final function getShift() {
		$modShift = arco::$view->modShift;
		for($x = 0;$x < count($modShift);$x++) {
	 		if (
				$modShift[$x]['old']['plugin'] == $this->getName() &&
				$modShift[$x]['old']['instance'] == $this->getInstance() &&
			 	$modShift[$x]['old']['module'] == $this->getModuleName()
	 		) {
	 			return $modShift[$x]['new'];
	 		}
		}
		throw new ArcoException("Unable to get shift data for plugin '".$this->getName()."' & module '".$this->getModuleName()."' & instance '".$this->getInstance()."'.");
	}
	
	/**
	 * Checks whether the current logged in user is allowed to do $perm in $this
	 * @param string perm Permission name
	 * @return boolean Is allowed to do perm
	 */
	public final function requirePerm($perm) {
		if (arco::$user->can_do_in($this,$perm) === false) {
			throw new PermissionDeniedException($perm);
		}
		return true;
	}
	
	/**
	 * Checks whether the current logged in user is allowed to do $perm
	 *
	 * @param string perm Permission name
	 * @return boolean Is allowed to do perm
	 */
	public final function requireGlobalPerm($perm) {
		if (arco::$user->can_do($perm) === false) {
			throw new PermissionDeniedException($perm);
		}
		return true;
	}
	
	/**
	 * Returns a new link object that points to uri relatively to this module
	 * e.g. $this->link("action=down&id=$r->id")->html("text","tooltip");
	 * this results in:
	 * prepending the ?page= strings
	 * modifying "action" to arco var "plugin_instance_action"
	 * & to &amp;
	 * and with ->html() it generates a <a>text</a> link 4 you with tooltip as tooltip
	 * it is possible to call more methods "in-a-row" ( link(..)->page()->in_html()->html() )
	 * 
	 * @param string $uri
	 */
	public function link($uri) {
		return new link($this, $uri);
	}
	
	/**
	 * Automatically registers module specific global objects
	 *
	 * @param string $varName
	 * @return object
	 */
	public function __get($varName) {
		switch($varName) {
			case "db":
				$this->db = arco::$db->get_driver_for($this);
				return $this->db;
			case "tpl":
				$this->tpl = arco::$view->get_templates_for($this);
				return $this->tpl;
			case "input":
				$this->input = arco::$engine->get_input_for($this);
				return $this->input;
			case "lang":
				$this->lang = arco::$lang->get_lang_for($this);
				return $this->lang;
			default:
				return null;
		}
	}
	
	/**
	 * Reserved function
	 *
	 * @final 
	 * @internal 
	 */
	public final function __construct() {
		// reserve function
		// plugin is only allowed to execute code when requested
		
	}
}

 
?>
