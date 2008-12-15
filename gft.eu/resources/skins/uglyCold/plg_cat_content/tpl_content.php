<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_cat_content_content extends template {

public function tpl_head($nav) {
$HTML = <<<ADCE
	$nav
	<div>
ADCE;
return $HTML;
}
public function tpl_content($name,$content,$iconurl) {
$HTML = <<<ADCE
	<h1>$name</h1>
	$content
ADCE;
return $HTML;
}
public function tpl_foot($nav) {
$HTML = <<<ADCE
	</div>
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