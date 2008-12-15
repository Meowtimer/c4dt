<?php
/*
 * Created on 04.08.2006
 *
 * @author Andre "Radonh" Zoledziowski - arctics.net
 */
 
 /**
  * System plugin
  */
 class login extends plugin {
 	public $plugin_modules  = array("login","logout");
 }
 
 class login_login extends module {
 	public function init() {
 	}
 	
 	public function make_html() {
 		$this->init(); // call it as defined by reference
 		
 		$tpl = $this->tpl;
 		$lang = $this->lang;
 		// --------------------------------
		// User is already logged in
		// --------------------------------
 		if (arco::$user->userId != ANONYMOUS) {
 			// --------------------------------
			// User has requested a logout
			// --------------------------------
			if (arco::$engine->input['login_action'] == "logout") {
				$this->logout();
				return $tpl->logged_out();
			}
			// --------------------------------
			// User has not interacted with our nice login plugin or logged in recently
			// --------------------------------
 			return $tpl->welcome(arco::$user->username);
 		}
 		// --------------------------------
		// User is not logged in
		// --------------------------------
		if (!array_key_exists("login_action",arco::$engine->input)) {
			// --------------------------------
			// no interaction
			// --------------------------------
			return $tpl->loginform("");
		}
		else { # u know, only 4 indent
	 		if (arco::$engine->input['login_action'] == "login") {
	 			// --------------------------------
				// User has requested a login
				// --------------------------------
	 			if (arco::$user->userId != ANONYMOUS) throw new UserException($lang->already_logged_in);
	 			$username = str_replace(
	 				array(" ","%"),
	 				array("",""),
	 				arco::$engine->input['login_username']);
	 			$username = arco::$db->make_safe($username);
	 			$data = arco::$db->select("users","*", "WHERE username = '$username'")->next;
				$this->new_login($data);
				return $tpl->logged_in();
	 		}
		}
 	}
 	
 	public function logout() {
 		arco::$user->kill();
 	}
 	
 	public function new_login($data) {
 		$lang = $this->lang;
 		if (md5(arco::$engine->input['login_password']) == $data['password']) {
// 			print_r($data);
// 			$user = new user($data);
			arco::$user->morph_to($data['id']);
 			// --------------------------------
			// whaahooo, i love try..catch %)
			// --------------------------------
 			try {
// 				$user->initiate();
 			}
 			catch (UserBlockedException $e) {
 				throw new UserException($lang->user_blocked);
 			}
 			catch (UserInactiveException $e) {
 				$message = $lang->user_inactive;
 				if ($user->activation_key == "admin") $message .= " ".$lang->user_inactive_due_admin;
 				else $message .= " ".$lang->user_inactive_due_email;
 				throw new UserException($message);
 			}
 		}
 		else {
 			throw new UserException($lang->wrong_password);
 		}
 	}
 }
 
 class login_logout extends module {
 	public function init() {
 	}
 	
 	public function make_html() {
 		$this->init();
 		
 	}
 }
 
?>
