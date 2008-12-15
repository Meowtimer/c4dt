<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_content_edit extends template {

public function tpl_form($tpl,$rows) {
$HTML = <<<ADCE
	<script type="text/JavaScript">
		var oForm;
		function add_field() {
			if (!oForm) {
				var forms = document.getElementsByTagName("form");
				for(var x in forms) {
					if (forms[x].name == "edit_content") {
						oForm = forms[x];
						break;
					}
				}
			}
			var oFieldName = document.createElement("input");
			oFieldName.type = "text";
			oFieldName.name = "{arco.name.new_field_name}[]";
			oForm.appendChild(oFieldName);
			var oFieldContent = document.createElement("textarea");
			oFieldContent.rows = 10;
			oFieldContent.cols = 50;
			oFieldContent.name = "{arco.name.new_field_content}[]";
			oForm.appendChild(oFieldContent);
			var br = document.createElement("br");
			oForm.appendChild(br);
		}
	</script>
	<form action="{arco.formAction}" name="edit_content" method="post">
	<textarea name="{arco.name.main_tpl}" rows="10" cols="50">$tpl</textarea><br>
	<input type="submit" name="{arco.name.submit}" value="edit" /><br>
	$rows
	</form>
	<a href="javascript:add_field();">{arco.lang.new_field}</a>
	
	
ADCE;
return $HTML;
}
public function tpl_row($name,$content) {
$HTML = <<<ADCE
<input type="text" name="{arco.name.field_name}[]" value="$name"/><textarea name="{arco.name.field_content}[]" rows="10" cols="50">$content</textarea><br>
ADCE;
return $HTML;
}
}
?>
