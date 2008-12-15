<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_adm_page_viewer_edit extends template {

public function tpl_head() {
$HTML = <<<ADCE
 <table>
  <tr>
   <td>
    <form action="{arco.formAction}" method="post">
ADCE;
return $HTML;
}
public function tpl_form($structures, $modules, $id, $locked = false, $pageName = "", $title = "") {
$HTML = <<<ADCE
     <input type="hidden" name="{arco.name.id}" value="$id" />
     {arco.lang.name}: <input type="text" name="{arco.name.page_name}" value="$pageName" /><br>
     {arco.lang.title}: <input type="text" name="{arco.name.title}" value="$title" /><br>
     {arco.lang.structure}: <select name="{arco.name.structure}">
     $structures
     </select><br>
     {arco.lang.locked}: <input type="checkbox" name="{arco.name.locked}" value="1" {?locked} checked{/locked} /><br>
     {arco.lang.modules}: $modules <br>
     <input type="submit" name="{arco.name.submit}" value=" edit " />
     </form>
ADCE;
return $HTML;
}
public function tpl_module($id, $name,$plugin,$instance,$module,$position,$position_order) {
$HTML = <<<ADCE
<a tooltip="<b>Position:</b> $position-$position_order<br><b>Plugin:</b> $plugin<br><b>Instance:</b> $instance<br><b>Module:</b> $module" href="{arco.suburl.ap("submit","delete").ap("id",$id)}">$name</a>
ADCE;
return $HTML;
}
public function tpl_option_row($text,$selected = false, $value = false) {
$HTML = <<<ADCE
     <option{?value} value=""{/value}{?selected} checked{/selected}>$text</option>
ADCE;
return $HTML;
}
public function tpl_foot($plugins,$instances,$modules) {
$HTML = <<<ADCE
   </td>
   <td>
    <table>
     <tr>
      <td>
       <select size="10" id="plugins" multiline>
       </select>
      </td>
      <td>
       >>
      </td>
      <td>
       <select size="10" id="instances">
       </select>
      </td>
      <td>
       >>
      </td>
      <td>
       <select size="10" id="modules">
       </select>
      </td>
     </tr>
    </table>
    <div id="stat">&nbsp;</div>
    <div style="visibility: hidden;" id="grab"><img src="{arco.const.skins}{arco.currentSkin}/edit.png" id="grabimg"><font id="grabtext">grab</font></div>
   </td>
  </tr>
 </table>
    <form action="{arco.formAction}" method="post" name="addmod" id="addmod">
     <input type="hidden" name="{arco.name.submit}" value="addmod">
    </form>
    <script type="text/JavaScript">
     var plugins = new Array();
     var instances = new Array();
     var modules = new Array();
$plugins
$instances
$modules
     
     var oPlugin = document.getElementById("plugins");
     var oInstance = document.getElementById("instances");
     var oModule = document.getElementById("modules");
     var oGrab = document.getElementById("grab");
     var oGrabImg = document.getElementById("grabimg");
     var oGrabText = document.getElementById("grabtext");
     var oStatus = document.getElementById("stat");
     var oChosenPosition;
     var isDragging = false;
     
     // --------------------------------
	 // Add plugins
	 // --------------------------------
     for(var x in plugins) {	
     	var e = document.createElement("option");
     	e.text = plugins[x];
     	e.value = x;
     	oPlugin.add(e, null);
     }
     
//     var zoneIdentifier = new Array();
     var zoneElements = new Array();
     var elements = document.getElementsByTagName("div");
	 var zone;
     for(var x in elements) {
     	if (!elements[x].getAttribute) continue;
     	if (elements[x].getAttribute("struct") != null) {
     		zone = elements[x].parentNode;
     		zone.className = "struct_zone";
     		zone.setAttribute("arcoposition", elements[x].getAttribute("struct"));
     		zoneElements.push(zone);
     	}
     }

     
//     for(var x in zoneElements) {
//     	zoneElements[x].absOffsetLeft = getOffsetLeft(zoneElements[x]);
//     	zoneElements[x].absOffsetTop = getOffsetTop(zoneElements[x]);
//     }
     
     function highlight_zone(obj) {
     	obj.style.backgroundColor = "yellow";
     }
     
     function unhighlight_zone(obj) {
     	obj.style.backgroundColor = "#666666";
     }
     
     function plugin_click() {
     	// --------------------------------
		// Clear instances list
		// --------------------------------
     	for(var x in oInstance.options) {
     		oInstance.remove(x);
     	}
     	// --------------------------------
		// Clear modules list
		// --------------------------------
     	for(var x in oModule.options) {
     		oModule.remove(x);
     	}
     	// --------------------------------
		// Add plugin instances
		// --------------------------------
     	for(var x = 0;x < instances[oPlugin.value].length;x++) {
     		var e = document.createElement("option");
     		e.text = instances[oPlugin.value][x];
     		e.value = x;
     		oInstance.add(e, null);
     	}
     	// --------------------------------
		// Add plugin modules
		// --------------------------------
     	for(var x = 0;x < modules[oPlugin.value].length;x++) {
     		var e = document.createElement("option");
     		e.text = modules[oPlugin.value][x];
     		e.value = x;
     		oModule.add(e, null);
     	}
     }
     
     function hideGrab() {
     	oGrab.style.visibility = "hidden";
     }
     
     function showGrab() {
     	oGrab.style.visibility = "visible";
     }
     
     function check_selection() {
     	hideGrab();
     	if (oPlugin.selectedIndex < 0) return;
     	if (oInstance.selectedIndex < 0) return;
     	if (oModule.selectedIndex < 0) return;
     	oGrabText.innerHTML = plugins[oPlugin.value] + "_" + instances[oPlugin.value][oInstance.value] + "_" + modules[oPlugin.value][oModule.value];
     	showGrab();
     }
     
     function execute_add_mod() {
     	var form = document.getElementById("addmod");
     	var plugin = document.createElement("input");
     	plugin.type = "hidden";
     	plugin.name = "{arco.name.plugin}";
     	plugin.value = plugins[oPlugin.value];
     	var instance = document.createElement("input");
     	instance.type = "hidden";
     	instance.name = "{arco.name.instance}";
     	instance.value = instances[oPlugin.value][oInstance.value];
     	var module = document.createElement("input");
     	module.type = "hidden";
     	module.name = "{arco.name.module}";
     	module.value = modules[oPlugin.value][oModule.value];
     	var pos = document.createElement("input");
     	pos.type = "hidden";
     	pos.name = "{arco.name.pos}";
     	pos.value = oChosenPosition.getAttribute("arcoposition");
     	var modName = document.createElement("input");
     	modName.type = "hidden";
     	modName.name = "{arco.name.modName}";
     	modName.value = prompt("Bezeichnung des neuen Modulfeldes?","");
     	var pageId = document.createElement("input");
     	pageId.type = "hidden";
     	pageId.name = "{arco.name.pageId}";
     	pageId.value = document.getElementsByName("{arco.name.id}")[0].value;
     	form.appendChild(plugin);
     	form.appendChild(instance);
     	form.appendChild(module);
     	form.appendChild(pos);
     	form.appendChild(modName);
     	form.appendChild(pageId);
     	form.submit();
     }
     
     function start_drag(e) {
     	if (isDragging) {
     		stop_drag(e);
     		return;
     	}
     	isDragging = true;
     	document.onmousemove = drag;
     	oStatus.innerHTML = "start drag";
     }
     
     function stop_drag(e) {
     	document.onmousemove = null;
     	isDragging = false;
     	//alert(oChosenPosition.getAttribute("arcoposition"));
     	oStatus.innerHTML = "stopped drag";
     	if (oChosenPosition) execute_add_mod();
     	return;
     }
     
     function drag(e) {
     	if (!isDragging) return false;
     	oStatus.innerHTML = "dragging...";
     	oGrab.style.position = "absolute";
     	var posY = getMousePosY(e);
     	var posX = getMousePosX(e);
     	oGrab.style.top = posY - 5 + "px";
     	oGrab.style.left = posX - 5 + "px";
     	for(var x in zoneElements) {
     		zoneElements[x].absOffsetLeft = getOffsetLeft(zoneElements[x]);
     		zoneElements[x].absOffsetTop = getOffsetTop(zoneElements[x]);
     		if ((posY > zoneElements[x].absOffsetTop) && (posY < zoneElements[x].absOffsetTop + zoneElements[x].offsetHeight)) {
     			if ((posX > zoneElements[x].absOffsetLeft) && (posX < zoneElements[x].absOffsetLeft + zoneElements[x].offsetWidth)) {
     				highlight_zone(zoneElements[x]);
     				oChosenPosition = zoneElements[x];
     			}
     			else {
     				unhighlight_zone(zoneElements[x]);
     				if (oChosenPosition == zoneElements[x]) {
     					oChosenPosition = null;
     				}
     			}
     		}
     		else {
     			unhighlight_zone(zoneElements[x]);
     			if (oChosenPosition == zoneElements[x]) {
     				oChosenPosition = null;
     			}
     		}
     	}
     }
     
     function highlight_grabber() {
     	oGrabImg.style.borderStyle = "solid";
     	oGrabImg.style.borderWidth = "1px 1px 1px 1px";
     	oGrabImg.style.borderColor = "blue";
     }
     
     function unhighlight_grabber() {
     	oGrabImg.style.borderStyle = "none";
     }
     
     oGrabImg.onmouseover = highlight_grabber;
     oGrabImg.onmouseout = unhighlight_grabber;
     oGrabImg.onclick = start_drag;
     oPlugin.onchange = check_selection;
     oInstance.onchange = check_selection;
     oModule.onchange = check_selection;
     oPlugin.onclick = plugin_click;
    </script>
ADCE;
return $HTML;
}
public function tpl_plugin_row($pluginId, $pluginName) {
$HTML = <<<ADCE
     plugins[$pluginId] = "$pluginName";
ADCE;
return $HTML;
}
public function tpl_new_instance($pluginId) {
$HTML = <<<ADCE
     instances[$pluginId] = new Array();
ADCE;
return $HTML;
}
public function tpl_new_module($pluginId) {
$HTML = <<<ADCE
     modules[$pluginId] = new Array();
ADCE;
return $HTML;
}
public function tpl_instance_row($pluginId, $instanceId, $instanceName) {
$HTML = <<<ADCE
     instances[$pluginId][$instanceId] = "$instanceName";
ADCE;
return $HTML;
}
public function tpl_module_row($pluginId, $moduleId, $moduleName) {
$HTML = <<<ADCE
     modules[$pluginId][$moduleId] = "$moduleName";
ADCE;
return $HTML;
}
}
?>
