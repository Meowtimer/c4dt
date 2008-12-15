<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_main extends template implements main_tpl {

public function tpl_head($title,$headaddition) {
$HTML = <<<ADCE
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> 
<html xml:lang="en" lang="en" xmlns="http://www.w3.org/1999/xhtml">
<head>
 <title>$title</title>
 <meta http-equiv="content-type" content="text/html;charset=utf-8" />
 <style type="text/css" media="all">
	@import url({arco.const.skins}{arco.currentSkin}/style.css);
 </style>
 $headaddition
 <script type="text/JavaScript">
 <!--
  // --------------------------------
  // Cross Browser
  // --------------------------------
  var dom = document.getElementById ? 1 : 0;
  var iex = document.createEventObject ? 1 : 0;
  var ns4 = document.layers ? 1 : 0;
  var ie6 = (document.compatMode && iex) ? 1 : 0;
  var ff = navigator.userAgent.search(/Firefox/) > -1 ? 1 : 0;
 
  // --------------------------------
  // Mouse Position Functions
  // --------------------------------
  function getMousePosY(e){
	if (iex) {
		var ev = document.createEventObject(); // natz den ie | frame independent workaround
		e = ev;
	}
	return iex ? e.clientY : e.pageY;
  }
  
  function getMousePosX(e){
	if (iex) {
		var ev = document.createEventObject(); // natz den ie | frame independent workaround
		e = ev;
	}
	return iex ? e.clientX : e.pageX;
  }
  // --------------------------------
  // Tool Tip // <* tooltip="infotext" />
  // --------------------------------
  var body;
  var toolTip;
  toolTip = document.createElement("div");
  toolTip.style.visibility = "hidden";
  toolTip.style.position = "absolute";
  //toolTip.style.height = "15px";
  toolTip.style.backgroundColor = "#CCCCCC";
  toolTip.style.color = "#000000";
  toolTip.style.padding = "0px 3px 1px 3px";
  toolTip.style.borderStyle = "solid";
  toolTip.style.borderWidth = "1px";
  toolTip.style.borderColor = "#888888"
  if (iex) toolTip.style.filter = "alpha(opacity=80)"; // ie does really suck!
  if (ff || ns4) toolTip.style.MozOpacity = 0.8; // well, Moz is not that well named, either
  toolTip.onmouseover = holdToolTip;
  toolTip.onmouseout = hideToolTip;
  
  var toolTipTimeout;
  var tipTool; // object that raised the toolTip
  
  function getOffsetLeft(obj) {
  	var left = obj.offsetLeft;
  	if (obj.offsetParent != null) left += getOffsetLeft(obj.offsetParent)
  	return left;
  }
  
  function getOffsetTop(obj) {
  	var left = obj.offsetTop;
  	if (obj.offsetParent != null) left += getOffsetTop(obj.offsetParent)
  	return left;
  }
  
  function showToolTip(e) {
  	if (toolTipTimeout) {
  		if (tipTool == this) {
  			holdToolTip(e);
  			return true;
  		}
  		else {
  			window.clearTimeout(toolTipTimeout);
  			hideToolTipNow();
  		}
  	}
  	tipTool = this;
  	//toolTip.style.top = getMousePosY(e) + "px";
  	//toolTip.style.left = getMousePosX(e) + "px";
  	toolTip.style.top = getOffsetTop(this) + this.offsetHeight + "px";
  	toolTip.style.left = getOffsetLeft(this) + this.offsetWidth + "px";
  	toolTip.style.visibility = "visible";
  	//var textNode = document.createTextNode("");
  	//if (!toolTip.hasChildNodes()) {
  	//	toolTip.appendChild(textNode);
  	//}
  	//toolTip.firstChild.data = this.toolTipText;
  	toolTip.innerHTML = this.toolTipText;
  }
  
  function holdToolTip(e) {
  	window.clearTimeout(toolTipTimeout);
  }
  
  function hideToolTip(e) {
  	if (toolTipTimeout) window.clearTimeout(toolTipTimeout);
  	toolTipTimeout = window.setTimeout("hideToolTipNow()",500);
  }
  
  function hideToolTipNow() {
  	toolTipTimeout = null;
  	toolTip.style.visibility = "hidden";
  }
  
  function searchBody(obj) {
  	var bodys = document.getElementsByTagName("body")
  	return bodys[0];
  }
  
  function searchToolTip(obj) {
 	for (var x = 0;x < obj.childNodes.length;x++) {
  		if (obj.childNodes[x].childNodes.length > 0) {
  			if (obj.childNodes[x].attributes && obj.childNodes[x].attributes.getNamedItem("tooltip") && obj.childNodes[x].attributes.getNamedItem("tooltip").nodeValue != "") {
	  			obj.childNodes[x].onmouseover = showToolTip;
	  			obj.childNodes[x].onmouseout = hideToolTip;
	  			obj.childNodes[x].toolTipText = obj.childNodes[x].attributes.getNamedItem("tooltip").nodeValue;
	  		}
			if (obj.childNodes[x].childNodes && obj.childNodes[x].childNodes.length > 0) {
	  			searchToolTip(obj.childNodes[x]);
	  		}
  		}
  	}
  }
  
  function initArco() {
    body = searchBody(document);
    body.appendChild(toolTip);
  	searchToolTip(body);
  }
 -->
 </script>
</head>
<body OnLoad="initArco();">
ADCE;
return $HTML;
}

public function tpl_foot() {
$HTML = <<<ADCE
</body>
</html>
ADCE;
return $HTML;
}

public function tpl_hyperlink($link,$text,$tooltip = false) {
$HTML = <<<ADCE
<a href="$link"{?tooltip} tooltip="$tooltip"{/tooltip}>$text</a><!--<a href="$link" tooltip="$tooltip">$text</a>-->
ADCE;
return $HTML;
}

public function tpl_hover_info($infotext) {
$HTML = <<<ADCE

ADCE;
return $HTML;	
}
}
?>