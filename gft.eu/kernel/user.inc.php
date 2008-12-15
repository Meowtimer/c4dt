<?php
/*
 * Created on 02.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 /**
  * User object(User/Permission system integrated)
  * @package ArcoVM
  * @author ZokRadonh
  * @version alpha 0.1
  */
 class user {
 	public $userId = ANONYMOUS;
 	
 	public $username;
 	public $rank;
 	public $activated;
 	public $blocked = false;
 	public $activationKey;

 	private $permissions = array();
 	
 	public $sessionId;
 	public $lastPage;
 	public $lastParameter;
 	public $lastTime;
 	public $ip;
 	
  	private $createTime;
 	private $expireTime;
 	
 	public $sessionVars;
 	
 	/**
 	 * 
 	 */
	 public function __construct() {
	 	$this->expireTime = 0;
		$sessionVars = new session();
 	}	
 	
 	/**
 	 * Creates a new user Object by session, cookie or a complete new one
 	 *
 	 */
 	public static function initiate() {
 		// TODO: Make login possible
		// --------------------------------
		// Session management
		// --------------------------------
		$rebind = false;
		if (isset($_SESSION['user'])) { // has && has
			if (!is_object($_SESSION['user']) || !$_SESSION['user'] instanceof user) throw new SecurityException("Invalid session! ".$_SERVER['REMOTE_ADDR']." tried to trick Arco.");
			arco::$user = $_SESSION['user'];
			if (arco::$db->select("sessions","session_id","WHERE session_id = '".arco::$user->sessionId."'")->num == 0) {
				arco::$user->kill();
			}
			logger::log("[Session] Got object from sid.");
		}
		elseif(isset($_COOKIE['session_hash'])) { // has && no
			arco::$user = self::load_by_cookie($_COOKIE['session_hash']);
			$rebind = true;
			logger::log("[Session] Got object from cookie.");
		}
		else { // no & no
			arco::$user = self::create_anonymous(); // create userobject
			$rebind = true;
			logger::log("[Session] Created anonymous session.");
		}
		arco::$user->lastPage = arco::$engine->currentPage;
		arco::$user->lastParameter = arco::$engine->last;
		// --------------------------------
		// Update session data
		// --------------------------------
		if ($rebind) arco::$user->bind_to_session();
		else arco::$user->save_session_in_database();
		// --------------------------------
		// Recognize User Agent 
		// --------------------------------
		if (arco::$user->sessionVars->isIEClient == null) {
			if (strstr($_SERVER['HTTP_USER_AGENT'],"MSIE")) {
				arco::$user->sessionVars->isIEClient = true;
				setcookie("ieclient",1);
			}
			else {
				arco::$user->sessionVars->isIEClient = false;
				setcookie("ieclient",0);
			}
		}
		// --------------------------------
		// Optimize database
		// --------------------------------
		user::optimize_database();
 	}
 	
 	/**
 	 * Creates a new anonymous user Object 
 	 *
 	 * @return new user Object
 	 */
  	public static function create_anonymous() {
 		$user = new user();
 		$user->userId = ANONYMOUS;
 		$user->blocked = false;
 		$user->activated = true;
  		$user->ip = $_SERVER['REMOTE_ADDR'];
 	 	$user->lastTime = time();
 		$user->lastPage = FALSE;
 		$user->lastParameter = FALSE;
 		$user->createTime = time();
 		$user->additionalVars = array();
 		return $user;
 	}
	
 	/**
 	 * Morphs this user Object to a new User 
 	 *
 	 * @param int $userId
 	 */
	public function morph_to($userId) {
		$this->userId = $userId;
		$this->update_by_database();
		$this->save_session_in_database();
	}
	
	/**
	 * Creates a new user Object with data fetched by database linked by session_id
	 *
	 * @param string $cookie session_id
	 * @return new user Object
	 */
 	public static function load_by_cookie($cookie) {
 		$data = arco::$db->select("sessions","WHERE session_id = '$cookie'")->next;
 		if (!count($data)) throw new InvalidCookieException();
  		$user = new user();
 		$user->apply_data($data);
 		$user->update_by_database();
 		return $user;
 	}

	/**
	 * Updates this user Object by the data in database
	 *
	 */
 	public function update_by_database() {
 		$r = arco::$db->select("users","username,rank,activation_key,blocked","WHERE `id` = $this->userId");
 		$data = $r->next;
 		$this->apply_data($data);
 		$this->activated = strlen($data['activation_key']) == 0;
 		$this->blocked = (bool) $data['blocked'];
 		$this->permissions = array();
 		$this->load_permissions();
 	}
 	
 	/**
 	 * Saves user Object in $_SESSION
 	 * Saves user data to database
 	 *
 	 */
 	public function bind_to_session() {
 		$this->sessionId = session_id();
		$this->save_session_in_database();
 		$_SESSION['user'] = $this;
 	}
 	
 	/**
 	 * Sets a cookie which saves the session_hash
 	 *
 	 * @param int $length optional expire time
 	 */
 	public function bind_to_cookie($length = false) {
  	 	$this->createTime = time();
  	 	if ($length < 0) $this->expireTime = 0;
 		else $this->expireTime = time() + ($length ? $length : arco::$engine->config['cookie_expire']);
 		setcookie("session_hash",$this->sessionId,$this->expireTime,arco::$engine->config['cookie_path']);	
 	}
 	
 	/**
 	 * Kills current session & user Object
 	 * Replaces arco::$user with an anonymous Object 
 	 *
 	 */
 	public function kill() {
 		setcookie("session_hash","",10,arco::$engine->config['cookie_path']);
 		$_SESSION = array();
 		session_destroy();
 		session_start();
 		arco::$user = user::create_anonymous();
 	}
 	
 	/**
 	 * Sets the members of this Object to the values got in $array
 	 * The keys of $array should have the form 'last_page' => "home"
 	 * This will be converted to $this->lastPage = "home"
 	 * 
 	 * @param assoc_array $array
 	 */
	public function apply_data($array) {
 		foreach($array as $key => $value) {
 			$key = preg_replace('/_([\w])/e','strtoupper($1)',$key);
			$this->$key = $value;
 		}
 	}
 	
 	/**
 	 * Saves user Object in database
 	 *
 	 * @param bool $onlyUpdate disallow registering this session as a new session
 	 * @return bool success
 	 */
 	public function save_session_in_database($onlyUpdate = false) {
 		$hashtable = array(
 			'session_id' => $this->sessionId,
 			'user_id' => $this->userId,
 			'create_time' => $this->createTime,
 			'expire_time' => $this->expireTime,
 			'ip' => $this->ip,
 			'last_time' => time(),
 			'last_page' => $this->lastPage,
 			'last_parameter' => $this->lastParameter
 			);
 		if (arco::$db->select("sessions","session_id","WHERE session_id = '$this->sessionId'")->num > 0)
 			arco::$db->update("sessions",$hashtable,"WHERE session_id = '$this->sessionId'");
 		elseif ($onlyUpdate === false)
 			arco::$db->insert("sessions",$hashtable);
 		else
 			return false;
 		return true;
 	}
 	
 	/**
 	 * Deletes old sessions
 	 *
 	 */
 	public static function optimize_database() {
 		arco::$db->delete("sessions","WHERE last_time < ".(time() - 3600 * 24));
 	}
 	
 	/**
 	 * Updates objects last_* values
 	 *
 	 * @param page $page
 	 */
 	public function update_for(page $page) {
 		$this->lastPage = $page->pageName;
 		$this->lastTime = time();
 		$this->lastParameter = $_GET;
 		$this->ip = $_SERVER['REMOTE_ADDR'];
 		$this->save_session_in_database();
 	}
 	
 	public function get_groups() {
 		// TODO: user::get_groups()
 	}
 	
 	/**
 	 * Loads all permissions into user->permissions
 	 */
 	public function load_permissions() {
 		// --------------------------------
		// sehe und staune
		// --------------------------------
 		$res = arco::$db->query("SELECT mask_perms.perm_value,perms.intern_name,perms.plugin_name,perms.instance_name,grp_masks.mask_priority,usr_grps.group_priority FROM " .
			"adce_permissions perms LEFT JOIN " .
				"(adce_mask_permissions mask_perms LEFT JOIN " .
					"(adce_group_masks grp_masks LEFT JOIN " .
						"(adce_user_groups usr_grps LEFT JOIN adce_users usrs" .
						" ON usr_grps.user_id = usrs.id)" .
					" ON grp_masks.group_id = usr_grps.group_id)" .
				" ON mask_perms.mask_id = grp_masks.mask_id)" .
			" ON mask_perms.permission_id = perms.id" .
			" WHERE usrs.id = '$id' ORDER BY group_priority,mask_priority");
		// --------------------------------
		// ganze permissions logik per mysql gelöst - mit einem einzigen query
		// --------------------------------
		while($res->next) {
			$perm = new permission($res->intern_name,$res->perm_value,$res->plugin_name,$res->instance_name);
			if (!$this->perm_exists($perm)) $this->add($perm);
		}
 	}
 	
 	/**
 	 * Checks whether this user can do $perm. $perm has to be a global permission
 	 * @param string $perm
 	 * @return boolean
 	 */
 	public function can_do($perm) {
 		if (is_array($this->permissions[$perm])) throw new ArcoException("Additional information are required for permission '$perm'. Plugin? Instance?");
 		if ($this->permissions[$perm] == P_GRANTED) return true;
 		else return false; // standard deny
 	}
 	
 	/**
 	 * Checks whether this user can do $perm for plugin instance referenced by $object
 	 * @param plugin/module $object
 	 * @param string $perm
 	 * @return boolean
 	 */
 	public function can_do_in($object,$perm) {
 		$pluginName = $object->getName();
 		$instanceName = $object->getInstance();
 
 		return $this->get_perm($perm,$pluginName,$instanceName);
 	}
 	
 	/**
 	 * Adds a permissions object to user->permissions array
 	 *
 	 * @param permission $perm
 	 */
  	public function add(permission $perm) {
 		$this->permissions[$perm->name][$perm->plugin][$perm->instance] = $perm->value;
 	}
 	
 	public function get_perm($permName,$plugin = false,$instance = false) {
 		$perm = new permission($permName,null,$plugin,$instance);
 		if ($this->perm_exists($perm)) {
 			if ($perm->main_perm) return $this->permissions[$perm->perm];
 			else return $this->permissions[$perm->perm][$perm->plugin][$perm->instance];
 		}
 		else throw new ArcoException("Cannot get permission. Permission '$perm->name' doesn't exist.");
 	}
 	
 	protected function perm_exists(permission $perm) {
 		if ($perm->main_perm) {
 			if (array_key_exists($perm->perm,$this->permissions) && !is_array($this->permissions[$perm->perm])) return true;
 		}
 		else {
 			if (array_key_exists($perm->perm,$this->permissions && is_array($this->permissions[$perm->perm]))) {
 				if (is_array($this->permissions[$perm->perm][$perm->plugin]) && is_array($this->permissions[$perm->perm][$perm->plugin][$perm->instance])) {
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 }
 
 class session {
 	private $vars = array();
 	
 	public function __set($key,$value) {
 		$this->vars[$key] = $value;
 	}
 	
 	public function __get($key) {
 		return $this->vars[$key];
 	}
 }
 
 class UserBlockedException extends ArcoException {
 	public function __construct() {
		parent::__construct("",0);
	}
 }
 
 class PermissionDeniedException extends UserException {
 	public function __construct($right) {
 		parent::__construct("You need the following permission to continue: '$right'");
 	}
 }
 
 class UserInactiveException extends ArcoException {
 	public function __construct() {
		parent::__construct("",0);
	}
 }
 
 class InvalidCookieException extends ArcoException {
 	public function __construct() {
		parent::__construct("",0);
	}
 }
?>