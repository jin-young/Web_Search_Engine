<?php

	function register($company, $title, $url, $content, $cost){
		include_once '../dbConn.php';

		$sql = "INSERT INTO `ads_info`(company, url, title, content, cost, keywords)
			  VALUES ('".$company."', '".$url."', '".$title."', '".$content."', '".$cost."', '')";	
		$result = mysql_query($sql, $conn);
		mysql_close($conn);
		return ($result==1) ? "success" : "fail";
	}

?>