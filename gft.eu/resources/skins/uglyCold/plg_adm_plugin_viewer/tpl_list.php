<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_adm_plugin_viewer_list extends template {

public function tpl_head() {
$HTML = <<<ADCE
    <table>
     <tr>
      <td>
       plugin name
      </td>
      <td>
       installed
      </td>
      <td>
       instances
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
ADCE;
return $HTML;
}
}
?>
