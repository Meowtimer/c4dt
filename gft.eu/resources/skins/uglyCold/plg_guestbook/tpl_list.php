<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_guestbook_list extends template {

public function tpl_head() {
$HTML = <<<ADCE
	<table>
ADCE;
return $HTML;
}

public function tpl_row($author,$text,$date) {
$HTML = <<<ADCE
	<tr><td>$author</td><td>$date</td></tr>
	<tr><td colspan="2">$text</td></tr>
	<tr><td colspan="2"></td></tr>
ADCE;
return $HTML;
}

public function tpl_foot() {
$HTML = <<<ADCE
	</table>
	<a href="{arco.url.page.adminpage}">adminpage</a>
	<a href="{arco.url.page.adminplugins}">adminplugins</a>
ADCE;
return $HTML;
}
}
   
?>
