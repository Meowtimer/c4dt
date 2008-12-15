<?php
   /*������������������������������������������*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_adm_dbcopy_show extends template {

public function tpl_content($product,$development,$copypdlink,$copydplink) {
$HTML = <<<ADCE
<div><a href="$copypdlink">Copy</a> product($product) database to development($development) database</div>
<div><a href="$copydplink">Copy</a> development($development) database to product($product) database</div>
ADCE;
return $HTML;
}
}
?>