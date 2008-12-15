<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class struct_standard extends structure {
public function structure($head,$foot,$left,$content,$headline) {
$HTML = <<<ADCE
$head
<div id="back">
 <div id="bottom">
  <div id="body">
  <!-- Headline Position -->
$headline
  <!-- Headline Position End -->
  <!-- Navigation Position -->
   <div id="navigation">
$left
   </div>
  <!-- Navigation Position End -->
  <!-- Content Position -->
   <div id="content">
$content
   </div>
  <!-- Content Position End -->
  </div>
 </div>
</div>
$foot
ADCE;
return $HTML;
}
}
?>
