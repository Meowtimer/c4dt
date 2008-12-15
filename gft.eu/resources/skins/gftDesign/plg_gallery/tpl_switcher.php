<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class tpl_gallery_switcher extends template {

public function tpl_head() {
$HTML = <<<ADCE
	<table>
	 <tr>
	 <td style="width: 10%;">
	 </td>
	 <td style="width: 80%;">
	 </td>
	 <td style="width: 10%;">
	 </td>
	 </tr>
ADCE;
return $HTML;
}
public function tpl_foot($withDia = false) {
$HTML = <<<ADCE
</table>
{?withDia}
<script type="text/javascript">
window.setTimeout("diashowNext()",3000);

function diashowNext() { 
	window.location = "$withDia";
}

</script>{/withDia}
ADCE;
return $HTML;
}
public function tpl_row($prev,$next,$imageHTML,$maxWidth,$maxHeight) {
$HTML = <<<ADCE
<tr>
<td style="vertical-align: middle; font-size: large;">
&nbsp;{?prev}<a style="text-decoration: none;" href="$prev">&lt;&lt;</a>{/prev}&nbsp;
</td>
<td style="height: {$maxHeight}px; width: {$maxWidth}px">
$imageHTML
</td>
<td style="vertical-align: middle; font-size: large; font-weight: bold;">
&nbsp;{?next}<a style="text-decoration: none;" href="$next">&gt;&gt;</a>{/next}&nbsp;
</td>
</tr>
ADCE;
return $HTML;
}
}
?>
