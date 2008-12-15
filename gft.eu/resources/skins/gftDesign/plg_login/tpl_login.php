<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_login_login extends template {

public function tpl_loginform($message) {
$HTML = <<<ADCE
	<form action="{arco.currentSite}" method="post">
		$message
	    <input type="hidden" name="login_action" value="login">
		{arco.lang.username}: <input type="text" name="login_username">
		{arco.lang.password}: <input type="password" name="login_password">
		<input class="button" type="submit" value="{arco.lang.logintxt}">
	</form>
ADCE;
return $HTML;
}
public function tpl_welcome($username) {
$HTML = <<<ADCE
	<form action="{arco.currentSite}" method="post">
	    <input type="hidden" name="login_action" value="logout">
	    <div class="text_bold">{arco.lang.welcome}$username</div>
		<input class="button" type="submit" name="logout" value="{arco.lang.logout}">
	</form>
ADCE;
return $HTML;
}
public function tpl_logged_out() {
$HTML = <<<ADCE
	<div>{arco.lang.logout_successful}</div>
	<div>{arco.lang.main.redirecting}</div>
	<meta http-equiv="refresh" content="1;url={arco.currentSite}">
ADCE;
return $HTML;
}
public function tpl_logged_in() {
$HTML = <<<ADCE
	<div>{arco.lang.login_successful}</div>
	<div>{arco.lang.main.redirecting}</div>
	<meta http-equiv="refresh" content="1;url={arco.currentSite}">
ADCE;
return $HTML;
}
}
?>
