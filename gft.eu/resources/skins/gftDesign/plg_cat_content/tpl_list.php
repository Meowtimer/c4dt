<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_cat_content_list extends template {

public function tpl_head($nav) {
$HTML = <<<ADCE
	$nav
	<table>
ADCE;
return $HTML;
}
public function tpl_row($name,$linkurl,$iconurl,$description) {
$HTML = <<<ADCE
	<tr>
	  <td><a href="$linkurl">$name</a></td>
	</tr>
ADCE;
return $HTML;
}
public function tpl_foot($nav) {
$HTML = <<<ADCE
	</table>
ADCE;
return $HTML;
}
public function tpl_nav_element($name,$linkurl) {
$HTML = <<<ADCE
&raquo; <a href="$linkurl">$name</a> &nbsp;
ADCE;
return $HTML;
}
}
?>