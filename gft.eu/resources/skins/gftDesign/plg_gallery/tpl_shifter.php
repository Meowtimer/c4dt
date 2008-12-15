<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_gallery_shifter extends template {

public function tpl_head() {
$HTML = <<<ADCE
	<table>
ADCE;
return $HTML;
}
public function tpl_foot($withDia = false) {
$HTML = <<<ADCE
</table>
ADCE;
return $HTML;
}
public function tpl_row($deleteLink,$upLink,$downLink,$insertLink,$imageHTML) {
$HTML = <<<ADCE
<tr>
<td style="vertical-align: middle">
<div>$deleteLink &nbsp; $upLink &nbsp; $downLink</div>
</td>
<td>
$imageHTML
</td>
</tr>
<tr>
<td>
&nbsp;
</td>
<td>
<div style="text-align: center">$insertLink</div>
</td>
</tr>
ADCE;
return $HTML;
}
}
?>
