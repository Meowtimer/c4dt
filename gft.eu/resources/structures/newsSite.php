<?php
   /*¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯*\
   | AUTOMATIC GENERATED FILE BY ADCE SYSTEM    |
   | DO NOT EDIT IT. YOUR CHANGES WILL BE LOST. |
   \*__________________________________________*/
   
class struct_newsSite extends structure {
public function structure($head,$foot,$left,$topContent,$leftContent,$rightContent,$headline) {
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
     $topContent
     <table style="width:100%;">
      <tr>
       <td style="width:50%;">
       $leftContent
       </td>
       <td style="width:50%;">
       $rightContent
       </td>
      </tr>
     </table>
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
