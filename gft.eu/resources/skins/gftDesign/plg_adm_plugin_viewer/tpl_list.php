<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_adm_plugin_viewer_list extends template {

public function tpl_head() {
$HTML = <<<ADCE
    <table class="whiteBorderTable">
     <tr>
      <td>
       <b>plugin name</b>
      </td>
      <td>
       <b>installed</b>
      </td>
      <td>
       <b>instances</b>
      </td>
     </tr>

ADCE;
return $HTML;
}
public function tpl_row($name,$installed,$instances) {
$HTML = <<<ADCE
     <tr>
      <td>
       $name
      </td>
      <td>
       $installed
      </td>
      <td>
       $instances
      </td>
     </tr>

ADCE;
return $HTML;
}
public function tpl_foot() {
$HTML = <<<ADCE
    </table>
    <a href="?page=adminnavi">Navigation Administration</a>
ADCE;
return $HTML;
}
}
?>
