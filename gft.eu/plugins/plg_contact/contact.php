<?php
/*
 * Created on 16.07.2007
 *
 */
 
 class contact extends plugin {
 	public $plugin_modules = array("form");
 }
 
 class contact_form extends module {
 	public function init() {
 		
 	}
 	
 	public function make_html() {
 		$this->init();
 		// --------------------------------
		// We really like that
		// --------------------------------
 		$tpl = arco::$view->get_templates_for($this);
 		$input = arco::$engine->get_input_for($this);
 		$lang = arco::$lang->get_lang_for($this);
 		
 		$template = $tpl->head();
 		
 		if ($_POST) {
	 		if ($input->name && $input->email && $input->message) {
	 			if (strstr($input->subject,"\n")) throw new SecurityException("Illegal operation.\nuser:".print_r($_SERVER,true)."\n$input->subject");
	 			if (strstr($input->subject,"\r")) throw new SecurityException("Illegal operation.\nuser:".print_r($_SERVER,true)."\n$input->subject");
	 			if (!preg_match('/^[\p{L} :;,	.&?!~+*()\/€$§<>-]*$/',$input->subject)) {
	 				$subject = "[gefilterter Betreff]";
	 			}
	 			else {
	 				$subject = $input->subject;
	 			}
	 			if (strstr($input->name,"\n")) throw new SecurityException("Illegal operation.");
	 			if (strstr($input->email,"\n")) throw new SecurityException("Illegal operation.");
	 			if (strstr($input->email,"\r")) throw new SecurityException("Illegal operation.");
	 			
	 			$message = "Neue Kontaktformular E-Mail:\n";
	 			$message .= "Name: ".$input->name."\n";
	 			$message .= "Original-Betreff: ".$input->subject."\n";
	 			$message .= "E-Mail: ".$input->email."\n";
	 			$message .= "Firma: ".$input->company."\n";
	 			$message .= "Addresse: ".$input->address."\n";
	 			$message .= "Telefon: ".$input->phone."\n";
	 			$message .= "\n\n------------------------------\n\n";
	 			$message .= $input->message;
	 			
	 			$message = preg_replace('/(content-type:|bcc:|cc:|to:|from:)/i',"",$message);
	 			
	 			$additionalHeader = "From: GFT Website Kontaktformular <noreply@gft.eu>\nContent-Type: text/plain; charset=\"UTF-8\"\n";
	 			
	 			mail("info@general-firetech.de","GFT WEB-MAIL: ".$subject,$message,$additionalHeader);
	 			
	 			$template .= $lang->success;
	 			return $template;
	 		}
	 		else {
	 			$template .= arco::$view->main->errorBox($lang->error);
	 		}
 		}
 		$template .= $tpl->form($input->name,$input->email,$input->company,$input->phone,$input->address,$input->subject,$input->message);
 		return $template;
 	}
 }
 
?>
