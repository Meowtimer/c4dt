<?php
   /*ŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻ*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_adm_plugin_viewer_create_instance extends template {

public function tpl_form($pluginName) {
$HTML = <<<ADCE
	<form action="{arco.formAction}" method="post">
		<input type="hidden" name="{arco.name.node}" value="make" />	
		<input type="hidden" name="{arco.name.pluginName}" value="$pluginName" />
		Instancename: <input type="text" name="{arco.name.instanceName}" />
		<input type="submit" value=" make " />
	</form>
ADCE;
return $HTML;
}
}
?>
