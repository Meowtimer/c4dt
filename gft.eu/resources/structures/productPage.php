<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class struct_productPage extends structure {
public function structure($head,$foot,$left,$topContent,$middleContent,$bottomContent,$headline) {
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
  <!-- Content Positions -->
   <div id="content">
    <div style="margin-bottom: 20px;">
     $topContent
    </div>
    <div style="margin-bottom: 20px;">
     $middleContent
    </div>
    <div style="margin-bottom: 20px;">
     $bottomContent
    </div>
   </div>
  <!-- Content Positions End -->
  </div>
 </div>
</div>
$foot
ADCE;
return $HTML;
}
}
?>
