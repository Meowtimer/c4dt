<?php
   /*������������������������������������������*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_contact_form extends template {

public function tpl_head() {
$HTML = <<<ADCE
	<div style="margin-top: 20px; margin-bottom: 10px;"><b>{arco.lang.form}</b></div>
ADCE;
return $HTML;
}

public function tpl_form($name,$email,$company,$phone,$address,$subject,$message) {
$HTML = <<<ADCE
	<form action="{arco.formAction}" method="post">
		<div>{arco.lang.company}:</div>
		<div><input type="text" size="40" name="{arco.name.company}" value="$company"/></div>
		<div>{arco.lang.name}*:</div>
		<div><input type="text" size="40" name="{arco.name.name}" value="$name"/></div>
		<div>{arco.lang.address}:</div>
		<div><textarea cols="40" rows="3" name="{arco.name.address}">$address</textarea></div>
		<div>{arco.lang.phone}:</div>
		<div><input type="text" size="40" name="{arco.name.phone}" value="$phone"/></div>
		<div>{arco.lang.email}*:</div>
		<div><input type="text" size="40" name="{arco.name.email}" value="$email"/></div>
		<div>{arco.lang.subject}:</div>
		<div><input type="text" size="40" name="{arco.name.subject}" value="$subject"/></div>
		<div>{arco.lang.message}*:</div>
		<div><textarea name="{arco.name.message}" rows="20" cols="80">$message</textarea></div>
		<div><input type="submit" name="{arco.name.submit}" value="{arco.lang.submit}"/></div>
	</form>
ADCE;
return $HTML;
}
}
?>
