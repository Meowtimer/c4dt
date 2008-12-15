<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_navigation_edit extends template {

public function tpl_head() {
$HTML = <<<ADCE
<form action="{arco.formAction}" method="post">
<table class="whiteBorderTable">
<tr>
<td>
text
</td>
<td>
link
</td>
<td>
parent
</td>
<td>
order
</td>
<td>
short
</td>
</tr>
ADCE;
return $HTML;
}

public function tpl_msg($msg) {
$HTML = <<<ADCE
<tr><td colspan="5">$msg</td></tr>
ADCE;
return $HTML;
}

public function tpl_row($id, $text,$link,$parent,$order,$short) {
$HTML = <<<ADCE
<tr>
<td>
<input type="text" size="25" name="{arco.name.text}[$id]" value="$text" />
</td>
<td>
<input type="text" size="35" name="{arco.name.link}[$id]" value="$link" />
</td>
<td>
$parent
</td>
<td>
<input type="text" size="4" name="{arco.name.order}[$id]" value="$order" />
</td>
<td>
$short
</td>
</tr>
ADCE;
return $HTML;
}

public function tpl_foot() {
$HTML = <<<ADCE
</table>
<input type="submit" value="edit" />
</form>
ADCE;
return $HTML;
}
}
?>
