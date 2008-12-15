<?php
   /*������������������������������������������*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_navigation_menu extends template {

public function tpl_noscript() {
$HTML = <<<ADCE
<noscript>
<div><div style="position: absolute;"><div><div><a href="arco.php?page=home"><img alt="" src="resources/skins/gftDesign/images/{arco.currentLang}/navigation/about.jpg"></a></div></div><div><div><a href="arco.php?page=news"><img alt="" src="resources/skins/gftDesign/images/{arco.currentLang}/navigation/news.jpg"></a></div></div><div><div><a href="arco.php?page=products"><img alt="" src="resources/skins/gftDesign/images/{arco.currentLang}/navigation/products.jpg"></a></div></div><div><div><a href="arco.php?page=references"><img alt="" src="resources/skins/gftDesign/images/{arco.currentLang}/navigation/references.jpg"></a></div></div><div><div><a href="arco.php?page=service"><img alt="" src="resources/skins/gftDesign/images/{arco.currentLang}/navigation/service.jpg"></a></div></div><div><div><a href="arco.php?page=rental"><img alt="" src="resources/skins/gftDesign/images/{arco.currentLang}/navigation/renting.jpg"></a></div></div><div><div><a href="arco.php?page=training"><img alt="" src="resources/skins/gftDesign/images/{arco.currentLang}/navigation/training.jpg"></a></div></div><div><div><a href="arco.php?page=contact"><img alt="" src="resources/skins/gftDesign/images/{arco.currentLang}/navigation/contact.jpg"></a></div></div><div><div><a href="arco.php?page=links"><img alt="" src="resources/skins/gftDesign/images/{arco.currentLang}/navigation/links.jpg"></a></div></div></div></div>
</noscript>
ADCE;
return $HTML;
}

public function tpl_head() {
$HTML = <<<ADCE
<!--<div id="menuAnchor" />-->
<script type="text/javascript">
  /* <![CDATA[ */
	var mainMenu = new ArcoMenu(0, null, 0);
	with(mainMenu) {

ADCE;
return $HTML;
}

public function tpl_js_head_menu($id,$depth) {
$HTML = <<<ADCE
		childItems[childItems.length-1].subMenu = new ArcoMenu($id, childItems[childItems.length-1], $depth);
		with(childItems[childItems.length-1].subMenu) {
		//childItems[$id].subMenu = new ArcoMenu($id, childItems[$id], $depth);
		//with(childItems[$id].subMenu) {

ADCE;
return $HTML;
}

public function tpl_js_foot_menu() {
$HTML = <<<ADCE

		}

ADCE;
return $HTML;
}

public function tpl_js_row($id,$itemId,$itemPic,$itemLink, $depth) {
$HTML = <<<ADCE
		childItems.push(new ArcoItem($itemId,"$itemPic","$itemLink", parentItem ? parentItem.subMenu : mainMenu));
		//childItems[$itemId] = new ArcoItem($itemId,"$itemPic","$itemLink", parentItem ? parentItem.subMenu : mainMenu);

ADCE;
return $HTML;
}

public function tpl_foot() {
$HTML = <<<ADCE

	}
	var menuAnchor = document.getElementById("navigation");
	if (!menuAnchor) alert("Anchor not found");
	
	menuAnchor.appendChild(mainMenu.generateObject(0));

	for (var child in mainMenu.childItems) {
		mainMenu.childItems[child].positionLayer();
	}
	/* ]]> */
</script>
ADCE;
return $HTML;
}


public function tpl_section($name) {
$HTML = <<<ADCE
<!-- Section: $name -->
ADCE;
return $HTML;
}

public function tpl_head_first($injection) {
$HTML = <<<ADCE
 <!-- head first -->
	<div style="position: absolute;">
	$injection

ADCE;
return $HTML;
}

public function tpl_foot_first() {
$HTML = <<<ADCE
 <!--  foot first -->
	</div>

ADCE;
return $HTML;
}

public function tpl_head_second($injection) {
$HTML = <<<ADCE

	<div>
	$injection
	  <div style="position: absolute; left: 93px; top: 0px;"><img src="resources/skins/gftDesign/images/design/subnavi_fade1.png" /></div>
	  <div style="position: absolute; left: 132px; top: 0px;">
	    <div><img src="{arco.const.skins}{arco.currentSkin}/images/design/subnavi_fade2.png"/></div>		<!-- top box height: 47px; -->
	    <div style="padding-top: 7px;  background: url({arco.const.skins}{arco.currentSkin}/images/design/subnavi_fade_stretch.png) repeat-y; width: 136px;">						<!-- stretching div width:| 22px | 110px |;-->
		  <div style="margin-left: 22px; background-color: #DEDEDE;">
		  	<div style="margin-left: -22px; margin-top: -54px;">

ADCE;
return $HTML;
}

public function tpl_foot_second() {
$HTML = <<<ADCE

	  		</div>
		  </div>
	    </div>
	  </div>
	</div>

ADCE;
return $HTML;
}

public function tpl_head_old($injection) {
$HTML = <<<ADCE

	  <div style="position: absolute; left: 132px; top: 0px;">
		  <div style="margin-left: 1px; background-color: #DEDEDE; margin-top: 0px;">
	$injection

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

	  <span id="menu_$id" style="visibility: hidden;">
	     <script type="text/JavaScript">
		    register_element($id);
		 </script>
	  </span>

ADCE;
return $HTML;
}
public function tpl_foot_old() {
$HTML = <<<ADCE

	    </div>
	</div>

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
