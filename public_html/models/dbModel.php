<?php

	function register($company, $title, $url, $content, $cost){
		$db_type = "mysql";
		$db_server = "warehouse.cims.nyu.edu";
		$db_name = "jj1233_search_engine";
		$db_user = "jj1233";
		$db_password = "sw9kc24m";

		$db_conn = mysql_connect($db_server, $db_user, $db_password);
		$db_selected = mysql_select_db($db_name, $db_conn);

		$query = "INSERT INTO `ads_info`(company, url, title, content, cost, keywords)
			  VALUES ('".$company."', '".$url."', '".$title."', '".$content."', '".$cost."', '')";	
		$result = mysql_query($query, $db_conn);
		mysql_close($db_conn);
		return ($result==1) ? "success" : "fail";
	}

?>