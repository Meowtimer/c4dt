<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_adm_page_viewer_create extends template {

public function tpl_head() {
$HTML = <<<ADCE
<form action="{arco.formAction}" method="post">
 <table style="width: 567px;">
  <tr>
   <td>
ADCE;
return $HTML;
}
public function tpl_form($structures, $name, $title, $locked = false) {
$HTML = <<<ADCE
     {arco.lang.name}: <input type="text" name="{arco.name.name}" value="" /><br />
     {arco.lang.title}: <input type="text" name="{arco.name.title}" value="" /><br />
     {arco.lang.structure}: <select name="{arco.name.structure}">
     $structures
     </select><br />
     {arco.lang.locked}: <input type="checkbox" name="{arco.name.locked}" value="1" {?locked}ckecked{/locked}/><br />
     <input type="submit" name="{arco.name.submit}" value="add" />
ADCE;
return $HTML;
}
public function tpl_option_row($text,$selected = false, $value = false) {
$HTML = <<<ADCE
     <option{?value} value=""{/value}{?selected} checked{/selected}>$text</option>
ADCE;
return $HTML;
}
public function tpl_foot() {
$HTML = <<<ADCE
   </td>
  </tr>
 </table>
</form>
ADCE;
return $HTML;
}
}
?>
