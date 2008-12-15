<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_login_logout extends template {

public function tpl_form() {
$HTML = <<<ADCE
	<form action="{arco.currentSite}" method="post">
		{arco.lang.username}: <input type="text" name="login_username">
		{arco.lang.password}: <input type="password" name="login_password">
		<input type="submit" value="inscribe">
	</form>
ADCE;
return $HTML;
}
}
?>
