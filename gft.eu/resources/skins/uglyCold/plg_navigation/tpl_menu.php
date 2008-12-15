<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_navigation_menu extends template {

public function tpl_head() {
$HTML = <<<ADCE

	<div style="position: absolute;">
ADCE;
return $HTML;
}
public function tpl_javascript_init() {
$HTML = <<<ADCE
	<script type="text/JavaScript">
		
		document.openMenus = new Array();
		document.onmousemove = arco_event_handler;
		
		document.menuObjects = new Array();
		document.menuItems = new Array();
		
	</script>
ADCE;
return $HTML;
}
public function tpl_row($preCompiledTemplate) {
$HTML = <<<ADCE
$preCompiledTemplate
ADCE;
return $HTML;
}
public function tpl_injection($id) {
$HTML = <<<ADCE

	  <span id="menu_$id" style="visibility: hidden;"><script type="text/JavaScript">
		var oSpan = document.getElementById("menu_$id"); // dreist, aber geil, wenn auch shice - fuck off dom
		var oMenu = oSpan.parentNode;
		oMenu.menuId = $id;
		oMenu.hide = hide;
		oMenu.show = show;
		oMenu.onmouseover = menuOver;
		oMenu.onmouseout = menuOut;
		oMenu.style.position = "absolute";
		{?id}oMenu.hide();{/id}
		document.menuObjects[$id] = oMenu;
		document.menuItems[$id] = new Array();
		</script>
	  </span>
	
ADCE;
return $HTML;
}
public function tpl_foot() {
$HTML = <<<ADCE

	</div>
	<br />
ADCE;
return $HTML;
}
public function tpl_javascript_execute() {
$HTML = <<<ADCE

	<script type="text/JavaScript">
		for (var x in document.menuObjects) {
			document.iCMenu = x;
			find_menu_items(document.menuObjects[x]);
		}
			
	</script>
ADCE;
return $HTML;
}
}
?>
