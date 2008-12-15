<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_adm_page_viewer_list extends template {

public function tpl_head() {
$HTML = <<<ADCE
 <table class="whiteBorderTable">
  <tr>
   <td>
    <b>page name</b>
   </td>
   <td>
    <b>structure</b>
   </td>
   <td>
    <b>title</b>
   </td>
   <td>
    <b>locked</b>
   </td>
   <td>
    <b>modules</b>
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
public function tpl_foot($link) {
$HTML = <<<ADCE
 </table>
 <div style="text-align: center; margin-top: 20px;">$link</div>
ADCE;
return $HTML;
}
public function tpl_module($plugin,$instance,$module,$moduleName) {
$HTML = <<<ADCE
<span tooltip="<b>Plugin:</b> $plugin<br /><b>Instanz:</b> $instance<br /><b>Modul:</b> $module<br />">$moduleName</span>
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
