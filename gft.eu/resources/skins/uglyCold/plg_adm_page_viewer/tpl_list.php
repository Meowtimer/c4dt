<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_adm_page_viewer_list extends template {

public function tpl_head() {
$HTML = <<<ADCE
 <table>
  <tr>
   <td>
    page name
   </td>
   <td>
    structure
   </td>
   <td>
    title
   </td>
   <td>
    locked
   </td>
   <td>
    modules
   </td>
  </tr>
ADCE;
return $HTML;
}
public function tpl_row($name,$structure,$title,$locked,$modules) {
$HTML = <<<ADCE
  <tr>
    <td>
     $name
    </td>
    <td>
     $structure
    </td>
    <td>
     $title
    </td>
    <td>
     $locked
    </td>
    <td>
     $modules
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
public function tpl_install_link($link) {
$HTML = <<<ADCE
<a href="$link" tooltip="hallo">install</a>
ADCE;
return $HTML;
}
public function tpl_instance_link($link) {
$HTML = <<<ADCE
<a href="$link">create</a>
ADCE;
return $HTML;
}
}
?>
