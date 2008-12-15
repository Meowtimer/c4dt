<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_guestbook_inscribe extends template {

public function tpl_form() {
$HTML = <<<ADCE
	<form action="{arco.currentSite}" method="post">
		Author: <input type="text" name="author">
		Message: <textarea name="text" rows="5" cols="20"></textarea>
		<input type="submit" value="inscribe">
	</form>
ADCE;
return $HTML;
}
}
   
?>
