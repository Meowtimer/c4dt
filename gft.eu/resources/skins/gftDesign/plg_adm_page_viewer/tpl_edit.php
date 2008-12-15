<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_adm_page_viewer_edit extends template {

public function tpl_head() {
$HTML = <<<ADCE
<form action="{arco.formAction}" method="post">
 <table>
  <tr>
   <td>
ADCE;
return $HTML;
}
public function tpl_form($structures, $modules, $id, $locked = false, $pageName = "", $title = "") {
$HTML = <<<ADCE
     <input type="hidden" name="{arco.name.page}" value="$id" />
     {arco.lang.name}: <input type="text" name="{arco.name.page_name}" value="$pageName" /><br />
     {arco.lang.title}: <input type="text" name="{arco.name.title}" value="$title" /><br />
     {arco.lang.structure}: <select name="{arco.name.structure}">
     $structures
     </select><br />
     {arco.lang.locked}: <input type="checkbox" name="{arco.name.locked}" value="1" {?locked}checked="checked"{/locked}/><br />
     {arco.lang.modules}: $modules <br />
     <input type="submit" name="{arco.name.submit}" value=" edit " />
ADCE;
return $HTML;
}
public function tpl_module($id, $name,$plugin,$instance,$module,$position,$position_order) {
$HTML = <<<ADCE
<a tooltip="<b>Position:</b> $position-$position_order<br /><b>Plugin:</b> $plugin<br /><b>Instance:</b> $instance<br /><b>Module:</b> $module" href="{arco.suburl.ap("submit","delete").ap("id",$id)}">$name</a>
ADCE;
return $HTML;
}
public function tpl_option_row($text,$selected = false, $value = false) {
$HTML = <<<ADCE
     <option{?value} value=""{/value}{?selected} selected{/selected}>$text</option>
ADCE;
return $HTML;
}
public function tpl_position($positionName) {
$HTML = <<<ADCE
<td><b>$positionName</b></td>
ADCE;
return $HTML;
}
public function tpl_positionSelect($positionName, $options) {
$HTML = <<<ADCE
      <td>
       <select size="5" name="{arco.name.position[$positionName]}" id="select$positionName">
        $options
       </select>
       <input type="submit" name="{arco.name.add}" posButton="true" id="button$positionName" OnClick="addTo('select$positionName');" value="$positionName" />
      </td>
ADCE;
return $HTML;
}
public function tpl_foot($plugins,$instances,$modules, $positions, $positionSelects) {
$HTML = <<<ADCE
   </td>
  </tr>
  <tr>
   <td>
    <table>
     <tr>
      <td>
       <select name="{arco.name.plugin}" size="10" id="plugins" OnClick="plugin_click();" multiline>
       </select>
      </td>
      <td>
       >>
      </td>
      <td>
       <select name="{arco.name.instance}" size="35" id="instances" OnChange="check_selection();" multiline>
       </select>
      </td>
      <td>
       >>
      </td>
      <td>
       <select name="{arco.name.module}" size="6" id="modules" OnChange="check_selection();" multiline>
       </select>
      </td>
     </tr>
    </table>
   </td>
  </tr>
  <tr>
   <td>
    <table>
     <tr>
      $positions
     </tr>
     <tr>
      $positionSelects
     </tr>
    </table>
    Modulname: <input type="text" name="{arco.name.modName}" value="$modName" />
   </td>
  </tr>
 </table>
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
     	e.value = plugins[x];
     	e.internId = x;
     	oPlugin.add(e, null);
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
     	for(var x = 0;x < instances[oPlugin.options[oPlugin.selectedIndex].internId].length;x++) {
     		var e = document.createElement("option");
     		e.text = instances[oPlugin.options[oPlugin.selectedIndex].internId][x];
     		e.value = instances[oPlugin.options[oPlugin.selectedIndex].internId][x];
     		e.internId = x;
     		oInstance.add(e, null);
     	}
     	// --------------------------------
		// Add plugin modules
		// --------------------------------
     	for(var x = 0;x < modules[oPlugin.options[oPlugin.selectedIndex].internId].length;x++) {
     		var e = document.createElement("option");
     		e.text = modules[oPlugin.options[oPlugin.selectedIndex].internId][x];
     		e.value = modules[oPlugin.options[oPlugin.selectedIndex].internId][x];
     		e.internId = x;
     		oModule.add(e, null);
     	}
     }
     
     function hide_buttons() {
     	var elements = document.getElementsByTagName("input");
     	for(var x in elements) {
     		if (elements[x].posButton == "true") {
     			elements[x].disabled = true;
     		}
     	}
     }
     function show_buttons() {
     	var elements = document.getElementsByTagName("input");
     	for(var x in elements) {
     		if (elements[x].posButton == "true") {
     			elements[x].disabled = false;
     		}
     	}
     }
     
     function addTo(selectName) {
     	oSelect = document.getElementById(selectName);
     	var e = document.createElement("option");
     	e.text = oModule.options[oModule.selectedIndex].text;
     	e.value = oPlugin.options[oPlugin.selectedIndex].text + "." + oInstance.options[oInstance.selectedIndex].text + "." + oModule.options[oModule.selectedIndex].text;
     	oSelect.add(e, null)
     }
  
     function check_selection() {
     	hide_buttons();
     	if (oPlugin.selectedIndex < 0) return false;
     	if (oInstance.selectedIndex < 0) return false;
     	if (oModule.selectedIndex < 0) return false;
     	show_buttons();
     	return true;
     }
     
     check_selection();
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
